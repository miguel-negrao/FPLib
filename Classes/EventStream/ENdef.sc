/*
To do:

add \name1.enIn

then

ENdef(\test).set(\name1, 50)
*/

ENdef  {

	classvar <>all;
	var <key;
	var <eventNetwork;

	*initClass { all = IdentityDictionary.new }

	*new { | key, f | //object is a function
		var check = this.checkArgs(\ENdef, \new, [key, f], [Symbol, [Function,Nil]]);

		var endef = all.at(key);
		endef = endef ?? {
			var x = this.basicNew( key, nil);
			all.put(key, x);
			x
		};
		f !? { endef.setSource(f) };
		^endef
	}

	*basicNew{ |key, endef|
		^super.newCopyArgs(key, endef)
	}

	start{
		eventNetwork !? _.start
	}

	stop{
		eventNetwork !? _.stop
	}

	clear{
		if(eventNetwork.notNil and: {eventNetwork.active}) {
			eventNetwork.stop;
			eventNetwork = nil;
		}
	}

	setSource { |f|
		var active = false;
		if(eventNetwork.notNil and: {eventNetwork.active}) {
			eventNetwork.stop;
			active = true;
		};
		if( f.notNil ) {
			//if event network already exists, and graph is compatible, re-use old state.
			eventNetwork = if(eventNetwork.notNil){eventNetwork.change(ENImperEval(f), runSignalReactimatesOnce:false)}{EventNetwork(ENImperEval(f))};
			if(active) { eventNetwork.start }
		} {
			eventNetwork = nil
		}
	}

}
		