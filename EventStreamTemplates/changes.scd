(
func:{ |eventSource|
	eventSource.storePrevious.select{ |tuple| tuple.at1 != tuple.at2 }.collect(_.at2)
},
desc: "Output only if new value different from last value",
name: "changes"
)
