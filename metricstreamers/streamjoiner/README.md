# Stream-joining metric streamer
The `MetricStreamJoiner` metric streamer is a "meta metric streamer", which
consumes one or more metric streams and produces a single new metric
stream by applying a join function to the consumed stream values.

The `MetricStreamJoiner`'s operation can be illustrated by the following
schematical image:

```
MetricStream a -> |
                  |
MetricStream b -> | MetricStreamJoiner d: joinScript(${a}, ${b}, ${c})
                  |
MetricStream c -> |
```

`MetricStreamJoiner` listens for values from existing `MetricStream`s `a`, `b`,
`c` and whenever a new value is received from either stream (and at least one
value has been observed on every stream and their timestamps are within
`maxTimeDifference` distance of each other) the joiner stream `d` produces a new
metric value being its "join function" applied to the most recently observed
value from every stream.

The `joinScript` can be an arbitrary JavaScript expression like

     a + b + c

or

     if (a > b + c) {
         a + b
     } else {
         c
     }

The only requirement that the `joinScript` must satisfy is that
*the end result of executing the last script statement/expression must be a
single numerical value*.


Just like other metric streamers, the `MetricStreamJoiner` can declare several
metric streams. The only requirement being that its input streams have been
declared already (a joined metric stream can even be defined in terms of other,
prior declared, joined metric streams).


## Limitations
The StreamJoiner's `MetricStream` does not support the query interface and,
hence, cannot be queried for old values. When queried it will always respond
with an empty `QueryResultSet`.


## Configuration
This is a sample configuration document for the `StreamJoiner`.

        {
            "type": "MetricStreamJoiner",
            "config": {
                "metricStreams": [
                    {
                        "id": "cluster.cpu.utilization",
                        "metric": "cpu",
                        "inputStreams": {
                            "requestedCPU": "requested.cpu",
                            "allocatableCPU": "allocatable.cpu"
                        },
                        "maxTimeDiff": { "time": 5, "unit": "seconds" },
                        "joinScript": [
                            "requestedCPU / allocatableCPU"
                        ]
                    },
                    ... more metric stream declarations
               ]
           }
        }


The fields carry the following semantics:

   - `metricStreams`: The collection of published metric streams. Required.
     - `id`: The id of the metric stream. This is the id that will be used by
       clients wishing to subscribe to this metric stream. Required.
     - `metric`: The name of the metric produced by this metric stream. This is
       the metric that will be set for produced `MetricValue`s. Optional. If
       left out, the `id` value is used.
     - `inputStreams`: Declares the input metric streams that are to be joined
       by this metric stream. Keys are *metric stream aliases*  and values are
       metric stream identifiers. The referenced metric streams must have been
       declared prior to this metric stream (note that this can be another
       joined metric stream). A *metric stream alias* needs to be a valid
       JavaScript identifier name, as these will be passed as variables to the
       `joinScript`. Required.
    - `maxTimeDiff`: The maximum difference in time between observed metric
      stream values for the joined metric stream to apply its `joinScript` and
      produce a new value. If stream metrics are farther apart than this, no new
      metric value is produced on the joined stream. This value should be set so
      that values that are close enough to be considered relevant to each other
      can be joined. The default is `0 s`, assuming that the input metric
      streams are scraped at the same instant.
   - `joinScript`: An array of JavaScript code lines for the join
      function. *The final statement/expression that the script executes
      must be a numerical value*. The script can assume that, when executed,
      each of the metric stream  aliases defined in the `inputStreams` section
      will be assigned a `Number` value.
