package gov.nasa.jpf.symbc.csar;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.RealExpr;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;
import com.microsoft.z3.Z3Exception;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.PropertyListenerAdapter;
import gov.nasa.jpf.jvm.bytecode.IfInstruction;
import gov.nasa.jpf.report.ConsolePublisher;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.symbc.SymbolicInstructionFactory;
import gov.nasa.jpf.symbc.concolic.PCAnalyzer;
import gov.nasa.jpf.symbc.numeric.BinaryLinearIntegerExpression;
import gov.nasa.jpf.symbc.numeric.BinaryNonLinearIntegerExpression;
import gov.nasa.jpf.symbc.numeric.BinaryRealExpression;
import gov.nasa.jpf.symbc.numeric.Constraint;
import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.IntegerConstant;
import gov.nasa.jpf.symbc.numeric.Operator;
import gov.nasa.jpf.symbc.numeric.PCChoiceGenerator;
import gov.nasa.jpf.symbc.numeric.PathCondition;
import gov.nasa.jpf.symbc.numeric.RealConstant;
import gov.nasa.jpf.symbc.numeric.SymbolicConstraintsGeneral;
import gov.nasa.jpf.symbc.numeric.SymbolicInteger;
import gov.nasa.jpf.symbc.numeric.SymbolicReal;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.SystemState;
import gov.nasa.jpf.vm.VM;

/**
 * This listener semantically characterizes a method via symbolic execution, then tests it 
 * against a database of previously characterized code to find a close match using the CSAR technique.
 * 
 * @author Andrew Hill
 *
 */
public class CodeTestingListener extends PropertyListenerAdapter {	
	private String databaseName = "xxx";
	private String database = "jdbc:mysql://localhost:xxxx/"+databaseName;
	private String password = "xxx";
	private String outfile = "/Users/Documents/workspace/tests/";
	Config confFile;
	List<BoolExpr> pathConstraintsAll = new ArrayList<BoolExpr>();
	Context ctx;
	int currentMid = -1; 

	/**
	 * @param conf
	 * @param jpf
	 */
	public CodeTestingListener(Config conf, JPF jpf) {
		jpf.addPublisherExtension(ConsolePublisher.class, this);
		confFile = conf;
		HashMap<String, String> cfg = new HashMap<String, String>();
		cfg.put("model", "true");
		ctx = new Context(cfg); 
	}
	
