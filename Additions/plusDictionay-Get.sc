+Dictionary{
	get{ |key|
		var x = this.at(key);
		^if(x.isNil){ None }{ Some(x) }
	}
}
