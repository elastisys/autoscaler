package com.elastisys.autoscaler.core.monitoring.metricstreamer.commons;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.slf4j.Logger;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.metronome.api.MetronomeEvent;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStreamMessage;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryOptions;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.QueryResultSet;
import com.elastisys.autoscaler.core.utils.stats.timeseries.TimeSeriesPredicates;
import com.elastisys.scale.commons.eventbus.EventBus;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.util.time.UtcTime;

/**
 * A class that can be used to implement a basic metric streaming loop for a
 * given set of {@link MetricStream}s.
 * <p/>
 * This class is implemented as a {@link Runnable} task that, when executed,
 * fetches new {@link MetricValue}s for the set of registered
 * {@link MetricStream}s and, if new values are available, pushes those values
 * onto an {@link EventBus} (as {@link MetricStreamMessage}s) for consumption by
 * interested listeners.
 * <p/>
 * The {@link MetricStreamingLoop} takes care of delivering streamed values in
 * monotonically increasing time-stamp order to consumers. That is, any already
 * observed values or "late arrivals" that would be delivered out of order are
 * suppressed from delivery.
 *
 * @see MetricStreamDriver
 */
public class MetricStreamingLoop implements Runnable {
    /**
     * The default first lookback to use for the first query to each
     * {@link MetricStream}.
     */
    public final static TimeInterval DEFAULT_FIRST_QUERY_LOOKBACK = new TimeInterval(5L, TimeUnit.MINUTES);

    private final Logger logger;
    /** Task execution service for performing work in separate threads. */
    private final ExecutorService executor;
    /** The {@link EventBus} onto which collected metric values are sent. */
    private final EventBus eventBus;

    /**
     * The collection of {@link MetricStream}s for which metrics are collected
     * and published.
     */
    private final List<MetricStream> metricStreams;

    /**
     * How far back in time to look on the first query to a
     * {@link MetricStream}. May be <code>null</code>, in which case
     * {@value #DEFAULT_FIRST_QUERY_LOOKBACK} is used.
     */
    private final TimeInterval firstQueryLookback;

    /**
     * Tracks the time-stamp of the most recent {@link MetricValue} published
     * for each {@link MetricStream}. This time-stamp becomes the start time of
     * the next query to the stream.
     */
    private final Map<MetricStream, DateTime> lastObservations;
    /** Contains fault details if the latest resize iteration failed. */
    private Optional<Throwable> lastFailure = Optional.empty();

    /**
     * {@link Lock} used to prevent a new streaming loop from starting when one
     * is already in progress.
     */
    private final Lock loopLock = new ReentrantLock();

    /**
     * Creates a new {@link MetricStreamingLoop} with default
     * {@code firstQueryLookback}.
     *
     * @param logger
     *            The logger to use.
     * @param executor
     *            Task execution service for performing work in separate
     *            threads.
     * @param eventBus
     *            The {@link EventBus} onto which collected metric values are
     *            published.
     * @param metricStreams
     *            The collection of {@link MetricStream}s for which metrics are
     *            to be collected and published.
     */
    public MetricStreamingLoop(Logger logger, ExecutorService executor, EventBus eventBus,
            List<MetricStream> metricStreams) {
        this(logger, executor, eventBus, metricStreams, null);
    }

    /**
     * Creates a new {@link MetricStreamingLoop}.
     *
     * @param logger
     *            The logger to use.
     * @param executor
     *            Task execution service for performing work in separate
     *            threads.
     * @param eventBus
     *            The {@link EventBus} onto which collected metric values are
     *            published.
     * @param metricStreams
     *            The collection of {@link MetricStream}s for which metrics are
     *            to be collected and published.
     * @param firstQueryLookback
     *            How far back in time to look on the first query to a
     *            {@link MetricStream}. May be <code>null</code>, in which case
     *            {@value #DEFAULT_FIRST_QUERY_LOOKBACK} is used.
     */
    public MetricStreamingLoop(Logger logger, ExecutorService executor, EventBus eventBus,
            List<MetricStream> metricStreams, TimeInterval firstQueryLookback) {
        this.logger = logger;
        this.executor = executor;
        this.eventBus = eventBus;

        this.metricStreams = new ArrayList<>(metricStreams);
        this.firstQueryLookback = Optional.ofNullable(firstQueryLookback).orElse(DEFAULT_FIRST_QUERY_LOOKBACK);

        this.lastObservations = new HashMap<>();
    }

