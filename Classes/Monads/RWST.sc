/*
(
x = RWSArr.return(1);
x = x.fmap(_+1);
x.run(Unit,Unit)
)

(
z = Do(
    x <- RWSArr.return(1);
    RWSArr.tell( ["I've gonna sum one to that"] );
    return (x+1)
);
z.runRWS.("reader","state")
)

(
z = Do(
    x <- RWSArr.get;
    RWSArr.put( x + 10 );
    RWSArr.get
);
z.runRWS.(Unit,0)
)


(
z = Do(
    x <- RWSArr.ask;
    RWSArr.tell( [ x ++ " 1 " ] );
    RWSArr.tell( [ x ++ " 2 " ] );
    return x
);
z.runRWS.("this is the reader payload",0)
)

(
z = (_+_) <%> 1.pure(RWS) <*> 1.pure(RWS);
z.runRWS.("this is the reader content","this is the initial state")
)

*/

//RWS r w s a = RWS { runRWS :: r -> s -> (a, s, w) }
/*
Issues, we can't write functions like pure, ask, etc that return a value depending on the values involved.
Perhaps specialize for each type ?
RWS {
    *writerMakeZero{ }

}

RWSArr {
*initClass {
writerMakeZeroFunc = { [] }

}

NetworkDesc : RWS {
    *writerMakeZero{ ^T([],[],[]) }

}

*/

//RWS r w s a = RWS { runRWS :: r -> s -> (a, s, w) }
RWS {

    var <runRWS;

    *writerMakeZero { } //need to overload this method in child classes

    *initClass{
        Class.initClassTree(TypeClasses);
        //type instances declarations:
        TypeClasses.addInstance(this,
            (
                'fmap': { |fa,f|
                    this.new({ |r,s|
                        var x = fa.run(r,s);
                        x.at1_( f.( x.at1) )
                    })
                },
                'bind': { |fa,f|
                    this.new({ |r,s|
                        var x1 = fa.run(r,s);
                        var x2 = f.(x1.at1).run(r, x1.at2);
                        x2.at3_( x1.at3 |+| x2.at3 )
                    })
                },
                'pure': { |a,class| "I'm on pure % %".format(a,class).postln; class.intpure(a) }
            )
        );
    }

    *new { |f| //r -> s -> (a, s, w)
        ^super.newCopyArgs(f)
    }

    run { |r,s|
        ^runRWS.(r, s)
    }

    //reader operations

    //ask = RWS $ \r s -> return (r, s, mempty)
    *ask { |class|
        ^this.new{ |r,s| T( r, s, this.writerMakeZero ) }
    }

    //writer operations

    //tell w = RWS $ \_ s -> return ((),s,w)
    *tell { |w|
        ^this.new{ |r,s| T(Unit, s, w) }
    }

    //state operations

    //get = RWS $ \_ s -> return (s, s, mempty)
    *get { |class|
        ^this.new{ |r,s| T( s, s, this.writerMakeZero ) }
    }

    //put s = RWS $ \_ _ -> return ((), s, mempty)
    *put { |s, class|
        ^this.new{ T( Unit, s, this.writerMakeZero ) }
    }

    *return { |a|
        ^this.new({ |r,s| T(a, s, this.writerMakeZero) })
    }

//Functor
    collect { |f|
        ^this.class.new({ |r,s|
            var x = this.run(r,s);
            x.at1_( f.( x.at1) )
        })
    }
//Monad
    >>= { |f|
        this.class.new({ |r,s|
            var x1 = this.run(r,s);
            var x2 = f.(x1.at1).run(r, x1.at2);
            x2.at3_( x1.at3 |+| x2.at3 )
        })
    }
    bind { |f| ^this >>= f }

    *pure { |a|
        ^this.new({ |r,s| T(a, s, this.writerMakeZero) })
    }




}

RWSArr : RWS {
    *writerMakeZero{ ^[] }
}


