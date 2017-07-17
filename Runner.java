package gov.nasa.jpf.symbc.andy;

import java.io.File;
import java.util.ArrayList;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.JPFConfigException;
import gov.nasa.jpf.JPFException;;

public class Runner {
	private final class Methods{
		private String name;
		private int numParams;
		private Methods(String a, int b){
			name = a;
			numParams = b;
		}
		public String getName()
		{
			return name;
		}
		public int getNumParams(){
			return numParams;
		}
	}
	
	private static String makeParams(int a){
		if (a == 0){
			return "";
		}
		String res = "sym#";
		for (int j = 0; j < a - 2; j++){
			res += "sym#";
		}
		if (a > 1){
			res += "sym";
		}
		else{
			res = res.substring(0, res.length()-1);
		}
		return res;
	}
	
	private static String makeClasspath(File f){
		String classpth = "";
		//System.out.println(f);
		
		// make a file from a
		if(f.isDirectory()){
			// if at least one class file, add it to classpth
			for (File g : f.listFiles()){
				if (g.isDirectory()){
					classpth += makeClasspath(g) + ";"; 
				}
			}
			String ttmp = f.getAbsolutePath().replace("/Users/ahill6/Documents/workspace/jpf-symbc", "${jpf-symbc}");
			//classpth += "${jpf-symbc}/../../../../.."+ttmp;
			classpth += ttmp;
		}
		// recursively check all sub-folders (assuming a is a folder)
		// if there is a .class in that folder, add it to the classpath
		//System.out.println(classpth);
		return classpth;
	}
	

	public static void main(String[] args){ 
		Runner runme = new Runner();
		String targetPreface = "IntroClassJavaCorrect";
		
		Methods[] methods = {runme.new Methods("median",3),runme.new Methods("medianResultChange",3),runme.new Methods("medianStructuralChange",3),
		runme.new Methods("medianRewrite",3),runme.new Methods("buggyMedian4ResultChange",3),runme.new Methods("notMedian",3),
		runme.new Methods("sumOrDifference",3),runme.new Methods("sumOrDifferenceResultChange",3),runme.new Methods("sumOrDifferenceRewrite",3),
		runme.new Methods("testMe2",3),runme.new Methods("testMe2ResultChange",3),runme.new Methods("testMe2StructuralChange",3),
		runme.new Methods("factorial",1),runme.new Methods("factorialResultChange",1),runme.new Methods("factorialBiggerResultChange",1),
		runme.new Methods("factorialRewrite",1),
		runme.new Methods("grade",3),runme.new Methods("gradeResultChange",3),runme.new Methods("gradeStructuralChange",3),
		runme.new Methods("minBasic",3),runme.new Methods("minResultChange",3),runme.new Methods("minStructuralChange",3),runme.new Methods("minRewrite",3),
		runme.new Methods("copies",3),runme.new Methods("copiesResultChange",3),runme.new Methods("copiesStructuralChange",3),runme.new Methods("copiesRewrite",3),
		runme.new Methods("blackjack",3),runme.new Methods("blackjackResultChange",3),runme.new Methods("blackjackStructuralChange",3),runme.new Methods("blackjackRewrite",3),
		runme.new Methods("makeChange",3),runme.new Methods("makeChangeResultChange",3),runme.new Methods("makeChangeStructuralChange",3),
		runme.new Methods("makeChangeRewrite",3),runme.new Methods("xor",3),runme.new Methods("xorResultChange",3),runme.new Methods("xorStructuralChange",3),
		runme.new Methods("xorRewrite",3),runme.new Methods("xorTruths",3),runme.new Methods("xorTruthsResultChange",3),runme.new Methods("xorTruthsStructuralChange",3),
		runme.new Methods("xorTruthsRewrite",3)};

		try {
			//for (int  j = 0; j < classes.length(); j++){
				//methods = classes[j].getMethods();
				// this initializes the JPF configuration from default.properties, site.properties
				// configured extensions (jpf.properties), current directory (jpf.properies) and
				// command line args ("+<key>=<value>" options and *.jpf)
				Config conf = JPF.createConfig(args);
	
				// ... modify config according to your needs
				conf.setProperty("target", targetPreface);
				//conf.setProperty("target", "org.apache.commons.math3.complex.Complex");
				conf.setProperty("classpath", "${jpf-symbc}/build/examples");
				//conf.setProperty("sourcepath", "${jpf-symbc}/src/examples");
				//conf.setProperty("target", classes[j].className);
				//conf.setProperty("target", "ToyTests");
				//conf.setProperty("classpath", "${jpf-symbc}/build/examples");
				//conf.setProperty("sourcepath", sourcep);
	
				conf.setProperty("symbolic.dp", "no_solver");
				//conf.setProperty("symbolic.dp", "yices");
				//conf.setProperty("listener", ".symbc.andy.AndyListenerPathConstraintsToFile");
				conf.setProperty("listener", ".symbc.andy.CodeCharacterizationListener");
				conf.setProperty("vm.storage.class", "nil");
				conf.setProperty("search.depth_limit", "30");
				//conf.setProperty("search.class", ".search.heuristic.BFSHeuristic");
				 
				//... explicitly create listeners (could be reused over multiple JPF runs)
				//MyListener myListener = ... 
	
				
				for (int i=0; i<methods.length; i++){
					// Get Method
					
					// Write new main for this
	

					//
					
					// Go
					String thisOne = conf.getTarget() + "." + methods[i].getName() + "(" + makeParams(methods[i].getNumParams()) + ")"; 
					conf.setProperty("symbolic.method", thisOne);
					System.out.println(thisOne);
					JPF jpf = new JPF(conf);
					// ... set your listeners
					//jpf.addListener(myListener);
	
					jpf.run();
					if (jpf.foundErrors()){
						System.out.println("Whoops");
					}
				}
				// ... process property violations discovered by JPF
				//runme.runZ3(String.valueOf(thisOne));
			//}
		} catch (JPFConfigException cx){
			// ... handle configuration exception
			// ...  can happen before running JPF and indicates inconsistent configuration data
			System.out.println("Config Exception");
		} catch (JPFException jx){
			// ... handle exception while executing JPF, can be further differentiated into
			// ...  JPFListenerException - occurred from within configured listener
			// ...  JPFNativePeerException - occurred from within MJI method/native peer
			// ...  all others indicate JPF internal errors
			System.out.println("JPF Exception");
			System.out.println(jx);
		}
		
		ArrayList<String> methodNames = new ArrayList<String>();
		for (Methods m : methods){
			methodNames.add(m.getName());
		}
		
		int n = 5;
		
		//DBInteractions db = new DBInteractions();
		//db.nNearest(methodNames.toArray(new String[methodNames.size()]), n, targetPreface);
		//System.exit(1);
		DistanceCalculations d = new DistanceCalculations();
		d.someMethodHere(methodNames.toArray(new String[methodNames.size()]), targetPreface);	
	}
}
