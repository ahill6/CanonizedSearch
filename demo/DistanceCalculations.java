package gov.nasa.jpf.symbc.repairproject;

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
import java.util.HashSet;
import java.util.List;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;

public class DistanceCalculations{
	String database = "jdbc:mysql://localhost:xxxx/searchrepair2";
	String password = "zzzz"; // TODO - move this to config file"
	//TODO - the path to your database and your database password should be put here
	
	public DistanceCalculations(){
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public void compareHavePCs(List<String> testStrings, String name, String filename, int giveTopN){
		/* Finds giveTopN closest semantic matches in database for Path Constraints in testStrings.  
		 * Writes results to filename (filename is absolute path)
		 */
		System.out.println("compare PCs");
		String newFile = filename.substring(0, 13) + filename.substring(13).replaceAll("[0-9]", "");

		String fileEdit = newFile + "Edit.txt";
		//String fileSub = newFile + "Sub.txt";

		write(name+"\n", fileEdit, true);
		//write(name+"\n", fileSub, true);
		
		levenshteinDistanceRunnerHavePCsMaximin(testStrings, giveTopN, fileEdit);
		//longestCommonSubstringRunnerHavePCsMinimax(testStrings, giveTopN, fileSub);
	}
	
	
	public void getTopMatchesDB(String[] testStrings, int giveTopN){
		/* Finds giveTopN closest semantic matches in database for each of the methods whose names
		 * are in testStrings.  Writes results to stdout.
		 */
	for (String t : testStrings){
		System.out.println(t);
		System.out.println("/////////////////////////////////////////////");
		//longestCommonSubsequenceRunner("TestPaths." + t, giveTopN);
		//longestCommonSubstringRunner( t, giveTopN);
		levenshteinDistanceRunner( t, giveTopN);
		System.out.println("/////////////////////////////////////////////");
		//z3NaiveRunner(t);
		//z3ComplicatedRunner(t);
	}
}

public void longestCommonSubsequenceRunner(String a, int topN){
	// get a list of all methods which are not a
	List<String> myConstraints = fetchConstraints(a);
	List<String> others = new ArrayList<String>();
	List<Answer> minResults = new ArrayList<Answer>();
	List<Integer> bests = new ArrayList<Integer>();
	List<Integer> tmpResults = new ArrayList<Integer>();
	List<Integer> intermediateResults = new ArrayList<Integer>();
	others = fetchOtherMethodNames(a);
	//System.out.println(others.size());
	
	for (String o : others){
		//System.out.println(o);
		List<String> constraints = fetchConstraints(o);
		//System.out.println(constraints.size());
		intermediateResults.clear();
		bests.clear();
		for (String c : constraints){
			//System.out.println(c);
			tmpResults.clear();
			for (int i = 0; i < myConstraints.size(); i++){
				tmpResults.add(longestCommonSubsequence(c,myConstraints.get(i)));
				//System.out.println("matched something");
				// somewhere here put a penalty term for using the same constraint to match everything.
			}
			int best = 0;
			//System.out.println(tmpResults.size());
			for (int i = 1; i < tmpResults.size(); i++){
				if (tmpResults.get(i) > tmpResults.get(best)){
					best = i;
				}
			}
			bests.add(best);
			intermediateResults.add(tmpResults.get(best));	
		}
		int minResult = intermediateResults.get(0);
		// TODO - try an average also scaled by the number of constraints in the sets / in the other set
		//int maxResult = intermediateResults.get(0);
		//int sumResult = 0;
		//int avgResult = ;
		//List<Integer>medResults = new ArrayList<Integer>();
		//int iqrResult = ;
		//int prodResult = 1;
		//int logProdResult = ;
		
		for (int ir : intermediateResults){
			int tmp = minResult;
			minResult = Math.min(tmp, ir);
			/*
			tmp = maxResult;
			maxResult = Math.max(tmp, ir);
			sumResult += ir;
			medResults.add(ir);
			prodResult *= ir;
			*/
		}
		// Quick run showed Min performing best
		
		int penalty = bests.size() - new HashSet(bests).size();
		Answer ansMin = new Answer(o);
		ansMin.setLCS(minResult);
		minResults.add(ansMin);
		/*
		Answer ansMax = new Runner().new Answer(o);
		ansMax.setLCS(maxResult);
		maxResults.add(ansMax);
		Answer ansAvg = new Runner().new Answer(o);
		ansAvg.setLCS(sumResult/intermediateResults.size());
		avgResults.add(ansAvg);
		*/

	}

	Collections.sort(minResults, new AnswerLCSComparator());
	Collections.reverse(minResults);
	
	System.out.println("LCS");
	System.out.println("**********************************************");
	for (int i = 0; i < topN; i++){
		Answer tmp = minResults.get(i);
		System.out.println(tmp.getName() + " " + tmp.getLCS());
	}
	System.out.println("**********************************************");
	
}


public void levenshteinDistanceRunnerHavePCs(List<String> myConstraints, int topN){
	/* This method is used if the Path Constraints are passed (rather than the name of the method).
	 * This allows a method to be tested without adding it to the database.
	 */
	
	List<String> others = new ArrayList<String>();
	List<Answer> maxResults = new ArrayList<Answer>();
	List<Integer> bests = new ArrayList<Integer>();
	List<Double> tmpResults = new ArrayList<Double>();
	List<Double> intermediateResults = new ArrayList<Double>();
	others = fetchAllMethodNames();
	boolean pr = false;
	for (String o : others){
		List<String> constraints = fetchConstraints(o);
		intermediateResults.clear();
		bests.clear();
		pr = false;
		for (String c : constraints){
			tmpResults.clear();
			for (int i = 0; i < myConstraints.size(); i++){
				if (o.contains("median")){
					pr = true;
				}
				double tmp = levenshteinDistance(c, myConstraints.get(i),pr);
				tmpResults.add(tmp);
				// somewhere here put a penalty term for using the same constraint to match everything.
			}
			int best = 0;
			for (int i = 1; i < tmpResults.size(); i++){
				if (tmpResults.get(i) < tmpResults.get(best)){
					best = i;
				}
			}
			bests.add(best);
			intermediateResults.add(tmpResults.get(best));	
		}
		double maxResult = intermediateResults.get(0);
		
		for (double ir : intermediateResults){
			double tmp = maxResult;
			maxResult = Math.max(tmp, ir);
		}
		// attempt 1 to implement penalty term for using the same PC to match all
		int one = new HashSet<Integer>(bests).size() ;
		double two = (double)bests.size();
		double penalty = two / one;
		Answer ansMax = new Answer(o);
		ansMax.setLCS(maxResult);
		maxResults.add(ansMax);
	}
	Collections.sort(maxResults, new AnswerLCSComparator());
	
	System.out.println("Levenshtein");
	System.out.println("---------------------------------------------");
	for (int i = 0; i < topN; i++){
		Answer tmp = maxResults.get(i);
		System.out.println(tmp.getName() + " " + tmp.getLCS());
	}
	System.out.println("---------------------------------------------");
}


public void levenshteinDistanceRunner(String a, int topN){
	/* This method is used if the Path Constraints are passed (rather than the name of the method).
	 * This allows a method to be tested without adding it to the database.  This method writes output to a file
	 * rather than to stdout.
	 */
	List<String> myConstraints = fetchConstraints(a);
	List<String> others = new ArrayList<String>();
	List<Answer> maxResults = new ArrayList<Answer>();
	List<Integer> bests = new ArrayList<Integer>();
	List<Double> tmpResults = new ArrayList<Double>();
	List<Double> intermediateResults = new ArrayList<Double>();
	others = fetchOtherMethodNames(a);

	for (String o : others){
		List<String> constraints = fetchConstraints(o);
		intermediateResults.clear();
		bests.clear();

		for (int i = 0; i < myConstraints.size(); i++){
			tmpResults.clear();
			for (String c : constraints){
				double tmp = levenshteinDistance(c, myConstraints.get(i), false);
				tmpResults.add(tmp);
			}

			double best = Collections.min(tmpResults);
			bests.add(tmpResults.indexOf(best));
			intermediateResults.add(best);	
			
		}
		
		Answer ansMax = new Answer(o);
		ansMax.setLCS(Collections.max(intermediateResults));
		maxResults.add(ansMax);
	}
	System.out.println("Levenshtein Distance");
	System.out.println("-------------------");
	Collections.sort(maxResults, new DoubleAnswerLCSComparator().reversed());
	for (int i = 0; i < topN; i++){
		Answer tmp = maxResults.get(maxResults.size() - (i+1));
		System.out.println(tmp.getName() + " " + tmp.getLCS() );
	}
}



public void levenshteinDistanceRunnerHavePCsMaximin(List<String> myConstraints, int topN, String file){
	/* This method is used if the Path Constraints are passed (rather than the name of the method).
	 * This allows a method to be tested without adding it to the database.  This method writes output to a file
	 * rather than to stdout.
	 */

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
				double tmp = levenshteinDistance(c, myConstraints.get(i), false);
				tmpResults.add(tmp);
			}
			
			double best = Collections.min(tmpResults);
			bests.add(tmpResults.indexOf(best));
			intermediateResults.add(best);	
			
		}
		/*
		double tmp = 0;
		for (double ir : intermediateResults){
			tmp += ir;
		}
		double avg = tmp/intermediateResults.size();
		*/
		Answer ansMax = new Answer(o);
		ansMax.setLCS(Collections.max(intermediateResults));
		//ansMax.setLCS(avg);

