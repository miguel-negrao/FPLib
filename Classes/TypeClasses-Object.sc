/*
    FP Lib
    Copyright 2012 Miguel Negrão.

    FP Lib: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FP Quark is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with FP Lib.  If not, see <http://www.gnu.org/licenses/>.

*/

+ Object {

    *typeClassError { |class|
        ^Error("Class "++this.class++"is not type instance of "++class)
    }

//Functor
    //collect { }

//Applicative Functor
    //by default tries to use Monadic bind
    //if it doesn't have you you'll get an error
    <*> {  |fa| ^this.bind{ |g| fa.collect( g ) } }

    // a <* b : ignore value of b
	<* { |fb|
		^{|x| {|y| x } } <%> this <*> fb
	}

    // a *> b : ignire value of a
	*> { |fb|
		^{ |x| {|y| y } } <%> this <*> fb
	}

	//note: only functions with explicit variables can be curried
	// functions of type { |...args| cannot be curried }
	<%> { |a| ^a.collect( this.curried ) }
	//args is an Array or LazyList
	<%%> { |args|
        var largs = args.asLazy;
        ^largs.tail.inject(largs.head.fmap(this.curried), { |a,b| a <*> b })
    }

//Monad
    >>= { |f| Object.typeClassError("Monad").throw }
    >>=| { |b| ^this >>= { b } }

    //by default bind redirects to >>=
    //this is needed because None can't use >>=
    bind { |f| ^this >>= f }
    bindIgnore { |b| ^this.bind{ b } }

    pure { |class| ^class.pure(this) }
    *pure { |class| ^class.pure(this) }

    join {
        ^this >>= I.d
    }

//Monoid
	|+| { |b| Object.typeClassError("Monoid").throw }
    //args can be used for hints about the type of the zero
	*zero { |args| Object.typeClassError("Monoid").throw }

//Traverse
    traverse { |f, type| Object.typeClassError("Traverse").throw }

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

//Monad related
    sequenceM { |monadClass|
        ^if(this.size == 0 ) {
            //with an empty list we need a hint in order to know which monad to use
            this.pure(monadClass)
        }{
            if(this.size == 1) {
                this.at(0).collect{ |x| [x] }
            } {
                var end = this.size-2;
                this[0..end].injectr(this.last.collect{ |x| [x] }, { |mstate,m|
                    m.bind{ |x| mstate.collect{ |y| [x]++y } }
                })
            }
        }

    }

    sequenceM_ { |monadClass|
        ^if(this.size == 0 ) {
            //with an empty list we need a hint in order to know which monad to use
            Unit.pure(monadClass)
        }{
            if(this.size == 1) {
                this.at(0).collect{ |x| [x] }
            } {
                var end = this.size-2;
                this[0..end].injectr(this.last.collect{ Unit }, { |mstate,m|
                    m.bindIgnore(mstate)
                })
            }
        }
    }

//Applicative related
    traverse { |f, type|
        var fclass;
        if(this.size == 0) {
            if( type.notNil ) {
                if(type.class.isMetaClass) {
                    //it's a class
                    this.pure( type )
                } {
                    //it's a function that constructs the pure instance
                    type.(this)
                }
            } {
                this
            }
        } {
            var pureAppliedToEmptyArray = if( type.notNil ) {
                if(type.class.isMetaClass) {
                    //it's a class
                    [].pure( type )
                } {
                    //it's a function that constructs the pure instance
                    type.([])
                }
            }{
                [].pure( f.(this[0]).getClass )
            };
            this.reverse.inject( pureAppliedToEmptyArray , { |ys,v|
                f.(v).fmap({ |z| { |zs| [z]++zs } }) <*> ys
            });
        }
    }

//Monoid
    |+| { |b| ^this ++ b }

    *zero { [] }

}


//Instances

+ SimpleNumber {

//Monoid
    |+| { |b|
        ^a + b
    }

    *zero { ^0 }
}

+ String {

//Monoid
    |+| { |b|
        ^a ++ b
    }

    *zero { ^"" }
}
 