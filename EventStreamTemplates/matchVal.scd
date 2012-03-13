(
func:{ |eventSource, array|
	eventSource
		.select{ |v| array.includes(v) }
		.collect{ |v| array.indexOf(v) }
},
desc: "Output only if event matches one of the values in the array, in which case the index of the value is returned",
name: "matchVal"
)
