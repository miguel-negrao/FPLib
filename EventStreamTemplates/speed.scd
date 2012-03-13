(
func:{ |eventSource|
	var x = eventSource
		.storePreviousWithT
		.collect{ |array|
			var x1,x2,t1,t2,a1,a2;
			//manual pattern matching, how boring...
			#a1,a2 = array;
			#t1,x1 = a1;
			#t2,x2 = a2;
			(x2 - x1).abs / (t2 - t1)
		};
	//creating new EventSource everytime a new event happens so we must use flatCollectR
	x | eventSource.flatCollectR{ WaitES(1.0, 0.0) }

},
desc: "Output speed at which values are arriving.",
name: "speed"
)