		//ansMax.setLCS(maxResult);
		maxResults.add(ansMax);
	}
	System.out.println("-------------------");
	String out = "";
	Collections.sort(maxResults, new DoubleAnswerLCSComparator().reversed());
	for (int i = 0; i < topN; i++){
		Answer tmp = maxResults.get(maxResults.size() - (i+1));
		out += tmp.getName() + " " + tmp.getLCS() + "\n";
		System.out.println(tmp.getName() + " " + tmp.getLCS() );
	}
	write(out, file, true);
}



public void longestCommonSubstringRunnerHavePCsMinimax(List<String> myConstraints, int topN, String file){
	/* This method is used if the Path Constraints are passed (rather than the name of the method).
	 * This allows a method to be tested without adding it to the database.
	 */

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
				// somewhere here put a penalty term for using the same constraint to match everything.
			}
			intermediateResults.add(Collections.max(tmpResults));
			
		}
		
		Answer ansMax = new Answer(o);
		ansMax.setLCS(Collections.min(intermediateResults));
		maxResults.add(ansMax);
	}
	System.out.println("-------------------");
	String out = "";
	Collections.sort(maxResults, new ReverseAnswerLCSComparator());
	for (int i = 0; i < topN; i++){
		Answer tmp = maxResults.get(i);
		out += tmp.getName() + " " + tmp.getLCS() + "\n";
		System.out.println(tmp.getName() + " " + tmp.getLCS());
	}
	
	write(out, file, true);
	
}

