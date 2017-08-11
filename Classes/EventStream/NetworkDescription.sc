/*
    FP Quark
    Copyright 2012 - 2017 Miguel Negrão.

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
	classvar <makeFRPGraphAnalysis;
	var
	<actuate, //IO[Unit]
	<pause, //IO[Unit]
	<finalIOES,
	<active = false,
	<frpGraphAnalysis;

	*initClass{
		/*
		type returned by makeFRPGraphRep
		Tupple(
		  1: class of object,
		  2: it has state | Some(T(o.state, o.pureFunc))
		  3: it is signal | Some(o.now),
		  4: list of next elements.
		)
		*/
		//recursion using methods has the potential for not working well...
		var f = { |o|
			//o.class.asString.postln;
			//o.pureFunc.collect{ |x| x.def.sourceCode.postln };
			T(  o.class.asString,
				if([ChildEventSource, ChildFPSignal].any{ |x| o.isKindOf(x) }){
					//"%: Some(T(o.state, o.pureFunc)) = Some(T(%, %)) : %".format(o.class.asString, o.state, o.pureFunc, o).postln;
					Some(T(o.state, o.pureFunc))
				}{
					None()
				},
				if( o.isKindOf(ChildFPSignal) || [HoldFPSignal, ApplyFPSignal, Val].includes(o.class) ){Some(o.now)}{None()},
				o.nodes.collect{ |n|
					f.(n)
				}
			)
		};
		makeFRPGraphAnalysis = f;
	}

	//networkDescription : Writer( Unit, Tuple3([IO(IO())], [EventStream (IO ())], [Signal] ) )
	//                                   eventHandlers       reactimates              signals with IO for reactimate
	//                                                                                when starting the network the actions stored in the now
	//                                                                                variable of these signals is executed once for initialization
	//                                                                                purposes
	*new{ |networkDescription, disableOnCmdPeriod = false, runSignalReactimatesOnce = true|
		var return = super.newCopyArgs( *this.prMakeActions(networkDescription, disableOnCmdPeriod, runSignalReactimatesOnce) ).init;

		if( disableOnCmdPeriod ) {
			CmdPeriod.add(return)
		};

		^return
	}

	*prMakeActions{ |networkDescription, disableOnCmdPeriod, runSignalReactimatesOnce|
		var tuple = if(networkDescription.class == Writer){
			networkDescription.w
		}{
			if(networkDescription.class == ENImperEval){
				networkDescription.resultWriter.w
			} {
				Error("EventNetwork: networkDescription must be either of class Writer or ENImperEval").throw
			}
		};

		//reactimates
		var finalIOES = tuple.at2.mreduce(EventSource);
		var f = { |v| v.unsafePerformIO };
		var doFinalIO = IO{ finalIOES.do(f) };
		var stopDoingFinalIO = IO{ finalIOES.stopDoing(f) };
		//run the action to get list of ios to run after compile
		var runSignalReactimatesOnceIO = if(runSignalReactimatesOnce){IO{ tuple.at3.do{ |fpsignal| fpsignal.now.unsafePerformIO }}}{IO{}};

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

		var actuate = doFinalIO >>=| registerIO >>=| runSignalReactimatesOnceIO;
        var pause = stopDoingFinalIO >>=| unregisterIO;
		^[actuate, pause, finalIOES]
	}

	init{
		frpGraphAnalysis = EventNetwork.makeFRPGraphAnalysis.(finalIOES);
	}

	change{ |networkDescription, disableOnCmdPeriod = false, runSignalReactimatesOnce = true|
		var check, shapeMatches, functionsMatch;
		var newActuate, newPause, newFinalIOES;
		var checkFRPSameGraph, putFRPState, makeFRPGraphAnalysis;

		//  returns T( shapeMatches, functionsMatch)
		checkFRPSameGraph = { |o, rep|

			if( o.class.asString != rep.at1 ) {
				//"% % classes don't match ".format(o.class.asString,rep.at1).postln;
				T(false,false)
			}{
				if( o.nodes.size != rep.at4.size ) {
					//"% size doesn't match".format(o.class.asString).postln;
					T(false,false)
				}{
					var storedFunc = rep.at2.collect(_.at2).monjoin; //T(_,Some(T(o.state, o.pureFunc)),_)
					var check = { |a,b| a.copy.removeEvery(" \n\t") == b.copy.removeEvery(" \n\t") }.lift
					.(
						o.pureFunc.collect{ |x| /*"o: %- o.class: % - o.pureFunc: %".format(o, o.class, x.class).postln; */
							x.def.sourceCode.asOption
						}.monjoin,
						storedFunc.collect{ |x| /*"storedFunc: %".format(x.class).postln; */
							x.def.sourceCode.asOption
						}.monjoin
					).getOrElse(true);

					if( check.not ) {
						//"% functions don't match: \n% - %".format(o.class.asString,
						//	o.pureFunc.collect({|x| x.def.sourceCode }), storedFunc.collect({|x| x.def.sourceCode })).postln;
						T(true,false)
					}{
						//"% functions match !!: \n% - %".format(o.class.asString,
						//	o.pureFunc.collect({|x| x.def.sourceCode }), storedFunc.collect({|x| x.def.sourceCode })).postln;
						if(o.nodes.size > 0 ){
							[o.nodes, rep.at4].flopWith{ |a,b|
								checkFRPSameGraph.(a,b)
							}.reduce({ |a,b| T(a.at1 && b.at1, a.at2 && b.at2) })
						}{
							T(true,true)
						}
					}
				}
			}
		};

		putFRPState = { |o, rep|
			//"putFRPState - start o: % rep:%".format(o, rep).postln;
			rep.at2.collect{ |oldState|
				//"Setting state to % in object %".format(oldState.at1, o).postln;
				o.state = oldState.at1
			};
			rep.at3.collect{ |oldNow|
				//"Setting now to % in object % with hash %" .format(oldNow, o, o.hash).postln;
				o.now = oldNow
			};
			[o.nodes, rep.at4].flopWith{ |o2,rep2|
				putFRPState.(o2,rep2)
			}
		};

		#newActuate, newPause, newFinalIOES = EventNetwork.prMakeActions(networkDescription, disableOnCmdPeriod, runSignalReactimatesOnce);

		frpGraphAnalysis = EventNetwork.makeFRPGraphAnalysis.(finalIOES);
		check = checkFRPSameGraph.( newFinalIOES, frpGraphAnalysis);
		shapeMatches = check.at1;
		functionsMatch = check.at2; //not used
		//stop old network
		if( this.active ) { pause.unsafePerformIO };
		//if topology matches put in state from old network
		if( shapeMatches ) {
			putFRPState.(newFinalIOES, frpGraphAnalysis);
		};
		//store data for new network
		actuate = newActuate;
		pause = newPause;
		finalIOES = newFinalIOES;
		frpGraphAnalysis = makeFRPGraphAnalysis.(newFinalIOES);
		//start new network
		if( this.active ) { actuate.unsafePerformIO };
	}

	start {
		if(active.not) {
			actuate.unsafePerformIO
		};
		active = true;
	}
	stop {
		if(active) {
			pause.unsafePerformIO
		};
		active = false;
	}

	cmdPeriod { this.pauseNow }

	//pick your favorite syntax:
	run { |bool|
		if( bool ) {
			this.actuateNow
		} {
			this.pauseNow
		}
	}

	//Helper funcs
	*returnDesc { |a| ^Writer( a, Tuple3([],[],[]) ) }
    *returnUnit { ^this.returnDesc(Unit) }
	*makePure { |a| ^this.returnDesc(a) }

