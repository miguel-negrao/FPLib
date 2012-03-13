(
func:{ |eventSource|
	eventSource.fold([0.0,0.0], { |state,x| [state[1],x] })
},
desc: "store current and previous event in an array. The initial values are assumed to be 0.0",
name: "storePrevious"
)
