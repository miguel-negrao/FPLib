+ OSCFunc {

    *asENInput { |path, srcID, recvPort, argTemplate, dispatcher|
        var es = EventSource().postln;
        var addHandler = IO{
            var p = "create".postln;
            var f = { |msg| es.fire(msg[0].postln) }.postln;
            var osc = OSCFunc(f, path, srcID, recvPort, argTemplate, dispatcher).postln;
			IO{ "free".postln;osc.free }.postln
		};
        ^Writer( es, Tuple3([addHandler],[],[]) )
    }
}