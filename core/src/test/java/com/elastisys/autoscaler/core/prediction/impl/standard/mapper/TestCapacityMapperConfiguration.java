package com.elastisys.autoscaler.core.prediction.impl.standard.mapper;

import static com.elastisys.autoscaler.core.prediction.impl.standard.mapper.CapacityMapperTestUtils.mapping;
import static com.elastisys.autoscaler.core.prediction.impl.standard.mapper.CapacityMapperTestUtils.mappings;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.autoscaler.core.prediction.impl.standard.config.CapacityMappingConfig;
import com.elastisys.autoscaler.core.prediction.impl.standard.mapper.CapacityMapper;

/**
 * Tests the configuration of the {@link CapacityMapper}.
 */
public class TestCapacityMapperConfiguration {
    static Logger logger = LoggerFactory.getLogger(TestCapacityMapperConfiguration.class);

    /** Object under test. */
    private CapacityMapper capacityMapper;

    @Before
    public void onSetup() {
        this.capacityMapper = new CapacityMapper(logger);
    }

    @Test(expected = IllegalArgumentException.class)
    public void configureWithNull() {
        this.capacityMapper.validate(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void configureWithNullMetric() {
        this.capacityMapper.validate(mappings(mapping(null, 1.0)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void configureWithNegativeMetricCapacity() {
        this.capacityMapper.validate(mappings(mapping("metric1", -1.0)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void configureWithZeroMetricCapacity() {
        this.capacityMapper.validate(mappings(mapping("metric1", 0.0)));
    }

    @Test
    public void configureSingleMapping() {
        List<CapacityMappingConfig> config = mappings(mapping("metric1", 1.0));
        this.capacityMapper.validate(config);
        this.capacityMapper.configure(config);

        assertThat(this.capacityMapper.getConfiguration(), is(config));
    }

    @Test
    public void configureMultipleMappings() {
        List<CapacityMappingConfig> config = mappings(mapping("metric1", 1.0), mapping("metric2", 2.0),
                mapping("metric3", 3.0));
        this.capacityMapper.validate(config);
        this.capacityMapper.configure(config);

        assertThat(this.capacityMapper.getConfiguration(), is(config));
    }

    @Test
    public void reconfigureMappings() {
        List<CapacityMappingConfig> config = mappings(mapping("metric1", 1.0));
        this.capacityMapper.validate(config);
        this.capacityMapper.configure(config);
        assertThat(this.capacityMapper.getConfiguration(), is(config));

        List<CapacityMappingConfig> newConfig = mappings(mapping("metric1", 1.0), mapping("metric2", 2.0));
        this.capacityMapper.validate(newConfig);
        this.capacityMapper.configure(newConfig);

    }
}
