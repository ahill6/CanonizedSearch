package gov.nasa.jpf.symbc.csar;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * @author Andrew Hill
 *
 */
public class StaticDistanceCalculations{
	private static String outfile;
	private static String database = "xxx";
	private static String base = "jdbc:mysql://localhost:xxxx/"+database;
	private static String password = "xxx";
	private static HashMap<String, List<String>> constraints = null;

	public StaticDistanceCalculations(){}
	
	/**
	 * @param out path to output file
	 */
	public StaticDistanceCalculations(String out){
		outfile = out;
	}
	
	/**
	 * Calls the set database to load potential matches so that searches will run more quickly.
	 * Note that this approach will require the whole database to fit in memory. 
	 */
	public static void prep(){
		if (constraints == null){
			constraints = new HashMap<String, List<String>>();
			fetchAllMethodConstraints();
		}
	}
	
	
	/**
	 * Calculates distance from the provided strings representing semantic constraints
	 * in canonical form to each of the methods in <i>others</i>.  Each method in others 
	 * must be in the database.
	 * 
	 * @param testString list of string-representation of semantic constraints
	 * @param others array of method names to which distance will be measured
	 */
	public static void distanceToX(List<String> testString, String[] others){
		double[] levenshtein = new double[others.length];
		double[] lcSub = new double[others.length];
		
		for (int i=0; i<others.length; i++){
			levenshtein[i] = distanceCalc(testString, others[i], "levenshtein");
			lcSub[i] = distanceCalc(testString, others[i], "LCSub");
		}
		
		String out = "";
		for (double d : levenshtein){
			out += d + " ";
		}
		for (double d : lcSub){
			out += d + " ";
		}
		out += "\n";
		
		write(out, outfile, true);
	}
	
	/**
	 * Calculate the distance between a list of constraints (constraints) and a the 
	 * constraints of a method (name) in the database, using a given string metric (method)
	 * 
	 * @param constraints 
	 * @param name
	 * @param method
	 * @return
	 */
	public static double distanceCalc(List<String> constraints, String name, String method){
		List<Double> tmpResults = new ArrayList<Double>();
		List<Double> intermediateResults = new ArrayList<Double>();
		List<String> myConstraints = fetchConstraints(name);
		
		for (int i = 0; i < myConstraints.size(); i++){
			tmpResults.clear();
			for (String c : constraints){
				double tmp = -1;
				if (method == "levenshtein"){
					tmp = levenshteinDistance(c, myConstraints.get(i));
				}
				else if (method == "LCSub"){
					tmp = longestCommonSubstring(c, myConstraints.get(i));
				}
				else{
					System.out.println(method +" is not a valid method!");
				}
				tmpResults.add(tmp);
			}
			double best = -1;
			if (method == "levenshtein"){
				best = Collections.min(tmpResults);
			}
			else if (method == "LCSub"){
				best = Collections.max(tmpResults);
			}
			intermediateResults.add(best);	
		}

		if (method == "levenshtein")
			return Collections.max(intermediateResults);
		else if (method == "LCSub")
			return Collections.min(intermediateResults);
		
		System.out.println("Something is horribly wrong)");
		System.exit(0);
		return Collections.max(intermediateResults);
		
	}
	
	
	/**
	 * Finds the nearest semantic matches to code which is not in the database by providing 
	 * a list of its semantic constraints in canonical form.  Writes output to a file.
	 * 
	 * @param testStrings list of semantic constraints
	 * @param name name of the method (to write to output file)
	 * @param topN number matches desired
	 * @param fileStart path to the output folder
	 * @param fileEnd output filename
	 */
	public static void compareHavePCs(List<String> testStrings, String name, int topN, String fileStart, String fileEnd){
		System.out.println("compare PCs");
		String newFile = fileStart + fileEnd.replaceAll("[0-9]", "");

		String fileEdit = newFile + "Edit.txt";
		String fileSub = newFile + "Sub.txt";

		write(name+"\n", fileEdit, true);
		write(name+"\n", fileSub, true);
		
		//fetchAllMethodConstraints();  // prep() has already retrieved all constraints
		levenshteinDistanceRunnerHavePCsMaximinMoreEfficient(testStrings, topN, fileEdit);
		longestCommonSubstringRunnerHavePCsMinimaxMoreEfficient(testStrings, topN, fileSub);
	}
	
	/**
	 * Finds the nearest semantic matches to code which is not in the database by providing 
	 * a list of its semantic constraints in canonical form.  Writes output to stdout.
	 * 
	 * @param testStrings list of semantic constraints
	 * @param giveTopN number matches desired
	 */
	public static void compareHavePCs(List<String> testStrings, int giveTopN){
		System.out.println("compare PCs");

		//levenshteinDistanceRunnerHavePCsMaximin(testStrings, giveTopN);
		levenshteinDistanceRunnerHavePCsMaximinMoreEfficient(testStrings, giveTopN);
		longestCommonSubstringRunnerHavePCsMinimaxMoreEfficient(testStrings, giveTopN);
	}
	
