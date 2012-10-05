EventNetwork {
	var <actuate, //IO[Unit]
	<pause; //IO[Unit]

	//networkDescription : Writer( Unit, Tuple3([IO(IO())], [EventStream[IO[Unit]]], [IO] ) )
	//                                     eventHandlers         reactimates        IOLater
	*new{ |networkDescription|
		var tuple = networkDescription.w;

		//reactimates
		var finalIOES = tuple.at2.reduce('|');
		var f  = { |v| v.unsafePerformIO };
		var doFinalIO = IO{ finalIOES.do(f) };
		var stopDoingFinalIO = IO{ finalIOES.stopDoing(f) };
		var iosLater = tuple.at3.sequence;

		//inputs
		var unregister;
		var registerIO = tuple.at1.sequence >>= { |x| IO{ unregister = x;
			Unit
		  } };
		var unregisterIO = IO{ unregister } >>= { |x|  x !? _.sequence; };

		^super.newCopyArgs( doFinalIO >>=| registerIO >>=| iosLater, stopDoingFinalIO >>=| unregisterIO )
	}

	*returnDesc { |a| ^Writer( a, Tuple3([],[],[]) ) }
    *returnUnit { ^this.returnDesc(Unit) }

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
		^Writer( es, Tuple3([addHandler],[],[]) )
	}

	*makeES { |addAction, removeAction|
        var addHandler;
		var es = EventSource();
		addHandler = IO{
			var action = { |sl| es.fire( sl.value ) };
			addAction.(action);
			IO{ removeAction.(action) }
		};
		^Writer( es, Tuple3([addHandler],[],[]) )
    }

}

ENTimer {
    var <delta, <maxTime;
    var <>action; //Option[Function]
    var <task, t = 0;

    *new { |delta = 0.1, maxTime = inf|
        ^super.newCopyArgs(delta, maxTime).init;
    }

    init {
        task = Task({
            inf.do{
                delta.wait;
                if( t >= maxTime) {
                    task.stop;
                };
                t = t + delta;
                action.do{ |f| f.(t) };
            }
        });
    }

    start {
        ^IO{ t = 0; task.start }
	}

	stop {
        ^IO{ task.stop };
	}

	pause {
        ^IO{ task.pause }

	}

	resume {
        ^IO{ task.resume }
	}
	actions {
        ^[ { |action| this.action_( Some(action) ) }, { this.action_(None) } ]
	}

    asENInput {
        ^EventNetwork.makeES( *this.actions )
    }

}

FRPGUICode {

	*makeENInput { |gui|
		var addHandler;
		var es = FPSignal(gui.value);
		var action = { |sl| es.value_( sl.value ) };
		addHandler = IO{
			gui.addAction(action);
			IO{ gui.removeAction(action)
		} };
		^Writer( es, Tuple3([addHandler],[],[IO{ action.value(gui) }]) )
	}

}

+ QView {

	asENInput {
		^FRPGUICode.makeENInput(this)
	}
}

+ SCView {

	asENInput {
		^FRPGUICode.makeENInput(this)
	}

}

+ MKtlElement {

	asENInput {
		var es = EventSource();
		var func = { |v| es.fire(v) };
		var internalES = this.eventSource;
		var addHandler = IO{ internalES.do(func); IO{ internalES.stopDoing(func) } };
		^Writer( es, Tuple3([addHandler],[],[]) )
	}

}