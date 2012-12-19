+ Object {

	esFromGUI {
		var x = EventSource();
		this.addAction({ |v| x.fire(v.value) });
		^x
	}

	signalFromGUI {
		var x = Var(this.value);
		this.addAction({ |v| x.value_(v.value) });
        ^x
	}

}