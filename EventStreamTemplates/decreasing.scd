(
func:{ |eventSource|
	eventSource
		.storePrevious
		.select{ |array| array[1] < array[0] }
		.collect{ |array| array[1] }
},
desc: "Output only when values are decreasing",
name: "decreasing"
)
