package gov.nasa.jpf.symbc.repairproject;

import java.io.File;
import java.util.ArrayList;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.JPFConfigException;
import gov.nasa.jpf.JPFException;;

public class Runner {
	public final class Methods{
		private String name;
		private int numParams;
		public Methods(String a, int b){
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
		Runner runme = new Runner();
		String targetPreface = "ToyTests";
		boolean databaseExists = false;
		
		Methods[] methods = {runme.new Methods("sumOrDifference",3),
				runme.new Methods("grade",3),runme.new Methods("returnMin",3),
				runme.new Methods("copies",3),
				runme.new Methods("makeChange",3),runme.new Methods("xor",3),
				runme.new Methods("sumOrDifference2",3),runme.new Methods("median",3),
				runme.new Methods("grade2",3),runme.new Methods("returnMin2",3),
				runme.new Methods("copies2",3),runme.new Methods("arrayMedian",3),
				runme.new Methods("makeChange2",3),runme.new Methods("xor2",3)};
		
		if (!databaseExists){
			try {
					Config conf = JPF.createConfig(args);
		
					// ... modify config according to your needs
					conf.setProperty("target", targetPreface);
					conf.setProperty("classpath", "${jpf-symbc}/build/examples");
					conf.setProperty("symbolic.dp", "no_solver");
					conf.setProperty("listener", ".symbc.repairproject.CodeCharacterizationListener");
					conf.setProperty("vm.storage.class", "nil");
					conf.setProperty("search.depth_limit", "30");
								
					for (int i=0; i<methods.length; i++){
						String thisOne = conf.getTarget() + "." + methods[i].getName() + "(" + makeParams(methods[i].getNumParams()) + ")"; 
						conf.setProperty("symbolic.method", thisOne);
						System.out.println(thisOne);
						JPF jpf = new JPF(conf);
		
						jpf.run();
						if (jpf.foundErrors()){
							System.out.println("Whoops");
						}
					}
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

		ArrayList<String> methodNames = new ArrayList<String>();
		for (Methods m : methods){
			methodNames.add(targetPreface+"."+m.getName());
		}

		int n = 5;

		DistanceCalculations d = new DistanceCalculations();
		d.getTopMatchesDB(methodNames.toArray(new String[methodNames.size()]), n);
	}
}