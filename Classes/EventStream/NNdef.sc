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

	/*
	This can currently take enough time between creating the bundle and sending it due to analysis to create late messages.
	not a big issue though.
	*/
	put { | index, obj, channelOffset = 0, extraArgs, now = true |
		var container, bundle, oldBus = bus;
		var currentEN, newEN, makeFRPGraphRep, checkFRPSameGraph, checkFRPSameGraphSub, frpGraphRep, putFRPState, previousFRPControls;
		NNdef.buildFRPControlNum = 0;
		NNdef.buildFRPControlIndex = index ?? 0;
		NNdef.buildCurrentNNdefKey = key;
		NNdef.buildControls = [];
		NNdef.setsToPerform = [];
		ENDef.tempBuilder = T([],[],[]);
		index = index ?? 0;
		if( eventNetworks.isNil ) { eventNetworks = Order.new };

		makeFRPGraphRep = { |o|
			//o.class.asString.postln;
			//o.pureFunc.collect{ |x| x.def.sourceCode.postln };
			T(  o.class.asString,
				if([ChildEventSource, ChildFPSignal].any{ |x| o.isKindOf(x) }){
					Some(T(o.state, o.pureFunc))
				}{
					None()
				},
				if(o.isKindOf(ChildFPSignal)){Some(o.now)}{None()},
				o.nodes.collect{ |n|
					makeFRPGraphRep.(n)
				}
			)
		};

		checkFRPSameGraph = { |en, o| checkFRPSameGraphSub.(en.finalIOES, o ) };
		//  returns T( shapeMatches, functionsMatch)
		checkFRPSameGraphSub = { |o, rep|

			if( o.class.asString != rep.at1 ) {
				//"% % classes don't match ".format(o.class.asString,rep.at1).postln;
				T(false,false)
			}{
				if( o.nodes.size != rep.at4.size ) {
					//"% size doesn't match".format(o.class.asString).postln;
					T(false,false)
				}{
					var storedFunc = rep.at2.collect(_.at2).monjoin;
					var check = { |a,b| a.copy.removeEvery(" \n\t") == b.copy.removeEvery(" \n\t") }.lift
					.(
						o.pureFunc.collect{ |x| x.def.sourceCode.asOption }.monjoin,
						storedFunc.collect{ |x| x.def.sourceCode.asOption }.monjoin
					).getOrElse(true);
					if( check.not ) {
						//"% functions don't match: \n% - %".format(o.class.asString,
						//	o.pureFunc.collect({|x| x.def.sourceCode }), storedFunc.collect({|x| x.def.sourceCode })).postln;
						T(true,false)
					}{
						//"% functions match !!: \n% - %".format(o.class.asString,
						//	o.pureFunc.collect({|x| x.def.sourceCode }), storedFunc.collect({|x| x.def.sourceCode })).postln
						if(o.nodes.size > 0 ){
							[o.nodes, rep.at4].flopWith{ |a,b|
								checkFRPSameGraphSub.(a,b)
							}.reduce({ |a,b| T(a.at1 && b.at1, a.at2 && b.at2) })
						}{
							T(true,true)
						}
					}
				}
			}
		};

		putFRPState = { |o, rep|
			rep.at2.collect{ |oldState|
				o.state = oldState.at1
			};
			rep.at3.collect{ |oldNow|
				o.now = oldNow
			};
			[o.nodes, rep.at4].flopWith{ |o2,rep2|
				putFRPState.(o2,rep2)
			}
		};

		currentEN = eventNetworks.at(index);
		currentEN !? {
			//if(currentEN.active) { currentEN.stop };
			//eventNetworks.removeAt(index);
			//store analysis of old FRP graph
			frpGraphRep = makeFRPGraphRep.(currentEN.finalIOES)
		};

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

		// Do FRP stuff here
		if( obj.isFunction || (obj.isKindOf(Association)) ) {
			newEN = EventNetwork(Writer(Unit, ENDef.tempBuilder));

			if( currentEN.notNil ) {
				var check, shapeMatches, functionsMatch;

				//compare new and old FRP graphs
				check = checkFRPSameGraph.( newEN, frpGraphRep);
				shapeMatches = check.at1;
				functionsMatch = check.at2;
				//if(shapeMatches.not) { "shapes don't match !".postln };
				//if(functionsMatch.not) { "functions don't match !".postln };

				if( functionsMatch ) {
					"same FRP graph with same functions, not destroying EN graph".postln;
				} {
					var newKeys;
					if( currentEN.active ) { currentEN.stop };
					eventNetworks = eventNetworks.put(index, newEN);
					if( shapeMatches ) {
						"same FRP graph but with functions, copying state from old graph".postln;
						putFRPState.(newEN.finalIOES, frpGraphRep);
					};
					newEN.start;
					frpControls.put(index, NNdef.buildControls);
					//only doing sets here, because the value of now might need to be
					//modyfied by putFRPState
					NNdef.setsToPerform.do{ |x|
						if(x.at1) {
							this.setUni(x.at2, x.at3.now)
						} {
							this.set(x.at2, x.at3.now)
						}
					};
					//should remove keys which are not set anymore.
					newKeys = NNdef.setsToPerform.collect{ |x| x.at2 };
					previousFRPControls !? { |x|
						var keysToRemove = x.copy.removeAll(NNdef.buildControls);
						//x.cs.postln;
						//NNdef.buildControls.cs.postln;
						//"keys to be removed: %".format(keysToRemove).postln;
						keysToRemove.do{ |x|
							this.nodeMap.unset(x);
					}}
				}
			} {
				eventNetworks = eventNetworks.put(index, newEN);
				newEN.start;
				frpControls.put(index, NNdef.buildControls);
				NNdef.setsToPerform.do{ |x|
					if(x.at1) {
						this.setUni(x.at2, x.at3.now)
					} {
						this.set(x.at2, x.at3.now)
					}
				};
			}
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

	prEnGeneric { |rate, lag = 0.1, key, spec, debug = false|

		var controlName = key ?? { NNdef.nextControl() };
		var thisUdef = NNdef.buildCurrentNNdefKey;
		NNdef.buildControls = NNdef.buildControls.addI(controlName);
		^if(spec.notNil){
			spec = spec.asSpec;
			NNdef(thisUdef).addSpec(controlName, spec);
			this.collect{ |v| IO{ NNdef(thisUdef).setUni(controlName, v) } }.enOut2;
			//delay this
			//NNdef(thisUdef).setUni(controlName, this.now);
			NNdef.setsToPerform = NNdef.setsToPerform.addI(T(true, controlName, this));
			if(debug) {
				this.collect( spec.map(_) ).enDebug(controlName.asString)
			};
			controlName.perform(rate, spec.map(this.now), lag)
		}{
			this.collect{ |v| IO{ NNdef(thisUdef).set(controlName, v) } }.enOut2;
			//delay this
			//NNdef(thisUdef).set(controlName, this.now);
			NNdef.setsToPerform = NNdef.setsToPerform.addI(T(false, controlName, this));
			if(debug) {
				this.enDebug(controlName.asString)
			};
			controlName.perform(rate, this.now, lag)
		};

	}

}


+ EventSource {

	enKr { |lag = 0.1, initialValue=0, key, spec, debug = false|

		var controlName = key ?? { NNdef.nextControl() };
		var thisUdef = NNdef.buildCurrentNNdefKey;
		NNdef.buildControls = NNdef.buildControls.addI(controlName);
		if(spec.notNil){
			spec = spec.asSpec;
			NNdef(thisUdef).addSpec(controlName, spec);
			this.collect{ |v| IO{ NNdef(thisUdef).setUni(controlName, v) } }.enOut;
			if(debug) {
				this.collect( spec.map(_) ).enDebug(controlName.asString)
			}
		}{
			this.collect{ |v| IO{ NNdef(thisUdef).set(controlName, v) } }.enOut;
			if(debug) {
				this.enDebug(controlName.asString)
			}
		};
		^controlName.kr(initialValue, lag)
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