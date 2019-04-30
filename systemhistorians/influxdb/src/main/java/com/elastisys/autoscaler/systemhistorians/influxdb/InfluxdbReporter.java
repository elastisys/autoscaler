package com.elastisys.autoscaler.systemhistorians.influxdb;

import static java.lang.String.format;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.systemhistorians.influxdb.inserter.InfluxdbInserter;

/**
 * A task intended for periodical execution that, when executed, pops
 * {@link MetricValue}s from a send queue and reports them to an InfluxDB
 * server. The send queue is filled by the {@link InfluxdbSystemHistorian} on a
 * different thread.
 *
 */
class InfluxdbReporter implements Runnable {

    /** A logger instance. */
    private final Logger logger;
    /**
     * Queue of data points which are to be reported to InfluxDB. The
     * {@link InfluxdbReporter} will pop values from this queue as they are
     * reported to InfluxDB. So {@link InfluxdbReporter} acts as the consumer of
     * the queue, while the queue is assumed to be filled up by the client of
     * this class.
     * <p/>
     * Soft references are used to prevent the queue from growing beyond the
     * JVM's memory allocation. They may be cleared at the discretion of the
     * JVM's garbage collector in response to low memory.
     */
    private final Queue<SoftReference<MetricValue>> sendQueue;

    /** Holds the latest observed error (if any). */
    private Optional<? extends Throwable> lastError;

    /** InfluxDB write client. */
    private final InfluxdbInserter inserter;
    /**
     * The maximum number of datapoints to send in a single call to InfluxDB. As
     * noted in the <a href=
     * "https://docs.influxdata.com/influxdb/v1.0/guides/writing_data/">InfluxDB
     * docs</a/> it may be necessary to split datapoints into smaller batches
     * once they exceed a few thousand points to avoid request time outs.
     */
    private final int maxBatchSize;

    /** Lock to prevent concurrent writes. */
    private final Lock lock = new ReentrantLock();

    /**
     * Creates a new {@link InfluxdbReporter}.
     *
     * @param logger
     *            A logger instance onto which the {@link InfluxdbReporter} will
     *            log its activity.
     * @param inserter
     *            InfluxDB write client.
     * @param sendQueue
     *            Queue of data points which are to be reported to InfluxDB. The
     *            {@link InfluxdbReporter} will pop values from this queue as
     *            they are reported to InfluxDB. So {@link InfluxdbReporter}
     *            acts as the consumer of the queue, while the queue is assumed
     *            to be filled up by the client of this class.
     *            <p/>
     *            Soft references are used to prevent the queue from growing
     *            beyond the JVM's memory allocation. They may be cleared at the
     *            discretion of the JVM's garbage collector in response to low
     *            memory.
     * @param maxBatchSize
     *            The maximum number of datapoints to send in a single call to
     *            InfluxDB. As noted in the <a href=
     *            "https://docs.influxdata.com/influxdb/v1.0/guides/writing_data/">InfluxDB
     *            docs</a/> it may be necessary to split datapoints into smaller
     *            batches once they exceed a few thousand points to avoid
     *            request time outs.
     */
    public InfluxdbReporter(Logger logger, InfluxdbInserter inserter, Queue<SoftReference<MetricValue>> sendQueue,
            int maxBatchSize) {
        this.logger = logger;
        this.inserter = inserter;
        this.sendQueue = sendQueue;
        this.maxBatchSize = maxBatchSize;

        this.lastError = Optional.empty();
    }

    @Override
    public void run() {
        this.lastError = Optional.empty();

        try {
            this.lock.lock();

            // defensive copy of datapoints, since it can change at any time.
            List<SoftReference<MetricValue>> references = new ArrayList<>(this.sendQueue);
            this.logger.debug("influxdb historian: {} datapoint(s) in send queue", references.size());

            // send data points in batches of maxBatchSize
            for (int i = 0; i < references.size(); i += this.maxBatchSize) {
                int remaining = references.size() - i;
                int batchSize = Math.min(this.maxBatchSize, remaining);
                List<SoftReference<MetricValue>> batch = references.subList(i, i + batchSize);
                sendBatch(batch);
                this.sendQueue.removeAll(batch);
            }
        } catch (Exception e) {
            this.logger.error("failed to write data points to InfluxDB: {}", e.getMessage(), e);
            this.lastError = Optional.of(e);
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * Sends a batch of {@link MetricValue}s to the InfluxDB server.
     *
     * @param batch
     */
    private void sendBatch(List<SoftReference<MetricValue>> batch) {
        // filter out any soft references that have been garbage collected
        // (indicated by the soft reference's referent being null
        List<MetricValue> datapoints = batch.stream().filter(ref -> ref.get() != null).map(SoftReference::get)
                .collect(Collectors.toList());

        if (datapoints.isEmpty()) {
            return; // nothing to do
        }

        this.logger.debug(format("influxdb historian: sending batch of size %d data points ...", datapoints.size()));
        this.inserter.insert(datapoints);
    }

    /**
     *
     * @return The error of the previous execution, if any.
     */
    public Optional<? extends Throwable> getLastError() {
        return this.lastError;
    }

}
