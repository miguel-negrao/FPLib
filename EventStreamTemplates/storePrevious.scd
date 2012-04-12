(
func:{ |eventSource|
	eventSource.inject( Tuple2(0.0,0.0), { |state,x| Tuple2( state.at2, x ) })
},
desc: "store current and previous event in an Tuple2. The initial values are assumed to be 0.0",
name: "storePrevious"
)
