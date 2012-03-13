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

EventStream{ }

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

    new{
        ^super.new.initEventSource
    }

    initEventSource {
        listeners = [];
    }

    addListener { |f| listeners = listeners ++ [f] }

    removeListener { |f| listeners.remove(f) }
    
    reset { listeners = [] }

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

    flatCollectR { |f, initialState|
		^FlatCollectedESR( this, f, initialState)
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

    dopost {
    	this.do(postln(_))
    }

    fire { |event|
	    //("running fire "++event++" "++this.hash).postln;
	    //copy is used here because listerFuncs might mutate the listeners variable
        listeners.copy.do( _.value(event) );
        ^Unit
    }

	//returns the corresponding signal
    hold { |initialValue|
    	^HoldFPSignal(this, initialValue)
    }

    remove { }
}

HoldFPSignal : FPSignal {
	var <now;
	var <change;
	var <listener;

	*new { |eventStream, initialValue|
		^super.new.init(eventStream, initialValue)
	}

	init { |eventStream, initialValue|
		change = eventStream;
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
        parent.removeListener( listenerFunc )
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

    *new { |parent, f, initial|
        ^super.new(initial).init(parent, f)
    }

    init { |parent, f|
        var thunk = { |x|
         	//"firing the FlatCollectedES".postln;
         	this.fire(x) 
        };
        state !? _.addListener(thunk);
        this.initChildEventSource(parent, { |event, lastES|
             var nextES;
             lastES !? _.removeListener( thunk );
             nextES = f.(event);
             nextES !? _.addListener( thunk );
             nextES
        })
    }
}

FlatCollectedESR : ChildEventSource {

    *new { |parent, f, initial|
        ^super.new(initial).init(parent, f)
    }

    init { |parent, f|
        var thunk = { |x|
         	//"firing the FlatCollectedES".postln;
         	this.fire(x)
        };
        state !? _.addListener(thunk);
        this.initChildEventSource(parent, { |event, lastES|
             var nextES;
             lastES !? _.removeListener( thunk );
             lastES !? { |x| x.tryPerform(\remove) };
             nextES = f.(event);
             nextES !? _.addListener( thunk );
             nextES
        })
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