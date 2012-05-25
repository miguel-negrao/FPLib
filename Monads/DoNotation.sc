DoNotation {
	classvar <>debug = false;

	*activate {
		
		var processDoBlock = { |string|
			var arrows = string.findAll("<-");
			var colons = string.findAll(";");
			var return = string.find("return");
			var lets = string.findAll("let ") ? [];
			var guard = string.find("|||");

			if( colons.size == 0) {
				Error("zero ;").throw;
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
					var rest = processDoBlock.(string[(lastColon+1)..]);
					m++guardExpression++" >>= { "++variable++varexpression++rest++" } "
				
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
				returnvalue
			}
		
		};
		
		var getClosingP = { |string, numpar, sindex|
			string.match({
				if( debug ) {
					"Syntax error: ~getClosingP cannot find close of Do block".postln;
				};
				None
			},{ |head,tail|
				switch(head) 
				{$(} { 
					//"adding p".postln;
					getClosingP.( string.tail, numpar+1, sindex+1) }
				{$)} { 
					var nextnpar = numpar -1;
					//"removing p".postln;
					if( nextnpar == 0 ) {
						Some(sindex)
					} {
						getClosingP.( string.tail, nextnpar, sindex+1)
					}
				} {
					getClosingP.( string.tail, numpar, sindex+1 )
				}					
			})
		};
		
		//var processStringNoNL = { |st| processString.(st.replace("\n","").replace(Char.tab.asString," ") ) };
		
		var processString = { |string|
			var starts = string.findAll("Do(") ? [];
			if( starts.size == 0 ) {
				string;
			} {
				var preends = starts.collect{ |i|
					getClosingP.( string[(i+3)..].as(Array).asLazy,1,0 ).collect{ |x| x+i+3 }
				}.sequence;
				var ends = if(preends.isKindOf(Array)){ Some(preends) } {preends};
				var doList = [Some(starts), ends].sequence;
				if( doList.isDefined) {
					var x = doList.get.flop.sort{ |inner1, inner2|
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
									Error("Do block syntax error: Do statements interlocked").throw
								}										
							}			
						}
					};
					var start = x[0][0];
					var end = x[0][1];		
					var newString = string[..(start-1)]++processDoBlock.(string[(start+3)..(end-1)])++string[(end+1)..];
					processString.(newString)
				} {
					Error("Do block syntax error: missing closing parenthesis").throw
				}
				
			} {
				string		
			}
		};		
		
		thisProcess.interpreter.preProcessor = { |code| processString.(code) };
		Unit
	}
	
	*deactivate {
		thisProcess.interpreter.preProcessor = nil;
		Unit
	}
		
}