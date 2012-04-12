+ Object {

	esFromGUI {
		var x = EventSource();
		this.action_{ |v| x.fire(v.value) };
		^x
	}

	signalFromGUI {
		var x = Var(this.value);
		this.action_{ |v| x.value_(v.value) };
        ^x
	}

}