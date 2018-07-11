/*

Example of setting up paged controls

*/

(
q = ();

//setup gui
q.sl = Slider();
q.but1 = Button();
q.but2 = Button();
q.text = StaticText();
q.window = Window().layout_(HLayout(q.sl, q.but1, q.but2, q.text));

q.defaults = { rrand(0.0,1.0) } ! 10;

q.numPages = q.defaults.size;

ENdef(\x, {
	//get guis as event streams
	//for this case event streams are better then signals
	var slE = q.sl.enIn;
	var but1E = q.but1.enIn;
	var but2E = q.but2.enIn;

	//page counter
	var increasePage = { |x| (x + 1).clip(0, q.numPages-1) };
	var decreasePage = { |x| (x - 1).clip(0, q.numPages-1) };

	var page = but1E.collect{ decreasePage }
	.merge(but2E.collect{ increasePage })
	.injectF(0);

	//display page
	var dpage = q.text.enSink( page.collect{ |x| [\string_, "page : %".format(x+1)] } );

	//page.enDebug("page");

	//paged slider
	var x = slE.paged(page, q.defaults);
	x.at1.collect{ |es, i|
		es.enDebug("page "++(i+1))
	};

	//x.at1 is an array of event sources, one per page
	//x.at2 is an event source which fires when there is a page change
	//and carries T(n,v) where n is the page which became current
	//and v is its value
	x.at2.enDebug("value");
	//reset slider on page change
	q.sl.enSinkValue( x.at2.collect(_.at2));

}).start;

q.window.front;
)

(
Endef(\x).stop;
q.window.close;
)

