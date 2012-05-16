
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
	
	<* { |fb|
		^{|x| {|y| x } } <%> this <*> fb
	}
	
	*> { |fb|
		^{ |x| {|y| y } } <%> this <*> fb
	}
	<%> { |a| ^a.fmap(this.curried) }
			
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
	|+| { |b| ^this.getTypeInstance(this, 'append', { |g| g.value(this,b) }) }
	*zero { |...args| ^this.getTypeInstance(this, 'zero', { |g| g.value(*args) }) } //args can be used for hints about the type of the zero

//Traverse
	traverse { |f| ^this.getTypeInstance(this, 'traverse', { |g| g.value(f, this) }) }
	sequence { ^this.traverse({|x| x}) }
	//F[_] : Applicative, A, B,  f: A => F[Unit], g: A => B
	collectTraverse { |f, g|
		^this.traverse({ |a|
			var fa = f.(a);
			{ g.(a) }.pure(fa.getClass) <*> f.(a)
		})
	}

	//F[_] : Applicative, A, B, C, f: F[B], g: A => B => C
	disperse { |f,g|
		^this.traverse({ |a| g.curried.(a).pure(f.getClass) <*> f })
	}

//Utils

	constf { ^{ |x| this } }
	
	*getClass {
		^if( this.class.isMetaClass ) {
			this
		} {
			this.class
		};
	}

	getClass {
		^if( this.class.isMetaClass ) {
			this
		} {
			this.class
		};
	}
}
