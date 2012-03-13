(
func:{ |eventSource|
	eventSource.collect([Process.elapsedTime,_])
},
desc: "store time when event occured",
name: "storeWithT"
)
