/*
*  	copied from Scala API
*  Scala API (c) 2002-2010, LAMP/EPFL
*  http://scala-lang.org/
*
* conversion to sc: Miguel Negrão, 2011
*/

Option {



    /*
                very intersting: traverse cannot be defined for Option in a dynamic language, because
                there is no specification of what the class of f.(a) should be for None...
                'traverse' : { |f,a|
                var fa = f.(a);
                if( a.isDefined) {
                f.a.fmap{ |x| Some(x) }
                } {
                None.pure(?????)
                }
                }*/

    *new{ |x| ^if (x.isNil){ None }{ Some(x) } }

    *empty{ ^None }

    *pure { |a| ^Some(a) }

    == { |ob|
        ^this.match(
            { |a|
                ob.match(
                    { |b|
                        a == b
                    },{
                        false
                    }
                )
            }, {
                false
            }
        )
    }

}

Some : Option {
    var value;

    *new{ |val| ^super.newCopyArgs(val) }

    match{ |fsome, fnone|
        ^fsome.(value)
    }

    isEmpty{ ^false }

    get{ ^value }

    isDefined{ ^true }

    getOrElse{ ^this.get }

    getOrElseDoNothing{ ^this.get }

    orNil{ ^this.get }

    collect{ |f| ^Some(f.(this.get)) }

    bind { |f| ^f.(this.get) }
    >>= { |f| ^f.(this.get) }

    select{ |p| ^if(p.(this.get)){ this }{ None } }

    do{ |f| f.(this.get) }

    exists{ ^this.get }

    orElse{ ^this }

    asArray{ ^[this.get]}

    flatten{ ^this.asArray }

    printOn { arg stream;
        stream << this.class.name << "(" << value << ")";
    }
}

None : Option {

    *match{ |fsome, fnone|
        ^fnone.()
    }
    *isEmpty{ ^true }

    *get{ Error("None.get").throw }

    *isDefined{ ^false }

    *getOrElse{ |default| ^default }

    *getOrElseDoNothing{ IO{ Unit } }

    *orNil{ ^nil }

    *collect{}

    *bind {}
    *bindIgnore {}

    *select{}

    *do{}

    *exists{ false }

    *orElse{ |alternative| ^alternative }

    *asArray{ ^[] }

    *flatten{ ^this.asArray }
    *printOn { arg stream;
        stream << "None";
    }

}

+ Object {

    asOption {
        ^Some(this)
    }

}

+ Nil {

    asOption {
        ^None
    }
}

+ Array {

    atOption { |i|
        var x = this.at(i);
        ^if(x.isNil) { None } { Some(x) }
    }

    catOptions {
        ^this.inject([], { |state, ma|
            ma.match({ |a|
                state ++ [a]
                },{
                    state
            })
        })
    }

    catOptions2 {
        ^if( this.isEmpty ) {
            None
        } {
            Some( this.catOptions )
        }
    }

}
