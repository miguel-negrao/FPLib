
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

/*
Imperative interface to the writer monad.
In the writer monad, new items are added to the log via >>=
Here a function is evaluated and items are added to the log via appendToResult
The current log is stored in the classvar tempBuilder which can be reached from any point in the code.
SuperCollider doesn't have do notation (unlike Haskell, Scala and C#) therefore the imperative interface is
easier to use.
*/
ENImperEval {
	// Tuple3([IO(IO())], [EventStream[IO[Unit]]], [FPSignal] )
	classvar <>tempBuilder;
	var <func;
	//Writer( Unit, Tuple3([IO(IO())], [EventStream[IO[Unit]]], [FPSignal] ) )
	var <resultWriter;

	*new { |func|
		^super.newCopyArgs(func).init
	}

	*appendToResult { |writer|
		if( tempBuilder.isNil ) { Error("Ran ENImperEval.appendToResult with uninitialized ENImperEval.tempBuilder").throw };
		tempBuilder = tempBuilder |+| writer.w;
		^writer.a
	}

	*evaluate { |f, args|
		var r;
		tempBuilder = T([],[],[]);
		r = f.value(*args);
		^Writer(r, tempBuilder);
	}

	init {
		resultWriter = ENImperEval.evaluate(func);
	}

}
/*
enIn returns an EventStream
enInSig return a Signal

by default one should use enIn.
*/
+ Object {

	enIn { |...args|
		^ENImperEval.appendToResult( this.asENInput(*args) );
	}

	enInSig { |...args|
		^ENImperEval.appendToResult( this.asENInputSig(*args) );
	}

	enSink { |signal|
		ENImperEval.appendToResult( this.sink(signal) );
    }

	enSinkValue { |signal|
		ENImperEval.appendToResult( this.sinkValue(signal) );
	}

	enUpdates {
		^ENImperEval.appendToResult(this.updatesEN())
	}

}

+ FPSignal {

	enOut {
		ENImperEval.appendToResult( this.reactimate );
	}

	enOut2 {
		ENImperEval.appendToResult( this.reactimate2 );
	}

	enDebug { |string|
		ENImperEval.appendToResult( this.debug(string) )
	}

}

+ EventStream {

	enOut {
		ENImperEval.appendToResult( this.reactimate );
	}

	enDebug { |string|
		ENImperEval.appendToResult( this.debug(string) )
	}

}

+ Node {

	enSetSink { |key, signal|
		ENImperEval.appendToResult( this.setSink(key, signal) );
	}
}

+ OSCFunc {

	*enIn { |path, srcID, recvPort, argTemplate, dispatcher, initialValue|
		^ENImperEval.appendToResult( this.asENInput(path, srcID, recvPort, argTemplate, dispatcher, initialValue) );
	}

	*enInSig{ |path, srcID, recvPort, argTemplate, dispatcher|
		^ENImperEval.appendToResult( this.asENInputSig(path, srcID, recvPort, argTemplate, dispatcher) );
	}

	*enInFull { |path, srcID, recvPort, argTemplate, dispatcher, initialValue|
		^ENImperEval.appendToResult( this.asENInputFull(path, srcID, recvPort, argTemplate, dispatcher, initialValue) );
	}

	*enInFullSig{ |path, srcID, recvPort, argTemplate, dispatcher|
		^ENImperEval.appendToResult( this.asENInputFullSig(path, srcID, recvPort, argTemplate, dispatcher) );
	}
}