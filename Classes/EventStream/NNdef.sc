NNdef : Ndef {

	classvar <>buildFRPControlNum;
	classvar <>buildCurrentNNdefKey;
	classvar <>allEventNetworks;
	classvar <>eventNetworkBuilder;


	*nextControl {
		var current = NNdef.buildFRPControlNum;
		NNdef.buildFRPControlNum = current + 1;
		^current
	}

	*clear { | fadeTime |
		super.clear(fadeTime);
		allEventNetworks.do(_.clear);
		allEventNetworks.clear;
	}

	put { | index, obj, channelOffset = 0, extraArgs, now = true |
		var currentEN, newEN;
		NNdef.buildFRPControlNum = 0;
		NNdef.buildCurrentNNdefKey = key;
		ENDef.tempBuilder = T([],[],[]);
		if( allEventNetworks.isNil) {
			allEventNetworks = ();
		};
		currentEN = allEventNetworks.at(key);
		currentEN !? { if(currentEN.active) { currentEN.stop } };
		this.nodeMap
		.controlNames
		.collect(_.name).select{ |n|
			"frpControl*".matchRegexp(n.asString)
		}.do(this.nodeMap.unset(_) );
		super.put(index, obj, channelOffset, extraArgs, now);
		if(obj.isFunction) {
			newEN = EventNetwork(Writer(Unit, ENDef.tempBuilder));
			allEventNetworks.put(key, newEN);
			newEN.start;
		}
	}

	clear { | fadeTime = 0 |
		super.clear(fadeTime);
		allEventNetworks.at(key) !? _.stop;
		allEventNetworks.put(key, nil)
	}

}

+ FPSignal {

	enKr { |lag = 0.1|

		var controlName = "frpControl%".format(NNdef.nextControl).asSymbol;
		var thisUdef = NNdef.buildCurrentNNdefKey;
		this.collect{ |v| IO{ NNdef(thisUdef).set(controlName, v) } }.enOut2;
		^controlName.kr(this.now, lag)
	}

}


+ EventSource {

	enKr { |lag = 0.1, initialValue=0|

		var controlName = "frpControl%".format(NNdef.nextControl).asSymbol;
		var thisUdef = NNdef.buildCurrentNNdefKey;
		this.collect{ |v| IO{ NNdef(thisUdef).set(controlName, v) } }.enOut;
		^controlName.kr(initialValue, lag)
	}

	enTr { |initialValue=0|

		var controlName = "frpControl%".format(NNdef.nextControl).asSymbol;
		var thisUdef = NNdef.buildCurrentNNdefKey;
		this.collect{ |v| IO{ NNdef(thisUdef).set(controlName, v) } }.enOut;
		^controlName.tr(initialValue)
	}

}