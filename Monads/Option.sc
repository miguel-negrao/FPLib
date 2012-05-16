/* 
*  	copied from Scala API
*  Scala API (c) 2002-2010, LAMP/EPFL
*  http://scala-lang.org/
*
* conversion to sc: Miguel Negrão, 2011
*/

Option {

  *initClass {

  	Class.initClassTree(TypeClasses);
  	//type instances declarations:

	TypeClasses.addInstance(Option,
		(
			'fmap': { |fa,f| fa.collect(f) },
			'bind' : { |fa,f| fa.flatCollect(f) },
			'pure' : { |a| Some(a) }
			/*
			very intersting: traverse cannot be defined for Option in a dynamic language, because
			there is no specification of what the class of f.(a) should be for None...
			'traverse' : { |f,a|
				var fa = f.(a);
				if( a.isDefined) {
					f.a.fmap{ |x| Some(x) }
				} {
					None.pure(?????)
				}
			}*/
		)
	);
  }
	  
  *new{ |x| ^if (x.isNil){ None }{ Some(x) } }
  
  *empty{ ^None }   

}

Some : Option {
  var value;
  
  *new{ |val| ^super.newCopyArgs(val) }
  
  isEmpty{ ^false }
  
  get{ ^value }
    
  isDefined{ ^true }
  
  getOrElse{ ^this.get }
  
  orNil{ ^this.get }
  
  collect{ |f| ^Some(f.(this.get)) }
  
  flatCollect{ |f| ^f.(this.get) }
  
  select{ |p| ^if(p.(this.get)){ this }{ None } }
  
  do{ |f| f.(this.get) }

  exists{ ^this.get }
  
  orElse{ ^this }
  
  asArray{ ^[this.get]}
  
  flatten{ ^this.asArray }
   
  printOn { arg stream;
	stream << this.class.name << "(" << value << ")";
  }
}

None : Option {
  *isEmpty{ ^true }
  
  *get{ Error("None.get").throw }
  
  *isDefined{ ^false }
  
  *getOrElse{ |default| ^default }
  
  *orNil{ ^nil }
  
  *collect{}
  
  *flatCollect{}
  
  *select{}
  
  *do{}

  *exists{ false }
  
  *orElse{ |alternative| ^alternative }
  
  *asArray{ ^[] }
  
  *flatten{ ^this.asArray }
  *printOn { arg stream;
	stream << "None";
  }
    
}

+ Object {

	asOption {
		^Some(this)
	}

}

+ Nil {

	asOption {
		^None
	}
}
