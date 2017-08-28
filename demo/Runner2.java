package gov.nasa.jpf.symbc.csar;

import java.io.File;
import java.io.FilenameFilter;
import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.JPFConfigException;
import gov.nasa.jpf.JPFException;;

/**
 * Demo code testing with CSAR
 * 
 * @author Andrew Hill
 */
public class Runner2 {
	/**
	 * Helper class which organizes storage of SPF-relevant method information 
	 * to simplify symbolic execution.
	 *
	 */
	private final class Methods{
		private String name;
		private int numParams;
		/**
		 * @param a method name
		 * @param b number of method parameters
		 */
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
	 * Gives an example of using CSAR to measure find closest methods.
	 * <p>
	 * In this case, it is assumed that the methods have identical names and number of input parameters, specified in <i>methods</i>;
	 * are in separate files, each of which includes <i>target</i> in the filename; and each file is in <i>targetFolder</i>
	 */
	public static void main(String[] args){ 
		Runner2 runme = new Runner2();
		String target = "ToyTests.java"; // Change to name of file(s) or keywords in names of files to be executed
		// TODO - targetFolder must be changed to be the path to the folder containing files which you wish to test
		String targetFolder = "/Users/Documents/workspace/jpf-symbc/src/examples";  
		Methods[] methods = {runme.new Methods("grade", 3)};
		
		String[] files = new File(targetFolder).list(new FilenameFilter() {
	    	  @Override
	    	  public boolean accept(File current, String name) {
	    	    return (new File(current, name).getName().contains(target));
	    	  }});

		int count = 0 ;
		int totes = files.length;
		for (String targetPreface : files){
			if (count < 0){ // Changing this value can be used to restart a run at a specific point as needed
				count++;
				continue;
			}
			
			System.out.println(count++ + "/" + totes);
			try {
					Config conf = JPF.createConfig(args);
		
					// ... modify config according to your needs
					conf.setProperty("target", targetPreface.replace(".java", ""));
					conf.setProperty("classpath", "${jpf-symbc}/build/examples");
					conf.setProperty("symbolic.dp", "no_solver");
					//conf.setProperty("listener", ".symbc.csar.CodeCharacterizationListener"); 
					conf.setProperty("listener", ".symbc.csar.CodeTestingListener");
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
	}
}