public void longestCommonSubstringRunner(String a, int topN){
	/* This method is used if the Path Constraints are passed (rather than the name of the method).
	 * This allows a method to be tested without adding it to the database.  This method writes output to a file
	 * rather than to stdout.
	 */
	List<String> myConstraints = fetchConstraints(a);
	List<String> others = new ArrayList<String>();
	List<Answer> maxResults = new ArrayList<Answer>();
	List<Integer> bests = new ArrayList<Integer>();
	List<Double> tmpResults = new ArrayList<Double>();
	List<Double> intermediateResults = new ArrayList<Double>();
	others = fetchOtherMethodNames(a);

	for (String o : others){
		List<String> constraints = fetchConstraints(o);
		intermediateResults.clear();
		bests.clear();

		for (int i = 0; i < myConstraints.size(); i++){
			tmpResults.clear();
			for (String c : constraints){
				double tmp = longestCommonSubstring(c, myConstraints.get(i));
				tmpResults.add(tmp);
			}
			
			double best = Collections.max(tmpResults);
			bests.add(tmpResults.indexOf(best));
			intermediateResults.add(best);	
			
		}
		Answer ansMax = new Answer(o);
		ansMax.setLCS(Collections.min(intermediateResults));
		maxResults.add(ansMax);
	}
	System.out.println("LCSub Distance");
	System.out.println("-------------------");
	Collections.sort(maxResults, new DoubleAnswerLCSComparator().reversed());
	//System.out.println(maxResults.size());
	for (int i = 0; i < topN; i++){
		Answer tmp = maxResults.get(maxResults.size() - (i+1));
		System.out.println(tmp.getName() + " " + tmp.getLCS() );
	}
}


