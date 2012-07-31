/*
Copied from Canvas3D by lijon 2011, and altered by Miguel Negrão

*/


FP3DPoligon {
    var <path;
    var <transforms;
    var <emissive; //color
    var <ambient=0.5; //number 0-1
    var <specular=0.5;//number 0-1
    var <diffuse=0.5;  //number 0-1
    var <shininess = 0.5; //number 0-1
    var <width;
    
    *new { |path, transforms, emissive, ambient=0.5, specular=0.5, diffuse=0.5, shininess = 0.5, width|
        ^super.newCopyArgs( path.collect( _.as(RealVector3D) ), transforms, emissive, ambient, specular, diffuse, shininess, width )
    }
    
    *icosahedronFaces { |transforms, emissive, ambient=0.5, specular=0.5, diffuse=0.5, shininess = 0.5, width|
        var t = (1.0 + 5.sqrt) / 2.0;
    
        var points = [ 
            RealVector3D[-1,  t,  0],
            RealVector3D[ 1,  t,  0],
            RealVector3D[-1, t.neg,  0],
            RealVector3D[ 1, t.neg,  0], 
            
            RealVector3D[ 0, -1,  t],
            RealVector3D[ 0,  1,  t],
            RealVector3D[ 0, -1, t.neg],
            RealVector3D[ 0,  1, t.neg], 
            
            RealVector3D[ t,  0, -1],
            RealVector3D[ t,  0,  1],
            RealVector3D[t.neg,  0, -1],
            RealVector3D[t.neg,  0,  1]
        ];
        
        ^[
        
        // 5 faces around point 0
        [0, 11, 5],
        [0, 5, 1],
        [0, 1, 7],
        [0, 7, 10],
        [0, 10, 11],
        
        // 5 adjacent faces
        [1, 5, 9],
        [5, 11, 4],
        [11, 10, 2],
        [10, 7, 6],
        [7, 1, 8], 
        
        // 5 faces around point 3
        [3, 9, 4],
        [3, 4, 2],
        [3, 2, 6],
        [3, 6, 8],
        [3, 8, 9],
        
        // 5 adjacent faces
        [4, 9, 5],
        [2, 4, 11],
        [6, 2, 10],
        [8, 6, 7],
        [9, 8, 1]
        ].collect{ |arr|
            arr.collect( points.at(_) )
        }               
    }
    
     *icosahedron { |transforms, emissive, ambient=0.5, specular=0.5, diffuse=0.5, shininess = 0.5, width|        
        ^this.icosahedronFaces.collect( this.new(_,transforms, emissive, ambient, specular, diffuse, shininess, width ) )        
    }
    
    *sphereFaces { |n|
        var triangles = this.icosahedronFaces().collect{ |tri| tri.collect( _.normalize ) };
        var splitTriangles = { |triangles, n|
            if( n == 0) {
                triangles
            } {
                var newTriangles = triangles.collect{ |tri|
                        var a,b,c, ab, bc, ac;
                        var f = { |x,y| (x + ((y-x)/2)).normalize };
                        #a,b,c = tri;                      
                        ab = f.(a,b);
                        bc = f.(b,c);
                        ac = f.(a,c);
                        [
                            [a,ab,ac],
                            [b,ab,bc],
                            [c,bc,ac],
                            [ab,bc,ac]
                        ]                          
                }.flatten;       
                splitTriangles.(newTriangles, n-1)        
            }
        };       
        ^splitTriangles.(triangles,n)
    }
    
    *sphere { |n, transforms, emissive, ambient=0.5, specular=0.5, diffuse=0.5, shininess = 0.5, width|
        ^this.sphereFaces(n).collect(
            this.new(_,transforms, emissive, ambient, specular, diffuse, shininess, width)
         )
    }    
    
    *cubeFaces { 
        
        [
        // Front face  
        [[-1.0, -1.0,  1.0],  
        [1.0, -1.0,  1.0],  
        [1.0,  1.0,  1.0],  
        [-1.0,  1.0,  1.0]],  
        
        // Back face  
        [[-1.0, -1.0, -1.0],  
        [-1.0,  1.0, -1.0],  
        [1.0,  1.0, -1.0],  
        [1.0, -1.0, -1.0]],  
        
        // Top face  
        [[-1.0,  1.0, -1.0],  
        [-1.0,  1.0,  1.0],  
        [1.0,  1.0,  1.0],  
        [1.0,  1.0, -1.0]],  
        
        // Bottom face  
        [[-1.0, -1.0, -1.0],  
        [1.0, -1.0, -1.0],  
        [1.0, -1.0,  1.0],  
        [-1.0, -1.0,  1.0]],  
        
        // Right face  
        [[1.0, -1.0, -1.0],  
        [1.0,  1.0, -1.0],  
        [1.0,  1.0,  1.0],  
        [1.0, -1.0,  1.0]],  
        
        // Left face  
        [[-1.0, -1.0, -1.0],  
        [-1.0, -1.0,  1.0],  
        [-1.0,  1.0,  1.0],  
        [-1.0,  1.0, -1.0]]];
        
    }
    
    *cube { |transforms, emissive, ambient=0.5, specular=0.5, diffuse=0.5, shininess = 0.5, width|
        ^this.cubeFaces.collect(
            this.new(_,transforms, emissive, ambient, specular, diffuse, shininess, width)
         )
    }   
    
}

