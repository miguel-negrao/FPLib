(
func:{ |eventSource, f, delta|
	eventSource | eventSource.flatCollect{ |v|
		TimerES(delta, inf).inject(0, _ + delta ).collect( f.(v,_) )
	}
},
desc: "Mimic a classic FRP behaviour. Runs a functions of time with start value the value received from the parent event source",
name: "behaviour"
)
