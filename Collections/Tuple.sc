/*
    FP Quark
    Copyright 2012 Miguel Negrão.

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

//Immutable Tuples

Tuple2{
	var <at1, <at2;

	*initClass{
		Class.initClassTree(TypeClasses);
		//type instances declarations:
		TypeClasses.addInstance(this,
			(
				'append': { |a,b| "append a:% b:%".format(a,b).postln; Tuple2( a.at1 |+| b.at1, a.at2 |+| b.at2 ) },
				'zero': { |class1, class2| Tuple2( class1.zero, class2.zero) }
			)
		);
	}


	*new { |at1, at2|
		^super.newCopyArgs(at1, at2)
	}

	collect { |f|
		^Tuple2( f.(at1), f.(at2) )
	}

	fmap { |f| ^this.collect(f) }

	== { |tuple|
		^(this.at1 == tuple.at1) && (this.at2 == tuple.at2)
	}

	printOn { arg stream;
		stream << "(" << at1 << ", " << at2 << ")"
	}

    at1_{ |v|
        ^Tuple2(v,this.at2)
    }

    at2_{ |v|
        ^Tuple2(this.at1,v)
    }
}

Tuple3{
	var <at1, <at2, <at3;

	*initClass{
		Class.initClassTree(TypeClasses);
		//type instances declarations:
		TypeClasses.addInstance(this,
			(
				'append': { |a,b| Tuple3( a.at1 |+| b.at1, a.at2 |+| b.at2, a.at3 |+| b.at3 ) },
				'zero': { |class1, class2| Tuple2( class1.zero, class2.zero) }
			)
		);
	}


	*new { |at1, at2, at3|
		^super.newCopyArgs(at1, at2, at3)
	}

	collect { |f|
		^Tuple3( f.(at1), f.(at2), f.(at3) )
	}

	fmap { |f| ^this.collect(f) }

	== { |tuple|
		^(this.at1 == tuple.at1) && (this.at2 == tuple.at2) && (this.at3 == tuple.at3)
	}

	printOn { arg stream;
		stream << "(" << at1 << ", " << at2 << ", " << at3 <<")"
	}
}

Tuple4{
	var <at1, <at2, <at3, <at4;

	*initClass{
		Class.initClassTree(TypeClasses);
		//type instances declarations:
		TypeClasses.addInstance(this,
			(
				'append': { |a,b| Tuple4( a.at1 |+| b.at1, a.at2 |+| b.at2, a.at3 |+| b.at3, a.at4 |+| b.at4 ) },
				'zero': { |class1, class2, class3, class4| Tuple4( class1.zero, class2.zero, class3.zero, class4.zero) }
			)
		);
	}

	*new { |at1, at2, at3, a4|
		^super.newCopyArgs(at1, at2, at3, a4)
	}

	collect { |f|
		^Tuple4( f.(at1), f.(at2), f.(at3), f.(at4) )
	}

	== { |tuple|
		^(this.at1 == tuple.at1) && (this.at2 == tuple.at2) && (this.at3 == tuple.at3) && (this.at4 == tuple.at4)
	}

	printOn { arg stream;
		stream << "(" << at1 << ", " << at2 << ", " << at3 << ", " << at4 <<")"
	}
}