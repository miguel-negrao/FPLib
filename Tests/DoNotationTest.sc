
/*

DoNotationUnitTest().run

*/


DoNotationUnitTest : UnitTest {

    test_processDoBlock {

        //stuff that should work
        this.assertEquals( DoNotation.processDoBlock("a; b"), "a >>= {  b }".success );
        this.assertEquals( DoNotation.processDoBlock("x <- a; b"), "a >>= { |x|  b }".success );
        this.assertEquals( DoNotation.processDoBlock("x <- a; return b"), "a.collect{ |x| b }".success);
        this.assertEquals( DoNotation.processDoBlock("x <- a; let v = x + 1; b"), "a >>= { |x| var v = x + 1; b }".success );
        this.assertEquals( DoNotation.processDoBlock("x <- a; let v = x + 1; return b"), "a.collect{ |x| var v = x + 1;b }".success );
        this.assertEquals( DoNotation.processDoBlock("x <- a ||| g1; return x "), "a.select { |x|  g1 }.collect{ |x| x }".success );
        this.assertEquals( DoNotation.processDoBlock("x <- a; let b = c; y <- d; let e = f; g"),"a >>= { |x| var b = c;d >>= { |y| var e = f; g } } ".success);
        //returns should be checked after last semicolon
        this.assertEquals( DoNotation.processDoBlock("a.return; b"),"a.return >>= {  b }".success);


        //stuff that should fail
        this.assertEquals( DoNotation.processDoBlock("a").class, Failure, "Do(a) should fail");
        this.assertEquals( DoNotation.processDoBlock("a;").class, Failure, "Do(a;) should fail");
        this.assertEquals( DoNotation.processDoBlock("a; <- <-").class, Failure, "Do(a; <- <-) should fail");
        this.assertEquals( DoNotation.processDoBlock("a <- expr1; b <- expr2").class, Failure, "Do(a <- expr1; b <- expr2) should fail");
    }

    test_removeComments {

        this.assertEquals( DoNotation.removeComments("hello/*to a lovelly */ world"), "hello world");
        this.assertEquals( DoNotation.removeComments("hello // world"), "hello ");
    }

    test_processString {

        this.assertEquals( DoNotation.processString("Do(a;b); Do(c;d)"), "a >>= { b }; c >>= { d }".success);
        this.assertEquals( DoNotation.processString("Do(a; let v = Do(c;d); return v +a)"), "a.collect{ var v = c >>= { d };v +a }".success);

        this.assertEquals( DoNotation.processString("Do(").class, Failure, "Do( should fail");
    }


}

