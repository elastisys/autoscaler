package com.elastisys.autoscaler.metricstreamers.cloudwatch.config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Verifies the correctness of the {@link NamespaceValidator}.
 */
public class TestNamespaceValidator {

    @Test
    public void onValidNamespaces() {
        assertTrue(NamespaceValidator.isValid("AWS/EC2"));
        assertTrue(NamespaceValidator.isValid("MyNamespace"));
        assertTrue(NamespaceValidator.isValid("1"));
        assertTrue(NamespaceValidator.isValid("1/2"));
        assertTrue(NamespaceValidator.isValid("MyNamespace:subnamespace"));
        assertTrue(NamespaceValidator.isValid("My/Name_space:ns#1"));
    }

    @Test
    public void onInvalidNamespaces() {
        assertFalse(NamespaceValidator.isValid("AWS/EC2*"));
        assertFalse(NamespaceValidator.isValid("$AWS/EC2"));
        assertFalse(NamespaceValidator.isValid("AWS/{EC2}"));
        assertFalse(NamespaceValidator.isValid("AWS/EC2?"));
    }

    @Test
    public void onTooLongNamespace() {
        assertTrue(NamespaceValidator.isValid(repeat('A', 255)));
        assertFalse(NamespaceValidator.isValid(repeat('A', 256)));
    }

    private static String repeat(char chr, int num) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < num; i++) {
            buf.append(chr);
        }
        return buf.toString();
    }

}
