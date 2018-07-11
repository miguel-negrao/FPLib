(

~but1 = Button();
~but2 = Button();
~text = StaticText();

w = Window(bounds:Rect(200,200,200,200)).layout_(
        HLayout( ~but1, ~but2, ~text )
    ).front;

//network
ENdef(\x, {
    //inputs
    var but1E = ~but1.enIn;
	var but2E = ~but2.enIn;

	var counter = but1E.collect{ { |x| x - 1 } }
	.merge( but2E.collect{ { |x| x + 1 } } )
	.injectF(0);

	~text.enSink( counter.collect{ |x| [\string_, x] } );

}).start;
)