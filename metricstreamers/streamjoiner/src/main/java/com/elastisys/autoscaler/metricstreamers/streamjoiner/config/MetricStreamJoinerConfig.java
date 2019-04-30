package com.elastisys.autoscaler.metricstreamers.streamjoiner.config;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.elastisys.autoscaler.core.monitoring.metricstreamer.api.MetricStream;
import com.elastisys.autoscaler.metricstreamers.streamjoiner.MetricStreamJoiner;

/**
 * A configuration for a {@link MetricStreamJoiner}, declaring its joined
 * {@link MetricStream}s.
 *
 * @see MetricStreamJoiner
 */
public class MetricStreamJoinerConfig {

    /**
     * Declares the {@link MetricStream}s that the {@link MetricStreamJoiner}
     * publishes.
     */
    private final List<MetricStreamDefinition> metricStreams;

    /**
     * Creates a {@link MetricStreamJoinerConfig}.
     *
     * @param metricStreams
     *            Declares the {@link MetricStream}s that the
     *            {@link MetricStreamJoiner} publishes.
     */
    public MetricStreamJoinerConfig(List<MetricStreamDefinition> metricStreams) {
        this.metricStreams = metricStreams;
    }

    /**
     * The {@link MetricStream} declarations of the {@link MetricStreamJoiner}.
     *
     * @return
     */
    public List<MetricStreamDefinition> getMetricStreams() {
        return Optional.ofNullable(this.metricStreams).orElse(Collections.emptyList());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.metricStreams);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MetricStreamJoinerConfig) {
            MetricStreamJoinerConfig that = (MetricStreamJoinerConfig) obj;
            return Objects.equals(this.metricStreams, that.metricStreams);
        }
        return false;
    }

    public void validate() throws IllegalArgumentException {
        try {
            getMetricStreams().forEach(s -> {
                try {
                    s.validate();
                } catch (IllegalArgumentException e) {
                    if (s.getId() != null) {
                        // make error message a bit more clear if an id is
                        // available
                        throw new IllegalArgumentException(s.getId() + ": " + e.getMessage(), e);
                    }
                    throw e;
                }
            });

            ensureUniqueStreamIds();
        } catch (Exception e) {
            throw new IllegalArgumentException("MetricStreamJoiner config: " + e.getMessage(), e);
        }
    }

    private void ensureUniqueStreamIds() throws IllegalArgumentException {
        Set<String> uniqueIds = new HashSet<>();
        getMetricStreams().forEach(s -> {
            if (uniqueIds.contains(s.getId())) {
                throw new IllegalArgumentException(String.format("duplicate metricStream id: %s", s.getId()));
            }
            uniqueIds.add(s.getId());
        });
    }
}