public void levenshteinDistanceRunnerHavePCsWithRenaming(List<String> myConstraints, int topN, String file){
	/* This method will attempt to find matches for which the constraints are close, but the variables were renamed
	 * in a different order.  This is one problem with the current renaming scheme.
	 */
	
	System.out.println("UNDER WORK, METHOD NOT COMPLETED");
	/*
	List<String> others = new ArrayList<String>();
	List<Answer> maxResults = new ArrayList<Answer>();
	List<Integer> bests = new ArrayList<Integer>();
	List<Double> tmpResults = new ArrayList<Double>();
	List<Double> tmpTmpResults = new ArrayList<Double>();
	List<Double> intermediateResults = new ArrayList<Double>();
	HashMap<String, String> varRename = new HashMap<String, String>();
	
    String pattern = "(.*)(\\d+)(.*)";
    Pattern r = Pattern.compile(pattern);
    
    
	others = fetchAllMethodNames();
	for (String o : others){
		List<String> constraints = fetchConstraints(o);
		intermediateResults.clear();
		bests.clear();
		
		for (int i = 0; i < myConstraints.size(); i++){
			tmpResults.clear();
			for (String c : constraints){
			    // Now create matcher object.
			    Matcher m = r.matcher(c);
			    if (m.find( )) {
			       for (xx x : m.)
			    }else {
			       System.out.println("NO MATCH");
			    }
				double tmp = levenshteinDistance(c, myConstraints.get(i), false);
				tmpResults.add(tmp);
			}
			
			double best = Collections.min(tmpResults);
			bests.add(tmpResults.indexOf(best));
			intermediateResults.add(best);	
		
			
		}
		
		double maxResult = intermediateResults.get(0);
		
		for (double ir : intermediateResults){
			double tmp = maxResult;
			maxResult = Math.max(tmp, ir);
		}
		// attempt 1 to implement penalty term for using the same PC to match all
		int one = new HashSet<Integer>(bests).size() ;
		double two = (double)bests.size();
		double penalty = two / one;
		//System.out.println(o + "\t" + two + "\t" + one);
		Answer ansMax = new Answer(o);
		ansMax.setLCS(Collections.max(intermediateResults)/intermediateResults.size());

		//ansMax.setLCS(maxResult);
		maxResults.add(ansMax);*/
	}	

