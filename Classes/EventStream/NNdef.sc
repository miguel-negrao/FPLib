NNdef : Ndef {

	classvar <>buildFRPControlNum;
	classvar <>buildFRPControlIndex;
	classvar <>buildCurrentNNdefKey;
	classvar <>eventNetworkBuilder;
	classvar <>buildControls;
	classvar <>setsToPerform;

	var <eventNetworks;
	//automatic NamedControl names created by enKr, etc methods.
	var <frpControlNames;

	//similar to NodeMap for FRP network controls
	//shared for all objects / eventNetworks
	var <>frpNodeMap;

	//IdentityDictionary
	//contains FRP controls similar to NamedControl in synth definitions
	//contains events of this type:
	//(default: Object, es:EventSource, listeners: [Function]));
	var <>frpStoreControls;
	//remove <> afterwards
	var <internalKeysArray;

	*new { | key, object |
		// key may be simply a symbol, or an association of a symbol and a server name
		var res, server, dict;

		if(key.isKindOf(Association)) {
			server = Server.named.at(key.value);
			if(server.isNil) {
				Error("Ndef(%): no server found with this name.".format(key)).throw
			};
			key = key.key;
		} {
			server = defaultServer ? Server.default;
		};

		dict = this.dictFor(server);
		res = dict.envir.at(key);
		if(res.isNil) {
			res = super.newToSuper(server).key_(key).initNNdef;
			dict.initProxy(res);
			dict.envir.put(key, res)
		};

		object !? { res.source = object };
		^res;
	}

	*nextControl {
		var current = NNdef.buildFRPControlNum;
		NNdef.buildFRPControlNum = current + 1;
		^"frpControl-%-%".format(buildFRPControlIndex, current).asSymbol
	}

	initNNdef {
		frpControlNames = Order.new;
		eventNetworks = Order.new;
		frpNodeMap = IdentityDictionary.new;
		frpStoreControls = IdentityDictionary.new;
	}

	internalKeys {
		^internalKeysArray.addAll(#[\out, \i_out, \gate, \fadeTime]);
	}

	addToInternalKeys { |controlName|
		internalKeysArray.add(controlName);
	}

	//copied from NodeProxy and altered
	put { | index, obj, channelOffset = 0, extraArgs, now = true |
		var container, bundle, oldBus = bus;
		var previousFRPControlNames;
		NNdef.buildFRPControlNum = 0;
		NNdef.buildFRPControlIndex = index ?? 0;
		NNdef.buildCurrentNNdefKey = key;
		NNdef.buildControls = [];
		NNdef.setsToPerform = [];
		ENImperEval.tempBuilder = T([],[],[]);
		index = index ?? 0;

		previousFRPControlNames = frpControlNames.at(index);
		internalKeysArray = List.new;

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
		if( obj.isFunction || (obj.isKindOf(Association)) || obj.isKindOf(FRPDef)) {
			var enDesc = Writer(Unit, ENImperEval.tempBuilder);
			var currentEN = eventNetworks.at(index);

			//for FPSignal#enKr Ndef is set with "now" value
			//NNdef.setsToPerform contains the actual FPSignals, and "now" value is only extracted after event network
			//might have been changed by copying "now" values from old network.
			var performSets = {
					frpControlNames.put(index, NNdef.buildControls);
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
				This can currently take enough time between creating the bundle and sending
				it due to analysis to create late messages.
				not a big issue though.
				*/

				newEN = currentEN.change(enDesc, runSignalReactimatesOnce: false);

				performSets.();
				//should remove keys created by FRP which are not set anymore.
				newKeys = NNdef.setsToPerform.collect{ |x| x.at2 };
				previousFRPControlNames !? { |x|
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
		frpControlNames = Order.new;
		eventNetworks = Order.new;
		frpNodeMap = IdentityDictionary.new;
		frpStoreControls = IdentityDictionary.new;
	}

	//the methods below are used with controls created with frp store methods
	enGet { |key|
		^frpNodeMap[key] ?? {
			frpStoreControls[key] !? { |d|	d['defaultValue'] }
		}
	}

	enSet { | ... args |
		//"% setting %".format(this.key, args).postln;
		args.pairsDo{ |key,val|
			frpNodeMap.put(key, val);
			frpStoreControls[key] !?{ |d|
				d['es'].fire(val);
			}
		};
	}

	enSetNoAction { | ... args |
		//"% setting %".format(this.key, args).postln;
		args.pairsDo{ |key,val|
			frpNodeMap.put(key, val);
		};
	}

	enUnset { |... keys|
		//"% unsetting %".format(this.key, keys).postln;
		keys.do{ |key|
			/*"unsetting key % to %".format(key, d.default).postln; */
			var d = frpStoreControls[key];
			if (frpStoreControls[key].notNil) {
				d['es'].fire(d['default']);
				frpNodeMap.put(key, d['default']);
			} {
				frpNodeMap.remoteAt(key);
			}
		};
	}

	asENInput { |key|
		^frpStoreControls[key] !? { |dict|
			var es = EventSource();
			var func = { |val| es.fire(val) };
			var listeners = dict['listeners'];
			var addHandler = IO{ listeners.add(func); IO{ listeners.remove(func) } };
			 Writer( es, Tuple3([addHandler],[],[]) )
		} ?? Writer( NothingES(), Tuple3([],[],[]) )
	}

	//copied from NodeProxy except for one line
	asCode { | includeSettings = true, includeMonitor = true, envir |
		var nameStr, srcStr, str, docStr, indexStr, key;
		var space, spaceCS;

		var isAnon, isSingle, isInCurrent, isOnDefault, isMultiline;

		envir = envir ? currentEnvironment;

		nameStr = envir.use { this.asCompileString };
		indexStr = nameStr;

		isAnon = nameStr.beginsWith("a = ");
		isSingle = this.objects.isEmpty or: { this.objects.size == 1 and: { this.objects.indices.first == 0 } };
		isInCurrent = envir.includes(this);
		isOnDefault = server === Server.default;

		//	[\isAnon, isAnon, \isSingle, isSingle, \isInCurrent, isInCurrent, \isOnDefault, isOnDefault].postln;

		space = ProxySpace.findSpace(this);
		spaceCS = try { space.asCode } {
			postln("// <could not find a space for proxy: %!>".format(this.asCompileString));
			""
		};

		docStr = String.streamContents { |stream|
			if(isSingle) {
				str = nameStr;
				srcStr = if (this.source.notNil) { this.source.envirCompileString } { "" };

				if ( isAnon ) {			// "a = NodeProxy.new"
					if (isOnDefault.not) { str = str ++ "(" ++ this.server.asCompileString ++ ")" };
					if (srcStr.notEmpty) { str = str ++ ".source_(" ++ srcStr ++ ")" };
				} {
					if (isInCurrent) { 	// ~out
						if (srcStr.notEmpty) { str = str + "=" + srcStr };

					} { 					// Ndef('a') - put sourceString before closing paren.
						if (srcStr.notEmpty) {
							str = str.copy.drop(-1) ++ ", " ++ srcStr ++ nameStr.last
						};
					}
				};
			} {
				// multiple sources
				if (isAnon) {
					str = nameStr ++ ";\n";
					indexStr = "a";
				};

				this.objects.keysValuesDo { |index, item|

					srcStr = item.source.envirCompileString ? "";
					isMultiline = srcStr.includes(Char.nl);
					if (isMultiline) { srcStr = "(" ++ srcStr ++ ")" };
					srcStr = indexStr ++ "[" ++ index ++ "] = " ++ srcStr ++ ";\n";
					str = str ++ srcStr;
				};
			};

			stream << str << if (str.keep(-2).includes($;)) { "\n" } { ";\n" };

			// add settings to compile string
			if(includeSettings) {
				stream << this.nodeMap.asCode(indexStr, true);
				//line added to NNdef
				stream << this.frpNodeMapAsCode(indexStr);
			};
			// include play settings if playing ...
			// hmmm - also keep them if not playing,
			// but inited to something non-default?
			if (this.rate == \audio and: includeMonitor) {
				if (this.monitor.notNil) {
					if (this.isMonitoring) {
						stream << this.playEditString(this.monitor.usedPlayN, true)
					}
				};
			};
		};

		isMultiline = docStr.drop(-1).includes(Char.nl);
		if (isMultiline) { docStr = "(\n" ++ docStr ++ ");\n" };

		^docStr
	}

	frpNodeMapAsCode { |namestring = ""|
		^String.streamContents({ |stream|
			if(frpNodeMap.notEmpty) {
				stream << namestring << ".enSet(" <<<* frpNodeMap.asKeyValuePairs << ");" << Char.nl;
			};
		});
	}

	nodeMaps {
		^(nodeMap: nodeMap.copy, frpNodeMap: frpNodeMap.copy)
	}

}

FRPDef {
	var <func;

	*new {|func|
		^super.newCopyArgs(func)
	}

	evaluate {
		func.value;
	}

	//compatability with NodeProxy makeProxyControl interface.
	proxyControlClass {
		^FRPPlayControl
	}
}

FRPPlayControl : AbstractPlayControl {

	build {
		source.evaluate();
	}

	play{}
	stop{}

}

+ Ndef {
	*newToSuper { |server|
		^super.new(server)
	}
}

+ FPSignal {

	enKr { |lag = 0.1, key, spec, debug = false, internal = false|
		^this.prEnGeneric(\kr, lag, key, spec, debug, internal)
	}

	enAr { |key, spec, debug = false, internal = false|
		^this.prEnGeneric(\ar, nil, key, spec, debug, internal)
	}

	enKrUni { |lag = 0.1, key, spec, debug = false, internal = false|
		^this.prEnGenericUni(\kr, lag, key, spec, debug, internal)
	}

	enArUni { |key, spec, debug = false, internal = false|
		^this.prEnGenericUni(\ar, nil, key, spec, debug, internal)
	}

	prEnGeneric { |rate, lag = 0.1, key, spec, debug = false, internal = false|

		var controlName = key ?? { NNdef.nextControl() };
		var thisUdef = NNdef.buildCurrentNNdefKey;
		NNdef.buildControls = NNdef.buildControls.addI(controlName);
		if(spec.notNil){
			spec = spec.asSpec;
			NNdef(thisUdef).addSpec(controlName, spec);
		};
		if (internal) {
			NNdef(thisUdef).addToInternalKeys(controlName);
		};
		this.collect{ |v| IO{ NNdef(thisUdef).set(controlName, v) } }.enOut2;
		//delay NNdef(thisUdef).set(controlName, this.now);
		NNdef.setsToPerform = NNdef.setsToPerform.addI( T(false, controlName, this) );
		if(debug) {
			this.enDebug(controlName.asString)
		};
		^controlName.perform(rate, this.now, lag)
	}

	prEnGenericUni { |rate, lag = 0.1, key, spec, debug = false, internal = false|

		var controlName = key ?? { NNdef.nextControl() };
		var thisUdef = NNdef.buildCurrentNNdefKey;
		NNdef.buildControls = NNdef.buildControls.addI(controlName);
		if(spec.isNil) {
			Error("FPSignal - enKrUni, enArUni: spec cannot be nil");
		};

		spec = spec.asSpec;
		NNdef(thisUdef).addSpec(controlName, spec);
		if (internal) {
			NNdef(thisUdef).addToInternalKeys(controlName);
		};
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

	enKr { |lag = 0.1, initialValue=0, key, spec, debug = false, internal = false|
		^this.prEnGeneric(\kr, lag, initialValue, key, spec, debug, internal)
	}

	enAr { |initialValue=0, key, spec, debug = false, internal = false|
		^this.prEnGeneric(\ar, nil, initialValue, key, spec, debug, internal)
	}

	enKrUni { |lag = 0.1, initialValue=0, key, spec, debug = false, internal = false|
		^this.prEnGenericUni(\kr, lag, initialValue, key, spec, debug, internal)
	}

	enArUni { |initialValue=0, key, spec, debug = false, internal = false|
		^this.prEnGenericUni(\ar, nil, initialValue, key, spec, debug, internal)
	}

	prEnGeneric { |rate, lag = 0.1, initialValue=0, key, spec, debug = false, internal = false|

		var controlName = key ?? { NNdef.nextControl() };
		var thisUdef = NNdef.buildCurrentNNdefKey;
		NNdef.buildControls = NNdef.buildControls.addI(controlName);
		if(spec.notNil){
			spec = spec.asSpec;
			NNdef(thisUdef).addSpec(controlName, spec);
		};
		if (internal) {
			NNdef(thisUdef).addToInternalKeys(controlName);
		};
		this.collect{ |v| IO{ NNdef(thisUdef).set(controlName, v) } }.enOut;
		if(debug) {
			this.enDebug(controlName.asString)
		}
		^controlName.perform(rate, initialValue, lag)
	}

	prEnGenericUni { |rate, lag = 0.1, initialValue=0, key, spec, debug = false, internal = false|

		var controlName = key ?? { NNdef.nextControl() };
		var thisUdef = NNdef.buildCurrentNNdefKey;
		NNdef.buildControls = NNdef.buildControls.addI(controlName);
		if(spec.isNil) {
			Error("FPSignal - enKrUni, enArUni: spec cannot be nil");
		};
		spec = spec.asSpec;
		NNdef(thisUdef).addSpec(controlName, spec);
		if (internal) {
			NNdef(thisUdef).addToInternalKeys(controlName);
		};
		this.collect{ |v| IO{ NNdef(thisUdef).setUni(controlName, v) } }.enOut;
		if(debug) {
			this.collect( spec.map(_) ).enDebug(controlName.asString)
		}
		^controlName.perform(rate, initialValue, lag)
	}

	enTr { |initialValue=0, key, debug = false, internal = false|

		var controlName = key ?? { NNdef.nextControl() };
		var thisUdef = NNdef.buildCurrentNNdefKey;
		if (internal) {
			NNdef(thisUdef).addToInternalKeys(controlName);
		};
		this.collect{ |v|
			IO{
				NNdef(thisUdef).set(controlName, v);
				NNdef(thisUdef).unset(controlName)
			}
		}.enOut;
		if(debug) {
			this.enDebug(controlName.asString)
		}
		^controlName.tr(initialValue)
	}

	prMakeStorableES {|initialValue, key, constructNewNode|

		var thisNNdef = NNdef(NNdef.buildCurrentNNdefKey);
		var es = EventSource();
		var actualInit = thisNNdef.enGet(key) ?? initialValue;

		var result = constructNewNode.(es, actualInit);

		var storeListeners = List.new;
		if(thisNNdef.frpStoreControls[key].notNil) {
			"Ovewritting event source store with key %".format(key).warn;
		};

		thisNNdef.frpStoreControls.put(key, (default:initialValue, es:es, listeners:storeListeners) );

		result.collect({ |val| IO{
			thisNNdef.enSetNoAction(key, val);
			storeListeners.do{ |f| f.(val) };
		}}).enOut;

		^result
	}

	injectFStore { |initialValue, key|
		var check = this.checkArgs(\EventSource, \injectFStore,
			[initialValue, key], [Object, Symbol]);
		var constructNewNode = { |es, actualInit|
			 (es.collect{ |x| {x} } | this).injectF(actualInit)
		};
		^this.prMakeStorableES(initialValue, key, constructNewNode);
	}

	injectFSigStore { |initialValue, key|
		var check = this.checkArgs(\EventSource, \injectFStore,
			[initialValue, key], [Object, Symbol]);
		var constructNewNode = { |es, actualInit|
			 (es.collect{ |x| {x} } | this).injectFSig(actualInit)
		};
		^this.prMakeStorableES(initialValue, key, constructNewNode);
	}

	//more efficient than .hold(v).store, one less call-back
	holdStore { |initialValue, key|
		var check = this.checkArgs(\EventSource, \holdStore,
			[initialValue, key], [Object, Symbol]);

		var constructNewNode = { |es, actualInit|
			 (es | this).hold(actualInit);
		};

		^this.prMakeStorableES(initialValue, key, constructNewNode);
	}

}

+ FPSignal {

	store { |key|
		var check = this.checkArgs(\FPSignal, \store,
			[key], [Symbol]);
		var constructNewNode = { |es, actualInit|
			(es | this.changes).hold(actualInit)
		};
		^this.prMakeStorableES(this.now, key, constructNewNode);
	}

}