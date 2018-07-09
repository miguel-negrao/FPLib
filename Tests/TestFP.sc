TestFP : UnitTest {

	test_CurryingProp1 {
		var f = {|x| x};
		this.assert(f.curried === f, "Currying a single variable function returns the same function")
	}

	test_CurryingProp2 {
		var f = {|x,y| x+y};
		var a = (1..100) >>= { |x|
			(1..100) >>= { |y|
				[f.(x,y)]
			}
		};
		var b = (1..100) >>= { |x|
			(1..100) >>= { |y|
				[f.curried.(x).(y)]
			}
		};
		this.assert(a == b, "A curried function should return the same result as the original function")
	}
}