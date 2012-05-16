LazyList {

	isEmpty {  }
	
	notEmpty { ^this.isEmpty.not }

	match{ |fempty, fcons|	}

	take { |n| }

	drop { |n| }

	asArray { }
	
	cycle {
		var f = { this.append({ { f.() } }) };
		^f.()		
	}

	*repeat { |a| 
		var g = { |a| LazyListCons(a, { this.repeat(a) }) };
		^g.(a)
	}

	*iterate { |a,f|
		var g = { |a,f| LazyListCons(a, { g.(f.(a), f) }) };
		^g.(a,f)
	}
	
	*replicate { |n,a|
		^LazyList.repeat(a).take(n)
	}

	*fromArray { |array|
		var l = LazyListEmpty;
		array.reverse.do{ |elem|
			l = LazyListCons(elem, l)
		};
		^l
	}
}

LazyListCons : LazyList {
	var <head, tailFunc, tailEvaluated;

	*new{ |head, tail|
		//if we already have a value store it in tailEvaluated otherwise store 
		//the function tailFunc to be evaluated later.
		^if( tail.isKindOf( Function ) ) {
			super.newCopyArgs(head, tail)
		} {
			super.newCopyArgs(head, nil, tail)
		}		
	}

	isEmpty { ^false  }

	tail { 
		//memoization
		^tailEvaluated ?? { tailEvaluated = tailFunc.value; tailEvaluated }
	}

	match{ |fempty, fcons|
		^fcons.value(head, this.tail)
	}

	take { |n|
		^if(n <= 0) {
		    LazyListEmpty
		} {
			LazyListCons( head, this.tail.take(n-1) )
		}
	}

	drop { |n|
		^if(n<=0) {
			this
		} {
			this.tail.drop(n-1)
		}
	}

	asArray {
    	^this.prToArray([])
	}

	prToArray { |array|
		^this.tail.value.prToArray( array.add(this.head) )
	}

	zip { |that|
	    ^if(that.isEmpty) {
	    	LazyListEmpty
	    } {
			LazyListCons( Tuple2(this.head, that.head), { this.tail.value.zip(that.tail.value) } )
		}
	}

	collect { |f|
		^LazyListCons( f.(this.head), { this.tail.value.collect(f) })
	}

	select { |pred|
		^if( pred.(head) ) {
			LazyListCons( head, { this.tail.select(pred) } )
		} {
			this.tail.select(pred)
		}
	}
	
	//can't use ++ with LazyListEmpty
	append { |that|
		var v = that.value; //append is supposed to be lazy on the list to append,
							//so either a LazyList was passed or a function was passed.
		^if( v.isKindOf(LazyListEmpty) ) {
			this
		} {
			^LazyListCons(this.head, { this.tail.append(v) } )
		}
	}
	
	printOn { arg stream;
		var array = this.take(21).asArray;		
		if( array.size == 21) {
			array.pop;
			stream << "LazyList[";
			array.printItemsOn(stream);
			stream << ",...]" ;
		} {
			stream << "LazyList" << array
		}
	}

}

LazyListEmpty : LazyList {

	*match{ |fempty, fcons|
		^fempty.value
	}

	*isEmpty { ^true }

	*take {}
	*asArray{ ^[] }
	*prToArray { |array| ^array }
	*zip {}
	*drop {}
	*collect {}
	*select { }
	*append { |that|
		^that
	}

}

+ Object {

	%%{ |that| ^LazyListCons(this, that) }
	
	//shortcuts
	repeat {
		^LazyList.repeat(this)
	}
	
	iterate { |f|
		^LazyList.iterate(this,f)
	}
	
	replicate { |n|
		^LazyList.replicate(n,this)
	}

}

+ Array {
    asLazy { ^LazyList.fromArray(this) }
	%% { |that| ^this.asLazyList.append(that) }
}

+ Stream {

	asLazyList {
		^LazyListCons( this.next, { this.asLazyList })
	}

}

+ Pattern {

	asLazyList {
		^this.asStream.asLazyList
	}

}