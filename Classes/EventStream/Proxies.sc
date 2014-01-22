VarProxy {

	classvar dict;

	*initClass {
		dict = IdentityDictionary.new;
	}

	*asENInput{ |key, value|
		var v = Var(value);
		if(dict.at(key).notNil){
			"overwritting current Var at %".format(key).warn
		};
		dict.put(key,v);
		^Writer( v, Tuple3([],[],[]) )
	}

	*enIn { |key, value|
		^ENDef.appendToResult( this.asENInput(key, value) )
	}

	*new{ |key,value|
		if( dict.at(key).notNil ) {
			dict.at(key).value_(value)
		} {
			"VarProxy: no Var at %.".format(key).postln;
		}
	}

}


/*

(

//network
~networkDescription = ENDef({
    //inputs
    var x = VarProxy.enIn(\x, 5);
    x.enDebug(\x)

});
//compile network
~network = EventNetwork(~networkDescription, true);

//start network
~network.start;

)

VarProxy(\x, 24)
VarProxy(\y, 24)

*/