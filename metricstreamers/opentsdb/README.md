# OpenTSDB metric streamer
The OpenTSDB metric streamer can be used to configure the 
`StandardMonitoringSubsystem` to stream metric values from
an [OpenTSDB](http://opentsdb.net/) server. 
It can be set up as follows in the configuration document of an autoscaler 
instance:

    "monitoringSubsystem": {
        "metricStreamer": {
            "type": "OpenTsdbMetricStreamer",
            "config: {
               ... opentsdb-specific configuration
            }
        },
        ...
    } 


## Configuration
This is a sample configuration document for the `OpenTsdbMetricStreamer`.


    {
        "metricStreamer": {
            "openTsdbHost": "host",
            "openTsdbPort": 4242,
            "pollInterval": { "time": 5, "unit": "seconds" },
            "metricStreams": [	
                { 
                    "id": "http.total.accesses.rate.stream", 
                    "metric": "http.total.accesses", 
                    "aggregator": "SUM", 
                    "convertToRate": true, 
                    "downsampling": { 
                        "function": "MEAN", 
                        "interval": { "time": 5, "unit": "minutes"}
                    },
                    "tags": { "host": ["*"] },
                    "dataSettlingTime": { "time": 30, "unit": "seconds"}
                    "queryChunkSize": { "time": 14, "unit": "days" }
                }
            ]
        }
	}


The fields carry the following semantics:

   - `openTsdbHost`: OpenTSDB server host name/IP address. Required.
   - `openTsdbPort`: OpenTSDB server port. Required. 
   - `pollInterval`: The polling interval for metric streams. Defaults to: 30 seconds.
   - `metricStreams`: The collection of published metric streams.
       - `id`: The id of the metric stream. This is the id that will be used by
	     clients wishing to subscribe to this metric stream. Required.
	   - `metric`: The OpenTSDB metric that the metric stream retrieves values
          for. Required.
       - `aggregator`: The aggregation function used to aggregate values
         in the metric stream. One of `MIN`, `MAX`, `SUM`, `AVG`. Required.
	   - `convertToRate`: When `true` the stream will feed the change rate of the
         metric, rather than the absolute values of the metric. Optional. 
         Default: `false`.
       - `downsampling`: The down-sampling to apply to metric values in the 
	     metric stream. Optional.
		   - `interval`: The sampling interval.
		   - `function`: The function used to aggregate data points within each 
		     sampling interval to a single value. One of `MIN`, `MAX`, `SUM`, 
			 `MEAN`.
       - `tags`: The collection of tags used to filter the metric values 
	     returned from the metric stream. Optional.
       - `dataSettlingTime`: The minimum age of requested data points. Values 
	     newer than this will never be requested from InfluxDB. This value can 
		 be regarded as the expected "settling time" of new data points.  
		 When requesting recent aggregate metric data points, there is always a
         risk of seeing partial/incomplete results before metric values from all
         sources have been reported. The data settling time is intended to give
         all sources a chance to report before fetching recent values.  
		 The value to set for this field depends on the reporting frequency of
         monitoring agents, but as a general rule-of-thumb, this value can be 
		 set to be about `1.5` times the length of the reporting-interval for
         monitoring agents. Optional.
	   - `queryChunkSize`: The maximum time period that a single query will 
	     attempt to fetch in a single call. A query with a longer time interval 
		 will be run incrementally, each fetching a sub-interval of this duration. 
		 This type of incremental retrieval of large result sets limits the
         amount of (memory) resources involved in processing large queries. 
		 Optional. Default: `30 days`.



