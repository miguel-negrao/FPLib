Reader{
	var <func; // r -> a
	
	*initClass{
		Class.initClassTree(TypeClasses);
		//type instances declarations:
		TypeClasses.addInstance(this,
			(
				'fmap': { |fa,f| fa.collect(f) },
				'bind': { |fa,f| fa.flatCollect(f) },
				'pure': { |a| Reader( { a } ) }
			)
		);
	}
	
	run { |...args|
		^func.value(*args)	
	}
	
	*new{ |f| ^super.newCopyArgs(f) }
	
	*ask { ^Reader( I.d ) }
	
	//change the environment locally
	local { |f| ^Reader( func <> f ) }
		
	collect{ |f| ^Reader( { |r| f.( func.(r) ) } ) }
	
	flatCollect { |f| ^Reader( { |r| f.(func.(r)).func.(r) } ) }

}

/*

f = Reader({ |x| x +1 });
g = Reader({ |y| y*2 });
(5.pure(Reader) >>=| f >>=| g).func.value

(f >>=).run(1)

*/