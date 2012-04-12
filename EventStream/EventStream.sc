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

   translated to SuperCollider by Miguel Negrao.
*/

EventStream{

	classvar <doFuncs;
	classvar <>debug = false;
	classvar <>buildFlatCollect;

	*initClass {
		Class.initClassTree(TypeClasses);
		//type instances declarations:

		TypeClasses.addInstance(EventSource,
			(
				'fmap': { |fa,f| fa.collect(f) },
				'bind' : { |fa,f| fa.flatCollect(f) }
			);
		);

		doFuncs = Dictionary.new;
		buildFlatCollect = [];
	}

}

EventSource : EventStream {
	classvar <eventStreamTemplateFolders;
    var <listeners;

    *initClass{
		eventStreamTemplateFolders =
			[this.filenameSymbol.asString.dirname.dirname +/+ "EventStreamTemplates",
			Platform.userAppSupportDir++"/Extensions/EventStreamTemplates/"];
	}

	doesNotUnderstand{ |selector ...args|
		var template = EventSource.getEventSourceTemplate(selector);
		^if( template.notNil ) {
			template[\func].value(this, *args)
		} {
			super.doesNotUnderstand(selector,*args)
		}
	}

	*cleanTemplateName{ |name|
		^name.asString.collect { |char| if (char.isAlphaNum, char, $_) };
	}

	*getTemplateFilePaths{ |templateName|
		var cleanTemplateName = this.cleanTemplateName(templateName);
		^eventStreamTemplateFolders.collect({|x| x +/+ cleanTemplateName ++ ".scd"});
	}

	*getEventSourceTemplate{ arg name;
		var path;
		this.getTemplateFilePaths(name).do{ |testpath|
			if( File.exists(testpath) ) {
				path = testpath;
			}
		};
		^if( name.notNil and: path.notNil ) {
			path.load
		} {
			"//" + this.class ++ ": - no EventSource template found for %: please make them!\n"
			.postf( this.cleanTemplateName(name) );
			("Templates should be placed at "++Platform.userAppSupportDir++"/Extensions/EventSourceTemplates/").postln;
			nil
		}
	}

	*availableTemplates{
		^EventSource.eventStreamTemplateFolders.collect{ |x|
			x.getPathsInDirectory.collect{ |y|
				y.removeExtension
			}
		}.flatten
	}

    *new{
        ^super.new.initEventSource
    }

    //private
    initEventSource {
    	var lastIndex = EventStream.buildFlatCollect.size-1;
    	if( (lastIndex != -1) and: {EventStream.buildFlatCollect[lastIndex].isEmpty}) {
    		EventStream.buildFlatCollect[lastIndex] = Some(this)
    	};
        listeners = [];
    }

    addListener { |f| listeners = listeners ++ [f]; ^Unit }

    removeListener { |f| listeners.remove(f); ^Unit }
    
    reset { listeners = []; ^Unit }

	//private
	proc { |initialState, f|
		^ChildEventSource( initialState ).initChildEventSource(this,f)
	}

    collect { |f|
        ^CollectedES(this,f)
    }

    select { |f|
        ^SelectedES(this, f)
    }

    fold { |initial, f|
        ^FoldedES(this, initial, f);
    }

    flatCollect { |f, initialState|
        ^FlatCollectedES( this, f, initialState)
    }

    | { |otherES|
        ^MergedES( this, otherES )
    }

    merge { |otherES|
    	^MergedES( this, otherES )
    }

    takeWhile { |f|
        ^TakeWhileES( this, f)
    }

    do { |f|
        this.addListener(f);
        ^Unit
    }

    doDef { |name, f|
		var dict = EventStream.doFuncs;
		f !? {
			var key = [ this, name ];
			this.do(f);
			dict.at( key ) !? { |oldf| this.stopDoing( oldf ) };
			dict.put( key, f)
		} ?? {
			var key = [ this, name ];
			dict.at( key ) !? { |oldf| this.stopDoing( oldf ) };
			dict.put( key , nil)
		};
		^Unit
    }

	stopDoing { |f|
		this.removeListener(f);
		^Unit
	}

    fire { |event|
	    //("running fire "++event++" "++this.hash).postln;
	    //copy is used here because listerFuncs might mutate the listeners variable
	    //change this to a FingerTree in the future.
        listeners.copy.do( _.value(event) );
        if(EventStream.debug) { postln("-> "++this++" : "++event)};
        ^Unit
    }

    //connect to an object that responds to .value_
    connect{ |object|
    	this.do{ |v| defer{ object.value_(v) } };
    	^Unit
    }

	//returns the corresponding signal
    hold { |initialValue|
    	^HoldFPSignal(this, initialValue)
    }

    remove { ^Unit }

    bus { |server, initVal = 0.0|
		server = server ?? {Server.default};
		if( server.serverRunning ) {
			var bus = Bus.control(server, initVal.asArray.size);
			var f = { |x| bus.setn( x.asArray ) };
			this.do(f);
			bus.setn( initVal.asArray );
			^Some( Tuple2( bus, f) )
		} {
			^None
		}
	}

}