//do we need a FP3DShape ?
FP3DTransform {

	*mIdentity {
		^[	#[1, 0, 0, 0],
			#[0, 1, 0, 0],
			#[0, 0, 1, 0],
			#[0, 0, 0, 1]];
	}
	*mTranslate {|tx, ty, tz|
		^[	#[1, 0, 0, 0],
			#[0, 1, 0, 0],
			#[0, 0, 1, 0],
			[tx, ty, tz, 1]];
	}
	*mScale {|sX, sY = (sX), sZ = (sX)|
		^[	[sX, 0, 0, 0],
			[0, sY, 0, 0],
			[0, 0, sZ, 0],
			#[0, 0, 0, 1]];
	}
	*mRotateX {|ax|
		^[	#[1, 0, 0, 0],
			[0, cos(ax), sin(ax), 0],
			[0, sin(ax).neg, cos(ax), 0],
			#[0, 0, 0, 1]];
	}
	*mRotateY {|ay|
		^[	[cos(ay), 0, sin(ay).neg, 0],
			#[0, 1, 0, 0],
			[sin(ay), 0, cos(ay), 0],
			#[0, 0, 0, 1]];
	}
	*mRotateZ {|az|
		^[	[cos(az), sin(az), 0, 0],
			[sin(az).neg, cos(az), 0, 0],
			#[0, 0, 1, 0],
			#[0, 0, 0, 1]];
	}
	
	*matrixMatrixMul {|matrix1, matrix2|
		var m0, m1, m2, m3;
		#m0, m1, m2, m3= matrix2;
		^Array.fill(4, {|x|
			Array.fill(4, {|y|
				(matrix1[x][0]*m0[y])+
				(matrix1[x][1]*m1[y])+
				(matrix1[x][2]*m2[y])+
				(matrix1[x][3]*m3[y])
			});
		});
	}
	*vectorMatrixMul {|vector, matrix|
		var v0, v1, v2, m0, m1, m2, m3;
		#v0, v1, v2= vector;
		#m0, m1, m2, m3= matrix;
		^RealVector3D[
			(v0*m0[0])+(v1*m1[0])+(v2*m2[0])+m3[0],
			(v0*m0[1])+(v1*m1[1])+(v2*m2[1])+m3[1],
			(v0*m0[2])+(v1*m1[2])+(v2*m2[2])+m3[2]
		];
	}
}

FP3DLightConf {
    var <lightDir, //a RealVector3D
        <lightColor; //a Color
    
    *new { |lightDir, lightColor|
        ^super.newCopyArgs( (lightDir ?? { RealVector3D[1.0,1.0,1.0] }).normalize,
            lightColor ?? { Color(0.3,0.3,0.3) } )
    }
}

FP3DScene {
    var <items;
    var <scale = 200;
    var <perspective = 0.5;
    var <distance = 2;
    var <transforms;
    var <lightConf; // Option[FP3DLightConf

    
    *new { |items, scale=200, perspective=0.5, distance, transforms, lightConf|
        ^super.newCopyArgs( items ? [], scale, perspective, distance,
           transforms ? [],  lightConf ?? { FP3DLightConf() } )    
    }

    project { |v, width, height|
        var x, y, z, p;
        z = v[2]*perspective+distance;
        x = scale*(v[0]/z)+(width/2);
        y = scale*(v[1]/z)+(height/2);
        ^Point(x, y);    
    }

    //returns a PenDrawing
    drawing { |width, height|
        var viewPos = RealVector3D[0.0,0.0,1.0];
        var paths0 = items.collect{ |item|  
            var transPath = item.path.collect{ |v|
                (item.transforms ++ transforms).inject(v.as(Array)++[1], { |s,t| FP3DTransform.vectorMatrixMul(s,t) });          
            };
            Tuple3( transPath, item, transPath[..(transPath.size-2)].sum/(transPath.size-1) ) 
        };
        var paths = paths0.sort{ |p1t,p2t|    
            p1t.at3.dist(viewPos) > p2t.at3.dist(viewPos)        
        };   

        var drawedShapes = paths.collect { |pt,i|
            var item = pt.at2;
            var transPath = pt.at1;
            //this assumes polygon is in just one plane
            var pathN = ((transPath[1] - transPath[0])
                .cross( transPath[2] - transPath[0])).normalize*1.neg;        
            var color = lightConf.collect{ |x|
                var dot = pathN <|> x.lightDir;
                var dot2 =  pathN <|> (x.lightDir + RealVector3D[0.0, 0.0, 1.0] ).normalize;
                
                item.emissive
                .add( x.lightColor * item.ambient)
                .add( x.lightColor * if(dot < 0) {dot * item.diffuse}{0} )
                .add( (x.lightColor * (dot2.abs**item.shininess) * item.specular) )
            }.getOrElse(item.emissive);    
                
            var shape = PenStepShape.polygon( transPath.collect( this.project(_, width, height) ) );
            //var normal = PenStepShape.polygon( [ (transPath.sum/transPath.size), pathN].collect( this.project(_, width, height) ) );
            PenDrawedShapes([shape], \fill, color, Color.black );           
        };
        ^PenDrawing( drawedShapes );
        
        /*paths0.do{ |pt, i|
            "path % is %".format(i,pt.at1).postln;
            "path % has center %".format(i,pt.at3).postln;
            "path % distance to view pos%".format(i,pt.at3.as(RealVector3D).dist(viewPos))
        };

        
        paths.do{ |pt|
                var p = this.project(*pt.at3);
                Pen.strokeColor = Color.black;
                Pen.fillColor = pt.at2.emissive;
                Pen.addArc(p, 4, 0, 2*pi);
                Pen.fill;
                Pen.addArc(p, 4, 0, 2*pi);
                Pen.stroke;                
        };
        "view pos %".format(viewPos).postln;
        "closest object has color %".format( paths.last.at2.emissive.asArray ).postln;
        "closest object distance to view pos %".format( paths.last.at3.as(RealVector3D).dist(viewPos) ).postln;
        */
    
    }
    
}



