# InfluxDB system historian
The InfluxDB system historian can be used to configure the 
`StandardMonitoringSubsystem` to push operational metrics for
an autoscaler to an [InfluxDB](https://docs.influxdata.com/influxdb) server. 
It can be set up as follows in the configuration document of an autoscaler 
instance:

    "monitoringSubsystem": {
        "systemHistorian": {
            "type": "InfluxdbSystemHistorian",
            "config: {
               ... influxdb-specific configuration
            }
        },
        ...
    } 

For more details on the available configuration settings for the 
`InfluxdbSystemHistorian`, refer to the Configuration section below.

## Configuration
This is a sample configuration document for the `InfluxdbSystemHistorian`.

    {
        "host": "localhost",
        "port": 8086,
        "database": "mydb",
        "security": {
            "https": false,
            "auth": { "username": "foo", "password": "bar" },
            "verifyCert": false,
            "verifyHost": false
        },
        "reportingInterval": { "time": 30, "unit": "seconds" },
		"maxBatchSize": 2000
    }

The fields carry the following semantics:

   - `host`: InfluxDB server host name/IP address. Required.
   - `port`: InfluxDB server port. Required.
   - `database`: The InfluxDB database to write to. An InfluxDB database acts 
     as a container for time series data. For example, `mydb`. Required. 
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
   - `reportingInterval`: The time interval between two successive reporting 
     attempts to InfluxDB. Default: 30 seconds.
   - `maxBatchSize`: The maximum number of datapoints to send in a single call 
     to InfluxDB. As noted in the [InfluxDB documentation](https://docs.influxdata.com/influxdb/v1.0/guides/writing_data/) it may be necessary to split 
	 datapoints into smaller batches once they exceed a few thousand points to 
	 avoid request time outs. Default: 1000.  



## Testing

To set up a local InfluxDB database server on your local machine, you may
want to run the official [InfluxDB Docker image](https://hub.docker.com/_/influxdb/):

  - Using a default configuration:
	
        docker run --name influxdb -p 8083:8083 -p 8086:8086 -v $PWD:/var/lib/influxdb influxdb:0.13
		
  - Using a custom config:
  
        # generate default config
		docker run --rm influxdb:0.13 influxd config > influxdb.conf
		
		# ... modify influxdb.conf
		
		# run with modified config
		docker run -p 8083:8083 -p 8086:8086 \
           -v $PWD/influxdb.conf:/etc/influxdb/influxdb.conf:ro \
            influxdb -config /etc/influxdb/influxdb.conf


You can either connect with InfluxDB over its web UI (port 8083), with
`curl` through the REST API (port 8086), or with the shell:

    docker run --rm --net=container:influxdb -it influxdb influx -host influxdb
	
