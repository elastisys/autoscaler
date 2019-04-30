package com.elastisys.autoscaler.metricstreamers.cloudwatch.stream;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.elastisys.autoscaler.metricstreamers.cloudwatch.stream.DownsamplingIntervalToCloudWatchPeriod;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * Exercise {@link DownsamplingIntervalToCloudWatchPeriod}.
 */
public class TestDownsamplingIntervalToCloudWatchPeriod {

    /** Object under test. */
    private DownsamplingIntervalToCloudWatchPeriod converter;

    @Before
    public void beforeTestMethod() {
        this.converter = new DownsamplingIntervalToCloudWatchPeriod();
    }

    /**
     * Even minutes are left unchanged when converting to CloudWatch
     * downsampling period.
     */
    @Test
    public void convertEvenMinutes() {
        assertThat(convert(new TimeInterval(0L, TimeUnit.MINUTES)), is(TimeInterval.seconds(0)));
        assertThat(convert(new TimeInterval(1L, TimeUnit.MINUTES)), is(TimeInterval.seconds(60)));
        assertThat(convert(new TimeInterval(2L, TimeUnit.MINUTES)), is(TimeInterval.seconds(120)));
        assertThat(convert(new TimeInterval(3L, TimeUnit.MINUTES)), is(TimeInterval.seconds(180)));

        assertThat(convert(new TimeInterval(60L, TimeUnit.SECONDS)), is(TimeInterval.seconds(60)));
        assertThat(convert(new TimeInterval(120L, TimeUnit.SECONDS)), is(TimeInterval.seconds(120)));

    }

    /**
     * A CloudWatch downsampling period must be a multiple of 60 seconds.
     * Therefore, intervals that aren't even minutes are rounded up to the
     * closest minute.
     */
    @Test
    public void roundUpToNearestMinute() {
        assertThat(convert(new TimeInterval(10L, TimeUnit.SECONDS)), is(TimeInterval.seconds(60)));
        assertThat(convert(new TimeInterval(30L, TimeUnit.SECONDS)), is(TimeInterval.seconds(60)));
        assertThat(convert(new TimeInterval(59L, TimeUnit.SECONDS)), is(TimeInterval.seconds(60)));
        assertThat(convert(new TimeInterval(90L, TimeUnit.SECONDS)), is(TimeInterval.seconds(120)));
        assertThat(convert(new TimeInterval(179L, TimeUnit.SECONDS)), is(TimeInterval.seconds(180)));
    }

    public TimeInterval convert(TimeInterval downsamplingInterval) {
        return this.converter.apply(downsamplingInterval);
    }
}
