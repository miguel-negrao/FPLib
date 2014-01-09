/*
   Functional Reactive Programming
   based on reactive-core
   http://www.reactive-web.co.cc/

   ////////////////////////////////////////////////////////////////////////////////////////////////////////
   Original license:
   https://github.com/nafg/reactive/blob/master/LICENSE.txt
   ////////////////////////////////////////////////////////////////////////////////////////////////////////
   Note, this is a draft and may be changed at any time.

   You may use this software under the following conditions:
   A. You must not use it in any way that encourages transgression of the Seven Noahide Laws (as defined by traditional Judaism; http://en.wikipedia.org/wiki/Seven_Laws_of_Noah is pretty good). They are:
     1. Theft
     2. Murder
     3. Adultery
     4. Polytheism
     5. Cruelty to animals (eating a limb of a living animal)
     6. Cursing G-d
   And they require a fair judicial system.

   B. You must not use it in any way that transgresses the Apache Software License.
   ////////////////////////////////////////////////////////////////////////////////////////////////////////

   translated to SuperCollider by Miguel Negrão.
*/


FPSignal {

	classvar <>buildFlatCollect;

	*new {
	   ^super.new.initFPSignal
	}

	initFPSignal {
		FPSignal.buildFlatCollect = FPSignal.buildFlatCollect.collect( _.orElse( Some(this) ) );
	}

	asFPSignal {
		^this
	}

	now { }

	changes { } //returns EventStream

	do { |f|
		f.(this.now);
		^this.changes.do(f);
	}

	doDef { |name, f|
		f.(this.now);
		^this.changes.doDef(name, f)
	}

	stopDoing { |f|
		^this.changes.stopDoing(f);
	}

	connect { |object|
		this.do{ |v| defer{ object.value_(v) } };
       ^Unit
	}

	reset {
		^this.changes.reset;
	}

	remove {
		^this.changes.remove;
	}

    inject { |init, f|
    	^FoldedFPSignal( this, init, f)
    }

	takeWhile { |f|
        ^TakeWhileFPSignal(this,f)
    }

    //audio
    bus { |server|
    	^this.changes.bus( server, this.now )
    }

    //EventNetwork related
    connectEN{ |object|
    	^this.collect{ |v| IO{ defer{ object.value_(v) } } }.reactimate;
    }

    reactimate{ //this stream should returns IOs
		^Writer( Unit, Tuple3([],[this.changes],[this.now]) )
	}

	asENInput {
		^Writer(this, Tuple3([],[],[]) )
	}

    debug { |string|
        ^this.collect{ |x| putStrLn(string++" : "++x) }.reactimate;
    }

    //utilities
    storePrevious {
        ^this.inject( Tuple2(0.0,0.0), { |state,x| Tuple2( state.at2, x ) })
    }

    storePreviousWithT {
        ^this.inject( Tuple2( Tuple2(0.0,0.0), Tuple2(0.0,0.0) ),
            { |state,x| Tuple2( state.at2, Tuple2( Process.elapsedTime, x ) ) })
    }

    storeWithT {
        ^this.collect( Tuple2(Process.elapsedTime,_) )
    }

    changed {
		^this.storePrevious.collect{ |tup| tup.at1 != tup.at2 }
    }

    //time related signal methods

	//


    //For these methods this should be signal with the time value
    integral { |tsig|
        var delta = tsig.delta;
        var inc =  (_*_) <%> this <@> delta.changes; //only updates when tsig updates
        ^inc.inject(0, {|state, inc|
            state + inc
        }).hold(0.0)
    }

	//to be used on time signal
	delta {
		^this.storePrevious.collect{ |tup| tup.at2-tup.at1 };
	}

	//to be used on time signal
    changeRate { |rateSig|
        ^rateSig.integral(this)
    }

	//to be used on time signal
    line{ |start, end, dur|

        ^this.collect{ |t|
            var x = (t/dur).min(1.0).max(0.0);
            ((1-x)*start) + (x*end)
        }
    }

	//to be used on time signal
    xline{ |start, end, dur|

        ^this.collect{ |t|
            var x = (t/dur).min(1.0).max(0.0);
            var ratio = end/start;
            (ratio ** x) * start
        }
    }

	//to be used on a time signal
    elapsedTime {
         ^this.delta.inject( 0, { |elapsedT, delta|
            elapsedT + delta
		})
    }


	//behaves like this signal until switchtime
	//from then on behaves like laterSig
	switchLater { |tSig, laterSig, switchTime|
		//need to identify the first node in the chain to remove it later
		var r = tSig
		.inject( Tuple2(0.0,0.0), { |state,x| Tuple2( state.at2, x ) });

		^r.collect{ |tup| tup.at2-tup.at1 }
		.inject( 0, { |elapsedT, delta|
            elapsedT + delta
		})
		//\counting, \over, \done
		.inject(\counting, { |st,x|
			switch(st)
			{\counting} {
				if(x>switchTime){\over}{\counting}
			}
			{ \over }{ \done }
			{ \done } { \done }
		})
		.changes
		.select( _  == \over )
		.switchSig({
			//I'm going to ask for forgivness and do a side-effect here...
			//since this is a one time only event we don't need to do all the
			//processing after this point
			r.remove;
			laterSig
		}, this)

	}


    //*************************

    linlin { |inMin, inMax, outMin, outMax, clip=\minmax|
        ^this.collect( _.linlin(inMin, inMax, outMin, outMax, clip) )
    }

    linexp { |inMin, inMax, outMin, outMax, clip=\minmax|
        ^this.collect( _.linexp(inMin, inMax, outMin, outMax, clip) )
    }

    nlin { |outMin, outMax, clip=\minmax|
        ^this.collect( _.linlin(0.0, 1.0, outMin, outMax, clip) )
    }

    nexp { |outMin, outMax, clip=\minmax|
        ^this.collect( _.linexp(0.0, 1.0, outMin, outMax, clip) )
    }

    explin { |inMin, inMax, outMin, outMax, clip=\minmax|
        ^this.collect( _.explin(inMin, inMax, outMin, outMax, clip) )
    }

    expexp { |inMin, inMax, outMin, outMax, clip=\minmax|
        ^this.collect( _.expexp(inMin, inMax, outMin, outMax, clip) )
    }

    lincurve { |inMin = 0, inMax = 1, outMin = 0, outMax = 1, curve = -4, clip = \minmax|
        ^this.collect( _.expexp(inMin, inMax, outMin, outMax, curve, clip) )
    }

    curvelin { |inMin = 0, inMax = 1, outMin = 0, outMax = 1, curve = -4, clip = \minmax|
        ^this.collect( _.expexp(inMin, inMax, outMin, outMax, curve, clip) )
    }

    + { |signal|
        ^(_+_) <%> this <*> signal.asFPSignal
    }

    * { |signal|
        ^(_*_) <%> this <*> signal.asFPSignal
    }

    / { |signal|
        ^(_/_) <%> this <*> signal.asFPSignal
    }

    - { |signal|
        ^(_-_) <%> this <*> signal.asFPSignal
    }

	//missing this combinator:
	//switchB :: forall t a. Behavior t a -> Event t (AnyMoment Behavior a) -> Behavior t a

//Functor
	collect { |f|
        ^CollectedFPSignal(this,f)
    }

//Aplicative Functor
    //apply signals to signals
    <*> { |fasignal|
		var fnow = this.now;
		var fasignalnow = fasignal.now;
		^ApplySignalES(this.changes, fasignal.changes, fnow, fasignalnow).hold( fnow.(fasignalnow) )
    }

    //apply time-varying function to EventSources
    <@> { |es|
        //apply a signal with a function to every incoming event
        ^es.collect{ |x| this.now.value(x) }
    }

     <@ { |es|
        //apply a signal with a function to every incoming event
        ^es.collect{ |x| this.now }
    }

	//alias
	sampleOn { |es|
        //apply a signal with a function to every incoming event
        ^es.collect{ |x| this.now }
    }


    switch { |f, initialState|
        ^FlatCollectedFPSignal( this, f, initialState)
    }

	selfSwitch { |f|
        ^SelfSwitchFPSignal( f, this)
    }

}

