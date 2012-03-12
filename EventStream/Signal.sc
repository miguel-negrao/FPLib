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

	now { }

	change { } //returns EventStream

	do { |f|
		f.(this.now);
		this.change.do(f)
	}

	collect { |f|
        ^CollectedFPSignal(this,f)
    }

    flatCollect { |f, initialState|
        ^FlatCollectedFPSignal( this, f, initialState)
    }

    flatCollectR { |f, initialState|
        ^FlatCollectedFPSignalR( this, f, initialState)
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
    var <change;
    var <now;

    *new{ |initialState, initialFunc|
        ^super.new
    }

    initChildFPSignal { |p,h, initialState, initialFunc|
    	state = initialState;
    	now = initialFunc.( initialState );
        change = SignalChangeES();
        parent = p;
        handler = h;
        listenerFunc = { |value|
         	//("listnerFunc called with value: "++value).postln;
        	state = handler.value(value, state)
        };
        parent.change.addListener( listenerFunc )
    }
    
    remove {
		parent.change.removeListener( listenerFunc )
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
            change.fire( x );
        }, Unit, { f.(parent.now) })
    }
}

FlatCollectedFPSignal : ChildFPSignal {

    *new { |parent, f|
        ^super.new.init(parent, f)
    }

    init { |parent, f|
        var thunk = { |x|
        	//("thunk was called with event "++x).postln;
            now = x;
            change.fire( x );
        };
        var initialState = f.(parent.now);
        initialState.change !? _.addListener(thunk);
        this.initChildFPSignal(parent, { |event, lastFPSignal|
             var nextFPSignal;
             //("handler called with event "++event++" and state "++state).postln;
             lastFPSignal.change !? _.removeListener( thunk );
             nextFPSignal = f.(event);
             thunk.( nextFPSignal.now );
             nextFPSignal.change !? _.addListener( thunk );
             nextFPSignal
        }, initialState, _.now)
    }
}

FlatCollectedFPSignalR : ChildFPSignal {

    *new { |parent, f|
        ^super.new.init(parent, f)
    }

    init { |parent, f|
        var thunk = { |x|
        	//("thunk was called with event "++x).postln;
            now = x;
            change.fire( x );
        };
        var initialState = f.(parent.now);
        initialState.change !? _.addListener(thunk);
        this.initChildFPSignal(parent, { |event, lastFPSignal|
             var nextFPSignal;
             //("handler called with event "++event++" and state "++state).postln;
             lastFPSignal.change !? _.removeListener( thunk );
             lastFPSignal.tryPerform(\remove);
             nextFPSignal = f.(event);
             thunk.( nextFPSignal.now );
             nextFPSignal.change !? _.addListener( thunk );
             nextFPSignal
        }, initialState, _.now)
    }
}

//A signal that never changes, and therefore never fires anything;
Val : FPSignal {
	var <now;
	var <change;

	*new { |now| ^super.newCopyArgs(now).init }

	init {
		change = EventSource();
	}
}

Var : Val {

    value { ^now }

    value_ { |v|
    	now = v;
    	change.fire(v);
    }

}