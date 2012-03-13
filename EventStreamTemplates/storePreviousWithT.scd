(
func:{ |eventSource|
	eventSource.fold([[0.0,0.0],[0.0,0.0]], { |state,x| [state[1],[Process.elapsedTime,x]] })
},
desc: "store current and previous event in an array with the event time stamp. The initial valuea are assumed to be [0.0,0.0]",
name: "storePreviousWithT"
)
