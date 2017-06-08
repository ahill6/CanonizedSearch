package gov.nasa.jpf.symbc.andy;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.PropertyListenerAdapter;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.VM;
import gov.nasa.jpf.vm.KernelState;
import gov.nasa.jpf.vm.SystemState;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.jvm.bytecode.IfInstruction;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.choice.ThreadChoiceFromSet;
import gov.nasa.jpf.report.ConsolePublisher;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.symbc.SymbolicInstructionFactory;
import gov.nasa.jpf.symbc.concolic.PCAnalyzer;
import gov.nasa.jpf.symbc.numeric.BinaryLinearIntegerExpression;
import gov.nasa.jpf.symbc.numeric.BinaryNonLinearIntegerExpression;
import gov.nasa.jpf.symbc.numeric.BinaryRealExpression;
import gov.nasa.jpf.symbc.numeric.Comparator;
import gov.nasa.jpf.symbc.numeric.Constraint;
import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.IntegerConstant;
import gov.nasa.jpf.symbc.numeric.IntegerExpression;
import gov.nasa.jpf.symbc.numeric.MinMax;
import gov.nasa.jpf.symbc.numeric.Operator;
import gov.nasa.jpf.symbc.numeric.PCChoiceGenerator;
import gov.nasa.jpf.symbc.numeric.PathCondition;
import gov.nasa.jpf.symbc.numeric.RealConstant;
import gov.nasa.jpf.symbc.numeric.RealConstraint;
import gov.nasa.jpf.symbc.numeric.RealExpression;
import gov.nasa.jpf.symbc.numeric.SymbolicConstraintsGeneral;
import gov.nasa.jpf.symbc.numeric.SymbolicInteger;
import gov.nasa.jpf.symbc.numeric.SymbolicReal;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.stream.Collectors;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.Model;
import com.microsoft.z3.RealExpr;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;
import com.microsoft.z3.Z3Exception;

public class CodeCharacterizationListener extends PropertyListenerAdapter {
	// helper classes to provide organization for expressions which does not already exist in JPF/SPF
	// TODO - verify not needed and remove
	private class NormalForm{
		private List<Term> left;
		private Comparator op;
		private double constant;
		private NormalForm(Comparator o, Term...terms){
			for (Term t : terms){
				left.add(t);
			}
			op = o;
		}
		
		private List<Term> getLeft(){
			return left;
		}
		
		private Comparator getOp(){
			return op;
		}
		
	}
	
	private class IntegerTerm{
		IntegerExpression coefficient;
		IntegerExpression base;
		IntegerExpression exponent;
		
		private IntegerTerm(IntegerExpression c, IntegerExpression b, IntegerExpression e){
			coefficient = c;
			base = b;
			exponent = e;
		}
	}
	
	private class RealTerm{
		RealExpression coefficient;
		RealExpression base;
		RealExpression exponent;
		private RealTerm(RealExpression c, RealExpression b, RealExpression e){
			coefficient = c;
			base = b;
			exponent = e;
		}
	}
	
	private class Term{
		Expression coefficient;
		Expression base;
		Expression exponent;
		boolean constant;
		private Term(Expression c, Expression b, Expression e, boolean con){
			coefficient = c;
			base = b;
			exponent = e;
			constant = con;
		}
	}
	
	// Class variables
	Config confFile;
	
	LinkedList<String> s;

	String outfile = "/Users/ahill6/Documents/workspace/tests/";
	String infile = "/Users/ahill6/Documents/workspace/tests/";
	String hashValue;
	String target;

	Set<Vector> methodSequences = new LinkedHashSet<Vector>(); // TODO - verify not needed and remove

	HashSet<String> varDeclarations = new HashSet<String>();
	HashMap<String, String> typeMap = new HashMap<String, String>();
	
	HashMap<Integer, List<BoolExpr>> pathConstraints = new HashMap<Integer, List<BoolExpr>>();
	List<BoolExpr> pathConstraintsAll = new ArrayList<BoolExpr>();
	
	Context ctx;
	
	boolean methodMade = false; // TODO - sloppy.  fix.
	int unitTestSetId = -1;
	int unitTestId = -1;
	
	public LinkedList<String> filesWritten = new LinkedList<String>();


	public CodeCharacterizationListener(Config conf, JPF jpf, String outfile) {
		jpf.addPublisherExtension(ConsolePublisher.class, this);
		confFile = conf;
		this.outfile = outfile;
		//target = confFile.getTarget();
		Object meth = confFile.get("symbolic.method");
		hashValue = String.valueOf(meth);
		//hashValue = String.valueOf(target.hashCode());
		HashMap<String, String> cfg = new HashMap<String, String>();
		cfg.put("model", "true");
		ctx = new Context(cfg); // Can also make this with no parameters
	}

	public CodeCharacterizationListener(Config conf, JPF jpf) {
		jpf.addPublisherExtension(ConsolePublisher.class, this);
		confFile = conf;
		//target = confFile.getTarget();
		Object meth = confFile.get("symbolic.method");
		hashValue = String.valueOf(meth);
		//hashValue = String.valueOf(target.hashCode());
		HashMap<String, String> cfg = new HashMap<String, String>();
		cfg.put("model", "true");
		ctx = new Context(cfg); // Can also make this with no parameters
	}

	/*
	// This extends the original push functionality in the default listener.  Keep for future use
	private void pushS(Object... args)
	{
		String toPush = "";
		for (int i=0; i<args.length; i++)
		{
			toPush += args[i].toString();
		}
		if (toPush != "")
				{
					s.push(toPush);
					//System.out.println("Pushing" + toPush);
				}		
	}
	 */
	
	// TODO - verify not needed and remove
	private void compareExpr(Expr one, Expr two){
		String string1 = "TestPaths.arrayMedian(sym#sym#sym)-unittests.tmp";
		String string2 = "TestPaths.arrayMedian(sym#sym#sym)-final.tmp";
		String string3 = "TestPaths.arrayMedian(sym#sym#sym)-declarations.tmp";
		BoolExpr a = ctx.parseSMTLIB2File(infile+string3, null, null, null, null);
		System.out.println(a);

		System.out.println("Compare expr");
		System.out.println(one.compareTo(two));
		System.exit(0);
	}
	
