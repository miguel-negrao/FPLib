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

+ View {
/*
Adds and removes the call-back action directly in the View object.
This is different from the more general fromAddHandler method.
*/
	asENInput {
		var addHandler;
		var es = EventSource();
		var action = { |o| es.fire( o.value ) };
		addHandler = IO{
			this.addAction(action);
			IO{ this.removeAction(action)
		} };
		^Writer( es, Tuple3([addHandler],[],[]) )
	}

	asENInputSig {
		var addHandler;
		var es = Var(this.value);
		var action = { |o| es.value_( o.value ) };
		addHandler = IO{
			this.addAction(action);
			IO{ this.removeAction(action)
		} };
		^Writer( es, Tuple3([addHandler],[],[]) )
	}

	mouseClicksEN {
		var es = EventSource();
		var f = {
			es.fire( Unit );
		};
		var addHandler = IO{
			this.mouseUpAction = this.mouseUpAction.addFunc(f);
			IO{ this.mouseUpAction.removeFunc(f) }
		};
		^Writer( es, Tuple3([addHandler],[],[]) )
	}

	enMouseClicks {
		^ENImperEval.appendToResult( this.mouseClicksEN );
	}

	mousePosEN {
		var sig = Var(Point(0,0));
		var f = { |view, x, y, modifiers|
			sig.value_( Point(x, y) );
		};
		var addHandler = IO{
			this.mouseOverAction = this.mouseOverAction.addFunc(f);
			IO{ this.mouseOverAction.removeFunc(f) }
		};
		^Writer( sig, Tuple3([addHandler],[],[]) )
	}

	enMousePos {
		^ENImperEval.appendToResult( this.mousePosEN );
	}

	mouseIsDownEN {
		var sig = Var( false);
		var fdown = { |view, x, y, modifiers|
			sig.value_( true );
		};
		var fup = { |view, x, y, modifiers|
			sig.value_( false );
		};
		var addHandler = IO{
			this.mouseDownAction = this.mouseDownAction.addFunc(fdown);
			this.mouseUpAction = this.mouseUpAction.addFunc(fup);
			IO{
				this.mouseDownAction.removeFunc(fdown);
				this.mouseUpAction.removeFunc(fup)
			}
		};
		^Writer( sig, Tuple3([addHandler],[],[]) )
	}

	enMouseIsDown {
		^ENImperEval.appendToResult( this.mouseIsDownEN );
	}

	keyDownEN {
		var es = EventSource();
		var f = { |v, char, mod, uni, keycode, key|
			es.fire( T(char, mod, uni, keycode, key) );
		};
		var addHandler = IO{
			this.keyDownAction = this.keyDownAction.addFunc(f);
			IO{ this.keyDownAction.removeFunc(f) }
		};
		^Writer( es, Tuple3([addHandler],[],[]) )
	}

	enKeyUp {
		^ENImperEval.appendToResult( this.keyDownEN );
	}

	keyUpEN {
		var es = EventSource();
		var f = { |v, char, mod, uni, keycode, key|
			es.fire( T(char, mod, uni, keycode, key) );
		};
		var addHandler = IO{
			this.keyUpAction = this.keyUpAction.addFunc(f);
			IO{ this.keyUpAction.removeFunc(f) }
		};
		^Writer( es, Tuple3([addHandler],[],[]) )
	}

	enKeyUp {
		^ENImperEval.appendToResult( this.keyUpEN );
	}

}