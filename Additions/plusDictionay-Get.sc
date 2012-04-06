+Dictionary{
	get{ |key|
		^this.at(key) !? Some(_) ?? None
	}
}