	/**
	 * Helper method to extract list of variables from constraints.
	 * This method does not maintain ordering.
	 * 
	 * @param expr logical expressions including variables
	 * @return   list of variables in the input parameter logical expressions
	 */
	private List<Expr> listVars(Expr[] expr){
		//System.out.println("list vars");
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
	
	/**
	 * Helper method to extract list of variables from constraints.
	 * This method does maintain ordering.
	 * 
	 * @param expr logical expressions including variables
	 * @return   list of variables in the input parameter logical expressions
	 */
	private List<Expr> listVarsKeepOrder(Expr[] expr){
		//System.out.println("list vars - keep order");
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
	
	
	/**
	 * Writes a list of boolean logical expressions to a predefined database.
	 * 
	 * @param pcs list of boolean logical expressions which represent Path Conditions through a program
	 */
	private void write(List<BoolExpr> pcs) {
		//System.out.println("write");
		writeConstraintsToDB(pcs);
	}
	
	/**
	 * Runs CSAR search against the list of boolean expressions passed in.  
	 * Closest matches are printed to outfile.
	 * 
	 * @param pcs list of boolean logical expressions which represent Path Conditions through a program
	 */
	private void test(List<BoolExpr> pcs) {
		//System.out.println("test");

		List<String> testStrings = new ArrayList<String>();
		for (BoolExpr g : pcs){
			testStrings.add(g + "\n");
		}
		
		try{
			StaticDistanceCalculations.prep();
			StaticDistanceCalculations.compareHavePCs(testStrings, confFile.getTarget(), 2, outfile+"/", confFile.getTarget());
			
			//String tmp = confFile.get("symbolic.method").toString().replace(confFile.getTarget()+".", "");
			//String methodName = tmp.split("[(]")[0]; //.split("[\.]")[1];
			//StaticDistanceCalculations.adaptiveCheck(testStrings, methodName, outfile+"/"+confFile.getTarget());

			// There also exists a method to measure the distance of the parameters from a predefined target (below)
			// NB - target must be represented as a string array which contains the filename.methodname of the target method(s)
			//StaticDistanceCalculations.distanceToX(testStrings, distanceToThisAsAMethod);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
		
	}
	
	int pathcount = 0;
	
	/* (non-Javadoc)
	 * @see gov.nasa.jpf.PropertyListenerAdapter#searchFinished(gov.nasa.jpf.search.Search)
	 */
	public void searchFinished(Search search) {
		//System.out.println("search finished");
		super.searchFinished(search);

		List<BoolExpr> pathConstraintsAllTmp = rename(green(pathConstraintsAll));
		pathConstraintsAll = pathConstraintsAllTmp;
		test(pathConstraintsAll);
	}
	
	/* (non-Javadoc)
	 * @see gov.nasa.jpf.PropertyListenerAdapter#searchStarted(gov.nasa.jpf.search.Search)
	 */
	public void searchStarted(Search search){
		//System.out.println("search started");
		super.searchStarted(search);
		currentMid = -1;
		String tmp = confFile.get("symbolic.method").toString().replace(confFile.getTarget()+".", "");
		String methodName = tmp.split("[(]")[0]; //.split("[\.]")[1];
		currentMid = addMethod(methodName);
		
		if (currentMid ==-1){
			System.out.println("Database Method Creation Problem for " + methodName);
			System.exit(0);
		}
	}


	/* (non-Javadoc)
	 * @see gov.nasa.jpf.PropertyListenerAdapter#stateAdvanced(gov.nasa.jpf.search.Search)
	 */
	public void stateAdvanced(Search search) {
		super.stateAdvanced(search);
		/*
		 * All this executes at the end of the symbolic execution
		 */
		if (search.isEndState()) {
			VM vm = search.getVM();
			ChoiceGenerator<?> cg = vm.getChoiceGenerator();

			if (!(cg instanceof PCChoiceGenerator)) {
				ChoiceGenerator<?> prev_cg = cg.getPreviousChoiceGenerator();
				while (!((prev_cg == null) || (prev_cg instanceof PCChoiceGenerator))) {
					prev_cg = prev_cg.getPreviousChoiceGenerator();
				}
				cg = prev_cg;
			}

			// if this is a multi-path method. For each path...
			if ((cg instanceof PCChoiceGenerator)
					&& ((PCChoiceGenerator) cg).getCurrentPC() != null) {
				PathCondition pc = ((PCChoiceGenerator) cg).getCurrentPC();

				List<BoolExpr> tmp1 = makeConstraints(pc);
				if (tmp1 != null){
					pathConstraintsAll.addAll(tmp1);
					pathConstraintsAll = minimalConstraintSet(pathConstraintsAll);
				}

				// solve the path condition
				if (SymbolicInstructionFactory.concolicMode) {
					SymbolicConstraintsGeneral solver = new SymbolicConstraintsGeneral();
					PCAnalyzer pa = new PCAnalyzer();
					pa.solve(pc, solver);
				} else
					pc.solve();
			} else {
				// no path condition!
				System.out.println("Single-path Program - How to capture me???");
				singlePath = true;
			}
		}
	}
	
	/**
	 * Combines the constraints to simplify/remove ones which are duplicates, keeping only logically incompatible constraints
	 * 
	 * @param list list of boolean expressions
	 * @return  minimal representation of the input boolean expressions
	 */
	public List<BoolExpr> minimalConstraintSet(List<BoolExpr> list){
		//System.out.println("minimal constraint set");
		List<BoolExpr> singleConstraintList = new ArrayList<BoolExpr>();
		singleConstraintList.add(list.remove(0));
		
		try{
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
			System.out.println("ERROR - minimal constraint set");
			System.out.println(list);
			System.out.println(e);
			throw e;
		}
		
		return singleConstraintList;
		
	}
	/**
	 * Helper for making canonical form.  Takes an expression and puts in form EXPR OP 0 for OP in {=, !=, <=}
	 * 
	 * @param t equality or inequality
	 * @return  input parameter with 0 as the right-hand side of the equality/inequality.
	 */
	private BoolExpr solveToZero(Expr t){
		// System.out.println("solve to zero");
		Expr[] sides = t.getArgs();
		
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
				System.out.println("ERROR - solve to zero");
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
				System.out.println("ERROR - unexpected type in Solve to Zero");
				System.out.println(t.getSExpr());
				System.exit(0);
				// throw new Exception("Unexpected type in Solve to Zero");
			}
		}
		return (BoolExpr) t;
	}
	
	/**
	 * Helper for making canonical form.  Turns > and >= into < and <=
	 * 
	 * @param t inequality
	 * @return  inequality which is < or <=
	 * @throws Exception
	 */
	private BoolExpr flip(Expr t) throws Exception{
		//System.out.println("flip");
		BoolExpr dPrime = null;
		Expr[] args = t.getArgs();
		if (args.length > 2){
			System.out.println("ERROR - flip.  Inequality with too many arguments!");
			System.out.println(t.getSExpr());
			System.exit(0);
			//throw new Exception("Inequality with too many arguments");
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
	
	/**
	 * Helper for making canonical form.  Converts < to <= by adding the minimum amount of the variable type
	 * (1 for ints, Long.MIN_VALUE for reals because longs can be converted to Reals by symbolic execution).
	 * 
	 * @param t inequality of form expr < 0
	 * @return  inequality of form expr <= 0
	 * @throws Exception
	 */
	private double addaBit(Expr t) throws Exception{
		//System.out.println("add a bit");
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
	
	/**
	 * Helper for making canonical form.  Driver method for conversion of equalities/inequalities
	 * 
	 * @param t equality or inequality
	 * @return  equality/inequality in canonical form
	 * @throws Exception
	 */
	public Expr normalize(Expr t) throws Exception{
		//System.out.println("normalize");
		if (t.isGE() || t.isGT()){
			return solveToZero(flip(t));
		}		
		else if (t.isNot() && t.getArgs().length == 1 && t.getArgs()[0].isEq())
		{
			return ctx.mkNot(solveToZero(t.getArgs()[0]));
		}
		else{ // This assumes the only alternatives are < ,  <=
			return solveToZero(t);
		}
	}
	
	
	/**
	 * Helper method for CSAR.  After making canonical form, variable names are standardized in order that 
	 * x < y and a < b can be recognized as the same constraint.
	 * 
	 * @param finalList list of boolean expressions representing Path Conditions
	 * @return  input list with standard naming enforced
	 */
	public List<BoolExpr> rename(List<BoolExpr> finalList){
		//System.out.println("rename");

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
					//throw e;
				}
			}
			i++;
		}
		
		return sortedMap.values().stream().collect(Collectors.toList());
	}
	
	/**
	 * Driver method for converting a single expression to canonical form.
	 * Canonical form is reached via the following procedure:
	 * 1) Lexicographically order statements
	 * 2) Convert to normal form (a1*x1 + a2*x2 + ... + an*xn OP 0 for OP in {=, !=, <=})
	 * 
	 * once all normalized statements collected
	 * 3) Rename variables v_i starting from left
	 * 
	 * @param t
	 * @return
	 * @throws Exception
	 */
	public BoolExpr canonize(Expr t) throws Exception{
		//System.out.println("canonize");
		Expr ret = null;

		// If the constraint is = or !=, only need to make normal form
		if (t.isEq() || (t.isNot() && t.getArgs().length == 1 && t.getArgs()[0].isEq())){
			try{
				ret = normalize(t);
			}
			catch(Exception e){
				System.out.println("ERROR - canonize - if");
				System.out.println(e);
				System.exit(0);
				//throw new Exception("Error in canonize");
			}
		}
		else{
			try{
				ret = normalize(t);
			}
			catch(Exception e){
				System.out.println("ERROR - canonize - else");
				System.out.println(e);
				//throw new Exception("Error in canonize - else");
			}
		}
		return (BoolExpr) ret;
	}
	
	/**
	 * 
	 * Implements the canonical form used in Visser, Geldenhuys, Dwyer 
	 * "Green: Reducing, Reusing and Recycling Constraints in Program Analysis"
	 * 
	 * @param rawConstraints
	 * @return  normalized constraints (not yet renamed)
	 */
	public List<BoolExpr> green(List<BoolExpr> rawConstraints){
		//System.out.println("green");
		List<BoolExpr> finalList = new ArrayList<BoolExpr>();
		
		// Canonize each constraint individually, then recombine
		for (BoolExpr r : rawConstraints){
			List<BoolExpr> intermediateList = new ArrayList<BoolExpr>();
			if (r.isAnd()){
				for (Expr t : r.getArgs()){
					try {
						intermediateList.add(canonize(t));
					} catch (Exception e) {
						e.printStackTrace();
						System.exit(0);
					}
				}
			}
			else{
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
	
	/**
	 * Add SPF Path Condition to current list of constraints
	 * 
	 * @param p current Path Condition of SPF Symbolic Execution
	 * @return  minimal logical description of all previous PCs (including current)
	 */
	public List<BoolExpr> makeConstraints(PathCondition p){
		//System.out.println("make constraints");
		List<BoolExpr> constraintList = new ArrayList<BoolExpr>();
		
		try{
			Constraint next = p.header;

			while (next != null){
				constraintList.add(addOneConstraint(next));
				next = next.getTail();
			}
			
			// check if this is unsatisfiable
			Solver s = ctx.mkSolver();
			
			for (BoolExpr b : constraintList){
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
			System.out.println("ERROR- make constraints");
			System.out.println(p);
			System.out.println(constraintList.size());
			System.out.println(e);
			throw e;
		}
	}

	/**
	 * Converts part of an SPF constraint into Z3 variables of the appropriate type.
	 * 
	 * @param expression SPF expression
	 * @return equivalent Z3 expression
	 */
	public Expr makeClause(Expression expression){
		//System.out.println("make clause");
		
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
			return addOneClause(makeClause(((BinaryRealExpression) expression).getLeft()),
					((BinaryRealExpression) expression).getOp(),
					makeClause(((BinaryRealExpression) expression).getRight()));
		}
		else {
			System.out.println("Need to add something to makeClause.  Namely");
			System.out.println(expression.getClass());
			//System.exit(0);
			//throw new Exception("Need to add " + expression.getClass() + " to makeClause");
		}
		return null;
	}
	
	
	/**
	 * Converts a full SPF constraint into Z3 equivalent
	 * 
	 * @param co SPF constraint
	 * @return  Z3 equivalent
	 */
	public BoolExpr addOneConstraint(Constraint co)
	{
		//System.out.println("add one constraint - constraint");
		String c = co.getComparator().toString().replaceAll("\\s", "");
		
		
		Expr l = makeClause(co.getLeft());
		Expr r = makeClause(co.getRight());
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
					System.out.println("ERROR - add one constraint - default");
					System.out.println(co.toString());
					break;
			}
		}
		catch(Exception e){
			System.out.println("ERROR - add one constraint - catch");
			System.out.println(co);
			System.out.println(e);
		}
		return tmp;
	}

	/**
	 * Helper for making logical clauses (parts of constraints).  Makes a logical expression out of pieces.
	 * 
	 * @param a left side of clause
	 * @param o operator (e.g. <, =, &&)
	 * @param b right side of clause
	 * @return  full expression combining the input  
	 */
	public Expr addOneClause(Expr a, Operator o, Expr b){
		//System.out.println("add one clause");
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
				System.exit(0);
				break;
			}
		return b;
	}

	public boolean singlePath = false;

	public void instructionExecuted(VM vm) {
		//System.out.println("instruction executed");
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

	/* (non-Javadoc)
	 * @see gov.nasa.jpf.PropertyListenerAdapter#propertyViolated(gov.nasa.jpf.search.Search)
	 */
	public void propertyViolated(Search search) {
		//System.out.println("property violated");
		VM vm = search.getVM();
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
			if (SymbolicInstructionFactory.concolicMode) {
				SymbolicConstraintsGeneral solver = new SymbolicConstraintsGeneral();
				PCAnalyzer pa = new PCAnalyzer();
				pa.solve(pc, solver);
			} else
				pc.solve();
		}
	}
	
	/**
	 * Writes list of canonical-form Path Conditions to previously specified database
	 * 
	 * @param pcs
	 */
	private void writeConstraintsToDB(List<BoolExpr> pcs){	
		//System.out.println("write constraints to db");
		try{
			Class.forName("com.mysql.jdbc.Driver");  
			
			Connection con = DriverManager.getConnection(database,"root",password); 
			
			int mid = currentMid;
			
			for (BoolExpr b : pcs)
			{
				
				// save a constraint to the db
				CallableStatement pc = con.prepareCall("call ADD_CONSTRAINT(?,?)");
				pc.setInt("M", mid);
				pc.setString("CON", b.getSExpr());
				pc.execute();
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
	 * Adds a method name to database.  Database ID of this method is returned so that constraints
	 * can be added once symbolic execution complete.
	 * 
	 * @param text method name
	 * @return  DB unique identifier of method just added
	 */
	private int addMethod(String text) {
		//System.out.println("add method");
		int uts = -1;
		try {
			Class.forName("com.mysql.jdbc.Driver");
			Connection con = DriverManager.getConnection(database,"root",password); 
			//System.out.println(text);
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

}