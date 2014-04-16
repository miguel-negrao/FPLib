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

ENdef  {

	classvar <>all;
	var <>key;
	var <eventNetwork;

	*initClass { all = () }

	*new { | key, object |
		var check = this.checkArgs(\ENdef, \new, [key, object], [Symbol, [Function,Nil]]);

		var makeNew = { |f|
			var x = this.basicNew( key, EventNetwork(ENDef(f)) );
			all.put(key, x);
			x
		};
		var en = all.at(key);
		^if( en.isNil) {
			if(object.isNil) {
				Error("ENdef no EN stored and no object to store").throw
			} {
				makeNew.(object)
			}
		} {
			if( object.notNil ) {
				var x;
				if(en.eventNetwork.active) { en.eventNetwork.stop };
				x = makeNew.(object);
				x.start;
				x

			} {
				en
			}
		}
	}

	*basicNew{ |key, en|
			^super.newCopyArgs(key, en)
	}

	start{
		eventNetwork !? _.start
	}

	stop{
		eventNetwork !? _.stop
	}



}
		