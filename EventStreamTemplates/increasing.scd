(
func:{ |eventSource|
	eventSource
		.storePrevious
		.select{ |tuple| tuple.at2 > tuple.at1 }
		.collect{ |tuple| tuple.at2 }
},
desc: "Output only when values are increasing",
name: "increasing"
)
