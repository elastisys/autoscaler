package com.elastisys.autoscaler.systemhistorians.influxdb.serializer;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.elastisys.autoscaler.core.api.types.MetricValue;

/**
 * A {@link Function} that serializes {@link MetricValue}s to the line protocol
 * representation required when submitting data points to InfluxDB. See the
 * <a href=
 * "https://docs.influxdata.com/influxdb/v1.0/guides/writing_data/">InfluxDB
 * documentation</a> for details.
 */
public class InfluxdbDataPointSerializer implements Function<MetricValue, String> {

    /**
     * Serializes a given {@link MetricValue} to the line protocol format
     * prescribed by InfluxDB.
     * <p/>
     * {@code <measurement>,<tag1>=<value1>,<tag2>=<value2> <fieldKey>=<value> <timestamp>}
     *
     * @param value
     *            a {@link MetricValue}
     * @return The line protocol representation of the {@link MetricValue}.
     */
    @Override
    public String apply(MetricValue value) {
        StringWriter writer = new StringWriter();
        // <measurement>
        writer.append(value.getMetric());

        // ,<tag1>=<value1>,<tag2>=<value2>
        if (value.getTags() != null && !value.getTags().isEmpty()) {
            writer.append(",");

            writer.append(String.join(",", tagValueList(value)));
        }

        // <fieldKey>=<value>
        writer.append(String.format(" %s=%s", "value", value.getValue()));

        // <timestamp>
        // the timestamp needs to be in nanoseconds
        writer.append(String.format(" %d000000", value.getTime().getMillis()));
        return writer.toString();
    }

    private List<String> tagValueList(MetricValue value) {
        List<String> tagValuePairs = new ArrayList<>();
        for (String tagKey : value.getTags().keySet()) {
            tagValuePairs.add(tagKey + "=" + value.getTags().get(tagKey));
        }
        return tagValuePairs;
    }

}
