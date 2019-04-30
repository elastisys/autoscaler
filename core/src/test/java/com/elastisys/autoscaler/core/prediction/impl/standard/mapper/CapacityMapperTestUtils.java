package com.elastisys.autoscaler.core.prediction.impl.standard.mapper;

import java.util.Arrays;
import java.util.List;

import com.elastisys.autoscaler.core.prediction.impl.standard.config.CapacityMappingConfig;

public class CapacityMapperTestUtils {

    public static CapacityMappingConfig mapping(String metric, double amountPerComputeUnit) {
        return new CapacityMappingConfig(metric, amountPerComputeUnit);
    }

    public static List<CapacityMappingConfig> mappings(CapacityMappingConfig... mappings) {
        return Arrays.asList(mappings);
    }

}