	/**
	 * Finds the nearest semantic matches to code which is not in the database by providing 
	 * a list of its semantic constraints in canonical form.  Follows the CSAR search technique.  
	 * Writes output to a file.
	 * 
	 * @param testStrings list of semantic constraints
	 * @param name name of the method (to write to output file)
	 * @param filename full path to output file
	 */
	/**
	 * @param testStrings
	 * @param name
	 * @param filename
	 */
	public static void adaptiveCheck(List<String> testStrings, String name, String filename){
		System.out.println("compare PCs");
		int giveTopN = 2;
		double ratio = 0.5;
		int limitN = 3;
		String newFile = filename.substring(0, 13) + filename.substring(13).replaceAll("[0-9]", "");

		String fileCombined = newFile + "Combined.txt";

		write(name+"\n", fileCombined, true);
		

		// run levenshtein.  If 1 is more than  x% better than 2, write and return True
		boolean success = adaptiveLevenshteinRunner(testStrings, giveTopN, fileCombined, ratio);
		if (!success){
			//else, run LCSub.  If more than N are returned for giveTopN, write the results, then "**UNPATCHABLE**"
			adaptiveLongestCommonSubstringRunner(testStrings, giveTopN, fileCombined, limitN);
		}
	}
	

/**
 * Runner to run a list of constraints against all those in the database using Levenshtein distance
 * as part of the CSAR search approach.
 * 
 * @param myConstraints semantic constraints in canonical form
 * @param topN number of matches to return
 * @param file output file
 */
private static boolean adaptiveLevenshteinRunner(List<String> myConstraints, int topN, String file, double ratio) {
	//System.out.println("levenshtein runner");
	List<Answer> maxResults = new ArrayList<Answer>();
	List<Integer> bests = new ArrayList<Integer>();
	List<Double> tmpResults = new ArrayList<Double>();
	List<Double> intermediateResults = new ArrayList<Double>();
	List<Double> bestTopNTracker = new ArrayList<Double>() {
	    public boolean add(Double mt) {
	        int index = Collections.binarySearch(this, mt);
	        if (index < 0) index = ~index;
	        super.add(index, mt);
	        return true;
	    }
	};
	
	double bestN = 1000;
	int counter = 0;
	boolean valid = true;
	
	for (String o : constraints.keySet()){
		List<String> constr = constraints.get(o);
		if (constr.size() == 0){
			continue;
		}
		intermediateResults.clear();
		bests.clear();
		valid=true;

		for (int i = 0; i < myConstraints.size(); i++){
			tmpResults.clear();
			double worst = -1;
			for (String c : constr){
				double tmp = levenshteinDistance(c, myConstraints.get(i));
				tmpResults.add(tmp);
			}
			
			double best = Collections.min(tmpResults);
			worst = Math.max(worst, best);
			if (worst > bestN){
				valid = false;
				break;
			}
			bests.add(tmpResults.indexOf(best));
			intermediateResults.add(best);	
			
		}
		
		if (valid){
			Answer ansMax = new StaticDistanceCalculations().new Answer(o);
			ansMax.setLCS(Collections.max(intermediateResults));
			double dist = ansMax.getLCS();
			if (counter < topN){
				bestTopNTracker.add(dist);
			}
			maxResults.add(ansMax);
			if (counter >= topN && dist < bestN){
				bestTopNTracker.add(dist);
				bestTopNTracker.remove(topN);
				bestN = bestTopNTracker.get(topN-1);
			}
		}
		
		counter++;
		}
		//System.out.println("-------------------");
		Collections.sort(maxResults, new StaticDistanceCalculations().new AnswerLCSComparator());
		double limit = maxResults.get(topN-1).getLCS(); //TODO - add so groups can tie
		String out = "";
		if (maxResults.get(0).getLCS() < limit*ratio){
			Answer tmp = maxResults.get(0);
			out = tmp.getName() + " " + tmp.getLCS() + "\n";
			write(out, file, true);
			return true;
		}
		return false;
	}

/**
 * Runner to run a list of constraints against all those in the database using Levenshtein distance.
 * 
 * @param myConstraints semantic constraints in canonical form
 * @param topN number of matches to return
 * @param file output file
 */
public static void levenshteinDistanceRunnerHavePCsMaximin(List<String> myConstraints, int topN, String file){
	//System.out.println("levenshtein runner");

	List<Answer> maxResults = new ArrayList<Answer>();
	List<Integer> bests = new ArrayList<Integer>();
	List<Double> tmpResults = new ArrayList<Double>();
	List<Double> intermediateResults = new ArrayList<Double>();
	
	for (String o : constraints.keySet()){
		List<String> constr = constraints.get(o);
		intermediateResults.clear();
		bests.clear();

		for (int i = 0; i < myConstraints.size(); i++){
			tmpResults.clear();
			for (String c : constr){
				double tmp = levenshteinDistance(c, myConstraints.get(i));
				tmpResults.add(tmp);
			}
			
			double best = Collections.min(tmpResults);
			bests.add(tmpResults.indexOf(best));
			intermediateResults.add(best);		
		}
		Answer ansMax = new StaticDistanceCalculations().new Answer(o);
		ansMax.setLCS(Collections.max(intermediateResults));
		maxResults.add(ansMax);
	}
	System.out.println(myConstraints.size());
	System.out.println("-------------------");
	Collections.sort(maxResults, new StaticDistanceCalculations().new DoubleAnswerLCSComparator());
	double limit = maxResults.get(topN-1).getLCS();
	int i = 0;
	String out = "";
	Answer tmp = maxResults.get(i);
	while(tmp.getLCS() <= limit){
		System.out.println(tmp.getName() + " " + tmp.getLCS() );
		out += tmp.getName() + " " + tmp.getLCS() + "\n";
		i++;
		tmp = maxResults.get(i);
	}
	write(out, file, true);
}


/**
 * Runner to run a list of constraints against all those in the database using Levenshtein distance.
 * Writes output to stdout.
 * 
 * @param myConstraints semantic constraints in canonical form
 * @param topN number of matches to return
 */
public static void levenshteinDistanceRunnerHavePCsMaximin(List<String> myConstraints, int topN){
	//System.out.println("levenshtein runner");

	List<String> others = new ArrayList<String>();
	List<Answer> maxResults = new ArrayList<Answer>();
	List<Integer> bests = new ArrayList<Integer>();
	List<Double> tmpResults = new ArrayList<Double>();
	List<Double> intermediateResults = new ArrayList<Double>();
	others = fetchAllMethodNames();

	for (String o : others){
		List<String> constraints = fetchConstraints(o);
		intermediateResults.clear();
		bests.clear();

		for (int i = 0; i < myConstraints.size(); i++){
			tmpResults.clear();
			for (String c : constraints){
				double tmp = levenshteinDistance(c, myConstraints.get(i));
				tmpResults.add(tmp);
			}
			
			double best = Collections.min(tmpResults);
			bests.add(tmpResults.indexOf(best));
			intermediateResults.add(best);	
			
		}
		
		Answer ansMax = new StaticDistanceCalculations().new Answer(o);
		ansMax.setLCS(Collections.max(intermediateResults));
		maxResults.add(ansMax);
	}
	System.out.println("-------------------");
	Collections.sort(maxResults, new StaticDistanceCalculations().new DoubleAnswerLCSComparator());
	double limit = maxResults.get(topN-1).getLCS();
	int i = 0;
	Answer tmp = maxResults.get(i);
	while(tmp.getLCS() <= limit){
		System.out.println(tmp.getName() + " " + tmp.getLCS() );
		i++;
		tmp = maxResults.get(i);
	}
}

/**
 * Runner to run a list of constraints against all those in the database using Levenshtein distance.  Includes early
 * termination on each comparison using Alpha-Beta pruning approach.
 * 
 * @param myConstraints semantic constraints in canonical form
 * @param topN number of matches to return
 * @param file output file
 */
public static void levenshteinDistanceRunnerHavePCsMaximinMoreEfficient(List<String> myConstraints, int topN, String file){
	//System.out.println("levenshtein runner");

	List<Answer> maxResults = new ArrayList<Answer>();
	List<Integer> bests = new ArrayList<Integer>();
	List<Double> tmpResults = new ArrayList<Double>();
	List<Double> intermediateResults = new ArrayList<Double>();
	List<Double> bestTopNTracker = new ArrayList<Double>() {
	    public boolean add(Double mt) {
	        int index = Collections.binarySearch(this, mt);
	        if (index < 0) index = ~index;
	        super.add(index, mt);
	        return true;
	    }
	};
	
	double bestN = 1000;
	int counter = 0;
	boolean valid = true;
	
	for (String o : constraints.keySet()){
		List<String> constr = constraints.get(o);
		if (constr.size() == 0){
			continue;
		}
		intermediateResults.clear();
		bests.clear();
		valid=true;

		for (int i = 0; i < myConstraints.size(); i++){
			tmpResults.clear();
			double worst = -1;
			for (String c : constr){
				double tmp = levenshteinDistance(c, myConstraints.get(i));
				tmpResults.add(tmp);
			}
			
			double best = Collections.min(tmpResults);
			worst = Math.max(worst, best);
			if (worst > bestN){
				valid = false;
				break;
			}
			bests.add(tmpResults.indexOf(best));
			intermediateResults.add(best);	
			
		}
		
		if (valid){
			Answer ansMax = new StaticDistanceCalculations().new Answer(o);
			ansMax.setLCS(Collections.max(intermediateResults));
			double dist = ansMax.getLCS();
			if (counter < topN){
				bestTopNTracker.add(dist);
			}
			maxResults.add(ansMax);
			if (counter >= topN && dist < bestN){
				bestTopNTracker.add(dist);
				bestTopNTracker.remove(topN);
				bestN = bestTopNTracker.get(topN-1);
			}
		}
		
		counter++;
	}
	System.out.println("-------------------");
	Collections.sort(maxResults, new StaticDistanceCalculations().new AnswerLCSComparator());
	double limit = maxResults.get(topN-1).getLCS();
	String out = "";
	int i = 0;
	Answer tmp = maxResults.get(i);
	while(tmp.getLCS() <= limit){
		System.out.println(tmp.getName() + " " + tmp.getLCS() );
		out += tmp.getName() + " " + tmp.getLCS() + "\n";
		i++;
		tmp = maxResults.get(i);
	}
	write(out, file, true);
}

/**
 * Runner to run a list of constraints against all those in the database using Longest Common Substring.
 * Includes capability for early termination on each comparison using Alpha-Beta pruning approach.
 * 
 * @param myConstraints semantic constraints in canonical form
 * @param topN number of matches to return
 * @param file output file
 */
public static void longestCommonSubstringRunnerHavePCsMinimaxMoreEfficient(List<String> myConstraints, int topN, String file){
	//System.out.println("levenshtein runner");
	if (constraints.size() < topN){
		System.out.println("not enough potential methods to return " + topN);
	}
		
	List<Answer> maxResults = new ArrayList<Answer>();
	List<Integer> bests = new ArrayList<Integer>();
	List<Double> tmpResults = new ArrayList<Double>();
	List<Double> intermediateResults = new ArrayList<Double>();
	List<Double> bestTopNTracker = new ArrayList<Double>() {
	    public boolean add(Double mt) {
	        int index = Collections.binarySearch(this, mt);
	        if (index < 0) index = ~index;
	        super.add(index, mt);
	        return true;
	    }
	};
	
	double bestN = 0;
	int counter = 0;
	boolean valid = true;
	
	for (String o : constraints.keySet()){
		List<String> constr = constraints.get(o);
		if (constr.size() == 0){
			continue;
		}
		intermediateResults.clear();
		bests.clear();
		valid=true;

		for (int i = 0; i < myConstraints.size(); i++){
			tmpResults.clear();
			double worst = 100;
			for (String c : constr){
				double tmp = longestCommonSubstring(c, myConstraints.get(i));
				tmpResults.add(tmp);
			}
			
			double best = Collections.max(tmpResults);
			worst = Math.min(worst, best);
			if (worst < bestN){
				valid = false;
				break;
			}
			bests.add(tmpResults.indexOf(best));
			intermediateResults.add(best);	
			
		}
		
		if (valid){
			Answer ansMax = new StaticDistanceCalculations().new Answer(o);
			ansMax.setLCS(Collections.min(intermediateResults));
			double dist = ansMax.getLCS();
			if (counter < topN){
				bestTopNTracker.add(dist);
			}
			maxResults.add(ansMax);
			if (counter >= topN && dist > bestN){
				bestTopNTracker.add(dist);
				bestTopNTracker.remove(topN);
				bestN = bestTopNTracker.get(topN-1);
			}
		}
		
		counter++;
	}
	System.out.println("-------------------");
	Collections.sort(maxResults, new StaticDistanceCalculations().new AnswerLCSComparator().reversed());
	double limit = maxResults.get(topN-1).getLCS();
	String out = "";
	int i = 0;
	Answer tmp = maxResults.get(i);
	while(tmp.getLCS() >= limit){
		System.out.println(tmp.getName() + " " + tmp.getLCS() );
		out += tmp.getName() + " " + tmp.getLCS() + "\n";
		i++;
		tmp = maxResults.get(i);
	}
	write(out, file, true);
}

/**
 * Runner to run a list of constraints against all those in the database using Levenshtein distance.
 * Includes capability for early termination on each comparison using Alpha-Beta pruning approach.
 * Writes output to stdout.
 * 
 * @param myConstraints semantic constraints in canonical form
 * @param topN number of matches to return
 */
public static void levenshteinDistanceRunnerHavePCsMaximinMoreEfficient(List<String> myConstraints, int topN){
	//System.out.println("levenshtein runner");

	List<Answer> maxResults = new ArrayList<Answer>();
	List<Integer> bests = new ArrayList<Integer>();
	List<Double> tmpResults = new ArrayList<Double>();
	List<Double> intermediateResults = new ArrayList<Double>();
	List<Double> bestTopNTracker = new ArrayList<Double>() {
	    public boolean add(Double mt) {
	        int index = Collections.binarySearch(this, mt);
	        if (index < 0) index = ~index;
	        super.add(index, mt);
	        return true;
	    }
	};
	
	double bestN = 1000;
	int counter = 0;
	boolean valid = true;
	
	for (String o : constraints.keySet()){
		List<String> constr = constraints.get(o);
		if (constr.size() == 0){
			continue;
		}
		intermediateResults.clear();
		bests.clear();
		valid=true;

		for (int i = 0; i < myConstraints.size(); i++){
			tmpResults.clear();
			double worst = -1;
			for (String c : constr){
				double tmp = levenshteinDistance(c, myConstraints.get(i));
				tmpResults.add(tmp);
			}
			
			double best = Collections.min(tmpResults);
			worst = Math.max(worst, best);
			if (worst > bestN){
				valid = false;
				break;
			}
			bests.add(tmpResults.indexOf(best));
			intermediateResults.add(best);	
			
		}
		
		if (valid){
			Answer ansMax = new StaticDistanceCalculations().new Answer(o);
			ansMax.setLCS(Collections.max(intermediateResults));
			double dist = ansMax.getLCS();
			if (counter < topN){
				bestTopNTracker.add(dist);
			}
			maxResults.add(ansMax);
			if (counter >= topN && dist < bestN){
				bestTopNTracker.add(dist);
				bestTopNTracker.remove(topN);
				bestN = bestTopNTracker.get(topN-1);
			}
		}
		
		counter++;
	}
	System.out.println("-------------------");
	Collections.sort(maxResults, new StaticDistanceCalculations().new AnswerLCSComparator());
	double limit = maxResults.get(topN-1).getLCS();
	int i = 0;
	Answer tmp = maxResults.get(i);
	while(tmp.getLCS() <= limit){
		System.out.println(tmp.getName() + " " + tmp.getLCS() );
		i++;
		tmp = maxResults.get(i);
	}
}

/**
 * Runner to run a list of constraints against all those in the database using Longest Common Substring.
 * Includes capability for early termination on each comparison using Alpha-Beta pruning approach.
 * Writes output to stdout.
 * 
 * @param myConstraints semantic constraints in canonical form
 * @param topN number of matches to return
 */
public static void longestCommonSubstringRunnerHavePCsMinimaxMoreEfficient(List<String> myConstraints, int topN){
	//System.out.println("levenshtein runner");
	System.out.println(myConstraints.size());
	List<Answer> maxResults = new ArrayList<Answer>();
	List<Integer> bests = new ArrayList<Integer>();
	List<Double> tmpResults = new ArrayList<Double>();
	List<Double> intermediateResults = new ArrayList<Double>();
	List<Double> bestTopNTracker = new ArrayList<Double>() {
	    public boolean add(Double mt) {
	        int index = Collections.binarySearch(this, mt);
	        if (index < 0) index = ~index;
	        super.add(index, mt);
	        return true;
	    }
	};
	
	double bestN = 0;
	int counter = 0;
	boolean valid = true;
	
	for (String o : constraints.keySet()){
		List<String> constr = constraints.get(o);
		if (constr.size() == 0){
			continue;
		}
		intermediateResults.clear();
		bests.clear();
		valid=true;

		for (int i = 0; i < myConstraints.size(); i++){
			tmpResults.clear();
			double worst = -1;
			for (String c : constr){
				double tmp = longestCommonSubstring(c, myConstraints.get(i));
				tmpResults.add(tmp);
			}
			
			double best = Collections.max(tmpResults);
			worst = Math.min(worst, best);
			if (worst < bestN){
				valid = false;
				break;
			}
			bests.add(tmpResults.indexOf(best));
			intermediateResults.add(best);	
			
		}
		
		if (valid){
			Answer ansMax = new StaticDistanceCalculations().new Answer(o);
			ansMax.setLCS(Collections.min(intermediateResults));
			double dist = ansMax.getLCS();
			if (counter < topN){
				bestTopNTracker.add(dist);
			}
			maxResults.add(ansMax);
			if (counter >= topN && dist > bestN){
				bestTopNTracker.add(dist);
				bestTopNTracker.remove(topN);
				bestN = bestTopNTracker.get(topN-1);
			}
		}
		
		counter++;
	}
	System.out.println("-------------------");
	Collections.sort(maxResults, new StaticDistanceCalculations().new AnswerLCSComparator());
	double limit = maxResults.get(topN-1).getLCS();
	int i = 0;
	Answer tmp = maxResults.get(i);
	while(tmp.getLCS() >= limit){
		System.out.println(tmp.getName() + " " + tmp.getLCS() );
		i++;
		tmp = maxResults.get(i);
	}
}

/**
 * Runner to run a list of constraints against all those in the database using Longest Common Substring
 * as part of the CSAR search approach.
 * 
 * @param myConstraints semantic constraints in canonical form
 * @param topN number of matches to return
 * @param file output file
 */
public static void adaptiveLongestCommonSubstringRunner(List<String> myConstraints, int topN, String file, int limitN){
	//System.out.println("levenshtein runner");
		if (constraints.size() < topN){
			System.out.println("not enough potential methods to return " + topN);
		}
			
		List<Answer> maxResults = new ArrayList<Answer>();
		List<Integer> bests = new ArrayList<Integer>();
		List<Double> tmpResults = new ArrayList<Double>();
		List<Double> intermediateResults = new ArrayList<Double>();
		List<Double> bestTopNTracker = new ArrayList<Double>() {
		    public boolean add(Double mt) {
		        int index = Collections.binarySearch(this, mt);
		        if (index < 0) index = ~index;
		        super.add(index, mt);
		        return true;
		    }
		};
		
		double bestN = 0;
		int counter = 0;
		boolean valid = true;
		
		for (String o : constraints.keySet()){
			List<String> constr = constraints.get(o);
			if (constr.size() == 0){
				continue;
			}
			intermediateResults.clear();
			bests.clear();
			valid=true;

			for (int i = 0; i < myConstraints.size(); i++){
				tmpResults.clear();
				double worst = 100;
				for (String c : constr){
					double tmp = longestCommonSubstring(c, myConstraints.get(i));
					tmpResults.add(tmp);
				}
				
				double best = Collections.max(tmpResults);
				worst = Math.min(worst, best);
				if (worst < bestN){
					valid = false;
					break;
				}
				bests.add(tmpResults.indexOf(best));
				intermediateResults.add(best);	
				
			}
			
			if (valid){
				Answer ansMax = new StaticDistanceCalculations().new Answer(o);
				ansMax.setLCS(Collections.min(intermediateResults));
				double dist = ansMax.getLCS();
				if (counter < topN){
					bestTopNTracker.add(dist);
				}
				maxResults.add(ansMax);
				if (counter >= topN && dist > bestN){
					bestTopNTracker.add(dist);
					bestTopNTracker.remove(topN);
					bestN = bestTopNTracker.get(topN-1);
				}
			}
			counter++;
		}
		
		Collections.sort(maxResults, new StaticDistanceCalculations().new AnswerLCSComparator().reversed());
		double limit = maxResults.get(topN-1).getLCS();
		String out = "";
		int i = 0;
		Answer tmp = maxResults.get(i);
		while(tmp.getLCS() >= limit){
			System.out.println(tmp.getName() + " " + tmp.getLCS() );
			out += tmp.getName() + " " + tmp.getLCS() + "\n";
			i++;
			tmp = maxResults.get(i);
		}
		if (i >= limitN){
			out += "**UNPATCHABLE**\n";
		}
		write(out, file, true);
	}

/**
 * Runner to run a list of constraints against all those in the database using Longest Common Substring.
 * Writes output to stdout
 * 
 * @param myConstraints semantic constraints in canonical form
 * @param topN number of matches to return
 */
public static void longestCommonSubstringRunnerHavePCsMinimax(List<String> myConstraints, int topN){

	List<String> others = new ArrayList<String>();
	List<Answer> maxResults = new ArrayList<Answer>();
	List<Integer> bests = new ArrayList<Integer>();
	List<Double> tmpResults = new ArrayList<Double>();
	List<Double> intermediateResults = new ArrayList<Double>();
	others = fetchAllMethodNames();
	
	for (String o : others){
		List<String> constraints = fetchConstraints(o);
		intermediateResults.clear();
		bests.clear();

		
		for (String c : constraints){
			tmpResults.clear();
			for (int i = 0; i < myConstraints.size(); i++){
				double tmp = longestCommonSubstring(c, myConstraints.get(i));
				tmpResults.add(tmp);
			}
			intermediateResults.add(Collections.max(tmpResults));
			
		}
		
		Answer ansMax = new StaticDistanceCalculations().new Answer(o);
		ansMax.setLCS(Collections.min(intermediateResults));
		maxResults.add(ansMax);
	}
	System.out.println("-------------------");
	Collections.sort(maxResults, new StaticDistanceCalculations().new AnswerLCSComparator().reversed());
	double limit = maxResults.get(topN-1).getLCS();
	int i = 0;
	Answer tmp = maxResults.get(i);
	while(tmp.getLCS() >= limit){
		System.out.println(tmp.getName() + " " + tmp.getLCS());
		i++;
		tmp = maxResults.get(i);
	}
	
	
}


/**
 * Runner to run a list of constraints against all those in the database using Longest Common Substring.
 * 
 * @param myConstraints semantic constraints in canonical form
 * @param topN number of matches to return
 * @param file output file
 */
public static void longestCommonSubstringRunnerHavePCsMinimax(List<String> myConstraints, int topN, String file){
	List<Answer> maxResults = new ArrayList<Answer>();
	List<Integer> bests = new ArrayList<Integer>();
	List<Double> tmpResults = new ArrayList<Double>();
	List<Double> intermediateResults = new ArrayList<Double>();
	for (String o : constraints.keySet()){
		List<String> constr = constraints.get(o);
		intermediateResults.clear();
		bests.clear();
		
		for (String c : constr){
			tmpResults.clear();
			for (int i = 0; i < myConstraints.size(); i++){
				double tmp = longestCommonSubstring(c, myConstraints.get(i));
				tmpResults.add(tmp);
			}
			intermediateResults.add(Collections.max(tmpResults));
		}
		
		Answer ansMax = new StaticDistanceCalculations().new Answer(o);
		ansMax.setLCS(Collections.min(intermediateResults));
		maxResults.add(ansMax);
	}
	System.out.println("-------------------");
	String out = "";
	Collections.sort(maxResults, new StaticDistanceCalculations().new AnswerLCSComparator().reversed());
	for (int i = 0; i < topN; i++){
		Answer tmp = maxResults.get(i);
		out += tmp.getName() + " " + tmp.getLCS() + "\n";
		System.out.println(tmp.getName() + " " + tmp.getLCS());
	}
	
	write(out, file, true);
	
}


/**
 * Writes string <i>s</i> to <i>file</i>.
 * If <i>append</i> is true, will append rather than overwrite.
 * 
 * @param s string to be written
 * @param file target file
 * @param append false-overwrite target file; true-append to end of target file
 */
private static void write(String s, String file, boolean append){
	BufferedWriter bw;
	try {
		bw = new BufferedWriter(new FileWriter(file, append));
		bw.append(s);
		bw.close();
	} 
	catch (IOException e) {
		e.printStackTrace();
	}
}


/**
 * Fetch the name of all methods in the database
 * @return
 */
private static List<String> fetchAllMethodNames(){		
	List<String> results = new ArrayList<String>();
	
	try{
		Class.forName("com.mysql.jdbc.Driver");  
		Connection con = DriverManager.getConnection(base,"root",password); 
		PreparedStatement ps = con.prepareStatement("select txt from "+database+".method_text_tmp");
		ps.execute();
		ResultSet rs = ps.getResultSet();
		
		while(rs.next())
		{
			String pc = rs.getString(1);
			results.add(pc);
		}
		
		
	} catch (ClassNotFoundException e) {
		e.printStackTrace();
		System.exit(1);
	} catch (SQLException e) {
		e.printStackTrace();
		System.exit(0);
	}
	
	return results;
}

/**
 * Fetch all constraints from the database, ordered by method
 */
private static void fetchAllMethodConstraints(){	
	try{
		Class.forName("com.mysql.jdbc.Driver");  
		Connection con = DriverManager.getConnection(base,"root",password); 
		PreparedStatement ps = con.prepareStatement("select distinct txt,pc from "+database+".method_text_tmp inner join "+database+".constraints_tmp using(mid)");
		ps.execute();
		ResultSet rs = ps.getResultSet();
		// TODO - look at make database a parameter

		while(rs.next())
		{
			String name = rs.getString(1);
			String constraint = rs.getString(2);
			if (constraints.size() < 1){
				List<String> tmp = new ArrayList<String>();
				tmp.add(constraint);
				constraints.put(name, tmp);
			}
			else{
				if (constraints.containsKey(name)){
					List<String> tmp = constraints.get(name);
					tmp.add(constraint);
					constraints.put(name, tmp);
				}
				else{
					constraints.put(name, Stream.of(constraint).collect(Collectors.toList()));
				}
			}
		}
		
		
	} catch (ClassNotFoundException e) {
		e.printStackTrace();
		System.exit(1);
	} catch (SQLException e) {
		e.printStackTrace();
		System.exit(0);
	}
}


/**
 * Fetch all names of methods in the database, excluding one.
 * 
 * @param name method name to exclude
 * @return
 */
private static List<String> fetchOtherMethodNames(String name){		
	List<String> results = new ArrayList<String>();
	
	try{
		Class.forName("com.mysql.jdbc.Driver");  
		Connection con = DriverManager.getConnection(base,"root",password); 
		PreparedStatement ps = con.prepareStatement("select txt from "+database+".method_text_tmp where txt!=?");
		ps.setString(1, name);
		ps.execute();
		ResultSet rs = ps.getResultSet();
		
		while(rs.next())
		{
			String pc = rs.getString(1);
			results.add(pc);
		}
	} catch (ClassNotFoundException e) {
		e.printStackTrace();
		System.exit(1);
	} catch (SQLException e) {
		e.printStackTrace();
		System.exit(0);
	}
	return results;
}

/**
 * Fetch the constraints associated with a single method name from the database.
 * 
 * @param name name of method whose constraints are sought
 * @return
 */
private static List<String> fetchConstraints(String name){		
	List<String> results = new ArrayList<String>();
	
	try{
		Class.forName("com.mysql.jdbc.Driver");  
		Connection con = DriverManager.getConnection(base,"root",password); 
		PreparedStatement ps = con.prepareStatement("select pc from "+database+".method_text_tmp inner join "+database+".constraints_tmp using(mid) where txt=?");
		ps.setString(1, name);
		ps.execute();
		ResultSet rs = ps.getResultSet();
		
		while(rs.next())
		{
			String pc = rs.getString(1);
			results.add(pc);
		}
	} catch (ClassNotFoundException e) {
		e.printStackTrace();
		System.exit(1);
	} catch (SQLException e) {
		e.printStackTrace();
		System.exit(0);
	}
	return results;
}


/**
 * Dynamic Programming Java implementation of LCS problem
 * This method is due to Saket Kumar and from
 * http://www.geeksforgeeks.org/dynamic-programming-set-4-longest-common-subsequence
 * 
 * @param a
 * @param b
 * @return
 */
public static int longestCommonSubsequence(String a, String b){
	char[] x = a.toCharArray();
	char[] y = b.toCharArray();
	int m = x.length;
	int n = y.length;
	int limit = Math.min(m, n);
	int L[][] = new int[m + 1][n + 1];
	
	for (int i=0; i<=m; i++){
		for (int j=0; j<=n; j++){
			if (i ==0 || j == 0){
				L[i][j] = 0;
			}
			else if (i < limit && (x[i-1] == y[i-1])){
				L[i][j] = L[i-1][j-1] + 1;
			}
			else{
				L[i][j] = Math.max(L[i-1][j], L[i][j-1]);
			}
		}
	}
	return L[m][n];
}

/**
 * Dynamic Programming Java implementation of LCSubstring problem
 * This method is an optimized version of the algorithm on wikipedia for longest common substring and from 
 * https://stackoverflow.com/questions/34805488/finding-all-the-common-substrings-of-given-two-strings
 * 
 * @param a
 * @param b
 * @return
 */
public static int longestCommonSubstring(String a, String b){
	int[][] table = new int[a.length()][b.length()];
	int longest = 0;
	// Set<String> result = new HashSet<String>(); // Can be uncommented if I need the longest substring itself
	
	for (int i=0; i<a.length(); i++){
		for (int j=0; j<b.length(); j++){
			if (a.charAt(i) != b.charAt(j)){
				continue;
			}
			
			table[i][j] = (i == 0 || j == 0) ? 1 : 1 + table[i-1][j-1];
			
			if (table[i][j] > longest){
				longest = table[i][j];
			}
		}
	}
	return longest;
}


/**
 * Based on the matrix implementation pseudocode in wikipedia article
 * on Levenshtein Distance (edit distance).
 * 
 * @param a
 * @param b
 * @param print
 * @return
 */
public static double levenshteinDistance(String a, String b){
	char[] s = a.replaceAll("[\r\t\n]", "").trim().toCharArray();
	char[] t = b.replaceAll("[\r\t\n]", "").trim().toCharArray();
	int substitutionCost = 1;
	int limit = Math.min(s.length, t.length);
	// throughout, dist[i][j] holds the Levenshtein Distance between the first
	// i characters of a and the first j characters of b.
	int[][] dist = new int[s.length+1][t.length+1];

	for (int j = 1; j <= t.length; j++){
		for (int i = 1; i <= s.length; i++){
			// doing initialization here instead of in a separate loop
			if (j == 0){
				dist[i][j] = i;
			}
			else if (i == 0){
				dist[i][j] = j;
			}
			else{
				dist[i][j] = 0;
			}
			
			// for calculating whether need to substitute this letter to make them match
			if (i < limit && j < limit && s[i] == t[j]){
				substitutionCost = 0;
			}
			else{
				if(i==s.length && j==t.length){
					substitutionCost = 0;
				}
				else{
					substitutionCost = 1;
				}
			}
			
			// checking deletion, insertion, substitution respectively
			int tmpDel = dist[i-1][j] + 1;
			int tmpIns = dist[i][j-1] + 1;
			int tmpSub = dist[i-1][j-1] + substitutionCost;
			int tmp1 = Math.min(tmpDel,  tmpIns);
			int tmp2 = Math.min(tmp1, tmpSub);
			dist[i][j] = tmp2;//Math.min(Math.min(dist[i-1][j] + 1, dist[i][j-1] + 1), dist[i-1][j-1] + substitutionCost);
			
		}
	}
	
	return dist[s.length][t.length];
}



/**
 * Class to ease storage of calculated distance values with the associated method names
 *
 */
private class Answer{
	protected String name;
	protected double lcs;
	private Answer(String a){
		name = a;
		lcs = 0;
	}
	
	private void setLCS(double d){
		lcs = d;
	}
	
	public String getName(){
		return name;
	}
	
	public double getLCS(){
		return lcs;
	}
}

/**
 * Comparator to be used with Answer
 */
public class AnswerLCSComparator implements Comparator<Answer> {
    @Override
    public int compare(Answer o1, Answer o2) {
    	return (int) (o1.getLCS() - o2.getLCS());
    }
}

/**
 * Comparator to be used with Answers with double values of LCS
 *
 */
public class DoubleAnswerLCSComparator implements Comparator<Answer> {
    @Override
    public int compare(Answer o1, Answer o2) {
    	double tmp1 = o1.getLCS();
    	double tmp2 = o2.getLCS();
    	if (tmp1 < tmp2){
    		return -1;
    	}
    	else if (tmp1 == tmp2){
    		return 0;
    	}
    	else{
    		return 1;
    	}
    }
}
}