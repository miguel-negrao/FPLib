(
func:{ |eventSource, t = 0.5|
	eventSource
		.storePrevious
		.select{ |array|
         	var x,y;
         	#x,y = array;
         	( x > t ) && ( y < t )
        }
        .collect{ \down }

},
desc: "Output when value crosses threshold while decreasing",
name: "thresholdUp"
)