SignalChangeES : EventSource {
	var ref;

	*new { |handler| ^super.new.initSignalChange(handler) }

    initSignalChange { |handler|
    	ref = handler
    }
}

ChildFPSignal : FPSignal {
    var <state;
    var <parent;
    var <listenerFunc;
    var <handler; //: (T, S) => S
    var <changes;
    var <now;

    *new{ |initialState, initialFunc|
        ^super.new
    }

    initChildFPSignal { |p,h, initialState, initialFunc|
    	state = initialState;
    	now = initialFunc.( initialState );
        changes = SignalChangeES();
        parent = p;
        handler = h;
        listenerFunc = { |value|
         	//("listnerFunc called with value: "++value).postln;
        	state = handler.value(value, state)
        };
        parent.changes.addListener( listenerFunc )
    }

    remove {
		parent.changes.removeListener( listenerFunc );
		^Unit
    }

}


CollectedFPSignal : ChildFPSignal {

    *new { |parent, f|
        ^super.new.init(parent, f)
    }

    init { |parent, f|
        this.initChildFPSignal(parent, { |event|
            var x = f.(event);
            now = x;
            changes.fire( x );
        }, Unit, { f.(parent.now) })
    }
}

FlatCollectedFPSignal : ChildFPSignal {

    *new { |parent, f, initialSignal|
        ^super.new.init(parent, f, initialSignal)
    }

    init { |parent, f, initialSignalArg|
    	var initSignal, initialState;
        var thunk = { |x|
        	//("thunk was called with event "++x).postln;
            now = x;
            changes.fire( x );
        };
        if(initialSignalArg.isNil) {
			//get first signal from the switcher signal
			FPSignal.buildFlatCollect = Some(None());
			initSignal = f.(parent.now);
			initialState = Tuple2(FPSignal.buildFlatCollect.get,initSignal);
			FPSignal.buildFlatCollect = None();
		} {
			//or use the provided first signal
			initSignal = initialSignalArg;
			initialState = Tuple2(None(),initSignal);
		};
        initSignal.changes !? _.addListener(thunk);
        this.initChildFPSignal(parent, { |event, tuple|
             var lastSigStart, lastSigEnd, nextSigStart, nextSigEnd;
             //start of the old created chain
             lastSigStart = tuple.at1;
             //end of old created chain
             lastSigEnd = tuple.at2;
             //stop receiving events from old chain
             lastSigEnd.changes !? _.removeListener( thunk );
              //disconnect the old chain from it's start point
             lastSigStart.do{ |x| x.tryPerform(\remove) };
             //let's discover where the new chain starts
             FPSignal.buildFlatCollect = Some(None());
             nextSigEnd = f.(event);
             //if a new EventSource was created the first one created will be here:
             nextSigStart = FPSignal.buildFlatCollect.get;
             //reset the global variable
             FPSignal.buildFlatCollect = None();
             thunk.( nextSigEnd.now );
             //start receiving events from new EventStream
             nextSigEnd.changes !? _.addListener( thunk );
             //store the new chain
             Tuple2(nextSigStart, nextSigEnd);

        }, initialState, { |x| x.at2.now })
    }
}

