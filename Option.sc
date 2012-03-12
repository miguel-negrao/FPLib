/* 
*  	copied from Scala API
*  Scala API (c) 2002-2010, LAMP/EPFL
*  http://scala-lang.org/
*
* conversion to sc: Miguel Negrão, 2011
*/

Option {
	  
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

