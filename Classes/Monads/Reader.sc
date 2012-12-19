Reader{
	var <func; // r -> a

	run { |...args|
		^func.value(*args)
	}

	*new{ |f| ^super.newCopyArgs(f) }

	*ask { ^Reader( I.d ) }

	//change the environment locally
	local { |f| ^Reader( func <> f ) }

//Functor
	collect{ |f| ^Reader( { |r| f.( func.(r) ) } ) }

//Monad
    *pure { |a|
        ^Reader({ a })
    }

	>>= { |f| ^Reader( { |r| f.(func.(r)).func.(r) } ) }
    bind { |f| ^Reader( { |r| f.(func.(r)).func.(r) } ) }

}

/*

f = Reader({ |x| x +1 });
g = Reader({ |y| y*2 });
(5.pure(Reader) >>=| f >>=| g).func.value

(f >>=).run(1)

*/