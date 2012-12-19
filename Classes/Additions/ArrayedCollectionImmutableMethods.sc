+ ArrayedCollection {

    swapI { arg i, j;
        ^this.copy.swap(i,j)
	}

    remoteAtI { arg index;
        ^this.copy.remoteAt(index)
    }

    addI { arg item;
        ^this.copy.add(item)
    }

    sortI { |f|
        ^this.copy.sort(f)
    }

    prependI { |item|
        ^[item]++this
    }



}



   /*
bench{

//faster
1000.collect{
var x = [1,2,3];
x.copy.add(3);
}
}

bench{
1000.collect{
var x = [1,2,3]++[3];
x
}
}

*/