EventNetwork {
	var <actuate, //IO[Unit]
	<pause; //IO[Unit]
	
	//networkDescription : Writer( Unit, Tuple2([IO(IO())], [EventStream[IO[Unit]]]) )
	*new{ |networkDescription|
		var tuple = networkDescription.w;
		
		//reactimates
		var finalIOES = tuple.at2.reduce('|');
		var f  = { |v| v.unsafePerformIO };
		var doFinalIO = IO{ finalIOES.do(f) };
		var stopDoingFinalIO = IO{ finalIOES.stopDoing(f) };
		
		//inputs
		var unregister;
		var registerIO = tuple.at1.sequence >>= { |x| IO{ unregister = x;
			Unit
		  } };
		var unregisterIO = IO{ unregister } >>= { |x|  x !? _.sequence; };
			
		^super.newCopyArgs( doFinalIO >>=| registerIO, stopDoingFinalIO >>=| unregisterIO ) 	
	}
	
	actuateNow { actuate.unsafePerformIO; ^Unit }
	pauseNow { pause.unsafePerformIO; ^Unit }
	
	*newTimer{ |delta = 0.1, maxTime = inf|
		var addHandler;
		var es = EventSource();
		addHandler = IO{ 
			var t = 0;
			var routine = fork{
				inf.do{
					delta.wait;
					if( t >= maxTime) {
						routine.stop;
					};
					t = t + delta;
					es.fire(t);
				}
			};
			IO{ routine.stop } };
		^Writer( es, Tuple2([addHandler],[]) )	
	}
}

FRPGUICode {

	*asENInput { |gui|
		var addHandler;
		var es = EventSource();
		addHandler = IO{ 
			var action = { |sl| es.fire( sl.value ) };
			gui.addAction(action);
			IO{ gui.removeAction(action) 
		} };
		^Writer( es, Tuple2([addHandler],[]) )	
	}

}

+ QView {

	asENInput {
		^FRPGUICode.asENInput(this)
	}
}

+ SCView {

	asENInput {
		^FRPGUICode.asENInput(this)
	}

}

+ MKtlElement {
	
	asENInput {
		var es = EventSource();
		var func = { |v| es.fire(v) };
		var internalES = this.eventSource;
		var addHandler = IO{ internalES.do(func); IO{ internalES.stopDoing(func) } };
		^Writer( es, Tuple2([addHandler],[]) )		
	}

}