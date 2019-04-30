package com.elastisys.autoscaler.metricstreamers.ceilometer.stream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import org.joda.time.Interval;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.telemetry.Sample;
import org.openstack4j.model.telemetry.SampleCriteria;
import org.openstack4j.model.telemetry.SampleCriteria.Oper;
import org.openstack4j.model.telemetry.Statistics;
import org.slf4j.Logger;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.monitoring.metricstreamer.commons.RateConverter;
import com.elastisys.autoscaler.metricstreamers.ceilometer.config.CeilometerFunction;
import com.elastisys.autoscaler.metricstreamers.ceilometer.config.CeilometerMetricStreamDefinition;
import com.elastisys.autoscaler.metricstreamers.ceilometer.config.Downsampling;
import com.elastisys.autoscaler.metricstreamers.ceilometer.converters.SampleValueConverter;
import com.elastisys.autoscaler.metricstreamers.ceilometer.converters.StatisticValueConverter;
import com.elastisys.scale.commons.openstack.OSClientFactory;

public class QueryCall implements Callable<List<MetricValue>> {

    private final Logger logger;
    /** Factory for creating authenticated OpenStack API clients. */
    private final OSClientFactory clientFactory;
    private final CeilometerMetricStreamDefinition stream;
    private final Interval interval;

    public QueryCall(Logger logger, OSClientFactory clientFactory, CeilometerMetricStreamDefinition stream,
            Interval queryInterval) {
        this.logger = logger;
        this.clientFactory = clientFactory;
        this.stream = stream;
        this.interval = queryInterval;
    }

    @Override
    public List<MetricValue> call() throws Exception {
        List<MetricValue> metricValues;

        OSClient<?> client = this.clientFactory.authenticatedClient();

        if (this.stream.getDownsampling().isPresent()) {
            // fetch aggregated statistics for the time period
            metricValues = fetchStatistics(client);
        } else {
            // fetch samples (raw datapoints)
            metricValues = fetchSamples(client);
        }
        Collections.sort(metricValues);

        this.logger.debug("retrieved {} values from Ceilometer for stream {}, metric {}, period {}: {}",
                metricValues.size(), this.stream.getId(), this.stream.getMeter(), this.interval, metricValues);

        // (optionally) apply rate conversion
        if (this.stream.isConvertToRate()) {
            if (metricValues.size() < 2) {
                this.logger.warn("not enough metric values to calculate rate. will wait for more values.");
                return Collections.emptyList();
            }
            metricValues = new RateConverter().apply(metricValues);
            this.logger.debug("stream {} values after rate conversion: {}", this.stream.getId(), metricValues);
        }

        return metricValues;
    }

    /**
     * Fetches samples (raw datapoints) over the given query interval for the
     * configured meter.
     * <p/>
     * This produces a query similar to the following query run with the
     * {@code ceilometer} command-line tool:
     *
     * <pre>
     * ceilometer sample-list --meter network.services.lb.total.connections.rate --query 'timestamp>2017-01-27T00:00:00.000Z;timestamp<=2017-01-27T08:00:00.000Z'
     * </pre>
     *
     * @param client
     *
     * @return
     */
    private List<MetricValue> fetchSamples(OSClient<?> client) {
        SampleCriteria criteria = SampleCriteria.create();
        withinInterval(criteria, this.interval);
        criteria.add("meter", Oper.EQUALS, this.stream.getMeter());
        if (this.stream.getResourceId().isPresent()) {
            criteria.resource(this.stream.getResourceId().get());
        }

        List<? extends Sample> samples = client.telemetry().samples().list(criteria);

        List<MetricValue> values = new ArrayList<>(samples.size());
        SampleValueConverter converter = new SampleValueConverter(this.stream.getMeter());
        for (Sample sample : samples) {
            values.add(converter.apply(sample));
        }

        return values;
    }

    /**
     * Fetches downsampled statistics over the given query interval for the for
     * the configured meter using the set downsampling function and period.
     * <p/>
     * This produces a query similar to the following query run with the
     * {@code ceilometer} command-line tool:
     *
     * <pre>
     * ceilometer statistics --meter network.services.lb.total.connections.rate --aggregate avg --period 3600 --query 'timestamp>2017-01-27T00:00:00.000Z;timestamp<=2017-01-27T08:00:00.000Z'
     * </pre>
     *
     * @param client
     *
     * @return
     */
    private List<MetricValue> fetchStatistics(OSClient<?> client) {
        SampleCriteria criteria = new SampleCriteria();
        withinInterval(criteria, this.interval);

        Downsampling downsampling = this.stream.getDownsampling().get();
        CeilometerFunction aggregationFunction = downsampling.getFunction();
        int downsamplingPeriod = (int) downsampling.getPeriod().getSeconds();

        List<? extends Statistics> statistics = client.telemetry().meters().statistics(this.stream.getMeter(), criteria,
                downsamplingPeriod);

        StatisticValueConverter converter = new StatisticValueConverter(this.stream.getMeter(), aggregationFunction);
        List<MetricValue> values = new ArrayList<>(statistics.size());
        for (Statistics stats : statistics) {
            this.logger.debug("got statistic: {}", stats);
            values.add(converter.apply(stats));
        }
        return values;
    }

    private void withinInterval(SampleCriteria criteria, Interval interval) {
        criteria.timestamp(Oper.GTE, interval.getStartMillis());
        criteria.timestamp(Oper.LTE, interval.getEndMillis());
    }

}
