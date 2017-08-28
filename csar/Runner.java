package gov.nasa.jpf.symbc.csar;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.JPFConfigException;
import gov.nasa.jpf.JPFException;;

/**
 * 
 * Demo Code Characterization (DB creation for CSAR)
 * 
 * @author Andrew Hill
 */
public class Runner {
	/**
	 * Helper class which organizes storage of SPF-relevant method information 
	 * to simplify symbolic execution.
	 *
	 */
	public final class Methods{
		private String name;
		private int numParams;
		/**
		 * @param a method name
		 * @param b number of method parameters
		 */
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
	
	/**
	 * Automatically generates an SPF symbolic execution call for use with symbolic.method.
	 * <p>
	 * Can be used to automate multiple SPF runs.
	 * 
	 * @param a number of method parameters
	 * @return  string representation of symbolic execution call to be given to SPF symbolic.method
	 */
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
	

	/**
	 * Gives an example of using a Methods array to symbolically characterize multiple methods.  In this case, the 
	 * methods are all located in the src/examples folder, in ToyTests.java.
	 */
	public static void main(String[] args){ 
		Runner runme = new Runner();
		String targetPreface = "ToyTests";
		boolean databaseExists = false;
		
		Methods[] methods = {runme.new Methods("grade2",3)};
		
		if (!databaseExists){
			try {
					Config conf = JPF.createConfig(args);
		
					// ... modify config according to your needs
					conf.setProperty("target", targetPreface);
					conf.setProperty("classpath", "${jpf-symbc}/build/examples");
					conf.setProperty("symbolic.dp", "no_solver");
					conf.setProperty("listener", ".symbc.csar.CodeCharacterizationListener");
					//conf.setProperty("listener", ".symbc.csar.CodeTestingListener");
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
	}
}