/*
(
~post = { |result|
    result.postln;
    ("dif t: "++(Process.elapsedTime -t)).postln
};
)

(
t = Process.elapsedTime;
x = Promise{ 2.wait; 2};
x.do(~post) //waits 2 seconds
)


(
t = Process.elapsedTime;
x = Promise{ 2.wait; 2};
y = x.collect(_+100);
y.do(~post ) //waits 2 seconds
)

(
t = Process.elapsedTime;
x = Promise{ 1.wait; 2};
y = Promise{ 3.wait; 4};
z = x >>= { |x| y };
z.do( ~post ); //will wait 3 seconds in total
)
*/

Promise2 {

    var value;
    var routine;
    var c;

    *new { |f|
        ^super.new.init(f)
    }

    init { |f|
        c = Condition();
        routine = Routine({
            value = f.();
            c.test = true;
            c.signal;
        }).play
    }

    get {
        c.wait;
        ^value
    }

    >>= { |f|
        ^Promise{
            var x = this.get;
            f.(x).get;
        }
    }

    collect{ |f|
        ^Promise{
            var x = this.get;
            f.(x)
        }
    }

    do{ |f|
        fork{
            f.(this.get)
        }
    }


}

