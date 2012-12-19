/*

f = { |x| x+1 };
g = { |x| x*20 };
(f &&& g).(10)

*/
+ AbstractFunction {

    // from Control.Arrow (&&&) :: a b c -> a b c' -> a b (c,c')
    &&& { |aFunction|
        ^{ |...args| T( this.valueArray(args), aFunction.valueArray(args) ) }
    }

}
        