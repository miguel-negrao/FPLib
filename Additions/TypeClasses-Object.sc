
+ Object {

	getTypeInstance { |object, function, f|

		//If 'class' is not an instance of the typeclass check if one of the parent
		//classes is.
		var class = if( object.class.isMetaClass ) {
			object
		} {
			object.class
		};
		//"function: % this: % object: %".format(function, this,object).postln;
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
	//note: only functions with explicit variables can be curried
	// functions of type { |...args| cannot be curried }
	<%> { |a| ^a.fmap(this.curried) }
	//args is an Array or LazyList
	<%%> { |args|
        var largs = args.asLazy;
        ^largs.tail.foldl(largs.head.fmap(this.curried), { |a,b| a <*> b })
    }


//Monad
	>>= { |f|
		^this.getTypeInstance(this, 'bind', { |g| g.value(this, f) });
	}
	>>=| { |b| ^this >>= { b } }
	//>> { |b| ^this >>= { b } }
	//forever { ^this >>=| this.forever }

	pure { |class|
		^this.getTypeInstance(class, 'pure', { |g| g.value(this) });
	}

    join {
        ^this >>= I.d
    }

//Monoid
	|+| { |b| ^this.getTypeInstance(this, 'append', { |g| g.value(this,b) }) }
	*zero { |...args| ^this.getTypeInstance(this, 'zero', { |g| g.value(*args) }) } //args can be used for hints about the type of the zero

//Traverse
	traverse { |f, type| ^this.getTypeInstance(this, 'traverse', { |g| g.value(f, this, type) }) }
	sequence { |type| ^this.traverse({|x| x}, type) }
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
/*
[1,2,3,4].injectr([],{ |s,x| s++[x]})
[Some(1), Some(2), Some(3)].sequenceMonad
[Some(1), Some(2), None].sequenceMonad
[Some(1), Some(2)].sequenceMonad
[Some(1)].sequenceMonad
[].sequenceMonad(Option)
*/
+ Array {

    sequenceMonad { |monadClass|
        ^if(this.size == 0 ) {
            //with an empty list we need a hint in order to know which monad to use
            this.pure(monadClass)
        }{
            if(this.size == 1) {
                this.at(0).collect{ |x| [x] }
            } {
                var end = this.size-2;
                this[0..end].injectr(this.last.collect{ |x| [x] }, { |mstate,m|
                    m >>= { |x| mstate.collect{ |y| [x]++y } }
                })
            }
        }

    }
}
