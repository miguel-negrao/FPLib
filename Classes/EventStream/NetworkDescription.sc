/*
    FP Quark
    Copyright 2012 - 2013 Miguel Negrão.

    FP Quark: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FP Quark is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with FP Quark.  If not, see <http://www.gnu.org/licenses/>.

    It is possible to add more type instances by adding the functions
    directly to the dict from the initClass function of the class that
    one wants to make an instance of some type class.
*/

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
        var iosLater = tuple.at3.sequence(IO);

		//inputs
		var unregister;
        var registerIO = tuple.at1.sequence(IO) >>= { |x| IO{ unregister = x;
			Unit
		  } };
        var unregisterIO = IO{
            if(unregister.notNil) {
                unregister.do(_.unsafePerformIO )
            }
        };
        var actuate = doFinalIO >>=| registerIO >>=| iosLater;
        var pause = stopDoingFinalIO >>=| unregisterIO;

        ^super.newCopyArgs( actuate, pause )
	}

	*returnDesc { |a| ^Writer( a, Tuple3([],[],[]) ) }
    *returnUnit { ^this.returnDesc(Unit) }

	actuateNow { actuate.unsafePerformIO; ^Unit }
	pauseNow { pause.unsafePerformIO; ^Unit }
    /*
    addAction { |f| } where is a function to call with the new event whenever
    a new event arrives.
    removeAction is an action to call to stop sending events
    */
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
        ^[ { |action| this.action_( Some(action) ) }, { this.action_(None()) } ]
	}

    asENInput {
        ^EventNetwork.makeES( *this.actions )
    }

}

FRPGUICode {

	*makeENInput { |gui|
		var addHandler;
		var es = Var(gui.value);
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

/*+ SCView {

	asENInput {
		^FRPGUICode.makeENInput(this)
	}

}*/

+ MKtlElement {

	asENInput {
        var es = Var(0.0);
		var func = { |v| es.value_(v) };
		var internalES = this.eventSource;
		var addHandler = IO{ internalES.do(func); IO{ internalES.stopDoing(func) } };
		^Writer( es, Tuple3([addHandler],[],[]) )
	}

}