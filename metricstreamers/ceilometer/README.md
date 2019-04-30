# Ceilometer metric streamer
The Ceilometer metric streamer can be used to configure the 
`StandardMonitoringSubsystem` to stream metric values from
an [OpenStack Ceilometer](https://wiki.openstack.org/wiki/Telemetry). 
It can be set up as follows in the configuration document of an autoscaler 
instance:

    "monitoringSubsystem": {
        "metricStreamer": {
            "type": "CeilometerMetricStreamer",
            "config: {
               ... ceilometer-specific configuration
            }
        },
        ...
    } 


## Configuration
This is a sample configuration document for the `CeilometerMetricStreamer`.


	{
		"metricStreamer": {
			"auth": {
				"keystoneUrl": "http://keystone.example.com:5000/v2.0",
				"v2Credentials": {
					"tenantName": "tenant",
					"userName": "clouduser",
					"password": "cloudpass"
				}
			},
			"region": "RegionOne",
			"pollInterval": { "time": 30, "unit": "seconds" },
			"metricStreams": [
				{
					"id": "connrate.stream",
					"meter": "network.services.lb.total.connections.rate",
					"downsampling": { "function": "Sum", "period": {"time": 1, "unit": "minutes"}},
					"convertToRate": false,
					"dataSettlingTime": { "time": 20, "unit": "seconds" },
					"queryChunkSize": { "time": 7, "unit": "days" }
				}
			]
		}
	}


The fields carry the following semantics:

   - `auth`: Declares how to authenticate with the OpenStack identity service
     (Keystone). Required.
   - `region`: The particular OpenStack region (out of the ones available in Keystone's
     service catalog) to connect to. Required. 
   - `pollInterval`: The polling interval for metric streams. Defaults to: 30 seconds.
   - `metricStreams`: The collection of published metric streams.
       - `id`: The id of the metric stream. This is the id that will be used by
	     clients wishing to subscribe to this metric stream. Required.
	   - `meter`: The particular Ceilometer meter to query. Required.
       - `resourceId`: A resource identifier which can be used narrow down the query to only
         retrieve metric values associated with a given OpenStack resource. Optional.
       - `downsampling`: A downsampling specification which can be set to query for 
	     statistics rather than raw samples. Optional.
		   - `period`: The downsampling period. It determines the distance between aggregated
             data points in the result set. The aggregation function `function`
             will be applied to all datapoints within each period in the requested
             query interval.
		   - `function`: A Ceilometer aggregation function to apply to data points within each
             `period` in a query interval.
	   - `convertToRate`: When `true` the stream will feed the change rate of the
         metric, rather than the absolute values of the metric. Optional. 
         Default: `false`.
       - `dataSettlingTime`: The minimum age of requested data points. Values 
	     newer than this will never be requested. This value can 
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
		 Optional. Default: `14 days`.



