package com.elastisys.autoscaler.metricstreamers.ceilometer.converters;

import static com.elastisys.autoscaler.metricstreamers.ceilometer.converters.ConverterTestUtils.interval;
import static com.elastisys.autoscaler.metricstreamers.ceilometer.converters.ConverterTestUtils.time;
import static com.elastisys.autoscaler.metricstreamers.ceilometer.converters.ConverterTestUtils.value;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.junit.Test;
import org.openstack4j.model.telemetry.Statistics;
import org.openstack4j.openstack.telemetry.domain.CeilometerStatistics;

import com.elastisys.autoscaler.metricstreamers.ceilometer.config.CeilometerFunction;
import com.elastisys.autoscaler.metricstreamers.ceilometer.converters.StatisticValueConverter;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Exercises the {@link StatisticValueConverter} class.
 */
public class TestStatisticValueConverter {

    @Test(expected = NullPointerException.class)
    public void applyOnNull() {
        StatisticValueConverter converter = new StatisticValueConverter("meter", CeilometerFunction.Average);
        converter.apply(null);
    }

    @Test
    public void applyOnSingleValue() {
        StatisticValueConverter converter = new StatisticValueConverter("meter", CeilometerFunction.Average);
        Interval interval = interval(0, 1);
        Statistics stats = new MockedStatistics(1.0, interval, CeilometerFunction.Average);

        assertThat(converter.apply(stats), is(value("meter", time(0), 1.0)));
    }

    @Test
    public void fromDocumentationPage() throws IOException {

        CeilometerStatistics parsed = parse("examples/statistics.json");

        final StatisticValueConverter avg = new StatisticValueConverter("foo", CeilometerFunction.Average);
        assertThat(avg.apply(parsed).getValue(), is(4.5));

        final StatisticValueConverter sampleCount = new StatisticValueConverter("foo", CeilometerFunction.SampleCount);
        assertThat(sampleCount.apply(parsed).getValue(), is(10.0));

        final StatisticValueConverter sum = new StatisticValueConverter("foo", CeilometerFunction.Sum);
        assertThat(sum.apply(parsed).getValue(), is(45.0));

        final StatisticValueConverter max = new StatisticValueConverter("foo", CeilometerFunction.Maximum);
        assertThat(max.apply(parsed).getValue(), is(9.0));

        final StatisticValueConverter min = new StatisticValueConverter("foo", CeilometerFunction.Minimum);
        assertThat(min.apply(parsed).getValue(), is(1.0));

        // period start time becomes the timestamp of the aggregated value
        assertThat(min.apply(parsed).getTime(), is(new DateTime("2013-01-04T16:00:00", DateTimeZone.UTC)));
    }

    private CeilometerStatistics parse(String resourceFile) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(new File(getClass().getClassLoader().getResource(resourceFile).getFile()),
                CeilometerStatistics.class);
    }

    /**
     * Constructs a Ceilometer {@link Statistics}.
     *
     * @param statistic
     *            The statistic for which the value is provided.
     * @param value
     *            The value.
     * @param interval
     *            The interval during which the statistics were collected.
     * @return
     */
    public static Statistics statistics(CeilometerFunction statistic, double value, Interval interval) {

        return new MockedStatistics(value, interval, statistic);
    }

    /**
     * Constructs a list of {@link Statistics} objects.
     *
     * @param statistics
     *            A vararg of {@link Statistics}.
     * @return A {@link List} of {@link Statistics} (actually backed by
     *         {@link ArrayList}).
     */
    public static List<Statistics> statistics(Statistics... statistics) {
        return Arrays.asList(statistics);
    }

    /**
     * Mock implementation of the {@link Statistics} interface, able to only
     * answer queries about a given {@link CeilometerFunction}.
     */
    static class MockedStatistics implements Statistics {
        private static final long serialVersionUID = 1L;

        private final Double value;
        private final Interval interval;
        private final CeilometerFunction statistic;

        public MockedStatistics(Double value, Interval interval, CeilometerFunction statistic) {
            this.value = value;
            this.interval = interval;
            this.statistic = statistic;
        }

        @Override
        public Double getAvg() {
            if (this.statistic.equals(CeilometerFunction.Average)) {
                return this.value;
            } else {
                throw new RuntimeException("MockedStatistic not prepared " + "for dealing with avg");
            }
        }

        @Override
        public Integer getCount() {
            if (this.statistic.equals(CeilometerFunction.SampleCount)) {
                return this.value.intValue();
            } else {
                throw new RuntimeException("MockedStatistic not prepared " + "for dealing with count");
            }
        }

        @Override
        public Double getDuration() {
            return new Long(this.interval.getEndMillis() - this.interval.getStartMillis()).doubleValue();
        }

        @Override
        public Date getDurationEnd() {
            return this.interval.getEnd().toDate();
        }

        @Override
        public Date getDurationStart() {
            return this.interval.getStart().toDate();
        }

        @Override
        public String getGroupBy() {
            throw new NullPointerException("group by not supported by " + "this mock");
        }

        @Override
        public Double getMax() {
            if (this.statistic.equals(CeilometerFunction.Maximum)) {
                return this.value;
            } else {
                throw new RuntimeException("MockedStatistic not prepared " + "for dealing with max");
            }
        }

        @Override
        public Double getMin() {
            if (this.statistic.equals(CeilometerFunction.Minimum)) {
                return this.value;
            } else {
                throw new RuntimeException("MockedStatistic not prepared " + "for dealing with min");
            }
        }

        @Override
        public Integer getPeriod() {
            return getDuration().intValue();
        }

        @Override
        public Date getPeriodEnd() {
            return this.interval.getEnd().toDate();
        }

        @Override
        public Date getPeriodStart() {
            return this.interval.getStart().toDate();
        }

        @Override
        public Double getSum() {
            if (this.statistic.equals(CeilometerFunction.Sum)) {
                return this.value;
            } else {
                throw new RuntimeException("MockedStatistic not prepared " + "for dealing with sum");
            }
        }

        @Override
        public String getUnit() {
            throw new NullPointerException("group by not supported by this mock");
        }

    }
}
