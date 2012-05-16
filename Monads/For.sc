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


	For comprehensions based on scala's for and haskell's do.

*/
For {

	*new { |... funcs|
		var recursiveFunc = { |prevValues,funcs|
			
			if(funcs.size == 2) {
				var v = funcs[0];
				if(v.size == 2) {
					v = v[0].value(*prevValues).select(v[1])
				}{
					v = v.value(*prevValues)	
				}; 
				v.fmap{ |x|
					funcs[1].value(*([x]++prevValues))
				}	
			} {
				var v = funcs.first;
				if(v.size == 2) {
					v = v[0].value(*prevValues).select(v[1])
				}{
					v = v.value(*prevValues)		
				};
				v >>= { |x| recursiveFunc.([x]++prevValues,funcs[1..]) }
			}
		};
		funcs = funcs.collect{ |x| if((x.class == Function) || (x.size > 1)){x}{ { x } } };
		if(funcs.size > 1) {		
			^recursiveFunc.([],funcs)				
		} {		
			"For needs at least two functions".warn
		}		
	}	
	
}