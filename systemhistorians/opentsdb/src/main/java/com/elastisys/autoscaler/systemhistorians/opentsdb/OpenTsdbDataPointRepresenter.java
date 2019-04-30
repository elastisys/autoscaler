package com.elastisys.autoscaler.systemhistorians.opentsdb;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.concurrent.TimeUnit;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.scale.commons.util.precond.Preconditions;

/**
 * Represents a given data point in the format required by the OpenTSDB's telnet
 * protocol. See <a href="http://opentsdb.net/metrics.html">OpenTSDB's metrics
 * discussion</a>.
 *
 */
public class OpenTsdbDataPointRepresenter {
    private static final String NON_WHITESPACE_REGEX = "\\S+";

    /**
     * Represents a given data point in the format required by the OpenTSDB
     * Telnet protocol. See
     * <a href="http://opentsdb.net/overview.html">OpenTSDB's overview page</a>.
     *
     * @param dataPoint
     *            The data point.
     * @return An OpenTSDB Telnet protocol representation of the data point and
     *         any associated tags and their values.
     */
    public static String representDataPoint(MetricValue dataPoint) {
        requireNonNull(dataPoint, "Data point cannot be null");
        checkArgument(!dataPoint.getTags().isEmpty(), "data point needs at least one tag");

        StringBuilder sb = new StringBuilder();
        sb.append(dataPoint.getMetric());
        sb.append(' ').append(dataPoint.getTime().getMillis() / TimeUnit.SECONDS.toMillis(1));
        sb.append(' ').append(dataPoint.getValue());
        for (String tagKey : dataPoint.getTags().keySet()) {
            checkArgument(tagKey.matches(NON_WHITESPACE_REGEX), "tag key must be a simple alphanumeric string, not %s",
                    tagKey);
            final String tagValue = dataPoint.getTags().get(tagKey);
            Preconditions.checkArgument(tagKey.matches(NON_WHITESPACE_REGEX),
                    "tag value must be a simple alphanumeric string, not {}", tagValue);
            sb.append(' ').append(tagKey).append('=').append(tagValue);
        }
        return sb.toString();
    }
}
