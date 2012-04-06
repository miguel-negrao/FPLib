(
func:{ |eventSource|
	eventSource.collect( Tuple2(Process.elapsedTime,_) )
},
desc: "store time when event occured",
name: "storeWithT"
)
