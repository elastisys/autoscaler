# CloudWatch metric streamer
The CloudWatch metric streamer can be used to configure the 
`StandardMonitoringSubsystem` to stream metric values from
the [AWS CloudWatch](https://aws.amazon.com/cloudwatch/) API. 
It can be set up as follows in the configuration document of an autoscaler 
instance:

    "monitoringSubsystem": {
        "metricStreamer": {
            "type": "CloudWatchMetricStreamer",
            "config: {
               ... cloudwatch-specific configuration
            }
        },
        ...
    } 


## Configuration
This is a sample configuration document for the `CloudWatchMetricStreamer`.

	{
		"metricStreamer": {
			"accessKeyId": "ABC...123",
			"secretAccessKey": "ABC...123",
			"region": "us-east-1",
			"pollInterval": { "time": 30, "unit": "seconds" },
			"metricStreams": [
				{
					"id": "cpu.utilization.stream",
					"namespace": "AWS/EC2",
					"metric": "CPUUtilization",
					"dimensions": { "AutoScalingGroupName": "group1" },
					"statistic": "Sum",
					"period": { "time": 60, "unit": "seconds" },
					"convertToRate": false,
					"dataSettlingTime": { "time": 2, "unit": "minutes" },
					"queryChunkSize": { "time": 1440, "unit": "minutes" }
				}
			]
		}
	}


The fields carry the following semantics:

   - `accessKeyId`: . Required.
   - `secretAccessKey`: . Required. 
   - `pollInterval`: The polling interval for metric streams. Defaults to: 30 seconds.
   - `metricStreams`: The collection of published metric streams.
       - `id`: The id of the metric stream. This is the id that will be used by
	     clients wishing to subscribe to this metric stream. Required.
	   - `namespace`: The Amazon CloudWatch namespace of the metric to fetch.
         Required.
       - `metric`: The CloudWatch metric that the metric stream retrieves
         values for. Required.
	   - `statistic`: The aggregation method to apply to the set of metric values.
         Can be any of `Sum`, `Average`, `Minimum`, `Maximum` and
         `SampleCount`. Required.
       - `period`: Specifies the time period over which to apply the statistic
         and, hence, the spacing between data points in the aggregated
         data. Must be at least `60` seconds and must be a
         multiple of `60`. Required.
       - `dimensions`: The dimensions (key-value pairs) used to narrow down the set
         of streamed metric values. Only metric values matching the
         specified dimensions will be returned.
       - `convertToRate`: When `true` the stream will feed the change rate of
         the metric rather than the absolute values of the metric. Optional. 
		 Default: `false`.
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
		 Optional. Default: 1440 data points (API-limit on query size).



