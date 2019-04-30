package com.elastisys.autoscaler.core.prediction.impl.standard.mapper;

import static com.elastisys.autoscaler.core.prediction.impl.standard.mapper.CapacityMapperTestUtils.mapping;
import static com.elastisys.autoscaler.core.prediction.impl.standard.mapper.CapacityMapperTestUtils.mappings;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.elastisys.autoscaler.core.prediction.impl.standard.api.CapacityTranslationTable;
import com.elastisys.scale.commons.util.collection.Maps;

/**
 * Verifies the behavior of the {@link StandardCapacityTranslationTable} class.
 */
public class TestStandardCapacityTranslationTable {

    @Test
    public void constructEmpty() {
        CapacityTranslationTable emptyTable = new StandardCapacityTranslationTable();
        assertThat(emptyTable.metrics(), is(set()));
        assertThat(emptyTable.containsMapping("metric"), is(false));
    }

    @Test
    public void constructWithEmptyMappings() {
        CapacityTranslationTable emptyTable = new StandardCapacityTranslationTable(mappings());
        assertThat(emptyTable.metrics(), is(set()));
        assertThat(emptyTable.containsMapping("metric"), is(false));
    }

    @Test
    public void constructWithSingleMetric() {
        CapacityTranslationTable table = new StandardCapacityTranslationTable(mappings(mapping("metric", 10.0)));
        assertThat(table.metrics(), is(set("metric")));
        assertThat(table.containsMapping("metric"), is(true));
        assertThat(table.computeUnitCapacity("metric"), is(10.0));
        assertThat(table.toComputeUnits("metric", 15.0), is(1.5));
        assertThat(table.toComputeUnits("metric", 10.0), is(1.0));
        assertThat(table.toComputeUnits("metric", 5.0), is(0.5));
    }

    @Test
    public void constructWithMultipleMetrics() {
        CapacityTranslationTable table = new StandardCapacityTranslationTable(
                mappings(mapping("metric1", 10.0), mapping("metric2", 20.0)));
        assertThat(table.metrics(), is(set("metric1", "metric2")));
        assertThat(table.containsMapping("metric1"), is(true));
        assertThat(table.containsMapping("metric2"), is(true));
        assertThat(table.computeUnitCapacity("metric1"), is(10.0));
        assertThat(table.computeUnitCapacity("metric2"), is(20.0));
        assertThat(table.toComputeUnits("metric1", 15.0), is(1.5));
        assertThat(table.toComputeUnits("metric1", 5.0), is(0.5));
        assertThat(table.toComputeUnits("metric2", 50.0), is(2.5));
        assertThat(table.toComputeUnits("metric2", 10.0), is(0.5));
    }

    @Test(expected = NullPointerException.class)
    public void constructWithNull() {
        new StandardCapacityTranslationTable(null);
    }

