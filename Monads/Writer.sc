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

//Writer Monad
Writer {
	var <a, <w; // ( A, W )
	//a is the main value
	//w is the annotation monoid
	
	*initClass{
		Class.initClassTree(TypeClasses);
		//type instances declarations:
		TypeClasses.addInstance(this,
			(
				'fmap': { |fa,f| fa.collect(f) },
				'bind': { |fa,f| fa.flatCollect(f) },
				'pure': { |a,class| Writer(a, class !? _.zero ?? {[]}) }
			)
		);
	}
	
	*new { |a,w| ^super.newCopyArgs(a,w) }
	
	runWriter { ^Tuple2(a,w) }
	
	execWriter { ^this.runWriter.at2 }

	collect { |f| ^Writer( f.(a), w ) }
	
	fmap { |f| ^this.collect(f) }
	
	flatCollect { |f| 
		var k = f.(a);
		^Writer( k.a, w |+| k.w )
	}
	
	>>= { |f| ^this.flatCollect(f) }
	
	tell { |w2|
		^Writer(a, w |+| w2)
	}	
	
	*tell { |w|
		^Writer( Unit, w)
	}		
	
	printOn { arg stream;
		stream << this.class.name << "( " << a << ", " << w << " )";
	}	
		
}

