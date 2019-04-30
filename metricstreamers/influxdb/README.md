# InfluxDB metric streamer
The InfluxDB metric streamer can be used to configure the 
`StandardMonitoringSubsystem` to stream metric values from
an [InfluxDB](https://docs.influxdata.com/influxdb) server. 
It can be set up as follows in the configuration document of an autoscaler 
instance:

    "monitoringSubsystem": {
        "metricStreamer": {
            "type": "InfluxdbMetricStreamer",
            "config: {
               ... influxdb-specific configuration
            }
        },
        ...
    } 

For more details on the available configuration settings for the 
`InfluxdbMetricStreamer`, refer to the Configuration section below.

## Configuration
This is a sample configuration document for the `InfluxdbMetricStreamer`.

    {
        "host": "localhost",
        "port": 8086,
        "security": {
            "https": false,
            "auth": { "username": "foo", "password": "bar" },
            "verifyCert": false,
            "verifyHost": false
        },
        "pollInterval": { "time": 30, "unit": "seconds" },
        "metricStreams": [
            {
                "id": "request.rate.stream",
                "metricName": "request_rate",
                "database": "mydb",
                "query": {
                    "select": "non_negative_derivative(mean(requests),1s)",
                    "from": "nginx",
                    "where": "cloud = 'aws' AND region =~ /us-east.*/",
                    "groupBy": "time(5m) fill(none)"
                },
                "dataSettlingTime":  { "time": 1, "unit": "minutes" },
                "queryChunkSize": { "time": 14, "unit": "days" }
            }
        ]
    }

The fields carry the following semantics:

   - `host`: InfluxDB server host name/IP address. Required.
   - `port`: InfluxDB server port. Required. 
   - `security`: Security settings for connecting with the server. Optional.  
     If left out, the InfluxDB server is assumed to run over HTTP and no 
     client authentication will be used.
       - `https`: If the InfluxDB server runs with HTTPS enabled, this option 
         should be `true`. A missing/`false` value  means that the server runs
         plain HTTP.
       - `auth`: Basic (username/password) credentials to use if the InfluxDB 
         server requires user authentication.
       - `verifyCert`: Set to `true` to enable server certificate verification 
         on SSL connections. If disabled, the server peer will not be verified,
         which is similar to using the `--insecure` flag in `curl`.  
         This option is only relevant when the server runs HTTPS.
       - `verifyHost`: Enables/disables hostname verification during SSL
         handshakes. If verification is enabled, the SSL handshake will only
         succeed if the URL's hostname and the server's identification
         hostname match.  
         This option is only relevant when the server runs HTTPS.
   - `pollInterval`: The polling interval for metric streams. Defaults to: 30 seconds.
   - `metricStreams`: The collection of published metric streams.
       - `id`: The id of the metric stream. This is the id that will be used by
         clients wishing to subscribe to this metric stream. Required.
       - `metricName`: The metric name that will be assigned to the
         `MetricValue`s produced by this stream. Optional. Default: `${id}`.
       - `database`: The InfluxDB database to query. An InfluxDB database acts 
          as a container for time series data. For example, `mydb`. Required.
       - `query`: a influx QL `SELECT` query to be periodically executed by the
         metric streamer. A query should avoid filtering on `time` in the
         `WHERE` clause, as selecting the approriate interval for which to fetch
         metrics will be handled by the metric streamer.  
         The query author needs to be aware of the following:  
         - Only select a single field/column in the `SELECT`
           statement. Any additional fields are ignored in the result
           processing.
         - The selected field/column must be a numeric value. If not,
           errors will be raised during result processing.
         - Semi-colons are disallowed. This protects against injecting
           additional modifying queries/statements.
         - It is entirely possible to put together queries that don't make
           sense. These won't be caught until runtime (when an attmept is
           made to execute the query against InfluxDB).
         - `select`: `SELECT` clause. For example, `mean('request_rate')` or
           `non_negative_derivative(max('value'),1s)`. The select statement
           should select one single field/column (only the first field is
           handled in result processing). The selected field/column must be a
           numeric value. If not, errors will be raised during result
           processing. Surround identifiers with double quotes to support a
           richer character set for identifiers.
         - `from`: `FROM` clause. Specifies the measurement to query for. For
           example, `cpu/system`. Surround identifiers with double quotes to
           support a richer character set for identifiers.
         - `where`: `WHERE` clause. Used to filter data based on tags/field
           values. Avoid filtering on `time` in the `WHERE` clause, as selecting
           the approriate interval for which to fetch metrics will be handled by
           the metric streamer. Behavior is undefined if `time`
           filters are inclued. Surround identifiers with double quotes to
           support a richer character set for identifiers.
         - `groupBy`: `GROUP BY` clause. Can be used to downsample data (if
           combined with an aggregation function in the `SELECT` clause), for
           example by specifying something like `time(10s) fill(none)`. Note: do
           not group on tags, since that will create different series in the
           result (one per group) and the result handler will only process the
           first series (the order may not be deterministic). Use a `WHERE`
           clause to query for a particular combination of tags.
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



## Testing

To set up a local InfluxDB database server on your local machine, you may
want to run the official [InfluxDB Docker image](https://hub.docker.com/_/influxdb/):

  - Using a default configuration:

        docker run --name influxdb -p 8083:8083 -p 8086:8086 -v $PWD:/var/lib/influxdb influxdb:1.0

  - Using a custom config:

        # generate default config
        docker run --rm influxdb:1.0 influxd config > influxdb.conf

        # ... modify influxdb.conf

        # run with modified config
        docker run -p 8083:8083 -p 8086:8086 \
           -v $PWD/influxdb.conf:/etc/influxdb/influxdb.conf:ro \
            influxdb -config /etc/influxdb/influxdb.conf


You can either connect with InfluxDB over its web UI (port 8083), with
`curl` through the REST API (port 8086), or with the shell:

    docker run --rm --net=container:influxdb -it influxdb influx -host influxdb