    @Override
    public void run() {
        // prevent new streaming loop when one is already in progress
        if (this.loopLock.tryLock()) {
            try {
                fetchAndStreamMetricValues();
            } catch (Throwable e) {
                this.logger.error("metric streaming loop failed: " + e.getMessage(), e);
                this.lastFailure = Optional.of(e);
            } finally {
                this.loopLock.unlock();
            }
        } else {
            this.logger.warn("Ignoring attempt to start another metric fetch while previous is still in progress");
        }
    }

    /**
     * Returns the {@link MetricStream}s that this {@link MetricStreamingLoop}
     * has been set up to use.
     *
     * @return
     */
    public List<MetricStream> getMetricStreams() {
        return this.metricStreams;
    }

    /**
     * Retrieves and delivers new metric values for each of the registered
     * {@link MetricStream}s. Queries are carried out in parallel and values are
     * streamed back in order of increasing time stamp. Any "late arrivals" that
     * would be delivered out of order for the stream are silently dropped.
     *
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private void fetchAndStreamMetricValues() throws InterruptedException, ExecutionException {
        Map<MetricStream, Future<List<MetricValue>>> metricRetrievals = startMetricRetrievals();
        // publish fetched values
        handleMetricRetrievals(metricRetrievals);
    }

    /**
     * Waits for completion of a bunch of metric value retrievals, and pushes
     * any new values onto the {@link EventBus}.
     *
     * @param metricRetrievals
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private void handleMetricRetrievals(Map<MetricStream, Future<List<MetricValue>>> metricRetrievals)
            throws InterruptedException, ExecutionException {
        this.lastFailure = Optional.empty();
        // total number of new metric values delivered to streams
        int numDeliveredValues = 0;
        for (MetricStream stream : metricRetrievals.keySet()) {
            try {
                List<MetricValue> metricValues = new ArrayList<>(metricRetrievals.get(stream).get());
                int delivered = deliver(stream, metricValues);
                numDeliveredValues += delivered;
            } catch (Throwable e) {
                this.lastFailure = Optional.of(e);
                this.logger.error(
                        format("failed to deliver values for metric stream '%s': %s", stream.getId(), e.getMessage()),
                        e);
            }
        }

        if (numDeliveredValues > 0) {
            this.logger.debug("delivered a total of {} new values", numDeliveredValues);
            // trigger a new resize iteration
            this.eventBus.post(MetronomeEvent.RESIZE_ITERATION);
        }
    }

    /**
     * Delivers any new metric values fetched for a certain {@link MetricStream}
     * to the {@link EventBus}. Returns the number of new values that were
     * delivered. Values are delivered in monotonically increasing time-stamp
     * order.
     * <p/>
     * Furthermore, already observed values or "late arrivals" that would be
     * delivered out of order are filtered out. That is, values older than last
     * observed value are filtered out.
     *
     * @param stream
     *            The {@link MetricStream} to stream values for.
     * @param metricValues
     *            The latest collection of {@link MetricValue}s collected for
     *            the given {@link MetricStream}.
     * @return The number of new metric values that were delivered.
     */
    private int deliver(MetricStream stream, List<MetricValue> metricValues) {
        Collections.sort(metricValues);
        List<MetricValue> newMetricValues = getNewMetricValues(metricValues, stream);
        this.logger.debug(format("%s: %d out of %d metric values newer than %s to deliver", stream.getId(),
                newMetricValues.size(), metricValues.size(), getLastObservation(stream).get(), stream.getMetric()));
        if (newMetricValues.isEmpty()) {
            return 0;
        }

        this.eventBus.post(new MetricStreamMessage(stream.getId(), newMetricValues));
        DateTime lastObservation = newMetricValues.get(newMetricValues.size() - 1).getTime();
        setLastObservation(stream, lastObservation);
        return newMetricValues.size();
    }

    /**
     * Starts a collection of asynchronous metric retrieval tasks, each of which
     * will retrieve metric values for one of the metric streams.
     *
     * @return
     */
    private Map<MetricStream, Future<List<MetricValue>>> startMetricRetrievals() {
        Map<MetricStream, Future<List<MetricValue>>> metricRetrievals = new HashMap<>();
        // For each metric stream, get the values that have been reported
        // since the last fetch. Do this in separate threads for increased
        // concurrency.
        for (MetricStream stream : this.metricStreams) {
            Interval interval = getNextQueryInterval(stream);
            Future<List<MetricValue>> metricRetrieval = startMetricRetrieval(stream, interval);
            metricRetrievals.put(stream, metricRetrieval);
        }
        return metricRetrievals;
    }

