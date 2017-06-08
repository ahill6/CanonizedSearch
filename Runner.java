package gov.nasa.jpf.symbc.andy;

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

	public static void main(String[] args){ 
		
		//Z3 prefix input?
		
		/*
		Context ctx = new Context();
		Expr a = ctx.mkIntConst("a");
		Expr b = ctx.mkIntConst("b");
		Expr c = ctx.mkInt("2");
		Expr g = ctx.mkInt(3);
		List<BoolExpr> tester = new ArrayList<BoolExpr>();
		List<BoolExpr> tester2 = new ArrayList<BoolExpr>();
		BoolExpr d = ctx.mkGt(ctx.mkAdd((ArithExpr)a,(ArithExpr)c), (ArithExpr)b);
		BoolExpr e = ctx.mkGt(ctx.mkAdd((ArithExpr)a,(ArithExpr)c), ctx.mkInt(2));
		BoolExpr f = ctx.mkAnd(d,e);
		tester.add(d);
		tester.add(e);
		tester.add(f);
		
		for (int i = 0; i < tester.size(); i++){
			BoolExpr kk = tester.get(i);
			System.out.println(kk);
			kk = (BoolExpr) kk.substitute(a, b);
			System.out.println(kk);
			tester2.add(kk);
		}
		for (BoolExpr kk : tester2){
			System.out.println(kk);
		}
		
		System.exit(1);
		*/
		
		//Expr a = ctx.mkEq((ArithExpr)ctx.mkIntConst("math.abs(100)"), (ArithExpr) ctx.mkIntConst("100")).simplify();
		
		
		
		Runner runme = new Runner();
		
		/*
		Methods[] methods = {runme.new Methods("median1a", 3), runme.new Methods("median1c", 3),
				runme.new Methods("median1b", 3), runme.new Methods("median1d", 3), runme.new Methods("notMedian", 3),
				runme.new Methods("arrayMedian", 3), runme.new Methods("buggyMedian1ResultChange", 3),
				runme.new Methods("buggyMedian2ResultChange", 3), runme.new Methods("buggyMedian1StructuralChange", 3),
				runme.new Methods("buggyMedian2StructuralChange", 3)};
		
		
		Methods[] methods = {runme.new Methods("testMe2", 3), runme.new Methods("factorial", 1),
				runme.new Methods("sumOrDifference", 3), runme.new Methods("hanoi", 4), runme.new Methods("grade", 3),
				runme.new Methods("returnMin1", 3), runme.new Methods("returnMin2", 3),
				runme.new Methods("copies", 3), runme.new Methods("blackjack", 3),
				runme.new Methods("makeChange", 3), runme.new Methods("xor", 3), runme.new Methods("xorTruths", 3)};
		*/
		//, runme.new Methods("addOneConstraint", 3),	runme.new Methods("makeClause", 1)};
		
		Methods[] methods = {runme.new Methods("testMe2", 3)};

		//Methods[] methods = {runme.new Methods("testMe3", 7), runme.new Methods("testMe4", 7), runme.new Methods("testMe5", 7)};
		try {
			//for (int  j = 0; j < classes.length(); j++){
				//methods = classes[j].getMethods();
				// this initializes the JPF configuration from default.properties, site.properties
				// configured extensions (jpf.properties), current directory (jpf.properies) and
				// command line args ("+<key>=<value>" options and *.jpf)
				Config conf = JPF.createConfig(args);
	
				// ... modify config according to your needs
				conf.setProperty("target", "ToyTests");
				conf.setProperty("classpath", "${jpf-symbc}/build/examples");
				conf.setProperty("sourcepath", "${jpf-symbc}/src/examples");
				//conf.setProperty("target", classes[j].className);
				//conf.setProperty("target", "ToyTests");
				//conf.setProperty("classpath", "${jpf-symbc}/build/examples");
				//conf.setProperty("sourcepath", "${jpf-symbc}/src/examples");
	
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
	
					// write("public static void main(String args[]){");
					// (new TestPaths()).testMe(
					// put in default values for all variables that are the appropriate type.
					// );}
					/*
					switch(type)
					{
					case String: write("\"\"");
						break;
					case boolean: write("false")
						break;
					default: write((type)0);
						break;
					}
					*/
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
	}
}