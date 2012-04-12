(
func:{ |eventSource, t = 1.0|
	eventSource
		.inject( Tuple3( 0.0, 0.0, true ), { |tuple, newV|
			var tOutputed = tuple.at1;
			var xOutputed = tuple.at2;
			var bool = tuple.at3;
		  	var newT = Process.elapsedTime;
		  	var shouldFire  =  ( newT - tOutputed ) >= t;
		  	if(shouldFire) { Tuple3( newT, newV, shouldFire ) } { Tuple3( tOutputed, xOutputed, shouldFire ) }
		})
		//only let event through if shouldFire is true
		.select(_.at3)
		//get x from Tuple2(t,x,bool)
		.collect(_.at2)

},
desc: "Only fire if new value arrived at least t seconds after last value. Does not remember values in between.",
name: "slowdown"
)
