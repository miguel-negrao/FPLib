(
func:{ |eventSource|
	eventSource.storePrevious.select{ |array| array[0] != array[1] }.collect(_[1])
},
desc: "Output only if new value different from last value",
name: "changes"
)
