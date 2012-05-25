
// Validation[E, X] 
Validation {

	*initClass{
		Class.initClassTree(TypeClasses);
		//type instances declarations:
		TypeClasses.addInstance(this,
			(
				'fmap': { |fa,f| fa.collect(f) },
				'apply': { |f,fa| fa.apply(f) },
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
