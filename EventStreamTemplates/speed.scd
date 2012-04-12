(
func:{ |eventSource|
	var x = eventSource
		.storePreviousWithT
		.collect{ |tuple|
			var a1 = tuple.at1;
			var a2 = tuple.at2;
            var t1 = a1.at1;
            var x1 = a1.at2;
            var t2 = a2.at1;
            var x2 = a2.at2;
			(x2 - x1).abs / (t2 - t1)
		};
	x | eventSource.flatCollect{ WaitES(1.0, 0.0) }

},
desc: "Output speed at which values are arriving.",
name: "speed"
)
