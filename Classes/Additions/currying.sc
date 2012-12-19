+ Function {

	//partial application with unknown number of arguments
	pa { |arg1| ^{ arg ...args; this.value(arg1,*args) } }
	curried1 { ^{ |x| this.pa(x) } }
	curried2 { ^{ |x| { |y| this.pa(x).(y) } } }
	curried3 { ^{ |x| { |y| {|z| this.pa(x).pa(y).(z) } } } }
	curried {
		var r = { |f,n|
			var result;
			if(n == 1){
				{ |x| f.(x) }
			} {
				{|x|	r.value(f.pa(x),n-1) }
			}
		};
		^r.(this,this.def.argNames.size)
	}

}
