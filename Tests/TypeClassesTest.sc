TypeInstancesTests : UnitTest {

    test_IO {

        this.assertEquals( IO{ 1 }.collect{ |x| x + 1 }.unsafePerformIO, 2, "IO collect" );
        this.assertEquals( IO{ IO{ 1 } }.join.unsafePerformIO, 1, "IO join" );
        this.assertEquals( (IO{ 1 } >>= { |x| IO{ x + 1 } }).unsafePerformIO, 2, "IO bind" );
        this.assertEquals( 1.pure(IO).unsafePerformIO, 1, "IO pure" );
        this.assertEquals( [ IO{ 1 }, IO{ 2 }, IO{ 3 }].sequenceM.unsafePerformIO, [1, 2, 3], "IO sequenceM");
        this.assertEquals( [ IO{ 1 }, IO{ 2 }, IO{ 3 }].sequenceM_.unsafePerformIO, Unit, "IO sequenceM");
        this.assertEquals( ({ |x,y| x + y } <%> IO{1} <*> IO{1}).unsafePerformIO, 2, "IO applicative");

    }

    test_Option {
        this.assertEquals( Some(1).collect{ |x| x + 1 } , Some(2), "Some collect");
        this.assertEquals( Some(1) >>= { |x| Some(x+1) } , Some(2), "Some bind 1");
        this.assertEquals( None() >>= { |x| Some(x+1) } , None(), "Some bind 2");
        this.assertEquals( Some(1) >>= { |x| None() } , None(), "Some bind 3");
        this.assertEquals( { |x,y| x + y } <%> Some(1) <*> Some(1), Some(2), "Reader applicative 1");
        this.assertEquals( { |x,y| x + y } <%> Some(1) <*> None(), None(), "Reader applicative 2");

    }

    test_Reader {
        this.assertEquals( 1.pure(Reader).run, 1, "Reader collect");
        this.assertEquals( Reader{ Reader{ 1 } }.join.run, 1, "Reader join" );
        this.assertEquals( (Reader{ 1 } >>= { |x| Reader{ x + 1 } }).run, 2, "Reader bind" );
        this.assertEquals( 1.pure(Reader).run, 1, "Reader pure" );
        this.assertEquals( [ Reader{ 1 }, Reader{ 2 }, Reader{ 3 }].sequenceM.run, [1, 2, 3], "Reader sequenceM");
        this.assertEquals( [ Reader{ 1 }, Reader{ 2 }, Reader{ 3 }].sequenceM_.run, Unit, "Reader sequenceM");
        this.assertEquals( Reader({ |x| x + 1}).run(1), 2, "Reader run");
        this.assertEquals( (Reader.ask >>= { |x| Reader(x + 1) }).run(1), 2, "Reader ask");
        this.assertEquals( ({ |x,y| x + y } <%> IO{1} <*> IO{1}).unsafePerformIO, 2, "IO applicative");
        this.assertEquals( ({ |x,y| x + y } <%> Reader{ |x| x } <*> Reader{ |x| x * 2 }).run(10), 30, "Reader applicative");
    }

    test_Traverse {
        this.assertEquals( [].traverse(I.d, Option), Some([]), "traverse id empty");
        this.assertEquals( [ Some(1) ].traverse(I.d, Option), Some( [1] ) , "traverse id one element");
        this.assertEquals( [ Some(1), Some(2), Some(3) ].traverse(I.d, Option), Some( [1,2,3] ), "traverse id 3 elements" );

    }

    test_Validation {
        this.assertEquals( [].traverse(I.d, Validation), Success([]), "traverse id empty");
        this.assertEquals( [ Success(1) ].traverse(I.d, Validation), Success( [1] ) , "traverse id one element");
        this.assertEquals( [ Success(1), Success(2), Success(3) ].traverse(I.d, Validation), Success( [1,2,3] ), "traverse id 3 elements" );
    this.assertEquals( [ [" hello "].fail, Success(1), [" world "].fail ].traverse(I.d, Validation), Failure( [ " world ", " hello " ] ), "traverse id 3 elements" );
    }


}



/*

TypeInstancesTests().run

*/
