package com.elastisys.autoscaler.metricstreamers.opentsdb.parser;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.elastisys.autoscaler.core.api.types.MetricValue;

/**
 * Parses {@link MetricValue}s in OpenTSDB's ASCII format into
 * {@link MetricValue} instances.
 * <p/>
 * Refer to
 * <a href="http://opentsdb.net/http-api.html#/q_Output_formats">OpenTSDB output
 * formats</a> for more details.
 */
public class OpenTsdbMetricValueParser {

    /*
     * The Pattern class is thread-safe according to the documentation.
     * (Matcher, however, is not -- but we don't use it that way, either.)
     */
    private final static Pattern tagPattern = Pattern.compile("(\\S+)=(\\S+)");
    private static final long MILLISECONDS_PER_SECOND = 1000;

    /**
     * Parses a single line of OpenTSDB ASCII format into a {@link MetricValue}.
     *
     * @param line
     *            The line containing the OpenTSDB ASCII representation of a
     *            metric value.
     * @return The parsed {@link MetricValue}.
     */
    public static MetricValue parseMetricValue(String line) {
        Objects.requireNonNull(line, "Line cannot be null");

        final String[] fragments = line.split(" ");
        checkArgument(fragments.length >= 3, "Too few fragments to parse to a metric value");

        final String metric = fragments[0];
        final DateTime timestamp = new DateTime(Long.parseLong(fragments[1]) * MILLISECONDS_PER_SECOND,
                DateTimeZone.UTC);
        final double value = Double.parseDouble(fragments[2]);
        final Map<String, String> tags = new HashMap<>();

        for (int i = 3; i < fragments.length; i++) {
            Matcher matcher = tagPattern.matcher(fragments[i]);
            if (matcher.matches()) {
                final String tagKey = matcher.group(1);
                final String tagValue = matcher.group(2);

                if (tags.containsKey(tagKey)) {
                    throw new IllegalArgumentException("Repeated definition of key " + tagKey);
                }

                tags.put(tagKey, tagValue);
            } else {
                throw new IllegalArgumentException("Could not parse tag from fragment " + fragments[i]);
            }
        }

        return new MetricValue(metric, value, timestamp, tags);
    }

}