private void write(String s, String file, boolean append){
	// write converted to Tmp.java

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


private List<String> fetchAllMethodNames(){		
	List<String> results = new ArrayList<String>();
	
	try{
		Class.forName("com.mysql.jdbc.Driver");  
		
		Connection con = DriverManager.getConnection(database,"root",password); 
		
		// for each unit test, make a unit test tied to this set id
		PreparedStatement ps = con.prepareStatement("select txt from searchrepair2.method_text_tmp");
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

private List<String> fetchOtherMethodNames(String name){		
	List<String> results = new ArrayList<String>();
	
	try{
		Class.forName("com.mysql.jdbc.Driver");  
		
		Connection con = DriverManager.getConnection(database,"root",password); 
		
		// for each unit test, make a unit test tied to this set id
		PreparedStatement ps = con.prepareStatement("select txt from searchrepair2.method_text_tmp where txt!=?");
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

private List<String> fetchConstraints(String name){		
	List<String> results = new ArrayList<String>();
	
	try{
		Class.forName("com.mysql.jdbc.Driver");
		
		Connection con = DriverManager.getConnection(database,"root",password); 
		
		// for each unit test, make a unit test tied to this set id
		//System.out.println(name);
		PreparedStatement ps = con.prepareStatement("select pc from searchrepair2.method_text_tmp inner join searchrepair2.constraints_tmp using(mid) where txt=?");
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

public int longestCommonSubsequence(String a, String b){
/* Dynamic Programming Java implementation of LCS problem
 * This method is due to Saket Kumar and from
 * http://www.geeksforgeeks.org/dynamic-programming-set-4-longest-common-subsequence */
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


public int longestCommonSubstring(String a, String b){
/* Dynamic Programming Java implementation of LCSubstring problem
 * This method is an optimized version of the algorithm on wikipedia for longest common substring and from 
 * https://stackoverflow.com/questions/34805488/finding-all-the-common-substrings-of-given-two-strings */
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
				// result.clear();
			}
			/*
			if (table[i][j] == longest){
				result.add(a.substring(i - longest + 1, i + 1));
			}
			*/
			
		}
	}
	return longest;
}

public double levenshteinDistance(String a, String b, boolean print){
/* Based on the matrix implementation pseudocode in wikipedia article
 * on Levenshtein Distance (edit distance).
 */
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

	if (print){
		System.out.println(s[48] == t[48]);
		System.out.println(s.length);
		System.out.println(t.length);
		System.out.println(dist[s.length][t.length]);
		for (int i=0;i<s.length+1;i++){
			System.out.print("\n");
			for(int j=0;j<t.length+1;j++)
				System.out.print(dist[i][j] + "-");
		}
	}
	
	return dist[s.length][t.length];
}



public boolean compareZ3(BoolExpr a, BoolExpr b){
	Expr[] aExprs = a.getArgs();
	Expr[] bExprs = b.getArgs();
	List<Expr> combinedExprs = new ArrayList<Expr>();
	
	
	// perfect equivalence : check whether for all values of input variables, the two are equal (how I would do that...?)
	return false;
}




private class Answer{
	protected String name;
	protected double dist;
	protected double lcs;
	private Answer(String a){
		name = a;
		dist = 0.0;
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
	
	public double getDistance(){
		return dist;
	}
}

public class AnswerLCSComparator implements Comparator<Answer> {
    @Override
    public int compare(Answer o1, Answer o2) {
    	return (int) (o1.getLCS() - o2.getLCS());
    }
}

public class DoubleAnswerLCSComparator implements Comparator<Answer> {
    @Override
    public int compare(Answer o1, Answer o2) {
    	if (o1.getLCS() <= o2.getLCS()){
    		return -1;
    	}
    	else{
    		return 1;
    	}
    }
}

public class ReverseAnswerLCSComparator implements Comparator<Answer> {
    @Override
    public int compare(Answer o1, Answer o2) {
    	return (int) (o2.getLCS() - o1.getLCS());
    }
}

public class AnswerDistanceComparator implements Comparator<Answer> {
    @Override
    public int compare(Answer o1, Answer o2) {
    	double one = o1.getDistance();
    	double two = o2.getDistance();
    	if (one < two){
    		return 1;
    	}
    	else if (one > two){
    		return -1;
    	}
    	else{
    		return 0;
    	}
    }
}
}