SelfSwitchFPSignal : ChildFPSignal {

	*new { |f, initialSignal|
		^super.new.init(f, initialSignal)
	}

	init { |f, initSignal|
		var initialState, register, thunk;

		initialState = Tuple2(None(),initSignal);

		state = initialState;

		now = initSignal.now.at1;

		changes = SignalChangeES();

		parent = initSignal;

		//events to path through to our listeners
		thunk = { |x|
			//("thunk was called with event "++x).postln;
			//first element of tuple carries the actual value
			var value = x.at1;

		};

		//react to event switch
		handler = { |event, tuple|
			var lastSigStart, lastSigEnd, nextSigStart, nextSigEnd;

			//start of the old created chain
			lastSigStart = tuple.at1;

			//end of old created chain
			lastSigEnd = tuple.at2;

			//stop receiving events from old chain
			lastSigEnd.changes !? _.removeListener( thunk );
			lastSigEnd.changes !? _.removeListener( listenerFunc );

			//disconnect the old chain from it's start point
			lastSigStart.do{ |x| x.tryPerform(\remove) };

			//let's discover where the new chain starts
			FPSignal.buildFlatCollect = Some(None());
			nextSigEnd = f.(event);
			//if a new EventSource was created the first one created will be here:
			nextSigStart = FPSignal.buildFlatCollect.get;
			//reset the global variable
			FPSignal.buildFlatCollect = None();

			register.( nextSigEnd );

			parent = nextSigEnd;
			now = nextSigEnd.now.at1;
			changes.fire( now );

			//store the new chain
			Tuple2(nextSigStart, nextSigEnd);

		};
		listenerFunc = { |value|
			//("listnerFunc called with value: "++value).postln;
			var actualVal;
			var switchEvent = value.at2;
			if(switchEvent.isDefined) {
				state = handler.value(switchEvent.get, state);
			} {
				actualVal = value.at1;
				now = actualVal;
				changes.fire( actualVal );
			};

		};
		register = { |signal|
			signal.changes.addListener( listenerFunc );
		};
		register.( initSignal );

	}
}


