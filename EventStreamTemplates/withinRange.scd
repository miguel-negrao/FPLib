(
func:{ |eventSource, a, b|
	eventSource.select{ |v| ( v >= a ) && ( v <= b) }
},
desc: "Output only values between a and b",
name: "withinRange"
)
