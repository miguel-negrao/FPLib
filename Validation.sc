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

	Validation -> Functional Error Handling
*/

// Validation[E, X] 
Validation {

	*initClass{
		Class.initClassTree(TypeClasses);
		//type instances declarations:
		TypeClasses.addInstance(this,
			(
				'fmap': { |fa,f| fa.collect(f) },
				'apply': { |f,fa| fa.apply(f) },
				'bind': { |fa,f| fa.flatCollect(f) },				
				'pure': { |a| Success(a) }
			)
		);
	}

	match { |fsuccess, ffail|
	
	}
	
	isSuccess { }
	
	isFailure { ^this.isSuccess.not }
  
	collect { |f|
		^this.match( Success(_) <> f, { |e| Failure(e) } )
	}
	
	fmap { |f|
		^this.collect(f)
	}
	
	flatCollect { |f|
		^this.match(f.(_), { |e| Failure(e) })
	}
	
	>>= { |f|
		^this.flatCollect(f)
	}
	
	apply { |f|
		^this.match({ |x| 
			f.match({ |k|
				Success( k.(x) )
			}, { |e2| Failure(e2) })		
		}, { |e1|
			f.match({ |x|
				Failure(e1)
			},{ |e2|
				Failure( e1 |+| e2 )
			})
		})
	}
}

Failure : Validation {
	var <e;
	
	*new { |e| ^super.newCopyArgs(e) }
	
	match { |fsuccess, ffail|	
		^ffail.(e)
	}
	
	isSuccess { ^false }
	
	printOn { arg stream;
		stream << this.class.name << "( " << e << " )";
	}	
}

Success : Validation {
	var <x;
	
	*new{ |x| ^super.newCopyArgs(x) }
	
	match { |fsuccess, ffail|	
		^fsuccess.(x)
	}
	
	isSuccess { ^true }
	
	printOn { arg stream;
		stream << this.class.name << "( " << x << " )";
	}	
		
}

+ Object {
	
	success {
		^Success(this)
	}
	
	fail {
		^Failure(this)
	}
	
	failLL {
		^Failure( this %% LazyListEmpty )
	}
}
