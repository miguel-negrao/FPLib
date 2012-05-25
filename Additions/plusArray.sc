+ Array {

	atOption { |i|
		var x = this.at(i);
		^if(x.isNil) { None } { Some(x) }		
	}

}