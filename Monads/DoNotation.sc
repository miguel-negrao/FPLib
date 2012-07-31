DoNotation {
	classvar <>debug = false;

	*processDoBlock { |string|
		var arrows = string.findAll("<-");
		var colons = string.findAll(";");
		var return = string.find("return");
		var lets = string.findAll("let ") ? [];
		var guard = string.find("|||");

		^if( colons.size == 0) {
			"Do block has no ;s !".fail;
		} {
			var variable, m, returnvalue;
			var colonsOfLets = None;
			var colonsAfterLastVar = if( lets.size > 0 ) {
				var colonsAfterLastVar = colons.select{ |i| 
					i > lets[0] 
				};
				var x = lets.collect{ |leti|
					var comma = colons.inject(None, { |state,commaj| 
						
						if(state.isDefined.not) { if( commaj > leti ) { Some(commaj) } { state } } { state }
					} );
					comma.do( colonsAfterLastVar.remove(_) );
					comma
				};
				colonsOfLets = x.sequence;
				colonsAfterLastVar;
			} {
				colons[1..]
			};	
			var lastColon = colonsOfLets.collect(_.last).getOrElse(colons[0]);
			var varexpression = if( lets.size > 0 ) { string[(colons[0]+1)..lastColon].replace("let","var")++" " } { "" };
			var thereIsAnArrowBeforeTheFirstComma = if( arrows.size == 0) {
				false
			} {
				arrows[0] < colons[0]
			};		
			var guardExpression = "";	
	
			if( thereIsAnArrowBeforeTheFirstComma ) {
				var ar0 = arrows[0];
				variable ="|"++string[..(ar0-1)].stripWhiteSpace++"| ";	
				if( guard.notNil ) {
					m = string[(ar0+2)..(guard-1)].stripWhiteSpace;
					guardExpression = ".select { "++variable++string[(guard+3)..(colons[0]-1)]++" }";
				} {
					m = string[(ar0+2)..(colons[0]-1)].stripWhiteSpace;
				}
			} {
				variable = "";
				m = string[..colons[0]-1].stripWhiteSpace		
			};		
			
			returnvalue = if( colonsAfterLastVar.size > 0 ) {
				var rest = this.processDoBlock(string[(lastColon+1)..]);
				rest >>= { |r| m++guardExpression++" >>= { "++variable++varexpression++r++" } " }
			
			}{
				//ends with return ?
				if( return.isNil ){
					var expression = string[(lastColon+1)..];
					m++guardExpression++" >>= { "++variable++varexpression++expression++" }"
				}{
					var expression = string[(return+6)..].stripWhiteSpace;
					m++guardExpression++".fmap{ "++variable++varexpression++expression++" }"
				};	
			
			};
			
			if(this.debug) {
				"lets: %".format(lets).postln;
				"colons: %".format(colons).postln;			
				"last colon: %".format(lastColon).postln;
				"colonsOfLets %".format(colonsOfLets).postln;
				"colonsAfterLastVar %".format(colonsAfterLastVar).postln;		
				"number of colons after last var is %".format(colonsAfterLastVar.size).postln;
				"thereIsAnArrowBeforeTheFirstComma : %".format(thereIsAnArrowBeforeTheFirstComma).postln;
				"varexpression %".format(varexpression).postln;
				"********************************************".postln;
				"returnvalue : %".format(returnvalue).postln;
				"********************************************".postln;
			};
			returnvalue.success
		}
	
	}
	
	// returns Validation[String, Int]
	*getClosingP { |string, numpar, sindex|
		^string.match({
			"Cannot find end of Do block".fail;
		},{ |head,tail|
			switch(head) 
			{$(} { 
				//"adding p".postln;
				this.getClosingP( string.tail, numpar+1, sindex+1) }
			{$)} { 
				var nextnpar = numpar -1;
				//"removing p".postln;
				if( nextnpar == 0 ) {
					sindex.success
				} {
					this.getClosingP( string.tail, nextnpar, sindex+1)
				}
			} {
				this.getClosingP( string.tail, numpar, sindex+1 )
			}					
		})
	}
	
	//var processStringNoNL = { |st| processString.(st.replace("\n","").replace(Char.tab.asString," ") ) };
	//returns Success( string ) of Failure
	*processString { |string| //string is Validation[String,String]
		var starts = string.findAll("Do(") ? [];
		^if( starts.size == 0 ) {
			string.success;
		} {
			var preends = starts.collect{ |i|
				this.getClosingP( string[(i+3)..].as(Array).asLazy,1,0 ).collect{ |x| x+i+3 }
			}.sequence;
			var ends = if(preends.isKindOf(Array)){ preends.success } {preends};
			var doList = [ starts.success, ends].sequence; // its a Failure( string) or a Success( [ starts, ends] )			
			doList >>= { |list|
				var floppedList = list.flop;
				var checkNesting = (floppedList >>= { |inner1|
					/*
					errors:
					Do(1     Do(2   )1   )2
					Do(2     Do(1   )2   )1
					*/
					floppedList >>= { |inner2|
						//errors:
						//Do(1     Do(2   )1   )2
						( (inner1[0] < inner2[0]) && (inner1[1] < inner2[1]) && (inner1[1] > inner2[0]) ) ||
						//Do(2     Do(1   )2   )1
						( (inner2[0] < inner1[0]) && (inner2[1] < inner1[1]) && (inner2[1] > inner1[0]) )
						//return true means we have a problem
					}
				} ).reduce('||');	
				if( checkNesting ) {
					"Do statements interlocked".fail
				} {			
					var orderedDoBloks = list.flop.sort{ |inner1, inner2|
						//first do is before second do
						if( (inner1[0]<inner2[0]) && (inner1[1] < inner2[1]) ) {
							true
						} {
							//first do is inside second do
							if( (inner1[0]>inner2[0]) && (inner1[1] < inner2[1]) ) {
								true
							} {
								//second do is inside first do
								if( (inner1[0]<inner2[0]) && (inner1[1] > inner2[1]) ) {
									false
								} {
									//first do is after second do
									if( (inner1[0]>inner2[0]) && (inner1[1] > inner2[1]) ) {
										false
									}
								}										
							}			
						}
					};
					var start = orderedDoBloks[0][0];
					var end = orderedDoBloks[0][1];		
					this.processDoBlock(string[(start+3)..(end-1)]) >>= { |x| 
						var newString = string[..(start-1)]++x++string[(end+1)..]; 
						this.processString(newString)
					};				
				}				
			}
		}
		
	}
	
	*processIOAssignment { |string|
		var do = string.find("Do(");
		var arrow = string.find("<-");
		// if there are not Do's consider that the arrow is for IO
		// if there are Do's the arrow must be before all Do's
		if( (do.isNil && arrow.notNil) || ( do.notNil && arrow.notNil and: { arrow < do } ) ) {
			IO.environmentVarForResult = string[..(arrow-1)].stripWhiteSpace;
			^this.processString(string[(arrow+2)..]);			
		} {
			IO.environmentVarForResult = nil;
			^this.processString(string)
		}	
	}		

	*activate {		
		thisProcess.interpreter.preProcessor = { |code| 
			this.processIOAssignment(code).match({|string|
				string			
			}, { |e| Error("Do Notation parsing error -> "++e).throw });
		};
		Unit
	}
	
	*deactivate {
		thisProcess.interpreter.preProcessor = nil;
		Unit
	}
		
}