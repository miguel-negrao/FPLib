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
   Some of the code is Copyright Miguel Negrão 2012
*/

EventStream{

	classvar <doFuncs;
	classvar <>debugInternal = false;
	classvar <>buildFlatCollect;

	*initClass {
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

    addListener { |f|
        //" addListener % % %".format(this, this.hash, f.def.sourceCode).postln;
        listeners = listeners ++ [f];
        ^Unit
    }

    removeListener { |f| listeners.remove(f); ^Unit }

    reset { listeners = []; ^Unit }

	//private
	proc { |initialState, f|
		^ChildEventSource( initialState ).initChildEventSource(this,f)
	}

    select { |f|
        ^SelectedES(this, f)
    }

    inject { |initial, f|
        ^FoldedES(this, initial, f);
    }

	injectF { |initial|
        ^FoldedFES(this, initial);
    }

    >>= { |f, initialState|
        ^FlatCollectedES( this, f, initialState)
    }

    //synonims
    switch { |f, initialState|
        ^FlatCollectedES( this, f, initialState)
    }

	//behaves like initial state until an event arrives then behaves like the
	//signals returned by f
	switchSig { |f, initialState|
		^FlatCollectedFPSignalHybrid( this, f, initialState)
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
	    //debug
        /*
        ("running fire "++event++" "++this.hash).postln;
        "listeners:".postln;
        listeners.do{ |f|
            " % ,  % ".format(f.def.sourceCode, f).postln;
        };*/

	    //copy is used here because listerFuncs might mutate the listeners variable
	    //change this to a FingerTree in the future.
        listeners.copy.do( _.value(event) );
        if(EventStream.debugInternal) { postln("-> "++this++" : "++this.hash++" : "++event)};
        ^Unit
    }

	debug { |string|
        ^this.collect{ |x| putStrLn(string++" : "++x) }.reactimate;
    }

    fireIO { |event|
		^IO{ this.fire(event) }
    }


	//returns the corresponding signal
    hold { |initialValue|
    	^HoldFPSignal(this, initialValue)
    }

    remove { ^Unit }

    //connect to an object that responds to .value_
    connect{ |object|
    	this.do{ |v| defer{ object.value_(v) } };
    	^Unit
    }

    connectIO{ |object|
    	^this.collect{ |v| IO{ defer{ object.value_(v) } } };
    }

    connectEN{ |object|
    	^this.collect{ |v| IO{ defer{ object.value_(v) } } }.reactimate;
    }

    reactimate{ //this stream should returns IOs
		^Writer( Unit, Tuple3([],[this],[]) )
	}

	asENInput {
		^Writer(this, Tuple3([],[],[]) )
	}

	//GUI additions

	makeSlider{ |minval=0.0, maxval=1.0, warp='lin', step=0.0, default|
		var spec = [minval, maxval, warp, step, default].asSpec;
		var slider = Slider(nil, Rect(100,100,50,100) );
		slider.action_{ |sl| this.fire(spec.map(sl.value)) }
	}

    bus { |server, initVal = 0.0|
		server = server ?? {Server.default};
		if( server.serverRunning ) {
			var bus = Bus.control(server, initVal.asArray.size);
			var f = { |x| bus.setn( x.asArray ) };
			this.do(f);
			bus.setn( initVal.asArray );
			^Some( Tuple2( bus, f) )
		} {
			^None()
		}
	}

    //utilities
    selectSome {
        ^this.select(_.isDefined).collect(_.get)
    }

    storePrevious { |init = 0.0|
        ^this.inject( Tuple2(init,init), { |state,x| Tuple2( state.at2, x ) })
    }

    storePreviousWithT {  |init = 0.0|
        ^this.inject( Tuple2( Tuple2(0.0, init), Tuple2(0.0, init) ),
            { |state,x| Tuple2( state.at2, Tuple2( Process.elapsedTime, x ) ) })
    }

    storeWithT {
        ^this.collect( Tuple2(Process.elapsedTime,_) )
    }


    onlyChanges {
		^this.storePrevious.select{ |tup| tup.at1 != tup.at2 }.collect(_.at2)
    }

	paged { |pageNumES, numPages, default= 0.0|
		var check = this.checkArgs(\EventSource, \paged,
			[pageNumES, numPages, default], [EventSource, SimpleNumber, SimpleNumber]);
		var merged = this.collect{ |x| T(\value, x) } | pageNumES.collect{ |x| T(\newPage, x) };
		var process = merged.inject( (pageNum:0, values: (default ! numPages), newValue:None(), pageChange:None() ),
			{ |state, x|
				//"state : % x: %".format(state,x).postln;
				if( x.at1 == \value ) {
					var val = x.at2;
					var page = state.at(\pageNum);
					(pageNum: state.pageNum, values: state.at(\values).put(page, val),
						newValue: Some( T(page, val) ), pageChange:None() )
				} {
					if( state.pageNum != x.at2 ) {
						(pageNum: x.at2, values: state.at(\values),
							newValue: None(), pageChange: Some( state.at(\values).at(x.at2) ) )
					} {
						state
					}
				}
			}
		);
		//var d1 = process.enDebug("process");
		var pageChangeES = process.collect(_.at(\pageChange)).selectSome;
		var pages = numPages.collect{ |n|
			process.select{ |x| x.at(\newValue).collect{ |x| x.at1 == n }.getOrElse(false) }.collect{ |x| x.at(\newValue).get.at2 }
		};
		^T(pages, pageChangeES )
	}


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

//Functor
    collect { |f|
		^CollectedES(this,f)
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

FoldedFES : ChildEventSource {

    *new { |parent, initial|
        ^super.new(initial).init(parent)
    }

    init { |parent|
		this.initChildEventSource(parent, { |event, state|
			var next = event.(state);
			this.fire( next );
			next
		})
    }
}

FlatCollectedES : ChildEventSource {

	var thunk;

    *new { |parent, f, init|
        ^super.new(Tuple2(None(),init)).init(parent, f)
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
             EventStream.buildFlatCollect = EventStream.buildFlatCollect.add(None());
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

ApplySignalES : EventSource {
	var <f, <x, <fval, <xval, <flistener, <xlistener;
    *new { |f, x, initf, initx|
        ^super.new.init(f, x, initf, initx)
    }

    init { |farg, xarg, initf, initx|
		f = farg;
		x = xarg;
		fval = initf;
		xval = initx;
        flistener = { |newf|
			fval = newf;
			this.fire( fval.( xval ) )
		};
		xlistener = { |newx|
			xval = newx;
			this.fire( fval.( xval ) )
		};
        f.addListener( flistener );
        x.addListener( xlistener );
    }

    remove {
		f.removeListener( flistener );
		x.removeListener( xlistener );
	}

}
