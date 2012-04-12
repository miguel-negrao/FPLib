(
func:{ |eventSource|
	eventSource.inject( Tuple2( Tuple2(0.0,0.0), Tuple2(0.0,0.0) ), { |state,x| Tuple2( state.at2, Tuple2( Process.elapsedTime, x ) ) })
},
desc: "store current and previous event in an tuple with the event time stamp. The initial valuea are assumed to be [0.0,0.0]",
name: "storePreviousWithT"
)