	// learning how to import an SMT-LIB file and use it.
	private void importModel(){
		String string1 = "TestPaths.arrayMedian(sym#sym#sym)-unittests.tmp";
		String string2 = "TestPaths.arrayMedian(sym#sym#sym)-final.tmp";
		String string3 = "TestPaths.arrayMedian(sym#sym#sym)-declarations.tmp";
		String testString = "test.txt";
		System.out.println("---------------------------------------------------");
		BoolExpr a = null;
		System.out.println("---------------------------------------------------");
		try{
			a = ctx.parseSMTLIB2File(infile+testString, null, null, null, null);
		}
		catch(JPFException e){
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		System.out.println(a);
		System.out.println(a.getArgs().length);
		System.out.println(a.getSort());
		System.out.println(a.getSExpr());
		System.out.println(a.getNumArgs());
		System.out.println(a.getFuncDecl());
		for(Expr b : a.getArgs()){
			System.out.println(b);
		}
	}
	
	// helper method to get a list of variables in constraints for renaming
	private List<Expr> listVars(Expr[] expr){
		Set<Expr> vars = new HashSet<Expr>();
		
		for (Expr e : expr){
			for (Expr f : e.getArgs()){
				if (f.isConst()){
					vars.add(f);
				}
				else{
					Expr[] tmp = {f};
					List<Expr> tmp2 = listVars(tmp);
					vars.addAll(tmp2);
				}
			}
		}
		return new ArrayList<Expr>(vars);
	}
	
	// helper method as above without using sets to not lose lexigraphic ordering
	private List<Expr> listVarsKeepOrder(Expr[] expr){
		List<Expr> vars = new ArrayList<Expr>();
		
		for (Expr e : expr){
			for (Expr f : e.getArgs()){
				if (f.isConst() && !vars.contains(f)){
					vars.add(f);
				}
				else{
					Expr[] tmp = {f};
					List<Expr> tmp2 = listVars(tmp);
					for (Expr g : tmp2){
						if (! vars.contains(g)){
							vars.add(g);
						}
					}
				}
			}
		}
		return vars;
	}
	
	
	private HashMap<String, ArrayList<Expr>> listVarsMap(Expr[] expr){
		HashMap<String, HashSet<Expr>> vars = new HashMap<String, HashSet<Expr>>();
		HashMap<String, ArrayList<Expr>> finVars = new HashMap<String, ArrayList<Expr>>();
		
		for (Expr e : expr){
			System.out.println(e.getSExpr());
			for (Expr f : e.getArgs()){
				if (f.isConst()){
					System.out.println(f.getSExpr());
					System.out.println(f.getSort());
					// find kind via method call
					String type = getVarTypeString(f);
					System.out.println(type);
					HashSet<Expr> tmp = vars.get(type);
					tmp.add(f);
					vars.put(type, tmp);
					System.out.println("done");
				}
				else{
					Expr[] tmp = {f};
					List<Expr> tmp2 = listVars(tmp);
					System.out.println("Else");
					for (Expr g : tmp2)
					{
						System.out.println(g.getSExpr());
						String type = getVarTypeString(g);
						System.out.println(type);
						HashSet<Expr> tmp3 = vars.get(type);
						System.out.println(tmp3);
						if (tmp3 != null){
							tmp3.add(g);
						}
						else{
							tmp3 = new HashSet<Expr>();
							tmp3.add(g);
						}
						System.out.println(tmp3);
						vars.put(type, tmp3);
						System.out.println("done done");
					}
				}
			}
		}
		//System.out.println("HERE SUCKA!");
		for (String h : vars.keySet()){
			//System.out.println(h);
			ArrayList<Expr> tmp = new ArrayList<Expr>();
			//System.out.println(tmp);
			tmp.addAll(vars.get(h));
			//System.out.println(tmp);
			finVars.put(h, tmp);
			//System.out.println("next");
		}
		return finVars;
	}
	
	private String getVarTypeString(Expr e){
		try{
			return e.getSort().toString();
		}
		catch(Exception ex){
			System.out.println(ex.toString());
			System.exit(0);
		}
		// need to use the isInt, isIntNum, etc. methods?
		return null;
	}
	
	private void write(HashMap<Integer, List<BoolExpr>> pcs) {
		// TODO - verify not needed and delete
		System.out.println("write hash map");
		String file = outfile + String.valueOf(hashValue) + "-final.tmp";

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file, false));
			for (Integer k : pathConstraints.keySet()){
				bw.append("Constraint # " + k + " : " + pathConstraints.get(k).size() + "\n");
				for (BoolExpr g : pathConstraints.get(k)){
					bw.append(g + "\n");
				}
				bw.append("\n");
			}
			filesWritten.add(file);
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void write(List<BoolExpr> pcs) {
		// print Path Constraints to file
		System.out.println("write");
		
		String finalFile = outfile + String.valueOf(hashValue) + "-final.tmp";
		String unitTestsFile = outfile + String.valueOf(hashValue) + "-unittests.tmp";
		String declarationsFile = outfile + String.valueOf(hashValue) + "-declarations.tmp";

		try {
			
			BufferedWriter bw = new BufferedWriter(new FileWriter(finalFile, false));
			bw.append(pcs.size() + "\n");
			for (BoolExpr g : pcs){
				bw.append(g + "\n");
			}
			bw.append("\n");
			filesWritten.add(finalFile);
			bw.close();
			
			BufferedWriter bw2 = new BufferedWriter(new FileWriter(unitTestsFile, false));
			BufferedWriter bw3 = new BufferedWriter(new FileWriter(declarationsFile, false));

			for (BoolExpr g : pcs)
			{
				Solver s = ctx.mkSolver();
				s.add(g);
				if(s.check() == Status.SATISFIABLE){
					Model m = s.getModel();
					
					/*
					for (BoolExpr be : s.getAssertions()){
						System.out.println(be);
					}
					System.out.println(m.toString());
					*/
					List<UnitTest> uTests = new ArrayList<UnitTest>();
					List<Expr> vars = listVars(s.getAssertions());
					HashMap<String, ArrayList<Expr>> varsUnit = listVarsMap(s.getAssertions());
					List<String> tmp = new ArrayList<String>();
					for (Expr v : vars){
						tmp.add(v.getSExpr() + "="+m.eval(v, false));
					}
					for (String ky : varsUnit.keySet()){
						List<String> unitsTmp = new ArrayList<String>();
						bw2.append(ky+"-");
						for (Expr val : varsUnit.get(ky)){
							unitsTmp.add(val.getSExpr());
							uTests.add(new UnitTest(val.getSExpr(), m.eval(val, false).toString(), ky));
						}
						bw2.append(unitsTmp.stream().collect(Collectors.joining(","))+";");
					}
					bw2.append("////");
					bw2.append(tmp.stream().collect(Collectors.joining(","))+"\n");
					bw3.append(m.toString()+"\n");
					
					// TODO - putting the uTest stuff here makes sense from a flow and scope perspective, but not from separation of concerns.
					// change?
					
					// Yup, almost certainly need to split this stuff up.
					// TODO - need to make this the actual text (do I?  not just a pointer to URL?), not just file name
					//addMethodToDatabase(hashValue, uTests);
					writeUnitTestsToDB(uTests);
				}
			}
			filesWritten.add(unitTestsFile);
			bw2.close();
			bw3.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private void write(String k, boolean append){
		String path = "";
		for (String var : varDeclarations) {
			//System.out.println(var);
			path += var + "\n";
		}
		k += "\n";

		//String file = outfile + String.valueOf(hashValue) + "path-" + pathcount + ".tmp";
		String file = outfile + String.valueOf(hashValue) + ".tmp";

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file, append));
			bw.append(k);
			//System.out.println("File: " + file);
			filesWritten.add(file);
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		pathCount++;
	}

