(
func:{ |eventSource, waitTime = 0.5, n = 2|
	eventSource
		.collect{ Process.elapsedTime }
        .fold([0.0], { |times, t|
        	if( (t - times[0]) < t ) {
        		times++[t]
        	} {
        		[t]
        	}
        })
        .select{ |times| times.size == n }
        .collect{ Unit }
},
desc: "Output a Unit if n consecutive events arrive within a period of waitTime seconds",
name: "neventsWithinT"
)
