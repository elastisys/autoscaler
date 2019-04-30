package com.elastisys.autoscaler.metricstreamers.cloudwatch.config;

import java.util.regex.Pattern;

/**
 * A validator that checks if a given namespace is a valid CloudWatch metric
 * namespace.
 * <p/>
 * Refer to the <a href=
 * "http://docs.aws.amazon.com/AmazonCloudWatch/latest/DeveloperGuide/cloudwatch_concepts.html"
 * >CloudWatch concepts introduction</a> for details.
 * 
 * 
 */
public class NamespaceValidator {
    /** Pattern describing allowed metric namespaces. */
    private static final Pattern ALLOWED_NAMESPACE_PATTERN = Pattern.compile("[0-9A-Za-z_/\\.\\-#:]+");

    /**
     * Returns <code>true</code> if the specified namespace is a valid
     * CloudWatch metric namespace.
     * <p/>
     * Refer to the <a href=
     * "http://docs.aws.amazon.com/AmazonCloudWatch/latest/DeveloperGuide/cloudwatch_concepts.html"
     * >CloudWatch concepts introduction</a> for details.
     * 
     * @param namespace
     *            A namespace.
     * @return <code>true</code> if the namespace is valid, <code>false</code>
     *         otherwise.
     */
    public static boolean isValid(String namespace) {
        if (namespace.length() > 255) {
            return false;
        }
        return ALLOWED_NAMESPACE_PATTERN.matcher(namespace).matches();
    }
}
