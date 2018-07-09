TestFRP : UnitTest {

	test_ENdefState_collect {
		var tup = EventNetwork.newAddHandler();
		var registerAction = tup.at1;
		var fire = tup.at2;
		var before, after;
		ENdef(\test).clear;
		ENdef(\test, {
			var es = EventNetwork.enFromAddHandler(registerAction);
			es
			.collect{ 'a' }
			//.enDebug("before")
			.collect{|x| IO{ before = x}}
			.enOut;
		}).start;
		fire.(nil);
		ENdef(\test, {
			var es = EventNetwork.enFromAddHandler(registerAction);
			es
			.collect{ 'b' }
			//.enDebug("after")
			.collect{|x| IO{ after = x}}
			.enOut;
		});
		fire.(nil);
		this.assert(after == 'b', "after re-evaluation the same FRP graph with a different function in a collect node, the new function should be used")
	}

	test_ENdefState_inject {
		var tup = EventNetwork.newAddHandler();
		var registerAction = tup.at1;
		var fire = tup.at2;
		var before, after;
		ENdef(\test).clear;
		ENdef(\test, {
			var es = EventNetwork.enFromAddHandler(registerAction);
			es
			.inject(0, _+1)
			//.enDebug("before")
			.collect{|x| IO{ before = x}}
			.enOut;
		}).start;
		fire.(nil);
		ENdef(\test, {
			var es = EventNetwork.enFromAddHandler(registerAction);
			es
			.inject(0, _ - 1)
			//.enDebug("after")
			.collect{|x| IO{ after = x}}
			.enOut;
		});
		fire.(nil);
		this.assert((before == 1) && (after == 0), "after re-evaluation the same FRP graph possibly with a different inject function, the previous inject state should be kept")
	}

	test_ENdefState_injectF {
		var tup = EventNetwork.newAddHandler();
		var registerAction = tup.at1;
		var fire = tup.at2;
		var before, after;
		ENdef(\test).clear;
		ENdef(\test, {
			var es = EventNetwork.enFromAddHandler(registerAction);
			es
			.collect({{|x|x+1}})
			.injectF(0)
			//.enDebug("before")
			.collect{|x| IO{ before = x}}
			.enOut;
		}).start;
		fire.(nil);
		ENdef(\test, {
			var es = EventNetwork.enFromAddHandler(registerAction);
			es
			.collect({{|x|x-1}})
			.injectF(0) //previous state is used instead of 0
			//.enDebug("after")
			.collect{|x| IO{ after = x}}
			.enOut;
		});
		fire.(nil);
		this.assert((before == 1) && (after == 0), "after re-evaluation the same FRP graph possibly with a different inject function, the previous inject state should be kept")
	}

	test_ENdefState_signal {
		var tup1 = EventNetwork.newAddHandler();
		var registerAction1 = tup1.at1;
		var fire1 = tup1.at2;
		var tup2 = EventNetwork.newAddHandler();
		var registerAction2 = tup2.at1;
		var fire2 = tup2.at2;
		var before, after;
		ENdef(\test).clear;
		ENdef(\test, {
			var es = EventNetwork.enFromAddHandler(registerAction1);
			var sig = EventNetwork.enFromAddHandler(registerAction2).hold("notok_");

			var es2 = (_++_) <%> sig <@> es;

			es2
			//.enDebug("before")
			.collect{|x| IO{ before = x}}
			.enOut;

		}).start;
		fire2.("ok_");
		fire1.("before");
		ENdef(\test, {
			var es = EventNetwork.enFromAddHandler(registerAction1);
			var sig = EventNetwork.enFromAddHandler(registerAction2).hold("notok_");
			//ok_ from before is used instead of notok_

			var es2 = (_++_) <%> sig <@> es;

			es2
			//.enDebug("after")
			.collect{|x| IO{ after = x}}
			.enOut;
		});
		fire1.("after");
		this.assert((before == "ok_before") && (after == "ok_after"),"after re-evaluation the same FRP graph signal state (now field of FPSignal) should be kept.")
	}

	test_ENdefState_clear {
		var tup1 = EventNetwork.newAddHandler();
		var registerAction1 = tup1.at1;
		var fire1 = tup1.at2;
		var tup2 = EventNetwork.newAddHandler();
		var registerAction2 = tup2.at1;
		var fire2 = tup2.at2;
		var before, after;
		ENdef(\test).clear;
		ENdef(\test, {
			var es = EventNetwork.enFromAddHandler(registerAction1);
			var sig = EventNetwork.enFromAddHandler(registerAction2).hold("notok_");

			var es2 = (_++_) <%> sig <@> es;

			es2
			//.enDebug("before")
			.collect{|x| IO{ before = x}}
			.enOut;

		}).start;
		fire2.("ok_");
		ENdef(\test).clear;
		ENdef(\test, {
			var es = EventNetwork.enFromAddHandler(registerAction1);
			var sig = EventNetwork.enFromAddHandler(registerAction2).hold("notok_");
			//notok_ is used due to clear.

			var es2 = (_++_) <%> sig <@> es;

			es2
			//.enDebug("after")
			.collect{|x| IO{ after = x}}
			.enOut;
		}).start;
		fire1.("after");
		this.assert(after == "notok_after", "after calling ENdef#clear state stored in inject and signal nodes should be cleared.")
	}

	test_NNdefStoreENStore {
		var tup = EventNetwork.newAddHandler();
		var registerAction = tup.at1;
		var fire = tup.at2;
		var after;
		NNdef(\a).clear;
		NNdef(\b).clear;
		NNdef(\a, {
			var es = EventNetwork.enFromAddHandler(registerAction);
			es
			//.enDebug("Inside NNDef('a')")
			.holdStore("nothing",'keyA');
			Silent.ar
		});
		NNdef(\b, {
			var es = NNdef(\a).enIn('keyA');
			es
			//.enDebug("Inside NNDef('b')")
			.collect{|x| IO{ after = x}}
			.enOut;
			Silent.ar
		});
		fire.("test 1");
		this.assert((after == "test 1") && (NNdef(\a).enGet('keyA') == "test 1"),
			"When one NNdef has an output node which is connected to an input node of another NNdef, firing an event in the first NNdef should also fire the event in the second NNdef and the value should be stored in the second NNdef")
	}

	test_NNdefInjectStore {
		var tup = EventNetwork.newAddHandler();
		var registerAction = tup.at1;
		var fire = tup.at2;
		var after, cond;
		NNdef(\a).clear;
		NNdef(\a).enSet('keyA', 20);
		NNdef(\b).clear;
		NNdef(\a, {
			var es = EventNetwork.enFromAddHandler(registerAction);
			es
			.collect({ |x| { |s| x+s }})
			.injectFStore(0,'keyA')
			.collect{|x| IO{ after = x}}
			.enOut;
			Silent.ar
		});
		fire.(1);
		cond = (after == 21);
		this.assert(cond,
			"If an stored value of an NNdef is set via enSet that value should be used instead of the default in the injectFStore")
	}

	test_NNdefInjectStore2 {
		var tup = EventNetwork.newAddHandler();
		var fire = tup.at2;
		var registerAction = tup.at1;
		var result;
		NNdef(\a).clear;
		NNdef('a', {
			var es = EventNetwork.enFromAddHandler(registerAction);
			es
			.collect{ {|x| 1-x}}
			.injectFStore(0, \toggle1)
			.collect{|val| IO{ result = val}}
			.enOut;
			Silent.ar
		});
		NNdef('a').enSet('toggle1', 1);
		fire.();
		this.assert(result == 0,
			"If an frpNodeMap key and value are set after evaluating the NNdef, the value should be used when the frp network receives a new event.");
	}

	test_NNdefAsCode {
		var tup = EventNetwork.newAddHandler();
		var fire = tup.at2;
		var code;
		//needs to be global variable otherwise we create open function
		//which cannot be stored
		~registerAction = tup.at1;
		NNdef(\a).clear;
		NNdef(\a, {
			var es = EventNetwork.enFromAddHandler(~registerAction);
			es
			.collect{ {|x| x+1}}
			.injectFStore(0, \counter)
			//.enDebug("Inside NNDef('a')")
			.collect{|val| IO{ ~result = val}}
			.enOut;
			Silent.ar
		});
		fire.();
		fire.();
		fire.();
		//state is now 3
		//get code that creates this NNdef in this state
		code = NNdef(\a).asCode;
		NNdef(\a).clear;
		tup = EventNetwork.newAddHandler();
		fire = tup.at2;
		~result = nil;
		~registerAction = tup.at1;
		code.interpret;
		fire.();
		//state when the NNdef was saved was 3 so now after firing once more should be 4
		this.assert(~result == 4, "Calling .asCode on an NNdef should save the current state of the FRP network via enSet.");
	}

}