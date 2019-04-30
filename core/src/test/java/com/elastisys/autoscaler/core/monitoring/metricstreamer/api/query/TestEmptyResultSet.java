package com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.NoSuchElementException;

import org.junit.Test;

import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.query.impl.EmptyResultSet;

/**
 * Exercise {@link EmptyResultSet}.
 */
public class TestEmptyResultSet {

    @Test
    public void test() {
        EmptyResultSet empty = new EmptyResultSet();
        assertFalse(empty.hasNext());

        try {
            empty.fetchNext();
            fail("should not be possible to get next element");
        } catch (NoSuchElementException e) {
            // expected
        }
    }

}
