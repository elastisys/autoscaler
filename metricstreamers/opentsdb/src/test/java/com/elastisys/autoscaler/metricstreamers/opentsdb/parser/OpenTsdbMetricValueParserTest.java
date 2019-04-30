package com.elastisys.autoscaler.metricstreamers.opentsdb.parser;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.metricstreamers.opentsdb.parser.OpenTsdbMetricValueParser;

/**
 * Verifies through some simple tests that the {@link OpenTsdbMetricValueParser}
 * works as expected for correct input and throws exceptions for incorrect
 * input.
 */
public class OpenTsdbMetricValueParserTest {

    /**
     * Tests that the valid string
     * <code>"proc.stat.cpu 1297574486 54.2 host=foo type=user"</code> (taken
     * from the <a href="http://opentsdb.net/http-api.html">OpenTSDB HTTP
     * API</a> page) is parsed correctly.
     */
    @Test
    public void parseValidMetricValue() {
        // proc.stat.cpu 1297574486 54.2 host=foo type=user
        Map<String, String> correctTags = new HashMap<>();
        correctTags.put("host", "foo");
        correctTags.put("type", "user");

        final MetricValue expectedMetricValue = new MetricValue("proc.stat.cpu", 54.2,
                new DateTime(1297574486000l, DateTimeZone.UTC), correctTags);

        final MetricValue actualMetricValue = OpenTsdbMetricValueParser
                .parseMetricValue("proc.stat.cpu 1297574486 54.2 host=foo type=user");

        assertEquals(expectedMetricValue, actualMetricValue);
    }

    /**
     * Test should fail due to nulled string.
     */
    @Test(expected = NullPointerException.class)
    public void parseInvalidNullString() {
        OpenTsdbMetricValueParser.parseMetricValue(null);
    }

    /**
     * Test should fail due to empty string.
     */
    @Test(expected = IllegalArgumentException.class)
    public void parseInvalidEmptyString() {
        OpenTsdbMetricValueParser.parseMetricValue("");
    }

    /**
     * Test should fail due to missing timestamp and value.
     */
    @Test(expected = IllegalArgumentException.class)
    public void parseInvalidMetricValueOnlyKey() {
        OpenTsdbMetricValueParser.parseMetricValue("invaild.foo");
    }

    /**
     * Test should fail due to missing value.
     */
    @Test(expected = IllegalArgumentException.class)
    public void parseInvalidMetricValueOnlyKeyAndTimestamp() {
        OpenTsdbMetricValueParser.parseMetricValue("invaild.foo 12345");
    }

    /**
     * Test should succeed, all required parts are present.
     */
    @Test
    public void parseValidMetricValueOnlyKeyTimestampAndValue() {
        OpenTsdbMetricValueParser.parseMetricValue("invaild.foo 12345 1.2");
    }

    /**
     * Test should fail since tag key does not have associated value.
     */
    @Test(expected = IllegalArgumentException.class)
    public void parseInvalidMetricValueUnparseableTagWithEquals() {
        OpenTsdbMetricValueParser.parseMetricValue("invaild.foo 12345 1.2 foo=");
    }

    /**
     * Test should fail due to prematurely ended input (tag key not followed by
     * equals sign and value).
     */
    @Test(expected = IllegalArgumentException.class)
    public void parseInvalidMetricValueUnparseableTagWithoutEquals() {
        OpenTsdbMetricValueParser.parseMetricValue("invaild.foo 12345 1.2 foo");
    }

    /**
     * Test should fail since second tag key does not have associated value.
     */
    @Test(expected = IllegalArgumentException.class)
    public void parseInvalidMetricValueUnparseableTags() {
        OpenTsdbMetricValueParser.parseMetricValue("invaild.foo 12345 1.2 foo=a f=");
    }

    /**
     * Test should fail due to repeated specification of value for a single tag
     * key.
     */
    @Test(expected = IllegalArgumentException.class)
    public void parseInvalidMetricValueRepeatedTagKey() {
        OpenTsdbMetricValueParser.parseMetricValue("invaild.foo 12345 1.2 foo=a bar=b foo=c");
    }

}
