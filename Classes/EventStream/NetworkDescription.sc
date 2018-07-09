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
	classvar <checkFRPSameGraph;
	classvar <putFRPState;

	var
	<actuate, //IO[Unit]
	<pause, //IO[Unit]
	<finalIOES,
	<active = false;

	*initClass{
		var f,g,h;
		//recursion using methods has the potential for not working well...

		/*
		type returned by makeFRPGraphRep
		T(
		1: Tupple(
		  1: class of object,
		  2: it has state | Some(T(o.state, o.pureFunc))
		  3: it is signal | Some(o.now),
		  4: list of next elements,
		),
		2: list of hashes seen so far
		)
		*/
		f = { |o, previousHashList|
			var newHashList;
			var parentsResult;
			if (previousHashList.isNil) { previousHashList = List.new };
			newHashList = previousHashList.add(o.hash);
			/*
			the hashes of all objects already see is kept as state
			such that the same object will not be processed twice
			*/
			parentsResult = o.nodes.inject( T(List.new, newHashList), { |t, n|
				var hashList = t.at2;
				var res, hashList2;
				if (hashList.includes(n.hash).not) {
					res = f.(n, hashList);
					hashList2 = res.at2;
					T(t.at1.add(res.at1), hashList2)
				} {
					//"not processing % because already seen".format(n.class).postln;
					t
				}
			});
			T(
				T(  o.class.asString,
					if([FoldedES, FoldedFES, FoldedFPSignal].any{ |x| o.class == x }){
						//"%: Some(T(o.state, o.pureFunc)) = Some(T(%, %)) : %".format(o.class.asString, o.state, o.pureFunc, o).postln;
						Some(T(o.state, o.pureFunc))
					}{
						None()
					},
					if( o.isKindOf(ChildFPSignal) || [HoldFPSignal, ApplyFPSignal, Val].includes(o.class) ){Some(o.now)}{None()},
					parentsResult.at1, //rest of the tree
				),
				parentsResult.at2
			)
		};
		makeFRPGraphAnalysis = f;

		g = { |a, b|
			if( a.at1 != b.at1 ) {
				//"% % classes don't match ".format(a.at1, b.at1).postln;
				false
			}{
				if( a.at4.size != b.at4.size ) {
					//"% size doesn't match".format(a.at1).postln;
					false
				}{
					if(a.at4.size > 0 ) {
						[a.at4, b.at4].flopWith{ |aa,bb|
							g.(aa,bb)
						}.reduce({ |aa,bb| aa && bb })
					}{
						true
					}

				}
			}
		};
		checkFRPSameGraph = g;

		h = { |o, rep, previousHashList|
			var newHashList;
			var parentsResult;
			if (previousHashList.isNil) { previousHashList = List.new };
			newHashList = previousHashList.add(o.hash);

			//"putFRPState - start o: % rep:%".format(o, rep).postln;
			rep.at2.collect{ |oldState|
				//"Setting state to % in object %".format(oldState.at1, o).postln;
				o.state = oldState.at1
			};
			rep.at3.collect{ |oldNow|
				//"Setting now to % in object % with hash %" .format(oldNow, o, o.hash).postln;
				o.now = oldNow
			};
			/*
			the hashes of all objects already see is kept as state
			such that the same object will not be processed twice
			*/
			[o.nodes, rep.at4].flop.inject( newHashList, { |hashList, n|
				var o2 = n[0];
				var rep2 = n[1];

				if (hashList.includes(o2.hash).not) {
					h.(o2, rep2, hashList);
				} {
					//"not processing % because already seen".format(o2.class).postln;
					hashList
				}
			});
		};
		putFRPState = h;
	}

	//networkDescription : Writer( Unit, Tuple3([IO(IO())], [EventStream (IO ())], [Signal] ) )
	//                                   eventHandlers       reactimates              signals with IO for reactimate
	//                                                                                when starting the network the actions stored in the now
	//                                                                                variable of these signals is executed once for initialization
	//                                                                                purposes
	*new{ |networkDescription, disableOnCmdPeriod = false, runSignalReactimatesOnce = true|
		var return = super.newCopyArgs( *this.prMakeActions(networkDescription, disableOnCmdPeriod, runSignalReactimatesOnce) );

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

	change{ |networkDescription, disableOnCmdPeriod = false, runSignalReactimatesOnce = true|
		var shapeMatches;
		var newActuate, newPause, newFinalIOES;
		var graphAnalysisPrevious, graphAnalysisNext;

		#newActuate, newPause, newFinalIOES = EventNetwork.prMakeActions(networkDescription, disableOnCmdPeriod, runSignalReactimatesOnce);

		//stop old network
		if( this.active ) { pause.unsafePerformIO };

		graphAnalysisPrevious = EventNetwork.makeFRPGraphAnalysis.(finalIOES).at1;
		graphAnalysisNext = EventNetwork.makeFRPGraphAnalysis.(newFinalIOES).at1;
		shapeMatches = EventNetwork.checkFRPSameGraph.( graphAnalysisNext, graphAnalysisPrevious);

		//if shape matches put in state from old network
		if( shapeMatches ) {
			//"Shape of FRP network matches, putting back old state".postln;
			EventNetwork.putFRPState.(newFinalIOES, graphAnalysisPrevious);
		};

		//store data for new network
		actuate = newActuate;
		pause = newPause;
		finalIOES = newFinalIOES;

		//start new network
		if( this.active ) { actuate.unsafePerformIO };
	}

	getFRPGraphAnalysis {
		 ^EventNetwork.makeFRPGraphAnalysis.(finalIOES)
	}

	start {
		if(active.not) {
			actuate.unsafePerformIO;
			active = true;
		};
	}
	stop {
		if(active) {
			pause.unsafePerformIO;
			active = false;
		};
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
