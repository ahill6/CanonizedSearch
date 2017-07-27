package gov.nasa.jpf.symbc.repairproject;

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
	
	
	public void someMethodHere(String[] testStrings, String preface){
	int giveTopN = 5;
	DBInteractions db = new DBInteractions();
	
	for (String t : testStrings){
		System.out.println(t);
		System.out.println("/////////////////////////////////////////////");
		longestCommonSubsequenceRunner(preface+"." + t, giveTopN);
		longestCommonSubstringRunner(preface+"." + t, giveTopN);
		levenshteinDistanceRunner(preface+"." + t, giveTopN);
		db.nNearest(t, giveTopN, preface);
		System.out.println("/////////////////////////////////////////////");
		//z3NaiveRunner(t);
		//z3ComplicatedRunner(t);

	// linear search through entire database to find the best match.  Have the buggy program PCs, one-by-one load the 
	// potential fix PCs and compare...somehow...with Z3 (start with testing for equivalence, then do something not so demanding)
	}
}
	
	
	public void someMethodHere(String[] testStrings, int giveTopN){
	for (String t : testStrings){
		//System.out.println(t);
		System.out.println("/////////////////////////////////////////////");
		longestCommonSubsequenceRunner("TestPaths." + t, giveTopN);
		longestCommonSubstringRunner("TestPaths." + t, giveTopN);
		levenshteinDistanceRunner("TestPaths." + t, giveTopN);
		System.out.println("/////////////////////////////////////////////");
		//z3NaiveRunner(t);
		//z3ComplicatedRunner(t);

	// linear search through entire database to find the best match.  Have the buggy program PCs, one-by-one load the 
	// potential fix PCs and compare...somehow...with Z3 (start with testing for equivalence, then do something not so demanding)
	}
}

public void longestCommonSubsequenceRunner(String a, int topN){
	// get a list of all methods which are not a
	List<String> myConstraints = fetchConstraints(a);
	//System.out.println(myConstraints.size());
	List<String> others = new ArrayList<String>();
	List<Answer> minResults = new ArrayList<Answer>();
	/*
	List<Answer> maxResults = new ArrayList<Answer>();
	List<Answer> avgResults = new ArrayList<Answer>();
	List<Answer> sumResults = new ArrayList<Answer>();
	*/
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

public void longestCommonSubstringRunner(String a, int topN){		
	List<String> myConstraints = fetchConstraints(a);
	List<String> others = new ArrayList<String>();
	List<Answer> minResults = new ArrayList<Answer>();
	List<Integer> bests = new ArrayList<Integer>();
	List<Integer> tmpResults = new ArrayList<Integer>();
	List<Integer> intermediateResults = new ArrayList<Integer>();
	others = fetchOtherMethodNames(a);
	
	for (String o : others){
		List<String> constraints = fetchConstraints(o);
		intermediateResults.clear();
		bests.clear();
		for (String c : constraints){
			tmpResults.clear();
			for (int i = 0; i < myConstraints.size(); i++){
				tmpResults.add(longestCommonSubstring(c,myConstraints.get(i)));
				// somewhere here put a penalty term for using the same constraint to match everything.
			}
			int best = 0;
			for (int i = 1; i < tmpResults.size(); i++){
				if (tmpResults.get(i) > tmpResults.get(best)){
					best = i;
				}
			}
			bests.add(best);
			intermediateResults.add(tmpResults.get(best));	
		}
		int minResult = intermediateResults.get(0);
		
		for (int ir : intermediateResults){
			int tmp = minResult;
			minResult = Math.min(tmp, ir);
		}
		// Quick run showed Min performing best
		
		int penalty = bests.size() - new HashSet(bests).size();
		Answer ansMin = new Answer(o);
		ansMin.setLCS(minResult);
		minResults.add(ansMin);
	}

	Collections.sort(minResults, new AnswerLCSComparator());
	Collections.reverse(minResults);
	
	System.out.println("LCSubstring");
	System.out.println("++++++++++++++++++++++++++++++++++++++++++++++");
	for (int i = 0; i < topN; i++){
		Answer tmp = minResults.get(i);
		System.out.println(tmp.getName() + " " + tmp.getLCS());
	}
	System.out.println("++++++++++++++++++++++++++++++++++++++++++++++");
	
}

public void levenshteinDistanceRunner(String a, int topN){
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
		for (String c : constraints){
			tmpResults.clear();
			for (int i = 0; i < myConstraints.size(); i++){
				double tmp = levenshteinDistance(c, myConstraints.get(i));
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
		
		int penalty = bests.size() - new HashSet(bests).size();
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

private List<String> fetchOtherMethodNames(String name){		
	List<String> results = new ArrayList<String>();
	
	try{
		Class.forName("com.mysql.jdbc.Driver");  
		
		Connection con = DriverManager.getConnection(db,"root",password); 
		
		// for each unit test, make a unit test tied to this set id
		PreparedStatement ps = con.prepareStatement("select txt from searchrepair.method_text_tmp where txt!=?");
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
		
		Connection con = DriverManager.getConnection(db,"root",password); 
		
		// for each unit test, make a unit test tied to this set id
		//System.out.println(name);
		PreparedStatement ps = con.prepareStatement("select pc from searchrepair.method_text_tmp inner join searchrepair.constraints_tmp using(mid) where txt=?");
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

public int longestCommonSubsequence(String a, String b, int topN){
	/* Dynamic Programming Java implementation of LCS problem
	 * This method is due to Saket Kumar and from
	 * http://www.geeksforgeeks.org/dynamic-programming-set-4-longest-common-subsequence */
		
		char[] x = a.toCharArray();
		char[] y = b.toCharArray();
		int m = x.length;
		int n = y.length;
		int L[][] = new int[m + 1][n + 1];
		
		for (int i=0; i<m; i++){
			for (int j=0; j<n; j++){
				if (i ==0 || j == 0){
					L[i][j] = 0;
				}
				else if (x[i-1] == y[i-1]){
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

public int longestCommonSubstring(String a, String b, int topN){
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

public double levenshteinDistance(String a, String b){
/* Based on the matrix implementation pseudocode in wikipedia article
 * on Levenshtein Distance (edit distance).
 */
	char[] s = a.toCharArray();
	char[] t = b.toCharArray();
	int substitutionCost = 0;
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
				substitutionCost = 1;
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

public double levenshteinDistance(String a, String b, int topN){
	/* Based on the matrix implementation pseudocode in wikipedia article
	 * on Levenshtein Distance (edit distance).
	 */
		char[] s = a.toCharArray();
		char[] t = b.toCharArray();
		int substitutionCost = 0;
		// throughout, dist[i][j] holds the Levenshtein Distance between the first
		// i characters of a and the first j characters of b.
		int[][] dist = new int[s.length+1][t.length+1];

		for (int j = 0; j < t.length; j++){
			for (int i = 0; i < s.length; i++){
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
				if (s[i] == t[j]){
					substitutionCost = 0;
				}
				else{
					substitutionCost = 1;
				}
				
				// checking deletion, insertion, substitution respectively
				dist[i][j] = Math.min(Math.min(dist[i-1][j] + 1, dist[i][j-1] + 1), dist[i-1][j-1] + substitutionCost);
				
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

public double somethingMoreSophisticatedZ3(BoolExpr a, BoolExpr b){
	// basically write a new thing to find max number of compatible constraints????
	return 0.0;
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