    /**
     * Returns the next query interval for a given metric stream. That is, a
     * query interval ranging from the last observed {@link MetricValue} in the
     * metric stream to the present time.
     *
     * @param stream
     * @return
     */
    private Interval getNextQueryInterval(MetricStream stream) {
        if (!getLastObservation(stream).isPresent()) {
            // first query to metric stream: determine initial query interval
            setLastObservation(stream, getInitialQueryStart());
        }

        return new Interval(getLastObservation(stream).get(), UtcTime.now());
    }

    /**
     * Starts an asynchronous task that will fetch metric values for a given
     * {@link MetricStream} over a given time interval.
     *
     * @param stream
     *            The {@link MetricStream} for which to retrieve
     *            {@link MetricValue}s.
     * @param interval
     *            The time interval to retrieve {@link MetricValue}s for.
     * @return A handle to the started computation.
     */
    private Future<List<MetricValue>> startMetricRetrieval(final MetricStream stream, final Interval interval) {
        this.logger.debug("querying stream {} for interval {} ...", stream.getId(), interval);
        Callable<List<MetricValue>> fetchTask = new FetchTask(stream, interval, new QueryOptions());
        Future<List<MetricValue>> metricRetrieval = this.executor.submit(fetchTask);
        return metricRetrieval;
    }

    /**
     * From a list of {@link MetricValue}s, filters out any {@link MetricValue}s
     * that would be delivered out of order for a given {@link MetricStream}.
     * That is, all {@link MetricValue}s with a time-stamp older than the last
     * observed for the stream.
     *
     * @param possiblyOldMetricValues
     * @param stream
     * @return
     */
    private List<MetricValue> getNewMetricValues(List<MetricValue> possiblyOldMetricValues, MetricStream stream) {
        List<MetricValue> newMetricValues = possiblyOldMetricValues;
        if (getLastObservation(stream).isPresent()) {
            DateTime lastObservationTime = getLastObservation(stream).get();
            newMetricValues = possiblyOldMetricValues.stream()
                    .filter(TimeSeriesPredicates.youngerThan(lastObservationTime)).collect(Collectors.toList());
        }
        return newMetricValues;
    }

    /**
     * @return The last exception that occurred, if any.
     */
    public Optional<Throwable> getLastFailure() {
        return this.lastFailure;
    }

    public Logger getLogger() {
        return this.logger;
    }

    ExecutorService getExecutorService() {
        return this.executor;
    }

    /**
     * Sets the time stamp for the last {@link MetricValue} observed for a given
     * {@link MetricStream}. This time-stamp becomes the start time of the next
     * query to the stream.
     *
     * @param stream
     * @param lastObservation
     */
    private void setLastObservation(MetricStream stream, DateTime lastObservation) {
        this.lastObservations.put(stream, lastObservation);
    }

    /**
     * Returns the time stamp of the last {@link MetricValue} observed for a
     * given {@link MetricStream}.
     *
     * @param stream
     * @return
     */
    private Optional<DateTime> getLastObservation(MetricStream stream) {
        return Optional.ofNullable(this.lastObservations.get(stream));
    }

    /**
     * Determines the start time to use for the first query interval to a
     * {@link MetricStream} based on the amount of lookback requested.
     *
     * @return
     */
    private DateTime getInitialQueryStart() {
        return UtcTime.now().minus(this.firstQueryLookback.getMillis());
    }

    /**
     * A callable task that runs a single query against a given
     * {@link MetricStream}.
     */
    private static class FetchTask implements Callable<List<MetricValue>> {
        private final MetricStream stream;
        private final Interval interval;
        private final QueryOptions options;

        public FetchTask(MetricStream stream, Interval interval, QueryOptions options) {
            this.stream = stream;
            this.interval = interval;
            this.options = options;
        }

        @Override
        public List<MetricValue> call() throws Exception {
            List<MetricValue> results = new ArrayList<>();
            QueryResultSet resultSet = this.stream.query(this.interval, this.options);
            while (resultSet.hasNext()) {
                results.addAll(resultSet.fetchNext().getMetricValues());
            }
            return results;
        }
    }
}
