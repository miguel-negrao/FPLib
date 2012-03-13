(
func:{ |eventSource, t = 1.0|
	eventSource
		.fold( [0.0,0.0,true], { |array,newV|
			var tOutputed, xOutputed, bool, newT, shouldFire;
			#tOutputed, xOutputed, bool = array;
		  	newT = Process.elapsedTime;
		  	shouldFire  =  ( newT - tOutputed ) >= t;
		  	(if(shouldFire) { [newT, newV,shouldFire] } { [tOutputed,xOutputed, shouldFire] })
		})
		//only let event through if shouldFire is true
		.select(_[2])
		//get x from [t,x,bool]
		.collect(_[1])

},
desc: "Only fire if new value arrived at least t seconds after last value. Does not remember values in between.",
name: "slowdown"
)