	private void write(String k){
		boolean append = false;
		String path = "";
		for (String var : varDeclarations) {
			//System.out.println(var);
			path += var + "\n";
		}

		String file = outfile + String.valueOf(hashValue) + "path-" + pathcount + ".tmp";
		//String file = outfile + target + "-path-" + pathcount + ".tmp";

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file, append));
			bw.append(k);
			//System.out.println("File: " + file);
			filesWritten.add(file);
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		pathCount++;
	}
	int pathcount = 0;
	
	public void searchFinished(Search search) {
		super.searchFinished(search);
		
		// send these results to a file
		//write(pathConstraints);

		List<BoolExpr> pathConstraintsAllTmp = rename(green(pathConstraintsAll));
		
		pathConstraintsAll = pathConstraintsAllTmp;
		write(pathConstraintsAll);
		//importModel();
	}
	
	public void searchStarted(Search search){
		super.searchStarted(search);
		unitTestSetId = -1;
		String tmp = confFile.get("symbolic.method").toString().replace(target+".", "");
		String methodName = tmp.split("[(]")[0];
		unitTestSetId = addMethod(methodName);
		if (unitTestSetId ==-1){
			System.out.println("Database Method Creation Problem for " + methodName);
		}
	}


	public void stateAdvanced(Search search) {
		super.stateAdvanced(search);
		/*
		 * All this executes at the end of the symbolic execution
		 */
		if (search.isEndState()) {
			VM vm = search.getVM();
			SystemState ss = vm.getSystemState();		
			KernelState ks = vm.getKernelState();
			ThreadInfo ti = vm.getCurrentThread();
			ChoiceGenerator<?> cg = vm.getChoiceGenerator();
			int next;

			if (!(cg instanceof PCChoiceGenerator)) {
				ChoiceGenerator<?> prev_cg = cg.getPreviousChoiceGenerator();
				while (!((prev_cg == null) || (prev_cg instanceof PCChoiceGenerator))) {
					prev_cg = prev_cg.getPreviousChoiceGenerator();
				}
				cg = prev_cg;
			}

			if (cg instanceof ThreadChoiceFromSet) {
				ThreadChoiceFromSet tcfs = (ThreadChoiceFromSet) cg;
				ChoiceGenerator cg2 = tcfs.getPreviousChoiceGenerator();
			}

			// if this is a multi-path method. For each path...
			if ((cg instanceof PCChoiceGenerator)
					&& ((PCChoiceGenerator) cg).getCurrentPC() != null) {
				PathCondition pc = ((PCChoiceGenerator) cg).getCurrentPC();
				//write(pc.toString());
				//write(pc.toString(), true);
				
				
				List<BoolExpr> tmp1 = makeConstraints(pc);
				System.out.println("Path Constraints");
				System.out.println(tmp1);
				if (tmp1 != null){
					pathConstraintsAll.addAll(tmp1);
					pathConstraintsAll = minimalConstraintSet(pathConstraintsAll);
				}

				// solve the path condition

				if (SymbolicInstructionFactory.concolicMode) { // TODO: cleaner
					SymbolicConstraintsGeneral solver = new SymbolicConstraintsGeneral();
					PCAnalyzer pa = new PCAnalyzer();
					pa.solve(pc, solver);
				} else
					pc.solve();

				ChoiceGenerator<?>[] cgs = ss.getChoiceGenerators();

				// Get the instructions for the PCs
				Vector<Instruction> instructionSequence = getInstructionSequence(cgs);

				// SSA for this path
				HashMap<String, String> ssa = new HashMap<String, String>();

				// used to generate the variable names in SSA format
				HashMap<String, Integer> incrementCounts = new HashMap<String, Integer>();

				pathcount++;
				System.out.println("\n\n\n\n");
				System.out.println("**********  " + pathcount + "  **********");
				// if the path has a condition
				if (instructionSequence.size() > 0) {

					// get the instructions for this scope of the path
					Instruction[] code = instructionSequence.get(0)
							.getMethodInfo().getInstructions();

					boolean done = false;

					s = new LinkedList<String>();
					varDeclarations = new HashSet<String>();

					//done = printBlock(code, 0, ssa, incrementCounts, ss, ti, ks);

					//printPath();

				}
			} else {
				// no path condition!
				System.out
				.println("Single-path Prorgram - How to capture me???");
				singlePath = true;
			}
		}
	}
	
	public List<BoolExpr> minimalConstraintSet(List<BoolExpr> list){
		// This combines the constraints to simplify/remove ones which are duplicates, keeping only logically incompatible constraints
		List<BoolExpr> singleConstraintList = new ArrayList<BoolExpr>();
		singleConstraintList.add(list.remove(0));
		
		try{
			//System.out.println("list to be minimized");
			for (BoolExpr b : list){
				boolean fail = true;
				BoolExpr outerTmp = null;
				for (Iterator<BoolExpr> iterator = singleConstraintList.iterator(); iterator.hasNext();) {
					BoolExpr c = iterator.next();
					Solver s = ctx.mkSolver();
					BoolExpr tmp = ctx.mkAnd(b,c);
					s.add(tmp);
					if (s.check() == Status.SATISFIABLE){
						iterator.remove();
						fail = false;
						if ((outerTmp == null) || (outerTmp != null && tmp.getNumArgs() < outerTmp.getNumArgs())){
							outerTmp = (BoolExpr)tmp.simplify();
							continue;
						}
					}
					else{
						//System.out.println("THIS IS WHAT FAILURE LOOKS LIKE");
						//System.out.println(b);
						//System.out.println(c);
					}
				}
				if (fail){
					singleConstraintList.add(b);
				}
				if (outerTmp != null){
					singleConstraintList.add(outerTmp);
				}
			}
		}
		catch(Exception e){
			System.out.println("minimal constraint set");
			System.out.println(list);
			System.out.println(e);
			throw e;
		}
		
		return singleConstraintList;
		
	}
	private BoolExpr solveToZero(Expr t){
		// Helper for canonization.  Takes an expression and puts in form EXPR OP 0 for OP in {=, !=, <=}
		System.out.println("solve to zero");
		// can only be = or <= (all binary operations) <-- != is handled by only passing = and negating after call
		// *** It's really starting to look like this would be easier before putting in Z3...but would that slow everything down significantly?
		Expr[] sides = t.getArgs();
		
		// NEED TO ORDER THE VARIABLES IN LEXOGRAPHIC ORDER...UGHS...
		// Also, with all the negation stuff, make sure that you aren't removing the normalization (a*x... <= 0)
		if (t.isNot()){
			Expr ugh = ctx.mkNot((BoolExpr)t).simplify();
			sides = ugh.getArgs();
			
			if (ugh.isLE()){
				return solveToZero(ctx.mkLt((ArithExpr)sides[1], (ArithExpr)sides[0]));
			}
			else if (ugh.isLT()){
				return solveToZero(ctx.mkLe((ArithExpr)sides[1], (ArithExpr)sides[0]));
			}
			else if (ugh.isGE()){
				return solveToZero(ctx.mkLt((ArithExpr)sides[0], (ArithExpr)sides[1]));
			}
			else if (ugh.isGT()){
				return solveToZero(ctx.mkLe((ArithExpr)sides[0], (ArithExpr)sides[1]));
			}
			else if (ugh.isEq()){
				return solveToZero(ugh);
			}
			else{
				System.out.println("umm...something's wrong in solve to zero");
				System.out.println(ugh.getSExpr());
				System.exit(0);
			}
		}
		else{
			if (t.isEq()){
				return ctx.mkEq(ctx.mkSub((ArithExpr)sides[0], (ArithExpr)sides[1]), ctx.mkInt(0));
			}
			else if (t.isLT()){ 
				double addTo = -1;
				try {
					addTo = addaBit(t);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	 			boolean isInt = false;
				if ((int)addTo == addTo){
					isInt = true;
				}
				
				Expr tmp = sides[0];
				Expr adder = null;
				
				if (isInt){
					adder = ctx.mkInt((int)addTo);
				}
				else{
					adder = ctx.mkReal((long)addTo);
				}
				if (addTo > 0){
					sides[0] = ctx.mkAdd((ArithExpr)tmp,(ArithExpr)adder);
				}
				
				return ctx.mkLt(ctx.mkSub((ArithExpr)sides[0], (ArithExpr)sides[1]), ctx.mkInt(0));
			}
			else if (t.isLE()){
				return ctx.mkLe(ctx.mkSub((ArithExpr)sides[0], (ArithExpr)sides[1]), ctx.mkInt(0));
			}
			else{
				System.out.println("unexpected type in Solve to Zero");
				System.out.println(t.getSExpr());
				System.exit(1);
				// TODO - Change this to an exception throw
			}
		}
		return (BoolExpr) t;
	}
	
	private BoolExpr flip(Expr t) throws Exception{
		// turns > and >= into < or <=
		System.out.println("flip");
		// HAVE >= (- a b) (+ 45 c)    a - b >= 45 + c
		// WANT <= (+ 45 c) (- a b)  45 + c <= a - b
		BoolExpr dPrime = null;
		Expr[] args = t.getArgs();
		if (args.length > 2){
			System.out.println("We got us a problem here.  This-here is an inequality with too many arguments!");
			System.out.println(t.getSExpr());
			System.exit(1); // once this is working, just throw an exception
		}
		if (t.isGT()){
			dPrime = ctx.mkLt((ArithExpr)args[1], (ArithExpr)args[0]);
		}
		else if (t.isGE()){
			dPrime = ctx.mkLe((ArithExpr)args[1], (ArithExpr)args[0]);
		}
		else{
			System.out.println("Here there be dragons");
			System.out.println(t.getSExpr());
			System.exit(1);
		}
		if (dPrime != null){
			return dPrime;
		}
		else{
			throw new Exception("dPrime is null.");
		}
	}
	
	private double addaBit(Expr t) throws Exception{
		// turns < into <= by adding a minimum amount
		System.out.println("add a bit");
		double adder = -1;
		String s = t.getFuncDecl().getSExpr();
		if (s.contains("Int")){
			adder = 1;
		}
		if (s.contains("Real")){
			adder = Long.MIN_VALUE;
		}
		if (adder == -1){
			System.out.println(t.getSExpr());
			throw new Exception("add a bit got a constraint without ints or reals");
		}
		return adder;
	}
	
	public Expr normalize(Expr t) throws Exception{
		System.out.println("normalize");
		//BoolExpr retrn;
		if (t.isGE() || t.isGT()){
			//retrn = flip(t);
			return solveToZero(flip(t));
			// flip all, subtract
		}		
		else if (t.isNot() && t.getArgs().length == 1 && t.getArgs()[0].isEq())
		{

			// TODO - not 100% sure this is right, check
			return ctx.mkNot(solveToZero(t.getArgs()[0]));
		}
		else{ // This assumes the only alternatives are < ,  <=
			return solveToZero(t);
		}
		/*
		// Should these below ever occur?
		else if (t.isAnd()){
			
		}
		else if (t.isOr()){
			
		}
		*/
	}
	
	
	public List<BoolExpr> rename(List<BoolExpr> finalList){
		System.out.println("rename");

		TreeMap<String, BoolExpr> sortedMap = new TreeMap<String, BoolExpr>();
		int i = 0;
		
		for (BoolExpr b : finalList){
			sortedMap.put(b.getSExpr(), b);
		}
		
		List<BoolExpr> list = sortedMap.values().stream().collect(Collectors.toList());
		 
		List<Expr> vars = listVarsKeepOrder(list.toArray(new Expr[list.size()]));
				
		for (Expr a : vars){
			for (Map.Entry<String, BoolExpr> entry : sortedMap.entrySet()){
				try{
					if (a instanceof IntExpr){
						sortedMap.put(entry.getKey(), (BoolExpr) entry.getValue().substitute(a, ctx.mkIntConst("v"+i)));
					}
					else if (a instanceof RealExpr){
						sortedMap.put(entry.getKey(), (BoolExpr) entry.getValue().substitute(a, ctx.mkRealConst("v"+i)));
					}
					else{
						System.out.println("Need to add ");
						System.out.println(a.getClass());
					}
				}
				catch (Exception e){
					System.out.println(e);
					System.exit(0);
				}
			}
			i++;
		}
		
		return sortedMap.values().stream().collect(Collectors.toList());
	}
	
	public BoolExpr canonize(Expr t) throws Exception{
		/*
		 * Canonize by following this procedure:
		 * 1) lexicographically order statements
		 * 2) Convert to normal form (a*x + b*y + ... + c*z OP 0 for OP in {=, !=, <=}
		 * 	2a) Turn >, >= to <=, <
		 *  2b) If <, make <= by adding a bit (minimum positive amount)
		 * 3) Rename variables with generic v_i starting from left
		 * 
		 * OUTPUT: all constraints in normal form, all variables v_i for some i (e.g. v0, v1, v2)
		 */
		// rearrange singletons (and tuples?) so x < y and y > x will match (i.e. make all inequalities point right)
		System.out.println("canonize");
		Expr ret = null;

		// If the constraint is = or !=, only need to make normal form
		if (t.isEq() || (t.isNot() && t.getArgs().length == 1 && t.getArgs()[0].isEq())){
			try{
				ret = normalize(t);
			}
			catch(Exception e){
				System.out.println("ERROR-ERROR");
				System.out.println(e);
			}
		}
		else{
			try{
				// here need to order variables, etc. pre-heuristic
				ret = normalize(t);
			}
			catch(Exception e){
				System.out.println("ERROR-ERROR");
				System.out.println(e);
			}
		}
		return (BoolExpr) ret;
	}
	
	public List<BoolExpr> green(List<BoolExpr> rawConstraints){
	/* The purpose of this method is to implement the canonization procedure
	 * used in Visser, Geldenhuys, Dwyer "Green: Reducing, Resuing and Recycling 
	 * Constraints in Program Analysis
	 */
		System.out.println("green");
		List<BoolExpr> finalList = new ArrayList<BoolExpr>();
		
	// Canonize each constraint individually, then recombine
		for (BoolExpr r : rawConstraints){
			List<BoolExpr> intermediateList = new ArrayList<BoolExpr>();
			if (r.isAnd()){
				for (Expr t : r.getArgs()){
	
					// In a perfect world, add slicing here
					
					try {
						intermediateList.add(canonize(t));
					} catch (Exception e) {
						e.printStackTrace();
						System.exit(0);
					}
				}
			}
			else{
				System.out.println(r);
				try {
					intermediateList.add(canonize(r));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			finalList.add(ctx.mkAnd(intermediateList.toArray(new BoolExpr[intermediateList.size()])));
		}


		return finalList;
	}
	
	public List<BoolExpr> makeConstraints(PathCondition p){
		System.out.println("make constraints");

		List<BoolExpr> constraintList = new ArrayList<BoolExpr>();
		
		try{

			//ArithExpr a = oneClause("2*(INT_56 +1.3) - (c / d) % 2.4 ^ 1");
			

			// first one has to be done manually...?
			//String l = p.header.getLeft().toString().replaceAll("\\s", "");
			//String r = p.header.getRight().toString().replaceAll("\\s", "");
			//String c = p.header.getComparator().toString().replaceAll("\\s", "");
			//constraintList.add(addOneConstraint(l, r, c));
			//int x = 0;
			//Constraint next = p.header.getTail();
			Constraint next = p.header;

			while (next != null){
				constraintList.add(addOneConstraint(next));
				next = next.getTail();
			}
			
			// check if this is unsatisfiable
			Solver s = ctx.mkSolver();
			
			for (BoolExpr b : constraintList){
				//System.out.println(b);
				s.add(b);
			}

			if (s.check() != Status.SATISFIABLE){
				return null;
			}
			else{
				
				//return green(minimalConstraintSet(constraintList));
				return minimalConstraintSet(constraintList);
			}
			
		}
		catch (Exception e){
			System.out.println("ERROR Make constraints ERROR");
			System.out.println(p);
			System.out.println(constraintList.size());
			System.out.println(e);
			throw e;
		}
	}
	
	public Constraint makeNormalForm(Constraint co){
		// TODO - not needed?
		System.out.println("make normal form");
		int ints = 0;
		Expression l = co.getLeft();
		Comparator o = co.getComparator();
		Expression r = co.getRight();
		
		if (l instanceof SymbolicInteger){
			ints ++;
			String tmp = l.toString();
			l = new SymbolicReal(tmp);
		}
		if (r instanceof SymbolicInteger){
			ints ++;
			String tmp = r.toString();
			r = new SymbolicReal(tmp);
		}
		
		if (r instanceof SymbolicReal && l instanceof SymbolicReal){
			RealExpression tmp = ((RealExpression)l)._minus((RealExpression)r);
			if (o == Comparator.GE || o == Comparator.GT){
				RealExpression tmp2 = tmp._neg();
				tmp = tmp2;
				Comparator oTmp = o.not();
				o = oTmp;
			}
			if (o == Comparator.LT){
				if (ints == 2){
					RealExpression tmp2 = tmp._plus(1);
					tmp = tmp2;
				}
				else{
					double minVal = Math.min(MinMax.getVarMinDouble(l.toString()), MinMax.getVarMinDouble(r.toString()));
					RealExpression tmp2 = tmp._plus(minVal);
				}
				o = Comparator.LE;
			}
			
			return new RealConstraint(tmp, o, new RealConstant(0));
		}

		return co;
	}
	
	public Expr makeClause(Expression expression){
		/* 
		 * Takes the pieces of a constraint and makes them into Z3 variables of the appropriate type.
		 * All sorts of stylistic problems by Java standards in this.
		 */
		
		System.out.println("make clause");
		//SATCanonizerService sat = new SATCanonizerService(null);
		//System.out.println(sat.canonize(expression, new Map<>));
		
		if (expression instanceof SymbolicInteger){
			return ctx.mkIntConst(expression.toString().replaceAll("\\s", ""));
		}
		else if (expression instanceof SymbolicReal){
			return ctx.mkRealConst(expression.toString().replaceAll("\\s", ""));
		}
		else if (expression instanceof IntegerConstant){
			try{
				return ctx.mkInt(expression.toString().replaceAll("\\s", ""));
			}
			catch (Z3Exception e){
				return ctx.mkInt(expression.toString().replaceAll("[^\\d.-]",""));
			}
		}
		else if (expression instanceof RealConstant){
			try{
				return ctx.mkReal(expression.toString().replaceAll("\\s", ""));
			}
			catch (Z3Exception e){
				return ctx.mkReal(expression.toString().replaceAll("[^\\d.-]",""));
			}
		}
		else if (expression instanceof BinaryLinearIntegerExpression){
			return addOneClause(makeClause(((BinaryLinearIntegerExpression) expression).getLeft()),
					((BinaryLinearIntegerExpression) expression).getOp(),
					makeClause(((BinaryLinearIntegerExpression) expression).getRight()));
			}
		else if (expression instanceof BinaryNonLinearIntegerExpression){
			return addOneClause(makeClause(((BinaryNonLinearIntegerExpression) expression).left),
					((BinaryNonLinearIntegerExpression) expression).op,
					makeClause(((BinaryNonLinearIntegerExpression) expression).right));
		}
		else if (expression instanceof BinaryRealExpression){
			return addOneClause(makeClause(((BinaryNonLinearIntegerExpression) expression).left),
					((BinaryNonLinearIntegerExpression) expression).op,
					makeClause(((BinaryNonLinearIntegerExpression) expression).right));
		}
		else {
			System.out.println("Need to add something to makeClause.  Namely");
			System.out.println(expression.getClass());
		}
		return null;
	}
	
	
	public BoolExpr addOneConstraint(Constraint co)
	{
		System.out.println("add one constraint - constraint");
		String c = co.getComparator().toString().replaceAll("\\s", "");
		
		
		Expr l = makeClause(co.getLeft());
		Expr r = makeClause(co.getRight());
		System.out.println("clauses made");
		BoolExpr tmp = null;
		
		try{
			switch(c){
				case "=": 
					tmp = ctx.mkEq(l, r);
					break;
				case "!=": 
					tmp = ctx.mkNot(ctx.mkEq(l, r));
					break;
				case "<": 
					tmp = ctx.mkLt((ArithExpr)l, (ArithExpr)r);
					break;
				case ">":
					tmp = ctx.mkGt((ArithExpr)l, (ArithExpr)r);
					break;
				case "<=": 
					tmp = ctx.mkLe((ArithExpr)l, (ArithExpr)r);
					break;
				case ">=":  
					tmp = ctx.mkGe((ArithExpr)l, (ArithExpr)r);
					break;
				default:
					System.out.println("??? in add one constraint");
					System.out.println(co.toString());
					break;
			}
		}
		catch(Exception e){
			System.out.println("ERROR Add one constraint ERROR");
			System.out.println(co);
			System.out.println(e);
		}
		return tmp;
	}
	
	public BoolExpr addOnez3Constraint(Constraint co)
	{
		System.out.println("add one constraint - constraint");
		String c = co.getComparator().toString().replaceAll("\\s", "");

		Expr l = makeClause(co.getLeft());
		Expr r = makeClause(co.getRight());
		System.out.println("clauses made");
		BoolExpr tmp = null;

		try{
			switch(c){
				case "=": 
					tmp = ctx.mkEq(l, r);
					break;
				case "!=": 
					tmp = ctx.mkNot(ctx.mkEq(l, r));
					break;
				case "<": 
					tmp = ctx.mkLt((ArithExpr)l, (ArithExpr)r);
					break;
				case ">":
					tmp = ctx.mkGt((ArithExpr)l, (ArithExpr)r);
					break;
				case "<=": 
					tmp = ctx.mkLe((ArithExpr)l, (ArithExpr)r);
					break;
				case ">=":  
					tmp = ctx.mkGe((ArithExpr)l, (ArithExpr)r);
					break;
				default:
					System.out.println("??? invalid Z3 constraint");
					System.out.println(co.toString());
					break;
			}
		}
		catch(Exception e){
			System.out.println("ERROR Add one constraint ERROR");
			System.out.println(co);
			System.out.println(e);
		}
		return tmp;
	}
	
	public Expr addOneClause(Expr a, Operator o, Expr b){
		System.out.println("add one clause");
		String c = o.toString().replaceAll("\\s", "");

		switch(c){
			case "+":
				return ctx.mkAdd((ArithExpr)a, (ArithExpr)b);
			case "-":
				return ctx.mkSub((ArithExpr)a, (ArithExpr)b);
			case "*":
				return ctx.mkMul((ArithExpr)a, (ArithExpr)b);
			case "/":
				return ctx.mkDiv((ArithExpr)a, (ArithExpr)b);
			case "%":
				return ctx.mkMod((IntExpr)a, (IntExpr)b);
			case "^":
				return ctx.mkPower((ArithExpr)a, (ArithExpr)b);
			case "&&":
				return ctx.mkAnd((BoolExpr)a, (BoolExpr)b);
			case "||":
				return ctx.mkOr((BoolExpr)a, (BoolExpr)b);
			default:
				System.out.println("You need to add an operation to addOneClause.  Namely");
				System.out.println(o);
				System.exit(1);
				break;
			}
		return b;
	}

	public BoolExpr addOneConstraint(String l, String r, String c)
	{
		// TODO - still needed?  
		System.out.println("add one constraint - string");
		
		BoolExpr tmp = null;
		try{
			switch(c){
				case "=": 
					tmp = ctx.mkEq(ctx.mkIntConst(l), ctx.mkIntConst(r));
					break;
				case "!=": 
					tmp = ctx.mkNot(ctx.mkEq(ctx.mkIntConst(l), ctx.mkIntConst(r)));
					break;
				case "<": 
					tmp = ctx.mkLt(ctx.mkIntConst(l), ctx.mkIntConst(r));
					break;
				case ">":
					tmp = ctx.mkGt(ctx.mkIntConst(l), ctx.mkIntConst(r));
					break;
				case "<=": 
					tmp = ctx.mkLe(ctx.mkIntConst(l), ctx.mkIntConst(r));
					break;
				case ">=":  
					tmp = ctx.mkGe(ctx.mkIntConst(l), ctx.mkIntConst(r));
					break;
				default:
					System.out.println("???");
					System.out.println(l + ", " + r + ", " + c);
					break;
			}
		}
		catch(Exception e){
			System.out.println("Add one constraint");
			System.out.println(l+" " + c + " " + r);
			System.out.println(e);
		}
		return tmp;
	}
	
	public boolean singlePath = false;

	public static int pathCount = 0;

	private String incrementVar(String varName,
			HashMap<String, Integer> incrementCounts) {
		System.out.println("Increment var");
		int counts;
		if (incrementCounts.containsKey(varName)) {
			counts = incrementCounts.get(varName);
			counts++;
		} else {
			counts = 1;
		}
		incrementCounts.put(varName, counts);
		return varName + counts;
	}


	private Vector<Instruction> getInstructionSequence(ChoiceGenerator<?>[] cgs) {
		System.out.println("get instruction sequence");
		// A method sequence is a vector of strings
		Vector<Instruction> instructionSequence = new Vector<Instruction>();
		ChoiceGenerator<?> cg = null;
		// explore the choice generator chain - unique for a given path.
		for (int i = 0; i < cgs.length; i++) {
			cg = cgs[i];
			if ((cg instanceof PCChoiceGenerator)) {
				Instruction insn = ((PCChoiceGenerator) cg).getInsn();

				if (insn != null) {
					instructionSequence.add(insn); // these are the instructions
					// just for the PCs
				}
			}
		}
		return instructionSequence;
	}

	public void instructionExecuted(VM vm) {
		System.out.println("instruction executed");
		if (!vm.getSystemState().isIgnored()) {
			SystemState ss = vm.getSystemState();
			Instruction insn = vm.getInstruction();

			if (insn != null && insn.getSourceLocation() != null) {

				ChoiceGenerator<?> cg = ss.getChoiceGenerator();
				if (cg != null && cg instanceof PCChoiceGenerator) {
					if (insn instanceof IfInstruction) {
						((PCChoiceGenerator) cg).setInsn(insn);
					}
				} else {

				}
			}
		}
	}

	public void propertyViolated(Search search) {
		System.out.println("property violated");
		VM vm = search.getVM();
		SystemState ss = vm.getSystemState();
		ChoiceGenerator<?> cg = vm.getChoiceGenerator();

		if (!(cg instanceof PCChoiceGenerator)) {
			ChoiceGenerator<?> prev_cg = cg.getPreviousChoiceGenerator();
			while (!((prev_cg == null) || (prev_cg instanceof PCChoiceGenerator))) {
				prev_cg = prev_cg.getPreviousChoiceGenerator();
			}
			cg = prev_cg;
		}

		if ((cg instanceof PCChoiceGenerator)
				&& ((PCChoiceGenerator) cg).getCurrentPC() != null) {

			PathCondition pc = ((PCChoiceGenerator) cg).getCurrentPC();
			// solve the path condition
			if (SymbolicInstructionFactory.concolicMode) { // TODO: cleaner
				SymbolicConstraintsGeneral solver = new SymbolicConstraintsGeneral();
				PCAnalyzer pa = new PCAnalyzer();
				pa.solve(pc, solver);
			} else
				pc.solve();
		}
	}
	
	public class UnitTest{
		String name;
		String value;
		String type;
		public UnitTest(String n, String v, String t){
			name = n;
			value = v;
			type = t;
		}
		
		public String toString(){
			return type + " " + name + " = " + value;
		}
	}
	
	private void writeUnitTestsToDB(List<UnitTest> uts){

		try{
			Class.forName("com.mysql.jdbc.Driver");  
			
			Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/SearchRepair","root","S3@rchR3p@1r"); 
			
			// for each unit test, make a unit test tied to this set id
			CallableStatement cStmt2 = con.prepareCall("{? = call GET_UID()}");
			cStmt2.registerOutParameter(1, java.sql.Types.INTEGER);
			cStmt2.execute();
			int unitTestId = cStmt2.getInt(1);
			for (UnitTest u : uts)
			{
				// make a unit test
				PreparedStatement ps = con.prepareStatement("insert into SearchRepair.UNIT_TESTS (UTID, TYPE, VALUE) VALUES (?,?,?) ");
				ps.setInt(1, unitTestId);
				ps.setString(2, u.type);
				ps.setString(3, u.value);
				ps.execute();
			}
			
			// add an entry to the unit tests <--> unit test sets with unitTestSetId and unitTestId
			PreparedStatement ps2 = con.prepareStatement("insert into SearchRepair.UT_SETS (UTID, UT_SET_ID) VALUES (?,?)");
			ps2.setInt(1, unitTestId);
			ps2.setInt(2, unitTestSetId);
			ps2.execute();
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private int addMethod(String text) {
		int uts = -1;
		try {
			// Connect to DB (ideally don't hard-code the password or connection string)
			Class.forName("com.mysql.jdbc.Driver");
			Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/SearchRepair","root","S3@rchR3p@1r"); 

			// New entry for METHOD_TEXT
			CallableStatement cStmt = con.prepareCall("{? = call ADD_METHOD_TMP(?)}");
			cStmt.registerOutParameter(1,java.sql.Types.INTEGER);
			cStmt.setObject("TXT", text);
			cStmt.execute();

			// Get the Unit Test Set ID as return value
			uts = cStmt.getInt(1);

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return uts;
	}
	
	public boolean addMethodToDatabase(String text, List<UnitTest> u){
		System.out.println(text);
		System.out.println(u);
		System.out.println("Stopping before adding method to database");
		System.exit(1);
		// open the files that contain the unit test data
		String path = "/Users/ahill6/Documents/workspace/tests/";
		//System.out.println(System.getProperty("user.dir"));

		File folder = new File (path);
		File[] unitTests = folder.listFiles(new FilenameFilter() {
		    @Override
		    public boolean accept(File dir, String name) {
		        return name.endsWith("-unittests.tmp");
		    }
		});
		File textFolder = new File(path+"/methods/");
		File[] methodText = textFolder.listFiles(new FilenameFilter() {
		    @Override
		    public boolean accept(File dir, String name) {
		        return name.endsWith(".java");
		    }
		});
		
		try{  
			/*
			Connection con = DriverManager.getConnection
					("jdbc:mysql://localhost/?user=root&password=rootpassword"); 
					Statement s=con.createStatement();
					int Result=s.executeUpdate("CREATE DATABASE test");
					*/
			Class.forName("com.mysql.jdbc.Driver");  
			
			Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306","root","S3@rchR3p@1r"); 
			
			// parse the files
			for (File f : methodText)
			{
				String texxt = null;
				
				// open file, read in data
				try{
					FileInputStream fis = new FileInputStream(f);
					byte[] data = new byte[(int) f.length()];
					fis.read(data);
					fis.close();
					text = new String(data, "UTF-8");
					}
				catch (IOException e){
					System.out.println("Problem with " + f.getName());
					continue;
				}
				
				// New entry for METHOD_TEXT
				CallableStatement cStmt = con.prepareCall("{? = call ADD_METHOD(?)}");
				cStmt.registerOutParameter(1,java.sql.Types.INTEGER);
				cStmt.setObject("txt", text);
				cStmt.execute();
				// Get the Unit Test Set ID as return value
				int uts = cStmt.getInt(1);
				
				// read in unit tests
				// open file, read in data
				try{
					FileInputStream fis = new FileInputStream(f);
					byte[] data = new byte[(int) f.length()];
					fis.read(data);
					fis.close();
					text = new String(data, "UTF-8");
					}
				catch (IOException e){
					System.out.println("Problem with " + f.getName());
					continue;
				}
				String[] cin = texxt.split(";");
				for (String s : cin){
					//parse s
					String[] worker = s.split("[;]");
					for (String w : worker){
						String[] tmp = w.split("-");
						String value = tmp[2];
						String name = tmp[1];
						String type = tmp[0];
					}
					
					String type = null;
					String value = null;
					String name;
					//Real-REAL_228;Int-c,a,b;////c=0,a=0,b=0,REAL_228=0
		
					// for each unit test, make a unit test tied to this set id
					CallableStatement cStmt2 = con.prepareCall("{? = call GET_UID()}");
					cStmt2.registerOutParameter(1, java.sql.Types.INTEGER);
					cStmt2.execute();
					int unitTestId = cStmt.getInt(1);
					
					// make a unit test
					PreparedStatement ps = con.prepareStatement("insert into SearchRepair.UNIT_TESTS (UTID, TYPE, VALUE) VALUES (?,?,?) ");
					ps.setInt(1, unitTestId);
					ps.setString(2, type);
					ps.setString(3, value);
					ps.execute();
					
					// add an entry to the unit tests <--> unit test sets with unitTestSetId and unitTestId
					PreparedStatement ps2 = con.prepareStatement("insert into SearchRepair.UT_SETS (UTID, UT_SET_ID) VALUES (?,?)");
					ps2.setInt(1, unitTestId);
					ps2.setInt(2, unitTestSetId);
					ps2.execute();
				}
			}
			
			//for that method id, add an entry to the set of unit tests table
				// ** somehow get back that new unit test set ID
			
			// add all the individual pieces (e.g. REAL_19=0; A=0;B=0;C=-1) to the unit tests table
				// ** somehow get back those unit test IDs...
			
			// add those unit test IDs to the unit test--> unit test set table
			
			// TODO - is that everything?
						
			// save database (all input one transaction(?))
			
			// NEXT!
			
			// put it in a database
			
			// save the database 
		 
			//here sonoo is database name, root is username and password  
			Statement stmt=con.createStatement();  
			//int result = stmt.executeUpdate("create database test");
			//System.out.println(result);
			//result = stmt.executeUpdate("create table test.emp(id int(10), name varchar(40), age int(3)");
			//result = stmt.executeUpdate("insert into test.emp values (1, 'yes', 10)");

			
			String[] cin = text.split(";");
			
			con.close();  
		}catch(Exception e){ System.out.println(e);}
		return true;  
	}
	
}
