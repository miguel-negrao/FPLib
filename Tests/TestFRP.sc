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
		this.assert((before == "ok_before") && (after == "ok_after"),"after re-evaluation the same FRP graph signal state (now value) should be kept.")
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
			.store("nothing",'keyA');
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
		this.assert((after == "test 1") && (NNdef(\a).get('keyA') == "test 1"),
			"When an NNdef which has an input node which is connected to an ouput node of another NNdef, firing an event in the first NNdef should also fire the event in the second NNdef and the value should be stored in the first NNdef")
	}

}