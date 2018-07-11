//sum with last value
//recursive expressions
/*

frp in haskell can express mutually recursive values
This allows a value to depend on it's own past value:

reactive-banana
sum to the last value:

e2 = (+) <$> b <@> e1
b = stepper 0 e2

unfortunatelly this cannot be done this way in SuperCollider so

f <$> sig <@> es

must be changed to

f.liftRecSampled({sig}, es)

{sig} will delay execution of sig

*/
(
//Setup GUI
var nb1 = NumberBox();
var nb2 = NumberBox();

w = Window(bounds:Rect(200,200,200,100)).layout_(
	HLayout( StaticText().string_("In:"), nb1, StaticText().string_("Out:"), nb2 )
    );

//Setup Event Network
//network
ENdef(\x, {
	var sumSig, sum;

    //in
    var nb1ES = nb1.enIn;

	sum = (_+_).liftRecSampled({sumSig}, nb1ES);
	sumSig = sum.hold(0);

	//out
	nb2.enSinkValue( sum );

}).start;

w.front;
)