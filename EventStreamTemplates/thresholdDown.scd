(
func:{ |eventSource, t = 0.5|
	eventSource
		.storePrevious
		.select{ |tuple|
         	var x = tuple.at1;
         	var y = tuple.at2;
         	( x > t ) && ( y < t )
        }
        .collect{ \down }

},
desc: "Output when value crosses threshold while decreasing",
name: "thresholdUp"
)
