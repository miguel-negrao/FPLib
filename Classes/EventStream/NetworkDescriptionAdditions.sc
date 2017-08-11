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

+ Object {

	sinkValue { |signal|
    	^signal.collect{ |v| IO{ defer{ this.value_(v) } } }.reactimate;
    }

	//signal should carry array of form ['methodName', arg1, arg2, ...]
	sink { |signal, method|
    	^signal.collect{ |v| IO{ defer{ this.perform(*v) } } }.reactimate;
    }

	*sink { |signal, method|
    	^signal.collect{ |v| IO{ defer{ this.perform(*v) } } }.reactimate;
    }

/*
using dependency system
this ES fires whenever the object is updated via 'changed'
*/
	updatesEN {
		var es = EventSource();
		var addHandler= IO{
			var action = { |a,b,c| es.fire( T(a,b,c) ) };
			this.addDependant(action);
			IO{ this.removeDependant.(action) }
		};
		^Writer( es, Tuple3([addHandler],[],[]) )
	}

}

+ Node {

	setSink { |key, o|
		^o.collect{ |v| this.setIO(key,v) }.reactimate
	}

}

+ OSCFunc {

	*asENInput { |path, srcID, recvPort, argTemplate, dispatcher|
        var es = EventSource();
        var addHandler = IO{
            var f = { |msg| es.fire(msg) };
            var osc = OSCFunc(f, path, srcID, recvPort, argTemplate, dispatcher);
			IO{ osc.free }
		};
        ^Writer( es, Tuple3([addHandler],[],[]) )
    }

	*asENInputSig { |path, srcID, recvPort, argTemplate, dispatcher, initialValue|
		var es = Var(initialValue);
        var addHandler = IO{
            var f = { |msg| es.value_(msg) };
            var osc = OSCFunc(f, path, srcID, recvPort, argTemplate, dispatcher);
			IO{ osc.free }
		};
        ^Writer( es, Tuple3([addHandler],[],[]) )
    }

	*asENInputFull { |path, srcID, recvPort, argTemplate, dispatcher|
        var es = EventSource();
        var addHandler = IO{
            var f = { |...args| es.fire(args) };
            var osc = OSCFunc(f, path, srcID, recvPort, argTemplate, dispatcher);
			IO{ osc.free }
		};
        ^Writer( es, Tuple3([addHandler],[],[]) )
    }

	*asENInputFullSig { |path, srcID, recvPort, argTemplate, dispatcher, initialValue|
		var es = Var(initialValue);
        var addHandler = IO{
            var f = { |...args| es.value_(args) };
            var osc = OSCFunc(f, path, srcID, recvPort, argTemplate, dispatcher);
			IO{ osc.free }
		};
        ^Writer( es, Tuple3([addHandler],[],[]) )
    }

}

+ MKtlElement {

	asENInput {
		var es = EventSource();
		var func = { |v| es.fire(v.value) };
		var addHandler = IO{ this.addAction(func); IO{ this.removeAction(func) } };
		^Writer( es, Tuple3([addHandler],[],[]) )
	}

	asENInputSig {
        var es = Var(this.value);
		var func = { |v| es.value_(v.value) };
		var addHandler = IO{ this.addAction(func); IO{ this.removeAction(func) } };
		^Writer( es, Tuple3([addHandler],[],[]) )
	}

}