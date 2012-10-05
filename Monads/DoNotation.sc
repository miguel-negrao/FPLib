/*

Todo:

implement validations
Allow function definitions inside, i.e. have ; if they are inside { }
Distinguish Do( from "Do("

Rules:

Inside do blocks:
- no ; other then those of the Do block
- You can have Do blocks inside other do blocks.

// m =  0
Do( expr1 ; expr2 ) -> expr1 >>= { expr2 }
Do( x <- a; expr ) -> a >>= { |x| expr }
Do( x <- a; return expr x ) -> a.fmap{ |x| expr x }

// m > 0;
Do( x <- expr1; ...rest ) -> expr1 >>= { |x| ...rest };
Do( x <- expr1; let a1 = expr2; let a2 = expr3; ...rest ) -> expr1 >>= { |x| var a1 = expr2; var a2 = expr3; ...rest };

*/


DoNotation {
    classvar <>debug = false;

    *optionToValidation { |o, string|
        ^o.match({|x| x.success },{ string.fail })
    }

    *processDoBlock { |string|
        //Validation[Int]
        var semicolons = this.optionToValidation(string.findAll(";").asOption, "Do block has no ;s !");
        //Option[Int]
        var lets = string.findAll("let ").asOption;
        //Option[Int]
        var arrow = string.find("<-").asOption;
        //Option[Int]
        var return = string.find("return").asOption;
        //Option[Int]
        var guard = string.find("|||").asOption;


        // findSemicolonOfLastLet gives position of ; of last let relative to this
        //expression and a list with the rest of the ;s.
        //returns Tuple2[ Option[int], LazyList ]
        // at1 -> pos of last; relative to let statement
        // at2 -> list with remaining ; after the ; of the last let statment of the block being analyzed
        //
        //Function : Tuple2[ Option[Int], LazyList ] LazyList LazyList ->  Tuple2[ Option[Int], LazyList ]
        var findSemicolonOfLastLet = { |state, lets, semicolons|
            if(this.debug) {
                "findSemicolonOfLastLet: %, %, %".format(state, lets, semicolons).postln;
            };
            lets.match(
                {   //there are no more lets
                    state
                },{ |letshead,letstail|
                    semicolons.match({
                        "there are lets but no more ; that is an error, no ?".fail
                        },{ |shead,stail|
                            //check that the next let is before the next ;
                            if(letshead < shead) {
                                //keep looking for more lets
                                findSemicolonOfLastLet.( Tuple2( Some( shead ), stail).success, letstail, stail)
                            } {
                                // let blah blah;  -->blah;(1)<--- let (2) ;
                                //there is a ; (1) before the next let (2) so that let not part of this let block
                                //just return and stop searching.
                                state
                            }
                    })
            })
        };

        //will now use >>= to colapse all possible failures
        ^(semicolons >>= { |semicolons|
            //inside validation

            //Validation[Tuple2[Option[Int],LazyList]]
            var semicolonsRelativeToLetsTuple = if(semicolons.size == 1 ) {
                //only one semicolon
                Tuple2(None, LazyListEmpty).success
            } {
                var restOfSemicolons = semicolons[1..].asLazy;
                var default = Tuple2(None, restOfSemicolons).success;
                lets.collect{ |lets|
                    findSemicolonOfLastLet.( default, lets.asLazy, restOfSemicolons )
                }.getOrElse( default )
            };

            semicolonsRelativeToLetsTuple >>= { |semicolonsRelativeToLetsTuple|

                //Option[Int]
                var positionOfLastSemicolonOfLetsExpression = semicolonsRelativeToLetsTuple.at1;

                //Int
                var posLastSemicolonToBeProcessed = positionOfLastSemicolonOfLetsExpression.getOrElse( semicolons[0] );

                //Bool
                var areThereSemicolonsAfterTheFirstSemicolon = semicolonsRelativeToLetsTuple.at2.asArray.size > 0;

                //String
                var varExpression = positionOfLastSemicolonOfLetsExpression.collect{ |p|
                    string[(semicolons[0]+1)..p].replace("let","var")++" "
                }.getOrElse("").stripWhiteSpace;


                //Option[Int]
                var arrowPosBeforeTheFirstSemicolonOption = arrow.flatCollect{ |pos|
                    if( pos < semicolons[0] ) {
                        Some(pos)
                    } {
                        None
                    }
                };

                //[String]
                //[first expression, variable]
                var strings = arrowPosBeforeTheFirstSemicolonOption.collect{ |arrowPos|
                    var variable = "|" ++ string[..(arrowPos-1)].stripWhiteSpace ++ "| ";
                    [
                        //first expression with guard
                        guard.collect{ |guard|
                            string[(arrowPos+2)..(guard-1)].stripWhiteSpace ++
                            ".select { "++variable++string[(guard+3)..(semicolons[0]-1)]++" }"
                        }.getOrElse(
                            string[(arrowPos+2)..(semicolons[0]-1)].stripWhiteSpace
                        ),
                        //variable name to the left of arrow
                        variable
                    ]

                }.getOrElse(
                    //just first expression no arrow
                    [string[..semicolons[0]-1].stripWhiteSpace, "", ""]
                );

                //Validation[String]
                //continue collapsing failures
                var returnvalue = if(areThereSemicolonsAfterTheFirstSemicolon){
                    //Nº ; > 1

                    //Validation[String]
                    var rest = this.processDoBlock(string[(posLastSemicolonToBeProcessed+1)..]);
                    rest.collect{ |rest|
                        strings[0]++" >>= { "++strings[1]++varExpression++rest++" } "
                    }
                } {
                    //Nº ; == 1

                    //ends with return ?
                    return.collect{ |return|
                        var endExpression = string[(return+6)..].stripWhiteSpace;
                        strings[0]++".fmap{ "++strings[1]++varExpression++endExpression++" }"
                    }.getOrElse({
                        var endExpression = string[(posLastSemicolonToBeProcessed+1)..];
                        strings[0]++" >>= { "++strings[1]++varExpression++endExpression++" }"
                    }.value).success
                };

                if(this.debug) {
                    "";
                    "*********processDoBlock*********************";
                    "semicolons: %".format(semicolons).postln;
                    "lets: %".format(lets).postln;
                    "semicolonsRelativeToLetsTuple: %".format(semicolonsRelativeToLetsTuple).postln;
                    "posLastSemicolonToBeProcessed: %".format(posLastSemicolonToBeProcessed).postln;
                    "areThereSemicolonsAfterTheFirstSemicolon: %".format(areThereSemicolonsAfterTheFirstSemicolon).postln;
                    "varExpression: %".format(varExpression).postln;
                    "arrowPosBeforeTheFirstSemicolonOption: %".format(arrowPosBeforeTheFirstSemicolonOption).postln;
                    "strings: %".format(strings).postln;
                    "********************************************".postln;
                    "returnvalue : %".format(returnvalue).postln;
                    "********************************************".postln;
                };
                returnvalue
            }
        })
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
    //returns Success( string ) or Failure
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
                    var doBlockString = this.removeComments( string[(start+3)..(end-1)] );
                    this.processDoBlock(doBlockString) >>= { |x|
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

    *removeComments { |string|
        /*
        remove anything between // and end of line
        remove anything between a /* and the next */
        */
        ^this.removeOneLineComments(
            this.removeBlockComments( string )
        )
    }

    *removeOneLineComments { |string|
        var start = string.find("//");
        ^if(start.notNil) {
            var end = string[(start+2)..].find("\n");
            if(end.isNil) {
                this.removeOneLineComments(string[..(start-1)])
            } {
                this.removeOneLineComments(string[..(start-1)]++string[(start+2+end+1)..])
            }
        } {
            string
        }
    }

    *removeBlockComments { |string|
        var start;
        start = string.find("/*");
        ^if(start.notNil) {
            var end = string[(start+2)..].find("*/");
            if(end.isNil) {
                Error("no matching */ inside a Do statement").throw
            } {
                this.removeBlockComments( string[..(start-1)]++string[(start+2+end+2)..] )
            }
        } {
            string
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