    @Test(expected = NullPointerException.class)
    public void constructWithNullMetric() {
        new StandardCapacityTranslationTable(mappings(mapping(null, 10.0)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructWithNegativeMachineCapacity() {
        new StandardCapacityTranslationTable(mappings(mapping("metric", -1.0)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void convertWithBadMetric() {
        CapacityTranslationTable table = new StandardCapacityTranslationTable(
                mappings(mapping("metric1", 10.0), mapping("metric2", 20.0)));
        table.toComputeUnits("badmetric", 10.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void convertWithNonPositiveValue() {
        CapacityTranslationTable table = new StandardCapacityTranslationTable(
                mappings(mapping("metric1", 10.0), mapping("metric2", 20.0)));
        table.toComputeUnits("metric1", -2.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void machineCapacityForBadMetric() {
        CapacityTranslationTable table = new StandardCapacityTranslationTable(
                mappings(mapping("metric1", 10.0), mapping("metric2", 20.0)));
        table.computeUnitCapacity("badmetric");
    }

    @Test
    public void setMappings() {
        CapacityTranslationTable table = new StandardCapacityTranslationTable();
        assertThat(table.metrics(), is(set()));
        Map<String, Double> capacityMappings = Maps.of(//
                "metric1", 10.0, //
                "metric2", 20.0);
        table.setMappings(capacityMappings);
        assertThat(table.metrics(), is(set("metric1", "metric2")));
        assertThat(table.containsMapping("metric1"), is(true));
        assertThat(table.containsMapping("metric2"), is(true));
        assertThat(table.computeUnitCapacity("metric1"), is(10.0));
        assertThat(table.computeUnitCapacity("metric2"), is(20.0));
    }

    /**
     * Test overwrite behavior of
     * {@link CapacityTranslationTable#setMappings(Map)}.
     */
    @Test
    public void overwriteExistingMappings() {
        CapacityTranslationTable table = new StandardCapacityTranslationTable();
        assertThat(table.metrics(), is(set()));
        Map<String, Double> capacityMappings = Maps.of(//
                "metric1", 10.0, //
                "metric2", 20.0);
        table.setMappings(capacityMappings);
        assertThat(table.metrics(), is(set("metric1", "metric2")));
        assertThat(table.containsMapping("metric1"), is(true));
        assertThat(table.containsMapping("metric2"), is(true));
        assertThat(table.computeUnitCapacity("metric1"), is(10.0));
        assertThat(table.computeUnitCapacity("metric2"), is(20.0));

        Map<String, Double> newMappings = Maps.of("metric3", 30.0);
        table.setMappings(newMappings);
        assertThat(table.metrics(), is(set("metric3")));
        assertThat(table.containsMapping("metric1"), is(false));
        assertThat(table.containsMapping("metric2"), is(false));
        assertThat(table.containsMapping("metric3"), is(true));
        assertThat(table.computeUnitCapacity("metric3"), is(30.0));
    }

    @Test
    public void setMappingsWithNullMap() {
        CapacityTranslationTable table = new StandardCapacityTranslationTable(mappings(mapping("oldmetric", 10.0)));
        assertThat(table.metrics(), is(set("oldmetric")));
        try {
            table.setMappings(null);
            fail("managed to setMappings with bad argument(s)");
        } catch (NullPointerException e) {
            // expected
        }
        // verify that table is unchanged
        assertThat(table.metrics(), is(set("oldmetric")));
    }

    @Test
    public void setMappingsWithNullMetric() {
        CapacityTranslationTable table = new StandardCapacityTranslationTable(mappings(mapping("oldmetric", 10.0)));
        assertThat(table.metrics(), is(set("oldmetric")));
        try {
            Map<String, Double> capacityMappings = Maps.of(//
                    null, 10.0, //
                    "metric2", 20.0);
            table.setMappings(capacityMappings);
            fail("managed to setMappings with bad argument(s)");
        } catch (NullPointerException e) {
            // expected
        }
        // verify that table is unchanged
        assertThat(table.metrics(), is(set("oldmetric")));
    }

    @Test
    public void setMappingsWithZeroMachineCapacity() {
        CapacityTranslationTable table = new StandardCapacityTranslationTable(mappings(mapping("oldmetric", 10.0)));
        assertThat(table.metrics(), is(set("oldmetric")));
        try {
            Map<String, Double> capacityMappings = Maps.of(//
                    "metric1", 10.0, //
                    "metric2", 0.0);
            table.setMappings(capacityMappings);
            fail("managed to setMappings with bad argument(s)");
        } catch (IllegalArgumentException e) {
            // expected
        }
        // verify that table is unchanged
        assertThat(table.metrics(), is(set("oldmetric")));
    }

    @Test
    public void setMappingsWithNegativeMachineCapacity() {
        CapacityTranslationTable table = new StandardCapacityTranslationTable(mappings(mapping("oldmetric", 10.0)));
        assertThat(table.metrics(), is(set("oldmetric")));
        try {
            Map<String, Double> capacityMappings = Maps.of("metric1", -1.0);
            table.setMappings(capacityMappings);
            fail("managed to setMappings with bad argument(s)");
        } catch (IllegalArgumentException e) {
            // expected
        }
        // verify that table is unchanged
        assertThat(table.metrics(), is(set("oldmetric")));
    }

    @Test
    public void setMappingsWithNullMachineCapacity() {
        CapacityTranslationTable table = new StandardCapacityTranslationTable(mappings(mapping("oldmetric", 10.0)));
        assertThat(table.metrics(), is(set("oldmetric")));
        try {
            Map<String, Double> capacityMappings = new HashMap<String, Double>();
            capacityMappings.put("metric1", null);
            table.setMappings(capacityMappings);
            fail("managed to setMappings with bad argument(s)");
        } catch (NullPointerException e) {
            // expected
        }
        // verify that table is unchanged
        assertThat(table.metrics(), is(set("oldmetric")));
    }

    @Test
    public void addMapping() {
        CapacityTranslationTable table = new StandardCapacityTranslationTable(
                mappings(mapping("metric1", 10.0), mapping("metric2", 20.0)));
        assertThat(table.metrics(), is(set("metric1", "metric2")));
        assertThat(table.containsMapping("metric1"), is(true));
        assertThat(table.containsMapping("metric2"), is(true));
        assertThat(table.computeUnitCapacity("metric1"), is(10.0));
        assertThat(table.computeUnitCapacity("metric2"), is(20.0));

        table.addMapping("metric3", 30.0);
        assertThat(table.metrics(), is(set("metric1", "metric2", "metric3")));
        assertThat(table.containsMapping("metric1"), is(true));
        assertThat(table.containsMapping("metric2"), is(true));
        assertThat(table.containsMapping("metric3"), is(true));
        assertThat(table.computeUnitCapacity("metric1"), is(10.0));
        assertThat(table.computeUnitCapacity("metric2"), is(20.0));
        assertThat(table.computeUnitCapacity("metric3"), is(30.0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void addExistingMapping() {
        CapacityTranslationTable table = new StandardCapacityTranslationTable(mappings(mapping("metric1", 10.0)));
        table.addMapping("metric1", 20.0);
    }

    @Test(expected = NullPointerException.class)
    public void addMappingWithNullMetric() {
        CapacityTranslationTable table = new StandardCapacityTranslationTable();
        table.addMapping(null, 20.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addMappingWithZeroMachineCapacity() {
        CapacityTranslationTable table = new StandardCapacityTranslationTable();
        table.addMapping("metric", 0.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addMappingWithNegativeMachineCapacity() {
        CapacityTranslationTable table = new StandardCapacityTranslationTable();
        table.addMapping("metric", -1.0);
    }

    @Test
    public void clear() {
        CapacityTranslationTable table = new StandardCapacityTranslationTable(
                mappings(mapping("metric1", 10.0), mapping("metric2", 20.0)));
        assertThat(table.metrics(), is(set("metric1", "metric2")));
        table.clear();
        assertThat(table.metrics(), is(set()));
        table.clear();
        assertThat(table.metrics(), is(set()));
    }

    private Set<String> set(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }
}
