NNdef : Ndef {

	classvar <>buildFRPControlNum;
	classvar <>buildFRPControlIndex;
	classvar <>buildCurrentNNdefKey;
	classvar <>eventNetworkBuilder;

	var <eventNetworks;

	*nextControl {
		var current = NNdef.buildFRPControlNum;
		NNdef.buildFRPControlNum = current + 1;
		^"frpControl-%-%".format(buildFRPControlIndex, current).asSymbol
	}

	put { | index, obj, channelOffset = 0, extraArgs, now = true |
		var currentEN, newEN;
		NNdef.buildFRPControlNum = 0;
		NNdef.buildFRPControlIndex = index;
		NNdef.buildCurrentNNdefKey = key;
		ENDef.tempBuilder = T([],[],[]);
		index = index ?? 0;
		if( eventNetworks.isNil ) { eventNetworks = Order.new };
		currentEN = eventNetworks.at(index);
		currentEN !? {
			if(currentEN.active) { currentEN.stop };
			eventNetworks.removeAt(index);
		};
		this.nodeMap
		.keys.as(Array).select{ |n|
			"frpControl*".matchRegexp(n.asString)
		}.do(this.nodeMap.unset(_) );
		super.put(index, obj, channelOffset, extraArgs, now);
		if( obj.isFunction || (obj.isKindOf(Association)) ) {
			newEN = EventNetwork(Writer(Unit, ENDef.tempBuilder));
			eventNetworks = eventNetworks.put(index, newEN);
			newEN.start;
		}
	}

	clear { | fadeTime = 0 |
		super.clear(fadeTime);
		eventNetworks.do( _.stop );
		eventNetworks = Order.new;
	}

}

+ FPSignal {

	enKr { |lag = 0.1|

		var controlName = NNdef.nextControl;
		var thisUdef = NNdef.buildCurrentNNdefKey;
		this.collect{ |v| IO{ NNdef(thisUdef).set(controlName, v) } }.enOut2;
		^controlName.kr(this.now, lag)
	}

}


+ EventSource {

	enKr { |lag = 0.1, initialValue=0|

		var controlName = NNdef.nextControl;
		var thisUdef = NNdef.buildCurrentNNdefKey;
		this.collect{ |v| IO{ NNdef(thisUdef).set(controlName, v) } }.enOut;
		^controlName.kr(initialValue, lag)
	}

	enTr { |initialValue=0|

		var controlName = NNdef.nextControl;
		var thisUdef = NNdef.buildCurrentNNdefKey;
		this.collect{ |v| IO{ NNdef(thisUdef).set(controlName, v) } }.enOut;
		^controlName.tr(initialValue)
	}

}