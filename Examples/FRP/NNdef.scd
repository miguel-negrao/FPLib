NdefMixer(s)

//to get an FPSignal or EventStream into the synthdef use .enKr
//controls are auto-created and auto-connected.
(
NNdef(\x, {
	var freq = MIDIMKtl('bcr20000').elementAt(\kn,0,0).enIn.nexp(100,1000).enKr;
	SinOsc.ar( freq ) * 0.1
}).play
)

//midi actions auto-released, no explicit bookkeeping needed.
(
NNdef(\x, {
	SinOsc.ar( 400 ) * 0.1
}).play
)

//on changing function event network is automatically destroyed and recreated
(
NNdef(\x, {
	var freq = MIDIMKtl('bcr20000').elementAt(\kn,0,0).enIn.nexp(100,1000).enKr;
	var amp = MIDIMKtl('bcr20000').elementAt(\bt,0,0).enIn.enKr;
	SinOsc.ar( freq ) * 0.1 * amp
}).play
)

//multiply server side
(
NNdef(\x, {
	var freq = MIDIMKtl('bcr20000').elementAt(\kn,0,0).enIn.nexp(100,1000).enKr;
	var bt = MIDIMKtl('bcr20000').elementAt(\bt,0,0).enIn.enKr;
	var sl = MIDIMKtl('bcr20000').elementAt(\kn,1,0).enIn.enKr;
	var amp = bt * sl;
	SinOsc.ar( freq ) * 0.5 * amp;
}).play
)

//multiply sclang side
(
NNdef(\x, {
	var freq = MIDIMKtl('bcr20000').elementAt(\kn,0,0).enIn.nexp(100,1000).enKr;
	var bt = MIDIMKtl('bcr20000').elementAt(\bt,0,0).enIn;
	var sl = MIDIMKtl('bcr20000').elementAt(\kn,1,0).enIn;
	var amp = (bt * sl).enKr;
	SinOsc.ar( freq ) * 0.5 * amp;
}).play
)

NNdef(\x).controlKeysValues
s.dumpOSC(1)

// 3 knobs -> 3 freqs
(
NNdef(\x, {
	var freqs = MIDIMKtl('bcr20000').elements[\kn][0][..8].collect{ |e| e.enIn.nexp(100,1000).enKr };
	SinOsc.ar( freqs ).sum * 0.1
}).play
)


NNdef(\x).clear