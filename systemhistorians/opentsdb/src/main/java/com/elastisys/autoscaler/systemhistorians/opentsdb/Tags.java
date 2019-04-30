package com.elastisys.autoscaler.systemhistorians.opentsdb;

import com.elastisys.autoscaler.core.api.types.MetricValue;
import com.elastisys.autoscaler.core.autoscaler.AutoScaler;

public class Tags {

    /**
     * {@link MetricValue} tag used to annotate OpenTSDB data points with their
     * origin {@link AutoScaler} instance.
     */
    public static final String AUTO_SCALER_ID = "autoScalerId";
}