/*
Given two functions (addAction, removeAction) which given a call-back register or deregister the call-back
returns an EventStream or FPSignal.
these two methods always call .value
*/
	*makeES { |addAction, removeAction|
        var addHandler;
		var es = EventSource();
		addHandler = IO{
			var action = { |o| es.fire( o.value ) };
			addAction.(action);
			IO{ removeAction.(action) }
		};
		^Writer( es, Tuple3([addHandler],[],[]) )
    }
	*makeSignal { |addAction, removeAction, currentValue|
        var addHandler;
		var signal = Var(currentValue);
		addHandler = IO{
			var action = { |o| signal.value_( o.value ) };
			addAction.(action);
			IO{ removeAction.(action) }
		};
		^Writer( signal, Tuple3([addHandler],[],[]) )
    }

/*
general mechanism to register call-back functions
	newAddHandler returns a registerFunction to be passed on to 'fromAddHandler' and a 'fire' action to be passed
	on to the event-loop call-back system.
*/
	*newAddHandler {
		//multiple handlers because multiple EventStreams can be created from this fire function.
		var handlers = List.new;
		//this is the function to be passed to the event-loop system.
		var fire = { |value|
			handlers.do{ |func|
				func.(value)
			}
		};
		var registerAction = { |actionFunc|
			IO{
				handlers.add(actionFunc);
				IO{ handlers.remove(actionFunc) }
			}
		};
		^T(registerAction, fire)
    }

	*fromAddHandler{ |registerAction|
		var es = EventSource();
		var action = { |v| es.fire( v ) };
		^Writer( es, Tuple3([registerAction.(action)],[],[]) )
    }

	*enFromAddHandler{ |registerAction|
		^ENImperEval.appendToResult( this.fromAddHandler(registerAction) );
	}

	*makeES2 {
		var tup = EventNetwork.newAddHandler();
		var registerAction = tup.at1;
		var fire = tup.at2;
		var es = EventNetwork.fromAddHandler(registerAction);
		^T(es, fire)
	}

	*makeSignal2 { |initialValue|
		var tup = this.makeES2();
		^tup.at1_(tup.at1.hold(initialValue))
	}

}