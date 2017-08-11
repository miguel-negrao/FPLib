NNdef : Ndef {

	classvar <>buildFRPControlNum;
	classvar <>buildFRPControlIndex;
	classvar <>buildCurrentNNdefKey;
	classvar <>eventNetworkBuilder;
	classvar <>buildControls;
	classvar <>setsToPerform;

	var <eventNetworks;
	var <frpControls;

	*nextControl {
		var current = NNdef.buildFRPControlNum;
		NNdef.buildFRPControlNum = current + 1;
		^"frpControl-%-%".format(buildFRPControlIndex, current).asSymbol
	}

	put { | index, obj, channelOffset = 0, extraArgs, now = true |
		var container, bundle, oldBus = bus;
		var previousFRPControls;
		NNdef.buildFRPControlNum = 0;
		NNdef.buildFRPControlIndex = index ?? 0;
		NNdef.buildCurrentNNdefKey = key;
		NNdef.buildControls = [];
		NNdef.setsToPerform = [];
		ENDef.tempBuilder = T([],[],[]);
		index = index ?? 0;
		if( eventNetworks.isNil ) { eventNetworks = Order.new };

		if( frpControls.isNil ) { frpControls = Order.new };
		previousFRPControls = frpControls.at(index);

		// START OF ORIGINAL NodeProxy#put code
		if(obj.isNil) { this.removeAt(index); ^this };
		if(index.isSequenceableCollection) {
			^this.putAll(obj.asArray, index, channelOffset)
		};

		bundle = MixedBundle.new;
		container = obj.makeProxyControl(channelOffset, this);
		container.build(this, index ? 0); // bus allocation happens here

		if(this.shouldAddObject(container, index)) {
			// server sync happens here if necessary
			if(server.serverRunning) { container.loadToBundle(bundle, server) } { loaded = false; };
			this.prepareOtherObjects(bundle, index, oldBus.notNil and: { oldBus !== bus });
		} {
			format("failed to add % to node proxy: %", obj, this).inform;
			^this
		};
		// END OF ORIGINAL NodeProxy#put code

		// Do FRP stuff here
		if( obj.isFunction || (obj.isKindOf(Association)) ) {
			var enDesc = Writer(Unit, ENDef.tempBuilder);
			var currentEN = eventNetworks.at(index);
			//for FPSignal#enKr Ndef is set with "now" value
			//NNdef.setsToPerform contains the actual FPSignals, and now value is only extracted after event network
			//might have been changed by copying "now" values from old network.
			var performSets = {
					frpControls.put(index, NNdef.buildControls);
					NNdef.setsToPerform.do{ |x|
						if(x.at1) {
							this.setUni(x.at2, x.at3.now)
						} {
							this.set(x.at2, x.at3.now)
						}
					};
			};
			var newEN;
			if( currentEN.notNil ) {
				var newKeys;
				/*
				This can currently take enough time between creating the bundle and sending it due to analysis to create late messages.
				not a big issue though.
				*/
				newEN = currentEN.change(enDesc, runSignalReactimatesOnce: false);

				performSets.();
				//should remove keys which are not set anymore.
				newKeys = NNdef.setsToPerform.collect{ |x| x.at2 };
				previousFRPControls !? { |x|
					var keysToRemove = x.copy.removeAll(NNdef.buildControls);
					//"keys to be removed: %".format(keysToRemove).postln;
					keysToRemove.do{ |x|
						this.nodeMap.unset(x);
					}
				}

			} {
				//only run initial signal reactimates on first evaluation of this graph.
				newEN = EventNetwork(enDesc, runSignalReactimatesOnce: true);
				newEN.start;
				performSets.();
			};
			eventNetworks = eventNetworks.put(index, newEN);

		};

		// Continue NodeProxy#put code
		this.putNewObject(bundle, index, container, extraArgs, now);
		this.changed(\source, [obj, index, channelOffset, extraArgs, now]);
	}

	clear { | fadeTime = 0 |
		super.clear(fadeTime);
		eventNetworks.do( _.stop );
		eventNetworks = Order.new;
	}
}

