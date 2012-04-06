//Immutable Tuples

Tuple2{
	var <at1, <at2;

	*new { |at1, at2|
		^super.newCopyArgs(at1, at2)
	}

	collect { |f|
		^Tuple2( f.(at1), f.(at2) )
	}

	printOn { arg stream;
		stream << "(" << at1 << ", " << at2 << ")"
	}
}

Tuple3{
	var <at1, <at2, <at3;

	*new { |at1, at2, at3|
		^super.newCopyArgs(at1, at2, at3)
	}

	collect { |f|
		^Tuple3( f.(at1), f.(at2), f.(at3) )
	}

	printOn { arg stream;
		stream << "(" << at1 << ", " << at2 << ", " << at3 <<")"
	}
}

Tuple4{
	var <at1, <at2, <at3, <at4;

	*new { |at1, at2, at3, a4|
		^super.newCopyArgs(at1, at2, at3, a4)
	}

	collect { |f|
		^Tuple4( f.(at1), f.(at2), f.(at3), f.(at4) )
	}

	printOn { arg stream;
		stream << "(" << at1 << ", " << at2 << ", " << at3 << ", " << at4 <<")"
	}
}