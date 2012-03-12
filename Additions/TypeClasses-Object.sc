
+ Object {	
	
	getTypeInstance { |object, function, f|
		//If 'class' is not an instance of the typeclass check if one of the parent
		//classes is.
		var class = if( object.class.isMetaClass ) {
			object
		} {
			object.class
		};
        ^TypeClasses.getSuperclassImplementation(class, function) !? f ?? {
        	Error(class.asString ++" does not implement the typeclass function: "++function).throw
        };
	}
	
	//Functor
	fmap { |f|
		^this.getTypeInstance(this, 'fmap',  { |g| g.value(this, f) })
	}
	
	//Applicative Functor
	<*> {  |fa|
		^this.getTypeInstance(this, 'apply',  { |g| g.value(this, fa) })
	}
	<%> { |a| ^a.fmap(this.curried) }
	|*|{ |b|
		^AplicativeBuilder(this,b) 	
	}
			
	//Monad	
	>>= { |f|
		^this.getTypeInstance(this, 'bind', { |g| g.value(this, f) });
	}
	>>=| { |b| ^this >>= { b } }
	//forever { ^this >>=| this.forever }

	pure { |class|
		^this.getTypeInstance(class, 'pure', { |g| g.value(this) });
	}
	
	//Monoid
	|+| { |a| }

	//Traverse
	traverse { |f| ^this.getTypeInstance(this, 'traverse', { |g| g.value(f, this) }) }
	sequence { ^this.traverse({|x| x}) }

}