NothingES : EventSource {
	fire { ^Unit }
}

HoldFPSignal : FPSignal {
	var <now;
	var <changes;
	var <listener;

	*new { |eventStream, initialValue|
		^super.new.init(eventStream, initialValue)
	}

	init { |eventStream, initialValue|
		changes = eventStream;
		now = initialValue;
		listener = { |v| now = v};
		eventStream.addListener( listener )
	}

}

ChildEventSource : EventSource {
    var <state;
    var <parent;
    var <listenerFunc;
    var <handler; //: (T, S) => S

    *new{ |initialState|
        ^super.new.initState( initialState )
    }

    initState{ |initialState|
        state = initialState;
    }

	//private
    initChildEventSource { |p,h, initialFunc|
        parent = p;
        handler = h;
        listenerFunc = { |value|
        	//("doing listener func of "++this.hash).postln;
        	state = handler.value(value, state)
        };
        parent.addListener(listenerFunc)
    }

    remove {
        parent.removeListener( listenerFunc ); ^Unit
    }

}

CollectedES : ChildEventSource {

    *new { |parent, f|
        ^super.new.init(parent, f)
    }

    init { |parent, f|
        this.initChildEventSource(parent, { |event|
            this.fire( f.(event) );
        })
    }
}

SelectedES : ChildEventSource {

    *new { |parent, f|
        ^super.new.init(parent, f)
    }

    init { |parent, f|
        this.initChildEventSource(parent, { |event|
             if( f.(event) ) {
                 this.fire( event )
             }
        })
    }
}

FoldedES : ChildEventSource {

    *new { |parent, initial, f|
        ^super.new(initial).init(parent, f)
    }

    init { |parent, f|
        this.initChildEventSource(parent, { |event, state|
             var next = f.(state, event);
             this.fire( next );
             next
        })
    }
}

FlatCollectedES : ChildEventSource {

	var thunk;

    *new { |parent, f, init|
        ^super.new(Tuple2(None,init)).init(parent, f)
    }

    init { |parent, f|
        thunk = { |x|
         	//"firing the FlatCollectedES".postln;
         	this.fire(x)
        };
        state.at2 !? _.addListener(thunk);
        this.initChildEventSource(parent, { |event, tuple|
             var lastESStart, lastESEnd, nextESStart, nextESEnd;
             //start of the old created chain
             lastESStart = tuple.at1;
             //end of old created chain
             lastESEnd = tuple.at2;
             //stop receiving events from old chain
             lastESEnd !? _.removeListener( thunk );
              //disconnect the old chain from it's start point
             lastESStart.do{ |x| x.tryPerform(\remove) };
             //let's discover where the new chain starts
             //I don't think that there is a situation where another flatcollect handler would be started here but who knows...
             EventStream.buildFlatCollect = EventStream.buildFlatCollect.add(None);
             nextESEnd = f.(event);
             //if a new EventSource was created the first one created will be here:
             nextESStart = EventStream.buildFlatCollect.pop(-1);
             //start receiving events from new EventStream
             nextESEnd !? _.addListener( thunk );
             //store the new chain
             Tuple2(nextESStart, nextESEnd);
        })
    }

        remove {
            parent.removeListener( listenerFunc );
            //if a new EventSource chain was created inside the flat collect it also must be removed.
            //this should recursevilly trickle down other flatCollects inside this one.
        	state.at1.collect(_.remove);
            ^Unit
        }
}

TakeWhileES : ChildEventSource {

    *new { |parent, f|
        ^super.new.init(parent, f)
    }

    init { |parent, f|
        this.initChildEventSource(parent, { |event|
            if( f.(event) ) {
                this.fire( event )
            } {
                parent.removeListener(listenerFunc);
            }
        })
    }
}

MergedES : EventSource {

    *new { |parent, parent2|
        ^super.new.init(parent, parent2)
    }

    init { |parent1, parent2|
        var thunk = this.fire(_);
        parent1.addListener( thunk );
        parent2.addListener( thunk );
    }
}