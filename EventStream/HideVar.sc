
//you can set a value as an internal change which gets propagated via the ES returned by changes
// you can acess an ES that only propagates external changes
// this allows to connect two things that will send values to each other avoiding feedback.

HideVar : Var {
	var <justValues;

	*new { |now|
		^super.new( Tuple2(\external,now) ).initFeedbackVar
	}

	initFeedbackVar {
		justValues = changes.collect(_.at2 );
	}

	value_  { |x|
		^super.value_( Tuple2( \external, x) )
	}

	changes {
		^justValues
	}

	now {
		^super.now.at2
	}

	//must be a Tuple2
	internalValue_ { |x|
		^super.value_( Tuple2( \internal, x) )
	}

	allChanges {
		^changes
	}

	externalChanges {
		^changes.select{ |x| x.at1 == \external }.collect( _.at2 );
	}

	internalChanges {
    	^changes.select{ |x| x.at1 == \internal }.collect( _.at2 );
    }

	do{ |f|
		^justValues.do(f)
	}

	stopDoing { |f|
		^justValues.stopDoing(f)
	}

}