//the thing causing the switching is an event source instead of signal
//initial signal must be provided !
FlatCollectedFPSignalHybrid : ChildFPSignal {

    *new { |parent, f, initialSignal|
        ^super.new.init(parent, f, initialSignal)
    }

    init { |parent, f, initialSignalArg|
    	var initSignal, initialState;
        var thunk = { |x|
        	//("thunk was called with event "++x).postln;
            now = x;
            changes.fire( x );
        };
		initSignal = initialSignalArg;
		initialState = Tuple2(None(),initSignal);

        initSignal.changes !? _.addListener(thunk);
        this.initChildFPSignal(parent, { |event, tuple|
             var lastSigStart, lastSigEnd, nextSigStart, nextSigEnd;
             //start of the old created chain
             lastSigStart = tuple.at1;
             //end of old created chain
             lastSigEnd = tuple.at2;
             //stop receiving events from old chain
             lastSigEnd.changes !? _.removeListener( thunk );
              //disconnect the old chain from it's start point
             lastSigStart.do{ |x| x.tryPerform(\remove) };
             //let's discover where the new chain starts
             FPSignal.buildFlatCollect = Some(None());
             nextSigEnd = f.(event);
             //if a new EventSource was created the first one created will be here:
             nextSigStart = FPSignal.buildFlatCollect.get;
             //reset the global variable
             FPSignal.buildFlatCollect = None();
             thunk.( nextSigEnd.now );
             //start receiving events from new EventStream
             nextSigEnd.changes !? _.addListener( thunk );
             //store the new chain
             Tuple2(nextSigStart, nextSigEnd);

        }, initialState, { |x| x.at2.now })
    }

	initChildFPSignal { |p,h, initialState, initialFunc|
    	state = initialState;
    	now = initialFunc.( initialState );
        changes = SignalChangeES();
        parent = p;
        handler = h;
        listenerFunc = { |value|
         	//("listnerFunc called with value: "++value).postln;
        	state = handler.value(value, state)
        };
        parent.addListener( listenerFunc )
    }

    remove {
		parent.removeListener( listenerFunc );
		^Unit
    }
}

FoldedFPSignal : ChildFPSignal {

    *new { |parent, initial, f|
        ^super.new.init(parent, initial, f)
    }

    init { |parent, initial, f|
    	var initfold = f.(initial, parent.now );
        this.initChildFPSignal(parent, { |event, state|
            var next = f.(state, event);
            now = next;
            changes.fire( next );
            next
        }, initfold, { |x| x })
    }
}

TakeWhileFPSignal : ChildFPSignal {

    *new { |parent, f|
		var pnow = parent.now;
		^if( f.(pnow) ) {
			super.new.init(parent, f)
		} {
			Val(pnow)
		}
    }

    init { |parent, f|
        this.initChildFPSignal(parent, { |event|
             if( f.(event) ) {
                changes.fire( event )
            } {
                this.remove;
            }
        }, Unit, { parent.now })
    }
}

//A signal that never changes, and therefore never fires anything;
Val : FPSignal {
	var <now;
	var <changes;

	*new { |now| ^super.newCopyArgs(now).init }

	init {
		changes = EventSource();
	}
}

Var : Val {

    value { ^now }

    value_ { |v|
    	now = v;
    	changes.fire(v);
    	^Unit
    }

	//GUI additions

	makeSlider{ |minval=0.0, maxval=1.0, warp='lin', step=0.0, default|
		var spec = [minval, maxval, warp, step, default].asSpec;
		var slider = Slider(nil, Rect(100,100,50,100) );
		slider.action_{ |sl| this.value_(spec.map(sl.value)) };
		slider.value_(spec.unmap(this.value));
		slider.front;
		^slider
	}

	getSlider{ |minval=0.0, maxval=1.0, warp='lin', step=0.0, default|
		var spec = [minval, maxval, warp, step, default].asSpec;
		var slider = Slider(nil, Rect(100,100,50,100) );
		slider.action_{ |sl| this.value_(spec.map(sl.value)) };
		slider.value_(spec.unmap(this.value));
		^slider
	}

}

+ Object {

	asFPSignal {
		^Val( this )
	}

}

+ Function {

	selfSwitch { |initArg|
		^this.value(initArg).selfSwitch(this)
	}

}