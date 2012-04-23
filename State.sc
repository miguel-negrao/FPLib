/*
    FP Quark
    Copyright 2012 Miguel Negrão.

    FP Quark: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FP Quark is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with FP Quark.  If not, see <http://www.gnu.org/licenses/>.
*/


State {
  	var <f;

  	*initClass{
		Class.initClassTree(TypeClasses);
		//type instances declarations:
		TypeClasses.addInstance(this,
			(
				'fmap': { |fa,f|
					State({ |s|
						var stuple = fa.f.(s);
						Tuple2( stuple.at1, f.(stuple.at2) )
					})
				},
				'bind': { |fa,f|
					State({ |s|
						var satuple = fa.f.(s);
						f.(satuple.at2).runState(satuple.at1)
					})
                },
				'pure': { |r| State({ |s| Tuple2(s,r) }) }
			)
		);
	}

  	//f: S => (S, A)
  	*new { |f| ^super.newCopyArgs(f) }

  	*put { |newState|
  		^State({ |oldState| Tuple2(newState,Unit) })
  	}

    *get { ^State({ |s| Tuple2(s,s) }) }

  	runState { |s| ^f.(s) }
  	evalState { |s| ^this.runState(s).at2 } //return the result
  	execState { |s|^this.runState(s).at1 } //return the state

	//g:S => S
	withState { |g| ^State(f <> g )  }

}