+ FPSignal {

	enKr { |lag = 0.1, key, spec, debug = false|
		^this.prEnGeneric(\kr, lag, key, spec, debug)
	}

	enAr { |key, spec, debug = false|
		^this.prEnGeneric(\ar, nil, key, spec, debug)
	}

	enKrUni { |lag = 0.1, key, spec, debug = false|
		^this.prEnGenericUni(\kr, lag, key, spec, debug)
	}

	enArUni { |key, spec, debug = false|
		^this.prEnGenericUni(\ar, nil, key, spec, debug)
	}

	prEnGeneric { |rate, lag = 0.1, key, spec, debug = false|

		var controlName = key ?? { NNdef.nextControl() };
		var thisUdef = NNdef.buildCurrentNNdefKey;
		NNdef.buildControls = NNdef.buildControls.addI(controlName);
		if(spec.notNil){
			spec = spec.asSpec;
			NNdef(thisUdef).addSpec(controlName, spec);
		};
		this.collect{ |v| IO{ NNdef(thisUdef).set(controlName, v) } }.enOut2;
		//delay NNdef(thisUdef).set(controlName, this.now);
		NNdef.setsToPerform = NNdef.setsToPerform.addI( T(false, controlName, this) );
		if(debug) {
			this.enDebug(controlName.asString)
		};
		^controlName.perform(rate, this.now, lag)
	}

	prEnGenericUni { |rate, lag = 0.1, key, spec, debug = false|

		var controlName = key ?? { NNdef.nextControl() };
		var thisUdef = NNdef.buildCurrentNNdefKey;
		NNdef.buildControls = NNdef.buildControls.addI(controlName);
		if(spec.isNil) {
			Error("FPSignal - enKrUni, enArUni: spec cannot be nil");
		};

		spec = spec.asSpec;
		NNdef(thisUdef).addSpec(controlName, spec);
		this.collect{ |v| IO{ NNdef(thisUdef).setUni(controlName, v) } }.enOut2;
		//delay this
		//NNdef(thisUdef).setUni(controlName, this.now);
		NNdef.setsToPerform = NNdef.setsToPerform.addI(T(true, controlName, this));
		if(debug) {
			this.collect( spec.map(_) ).enDebug(controlName.asString)
		};
		^controlName.perform(rate, spec.map(this.now), lag)
	}

}


+ EventSource {

	enKr { |lag = 0.1, initialValue=0, key, spec, debug = false|
		^this.prEnGeneric(\kr, lag, initialValue, key, spec, debug)
	}

	enAr { |initialValue=0, key, spec, debug = false|
		^this.prEnGeneric(\ar, nil, initialValue, key, spec, debug)
	}

	enKrUni { |lag = 0.1, initialValue=0, key, spec, debug = false|
		^this.prEnGenericUni(\kr, lag, initialValue, key, spec, debug)
	}

	enArUni { |initialValue=0, key, spec, debug = false|
		^this.prEnGenericUni(\ar, nil, initialValue, key, spec, debug)
	}

	prEnGeneric { |rate, lag = 0.1, initialValue=0, key, spec, debug = false|

		var controlName = key ?? { NNdef.nextControl() };
		var thisUdef = NNdef.buildCurrentNNdefKey;
		NNdef.buildControls = NNdef.buildControls.addI(controlName);
		if(spec.notNil){
			spec = spec.asSpec;
			NNdef(thisUdef).addSpec(controlName, spec);
		};
		this.collect{ |v| IO{ NNdef(thisUdef).set(controlName, v) } }.enOut;
		if(debug) {
			this.enDebug(controlName.asString)
		}
		^controlName.perform(rate, initialValue, lag)
	}

	prEnGenericUni { |rate, lag = 0.1, initialValue=0, key, spec, debug = false|

		var controlName = key ?? { NNdef.nextControl() };
		var thisUdef = NNdef.buildCurrentNNdefKey;
		NNdef.buildControls = NNdef.buildControls.addI(controlName);
		if(spec.isNil) {
			Error("FPSignal - enKrUni, enArUni: spec cannot be nil");
		};
		spec = spec.asSpec;
		NNdef(thisUdef).addSpec(controlName, spec);
		this.collect{ |v| IO{ NNdef(thisUdef).setUni(controlName, v) } }.enOut;
		if(debug) {
			this.collect( spec.map(_) ).enDebug(controlName.asString)
		}
		^controlName.perform(rate, initialValue, lag)
	}

	enTr { |initialValue=0, key, debug = false|

		var controlName = key ?? { NNdef.nextControl() };
		var thisUdef = NNdef.buildCurrentNNdefKey;
		this.collect{ |v| IO{ NNdef(thisUdef).set(controlName, v); NNdef(thisUdef).unset(controlName) } }.enOut;
		if(debug) {
			this.enDebug(controlName.asString)
		}
		^controlName.tr(initialValue)
	}

}