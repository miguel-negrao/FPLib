(
func:{ |eventSource, t = 0.5|
	eventSource.thresholdUp(t) | eventSource.thresholdDown(t)
},
desc: "Output when value crosses threshold",
name: "threshold"
)