/*
A monad transformer of sorts for Reader, Writer, State

(
x = RWSTArrE.return(1);
x = x.fmap(_+1);
x.run(Unit,Unit)
)

(
z = Do(
    x <- RWSArr.return(1);
    RWSArr.tell( ["I've gonna sum one to that"] );
    return (x+1)
);
z.runRWS.("reader","state")
)

(
z = Do(
    x <- RWSTArrE.get;
    RWSTArrE.put( x + 10 );
    RWSTArrE.get
);
z.runRWS.(Unit,0)
)

RWSTArrE.get.runRWS.(Unit,0)
RWSTArrE.put(30).runRWS.(Unit,0)
RWSTArrE.modify(_+10).runRWS.(Unit,0)

(
z = Do(
    x <- RWSTArrE.ask; //get config
    RWSTArrE.tell( [ x ++ " 1 " ] ); //log stuff
    RWSTArrE.tell( [ x ++ " 2 " ] ); //log stuff
    RWSTArrE.modify(_+20); //change state
    return x
);
z.runRWS.("this is the reader payload",0)
)

//failing
(
z = Do(
    RWSTArrE.return(1);
    RWSTArrE.fail("computation failed")
);
z.runRWS.("reader","state")

With Maybe monad
(
z = Do(
    RWSTArrO.return(1);
    RWSTArrO.none2
);
z.runRWS.("reader","state")

(
z = Do(
    RWSTArrO.return(1);
    RWSTArrO.modify(_++" is this")
);
z.runRWS.("reader","state")
)




Do( RWSTArrE.retur;  a )
*/

//A monad transformer of sorts for Reader, Writer, State monads
RWST {

    var <runRWS;

   //can't infer from classes at run time
    *writerZero { } //need to overload this method in child classes
    *monadReturn { |a| } //need to overload this method in child classes

    *initClass{
        Class.initClassTree(TypeClasses);
        //type instances declarations:
        TypeClasses.addInstance(this,
            (
                'fmap': { |fa,f|
                    fa.fmap(f)
                },
                'bind': { |fa,f|
                    fa >>= f
                },
                'pure': { |a,class| "I'm on pure % %".format(a,class).postln; class.intpure(a) }
            )
        );
    }

    *new { |f| //r -> s -> (a, s, w)
        ^super.newCopyArgs(f)
    }

    run { |r,s|
        ^runRWS.(r, s)
    }

    //type classes methods
    fmap{ |f|
        ^this.class.new({ |r,s|
            this.run(r,s).fmap{ |t| t.at1_( f.( t.at1) ) }
        })
    }

    >>= { |f|
        ^this.class.new({ |r,s|
            this.run(r,s) >>= { |t1|
                var s1 = t1.at2;
                f.( t1.at1 ).run(r,s1).fmap{ |t2|
                    t2.at3_( t1.at3 |+| t2.at3 )
                }
            }
        })
    }

    //reader operations

    //ask = RWS $ \r s -> return (r, s, mempty)
    *ask { |class|
        ^this.new{ |r,s| this.monadReturn( T( r, s, this.writerZero ) ) }
    }

    //writer operations

    //tell w = RWS $ \_ s -> return ((),s,w)
    *tell { |w|
        ^this.new{ |r,s| this.monadReturn( T(Unit, s, w) ) }
    }

    //state operations

    //get = RWS $ \_ s -> return (s, s, mempty)
    *get {
        ^this.new{ |r,s| this.monadReturn( T( s, s, this.writerZero ) ) }
    }

    //put s = RWS $ \_ _ -> return ((), s, mempty)
    *put { |s|
        ^this.new{ this.monadReturn( T( Unit, s, this.writerZero ) ) }
    }

    *modify { |f|
        ^this.get >>= { |s| this.put( f.(s) ) }
    }

    *return { |a|
        ^this.new({ |r,s| this.monadReturn( T(a, s, this.writerZero) ) })
    }

}

RWSTArr : RWST {
    *writerZero{ ^[] }
}

RWSTArrE : RWSTArr {
    *monadReturn{ |a| ^a.success }
    *fail{ |s| ^this.new({ |r,s| s.fail }) }
}

RWSTArrO : RWSTArr {
    *monadReturn{ |a| ^Some(a) }
    *n{ ^this.new({ |r,s| None }) }
}