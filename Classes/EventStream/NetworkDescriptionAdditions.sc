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

FRPGUIProxy {
	//Option[QView]
	var <view;
	//Option[Function]
	var <f;

	*new { |view|
		^super.newCopyArgs(view.asOption)
	}

	addAction { |g|
		f = Some(g);
		view.collect{ |x| x.addAction(g) }
	}

	removeAction { |g|
		f = Some(g);
		view.collect{ |x| x.removeAction(g) }
	}

	asENInput {
		^FRPGUICode.makeENInput(this)
	}

	view_ { |newView|
		{ |view, action| view.removeAction(action) } <%> view <*> f;
		newView.addAction(_) <%> f;
		view = Some(newView)
	}

	removeView {
		{ |view, action| view.removeAction(action) } <%> view <*> f;
		view = None();
	}

	value {
		^(_.value <%> view).getOrElse(0.0)
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

	*makeENInputES { |gui|
		var addHandler;
		var es = EventSource();
		var action = { |sl| es.fire( sl.value ) };
		addHandler = IO{
			gui.addAction(action);
			IO{ gui.removeAction(action)
		} };
		^Writer( es, Tuple3([addHandler],[],[]) )
	}

}

+ Node {

	setSink { |key, signal|
		^signal.collect{ |v| this.setIO(key,v) }.reactimate
	}

	enSetSink { |key, signal|
		ENDef.appendToResult( this.setSink(key, signal) );
	}
}

+ OSCFunc {

	*asENInputES { |path, srcID, recvPort, argTemplate, dispatcher|
        var es = EventSource();
        var addHandler = IO{
            var f = { |msg| es.fire(msg) };
            var osc = OSCFunc(f, path, srcID, recvPort, argTemplate, dispatcher);
			IO{ osc.free }
		};
        ^Writer( es, Tuple3([addHandler],[],[]) )
    }

	*asENInput { |path, srcID, recvPort, argTemplate, dispatcher, initialValue|
		var es = Var(initialValue);
        var addHandler = IO{
            var f = { |msg| es.value_(msg) };
            var osc = OSCFunc(f, path, srcID, recvPort, argTemplate, dispatcher);
			IO{ osc.free }
		};
        ^Writer( es, Tuple3([addHandler],[],[]) )
    }

	*enIn { |path, srcID, recvPort, argTemplate, dispatcher, initialValue|
		^ENDef.appendToResult( this.asENInput(path, srcID, recvPort, argTemplate, dispatcher, initialValue) );
	}

	*enInES{ |path, srcID, recvPort, argTemplate, dispatcher|
		^ENDef.appendToResult( this.asENInputES(path, srcID, recvPort, argTemplate, dispatcher) );
	}
}

+ QView {

	asENInput {
		^FRPGUICode.makeENInput(this)
	}

	asENInputES {
		^FRPGUICode.makeENInputES(this)
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
		var func = { |v| es.value_(v.value) };
		var addHandler = IO{ this.addAction(func); IO{ this.removeAction(func) } };
		^Writer( es, Tuple3([addHandler],[],[]) )
	}

	asENInputES {
		var es = EventSource();
		var func = { |v| es.fire(v.value) };
		var addHandler = IO{ this.addAction(func); IO{ this.removeAction(func) } };
		^Writer( es, Tuple3([addHandler],[],[]) )
	}

}

+ Synth {

	setSink { |signal|
		^signal.collect{ |xs| IO{ this.set(*xs) } }.reactimate
	}

	*newSink { |signal|
		^signal.collect{ |xs| IO{ this.new(*xs) } }.reactimate
	}

}