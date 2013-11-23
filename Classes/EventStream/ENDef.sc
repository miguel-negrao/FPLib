
ENDef {
	// Tuple3([IO(IO())], [EventStream[IO[Unit]]], [IO] )
	classvar <>tempBuilder;
	var <func;
	//Writer( Unit, Tuple3([IO(IO())], [EventStream[IO[Unit]]], [IO] ) )
	var <resultWriter;

	*new { |func|
		^super.newCopyArgs(func).init
	}

	*appendToResult { |writer|
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
		resultWriter = ENDef.evaluate(func);
	}

}

+ Object {

	enIn {
		^ENDef.appendToResult( this.asENInput );
	}

	enSink { |signal|
		ENDef.appendToResult( this.sink(signal) );
    }

	enSinkValue { |signal|
		ENDef.appendToResult( this.sinkValue(signal) );
	}

}

+ FPSignal {

	enOut {
		ENDef.appendToResult( this.reactimate );
	}

	withKey { |key|
		^this.collect{ |v| [key,v] }
	}

	enDebug { |string|
		ENDef.appendToResult( this.debug(string) )
	}

}

+ EventStream {

	enOut {
		ENDef.appendToResult( this.reactimate );
	}

	enDebug { |string|
		ENDef.appendToResult( this.debug(string) )
	}

}