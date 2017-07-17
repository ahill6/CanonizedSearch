package gov.nasa.jpf.symbc.miner;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.JPFConfigException;
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.symbc.miner.FreshStart.Methods;

public class Preprocessing{
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
	
	protected class SimplerJavaFileObject extends SimpleJavaFileObject{
	protected SimplerJavaFileObject(URI uri, Kind kind) {
			super(uri, kind);
		}
	}

	HashMap<Method, String> methods;
	HashMap<String, List<String>> methodBodies;
	HashMap<String, List<String>> methodCalls;
	String className;
	
	public Preprocessing(){
		methods = new HashMap<Method, String>();
		methodBodies = new HashMap<String, List<String>>();
		methodCalls = new HashMap<String, List<String>>();
		className = null;
	}
		
	public List<Methods> getMethods(){
		List<Methods> ret = new ArrayList<Methods>();
		for (Method m : methods.keySet()){
			if (!m.getName().contains("main")){
				ret.add(new FreshStart().new Methods(m.getName(), m.getNumParams()));
			}
		}
		return ret;
	}
	
	public void visitAST(String type) {
			ASTParser parser = ASTParser.newParser(AST.JLS3);
			parser.setSource(type.toCharArray());
			parser.setKind(ASTParser.K_COMPILATION_UNIT);
			parser.setResolveBindings(true);
			ASTNode root = parser.createAST(null);
			CompilationUnit cu = (CompilationUnit)root.getRoot();

			GenericASTVisitor bob = new GenericASTVisitor(cu);
			cu.accept(bob);

			System.out.println("AST walk done");
			methodCalls = bob.getMethodInvocations();
			methodBodies = bob.getMethodMap();
			methods = bob.getMethodDetails();
			className = bob.getClassName();
		}
	
	private Object whatKind(String a){
		if (a.compareTo("int") == 0 || a.compareTo("Integer") == 0){
			return "0";
		}
		else if (a.compareTo("double") == 0 || a.compareTo("Double") == 0){
			return "0.0";
		}
		else if (a.compareTo("String") == 0){
			return "";
		}
		else if (a.compareTo("boolean") == 0 || a.compareTo("Boolean") == 0){
			return true;
		}
		else if (a.compareTo("long") == 0 || a.compareTo("Long") == 0){
			return "(long)0";
		}
		else if (a.compareTo("short") == 0){
			return "((short) 0)";
		}
		else if (a.contains("int[]")){
			return "new int[]{0,0,0}";
		}
		else if (a.contains("double[]")){
			return "new double[]{0.0,1.0,2.0}";
		}
		else if (a.contains("float[]")){
			return "new float[]{(float)1.2, (float)2.3}";
		}
		else if (a.contains("long[]")){
			return "new long[]{(long)1.2, (long)2.3}";
		}
		else if (a.contains("short[]")){
			return "new short[]{(short)1.2, (short)2.3}";
		}
		else{
			return "null";
		}
	}
	
	public void transferToTemp(File in, File out){
		try {
			FileUtils.copyFile(in, out);
			} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeBugs(int b, String project, int bugNumber){
		String original;
		try {
			original = readFileToString("/Users/ahill6/Documents/workspace/jpf-symbc/src/examples/Tmp2.java");
			visitAST(original);
			Method t = findBuggy(b);
			// need to get the method text
			String tm = "//" + project + "-" + bugNumber + "\n" + t.getText() + "\n";
			write(tm, "/Users/ahill6/Documents/workspace/jpf-symbc/src/examples/Tmp.java", true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			return;
		}
	}
	
	public String generateMain(List<Method> me){
		System.out.println("generate main");
		String methodsSetup = "Methods[] methods = {";
		char nextVar = 'a';
		String ret = "public static void main (String[] args){ \n\t";
		for (Method m : me){
			String tmp = singleFunctionCallInMain(m, nextVar);
			nextVar = tmp.charAt(tmp.length()-1);
			ret += tmp.substring(0, tmp.length()-1);
			methodsSetup += "new Methods(\"" + m.getName() + "\"," + m.getNumParams() + "),";
		}
		System.out.println("Number of Methods: " + me.size());
		methodsSetup = methodsSetup.substring(0, methodsSetup.length()-1) + "};";
		write(methodsSetup, "/Users/ahill6/Documents/workspace/jpf-symbc/src/examples/Tmp.java");

		
		ret += "}\n";
		
		return ret; // main generated, method closed, class still open
	}
	
	public String singleFunctionCallInMain(Method m, char nextVar){
		String ret = "";
		if (m.getReturnType().compareTo("void") != 0){
			ret += m.getReturnType() + " " + nextVar + " = ";
			nextVar++;
		} 
		if (!m.isStatic){
			ret += m.getConstructor().getConstructorCall();
		}
		ret += m.getName();
		ret += generateVars(m); // should return "(0,0,true,0);"
	
		return ret+nextVar; 
	}
	
	public String generateMain(Method m){
		System.out.println("generate main");
		char nextVar = 'a';
		String ret = "public static void main (String[] args){ \n\t";
		//System.out.println(m.getReturnType());
		if (m.getReturnType().compareTo("void") != 0){
			System.out.println("okay return type");
			ret += m.getReturnType() + " " + nextVar + " = ";
		} 
		if (!m.isStatic){
			ret += m.getConstructor().getConstructorCall();
		}
		ret += m.getName();
		ret += generateVars(m); // should return "(0,0,true,0);"
		ret += "}\n";
		System.out.println("\n This is ret \n");
		System.out.println(ret);
		
		return ret; // main generated, method closed, class still open
	}
	
	public String generateVars(Method m){
		if (m.getNumParams() == 0){
			return "();";
		}
		String retu = "(";
		for (Object o : m.getParams()){
			retu += o + ",";
		}
		
		retu = retu.substring(0, retu.length()-1);
		retu += "); \n";
		
		return retu;
	}
	
	public void write(String s, String file){
		// write converted to Tmp.java
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter(file, false));
			bw.append(s);
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public void write(String s, String file, boolean append){
		// write converted to Tmp.java
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter(file, append));
			bw.append(s);
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public String writeMethod(String source, String to){
		// get the body for String s, write it to the output string.
		for (String b : methodBodies.get(source)){
			to += b;
			to += "\n";
			}
		
		return to;
	}
	
	public Method findBuggy(int a){
		Method result = null;
		int currentBest = -1;
		for (Method m : methods.keySet()){
			if (m.getStart() <= a && m.getStart() > currentBest){
				result = m;
				currentBest = m.getStart();
			}
		}
		return result;
	}
	
	public Method reformatTmp(int lineNum){
		// read in what is there now, add a main (with all method calls), write
		try {
			String original = readFileToString("/Users/ahill6/Documents/workspace/jpf-symbc/src/examples/Tmp.java");
			
			visitAST(original);
			
			Method buggy = findBuggy(lineNum);

			// THIS ONE IS NOT WORKING AT PRESENT
			//original = original.replace("\\s*(public)\\s*(static)\\s*(void)\\s*(main)", "public void main2");
			// ghettofied replacement for above
			original = original.replace("public static void main", "public static void main2");
			// get rid of package declaration
			original = original.replace("package ", "//package ");
						
			// change all method declarations to be static
			/*
			for (Method m : methods.keySet()){
				String tmp = m.getMethodDeclaration();
				if (!tmp.contains("static")){ // could also here do an isStatic check
					if (tmp.contains("public")){
						tmp = tmp.replace("public ", "public static ");
					}
					else if (tmp.contains("private")){
						tmp = tmp.replace("private ", "private static ");
					}
					else if (tmp.contains("protected")){
						tmp = tmp.replace("protected ", "protected static ");
					}
					else{
						tmp = "static " + tmp;
					}
				}
				original = original.replace(m.getMethodDeclaration(), tmp);
			}
			*/
			
			// need to remove the last "}" and add a main method (assuming they haven't put extra non-public classses at bottom)
			//original.lastIndexOf('}');
			original = original.substring(0, original.lastIndexOf('}'));
			// TODO - if Java is really making a new string each time, is this affecting memory?  
			// Is there a better way to do this?
			
			original += generateMain(buggy);
			original += "}";
			//original = original.replaceAll(className+"(?!\\w)", "Tmp");

						
			write(original, "/Users/ahill6/Documents/workspace/jpf-symbc/src/examples/"+className+".java");
			return buggy;
		} catch(IOException e){
			e.printStackTrace();
		}
		return null;
	}
			
	public void reformatTmp(){
		// read in what is there now, add a main (with all method calls), write
		try {
			String original = readFileToString("/Users/ahill6/Documents/workspace/jpf-symbc/src/examples/Tmp.java");
			
			visitAST(original);
			
			// THIS ONE IS NOT WORKING AT PRESENT
			//original = original.replace("\\s*(public)\\s*(static)\\s*(void)\\s*(main)", "public void main2");
			// ghettofied replacement for above
			original = original.replace("public static void main", "public static void main2");
			// get rid of package declaration
			original = original.replace("package ", "//package ");

			
			// need to remove the last "}" and add a main method (assuming they haven't put extra non-public classses at bottom)
			//original.lastIndexOf('}');
			original = original.substring(0, original.lastIndexOf('}'));			
			original += generateMain(new ArrayList<Method>(methods.keySet()));
			original += "}";
						
			write(original, "/Users/ahill6/Documents/workspace/jpf-symbc/src/examples/"+className+".java");
		} catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public String generateMainForIntroClassJava(List<Method> me){
		System.out.println("generate main");
		char nextVar = 'a';
		String ret = "public static void main (String[] args){ \n\t";
		//System.out.println(m.getReturnType());
		for (Method m : me){
			if (m.getName().contains("main")){
				continue;
			}
			if (m.getReturnType().compareTo("void") != 0){
				ret += m.getReturnType() + " " + nextVar + " = ";
				nextVar++;
			} 
			if (!m.isStatic){
				ret += m.getConstructor().getConstructorCall();
			}
			ret += m.getName();
			ret += generateVars(m); // should return "(0,0,true,0);"
		}
		ret += "}\n";
		System.out.println("\n This is ret \n");
		System.out.println(ret);
		
		return ret; // main generated, method closed, class still open
	}
	
		
	public boolean reformatForIntroClassJava(String filename){
		try {
			//change filepath to subdirectory/target/classes/introclassJava/therightfile (not the one with 'Obj' in it)
			String original = readFileToString(filename);
			visitAST(original);
			
			// ONLY NEED TO ADD MAIN, CONSIDER WHETHER METHOD TO CALL IS STATIC OR NOT, ADD METHOD CALL TO MAIN
			// TODO - Make this line actually work 
			original = original.replace("public static void main", "public static void main2");
						
			// need to remove the last "}" and add a main method (assuming they haven't put extra non-public classses at bottom)
			original.lastIndexOf('}');
			original = original.substring(0, original.lastIndexOf('}'));
			// TODO - if Java is really making a new string each time, is this affecting memory?  
			// Is there a better way to do this?
			System.out.println("Pre generate main");
			original += generateMainForIntroClassJava(new ArrayList<Method>(methods.keySet()));
			System.out.println("post generate main");
			original += "}";
			System.out.println("It's all done but the writing");
			
			write(original, filename);
			return true;
		} catch(IOException e){
			e.printStackTrace();
		}
		return false;
	}
	
	public Method reformatTmp2(int lineNum, String filename){
		/// I am treating filename as an absolute path including the filename itself
		///
		// read in what is there now
		System.out.println(lineNum);
		try {
			String original = readFileToString(filename);
			visitAST(original);

			Method buggy = findBuggy(lineNum);

			// ONLY NEED TO ADD MAIN, CONSIDER WHETHER METHOD TO CALL IS STATIC OR NOT, ADD METHOD CALL TO MAIN
			// TODO - Make this line actually work 
			original = original.replace("public static void main", "public static void main2");
						
			// need to remove the last "}" and add a main method (assuming they haven't put extra non-public classses at bottom)
			original.lastIndexOf('}');
			original = original.substring(0, original.lastIndexOf('}'));
			// TODO - if Java is really making a new string each time, is this affecting memory?  
			// Is there a better way to do this?
			System.out.println("Pre generate main");
			original += generateMain(buggy);
			System.out.println("post generate main");
			original += "}";
			System.out.println("It's all done but the writing");

						
			write(original, filename);
			return buggy;
		} catch(IOException e){
			e.printStackTrace();
		}
		return null;
	}
	
	
	public void callSPF(Method m){
		try {
			Config conf = JPF.createConfig(new String[0]);
			conf.setProperty("target", "Tmp");
			conf.setProperty("classpath", "${jpf-symbc}/build/examples");
			conf.setProperty("sourcepath", "${jpf-symbc}/src/examples");
			conf.setProperty("symbolic.dp", "no_solver");
			conf.setProperty("listener", ".symbc.andy.CodeCharacterizationListener");
			conf.setProperty("vm.storage.class", "nil");
			conf.setProperty("search.depth_limit", "30");

			String thisOne = conf.getTarget() + "." + m.getName() + "(" + makeParams(m.getNumParams()) + ")"; 
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
		catch (JPFConfigException cx){
			System.out.println("Config Exception");
			System.out.println(cx);
		}
		catch (JPFException jx){
			System.out.println("JPF Exception");
			System.out.println(jx);
		}
	}
	
	public void callSPF(Methods m, String target, String classp){
		try {
			Config conf = JPF.createConfig(new String[0]);
			conf.setProperty("target", target);
			conf.setProperty("classpath", classp);
			conf.setProperty("symbolic.dp", "no_solver");
			conf.setProperty("listener", ".symbc.andy.CodeCharacterizationListener");
			conf.setProperty("vm.storage.class", "nil");
			conf.setProperty("search.depth_limit", "30");

			String thisOne = conf.getTarget() + "." + m.getName() + "(" + makeParams(m.getNumParams()) + ")"; 
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
		catch (JPFConfigException cx){
			System.out.println("Config Exception");
			System.out.println(cx);
		}
		catch (JPFException jx){
			System.out.println("JPF Exception");
			System.out.println(jx);
		}
	}
	
	public void callSPF(Set<Method> me) throws IOException{
		if (me.size() == 0){
			throw new IOException("Empty methods set passed to SPF call");
		}
		try {
			for (Method m : me){
				Config conf = JPF.createConfig(new String[0]);
				conf.setProperty("target", "Tmp");
				conf.setProperty("classpath", "${jpf-symbc}/build/examples");
				conf.setProperty("sourcepath", "${jpf-symbc}/src/examples");
				conf.setProperty("symbolic.dp", "no_solver");
				conf.setProperty("listener", ".symbc.andy.CodeCharacterizationListener");
				conf.setProperty("vm.storage.class", "nil");
				conf.setProperty("search.depth_limit", "30");
				String thisOne = conf.getTarget() + "." + m.getName() + "(" + makeParams(m.getNumParams()) + ")"; 
				conf.setProperty("symbolic.method", thisOne);
				//String thisOne = conf.getTarget() + "." + m.getName() + "(" + makeParams(m.getNumParams()) + ")"; 
				//conf.setProperty("symbolic.method", thisOne);
				System.out.println(thisOne);
				JPF jpf = new JPF(conf);
				// ... set your listeners
				//jpf.addListener(myListener);
	
				jpf.run();
				if (jpf.foundErrors()){
					System.out.println("Whoops");
				}
			}
		}
		catch (JPFConfigException cx){
			System.out.println("Config Exception");
			System.out.println(cx);
		}
		catch (JPFException jx){
			System.out.println("JPF Exception");
			System.out.println(jx);
		}
	}
	
	public static List<File> populateFileList(String path){
		File tmp = new File(path);
		List<File> children = new ArrayList<File>();
		// filter for only folders and .java
        File[] files = tmp.listFiles();
        for (File child : files) {
            if (child.isDirectory()) {
                children.addAll(populateFileList(child.getAbsolutePath()));
            } else if (child.toString().substring(child.toString().lastIndexOf('.')+1).equals("txt")){
            	// TODO - change this back to java once the fixed downloader has run
                children.add(child);
            }
        }
        
        return children;
    }
	
	public static String readFileToString(String filePath) throws IOException {
		// Read file content into a string
		StringBuilder fileData = new StringBuilder(1000);
		BufferedReader reader = new BufferedReader(new FileReader(filePath));
		
		filePath.split("/");
		
		char[] buf = new char[10];
		int numRead = 0;
		while ((numRead = reader.read(buf)) != -1) {
			String readData = String.valueOf(buf, 0, numRead);
			fileData.append(readData);
			buf = new char[1024];
		}
 
		reader.close();
		return fileData.toString();
	}
	 
	class GenericASTVisitor extends ASTVisitor {
		private HashMap<String, List<String>> methodMap;
		private HashMap<String, List<String>> methodInvocations;
		private HashMap<Method, String> methodDetails;
		private String name;
		private String activeMethod;
		private Constructor constructor;
		private CompilationUnit source;
		private ASTRewrite rewrite;
	 
		public GenericASTVisitor(CompilationUnit s) {
			super();
			methodMap = new HashMap<String, List<String>>();
			methodInvocations = new HashMap<String, List<String>>();
			methodDetails = new HashMap<Method, String>();
			name = null;
			activeMethod = null;
			source = s;
			rewrite = null;
			constructor = null;
		}
		 
		public GenericASTVisitor(CompilationUnit s, ASTRewrite ast) {
			super();
			methodMap = new HashMap<String, List<String>>();
			methodInvocations = new HashMap<String, List<String>>();
			methodDetails = new HashMap<Method, String>();
			name = null;
			activeMethod = null;
			source = s;
			rewrite = ast;
			constructor = null;
		}
		
		public HashMap<String, List<String>> getMethodMap(){
			return methodMap;
		}
		
		public HashMap<String, List<String>> getMethodInvocations(){
			return methodInvocations;
		}
		
		public HashMap<Method, String> getMethodDetails(){
			return methodDetails;
		}
		
		public String getClassName(){
			return name;
		}
		
		@Override
		public boolean visit(TypeDeclaration t){
			if (name == null){
				name = t.getName().toString();
			}
			
			if (rewrite != null){
				ImportDeclaration im = rewrite.getAST().newImportDeclaration();
				im.setName(rewrite.getAST().newName("trial.trial"));
				im.setStatic(false);
				//rewrite.replace(i, im, null);
				ListRewrite lrw = rewrite.getListRewrite(source, CompilationUnit.IMPORTS_PROPERTY);
				lrw.insertLast(im, null);
			}
			
			return super.visit(t);
		}
		
		@Override
		public boolean visit(ImportDeclaration i){

			return super.visit(i);
		}
		
		@Override
		public boolean visit(MethodInvocation m){
			if (methodInvocations.containsKey(activeMethod)){
				List<String> tmp = methodInvocations.get(activeMethod);
				tmp.add(m.getName().toString());
				methodInvocations.put(activeMethod, tmp);
			}
			else{
				List<String> tmp = new ArrayList<String>();
				tmp.add(m.getName().toString());
				methodInvocations.put(activeMethod, tmp);
			}
			
			return super.visit(m);
		}
		
		@Override
		public boolean visit(MethodDeclaration m){
			if (m.isConstructor()){
				if ((constructor == null)){
					constructor = new Constructor(m.getName().toString());
					List<String> tmp = new ArrayList<String>();
					for (Object s : m.parameters()){
						String[] t = s.toString().split(" ");
						tmp.add(t[0]);
					}

					constructor.addParameter(tmp);
				}
			
				return super.visit(m);
				}
			String key1 = m.getName().toString();
			activeMethod = key1;
			
			if (methodMap.containsKey(key1)){
				List<String> tmp = methodMap.get(key1);
				tmp.add(m.toString());
				methodMap.put(key1, tmp);
			}
			else{
				List<String> tmp = new ArrayList<String>();
				tmp.add(m.toString());
				methodMap.put(key1, tmp);
			}
			
			List<Object> param = new ArrayList<Object>();
			String returnType = "void";
			for (Object s : m.parameters()){
				String[] t = s.toString().split(" ");
				param.add(whatKind(t[0]));
				returnType = m.getReturnType2().toString();
			}
			boolean statc = Modifier.isStatic(m.getModifiers());
			String one = m.toString().replace(m.getBody().toString(), "");
			if (m.getJavadoc() != null){
				one = one.replace(m.getJavadoc().toString(), "");
			}
			String declaration = one.trim();
			//m.toString().replace(m.getBody().toString(), "").replace(m.getJavadoc().toString(), "");
			int start = source.getLineNumber(source.getExtendedStartPosition(m));
			//methodDetails.put(new Method(m.getName().toString(), m.parameters().size(), param, returnType, start, declaration, statc, constructor), m.getName().toString());
			methodDetails.put(new Method(m.getName().toString(), m.parameters().size(), param, returnType, start, declaration, statc, constructor, m.toString()), m.getName().toString());
			return super.visit(m);
		}

		/*
		@SuppressWarnings("deprecation")
		public void preVisit(ASTNode a){
			System.out.println(a.getClass().toString());
			System.out.println(a.toString());
			if (a instanceof PackageDeclaration || a instanceof Javadoc){
				return;
			}
			else if (a instanceof TypeDeclaration){
				System.out.println(a.toString());
			}
			else{
				everything += a.toString();
			}
		}*/
	}
	public class Constructor{
		private List<String> parameters;
		private String name;
		private Constructor(String n){
			name = n;
		}
		public void addParameter(List<String> a){
			parameters = a;
		}
		public String getConstructorCall(){
			// parameters will be something like <0,null,0.0,true>
			// want something like "(new Tmp(0,null,0.0,true)."
			if (parameters.size() > 0){
				String ret = "(new " + name + "(";
				for (String p : parameters){
					ret += whatKind(p).toString() + ",";
				}
				ret = ret.substring(0, ret.length()-1);
				return ret + ")).";
			}
			else{
				return "(new " + name + "()).";
			}
		}
		
	}
	public class Method{
		private String name;
		private int numParams;
		private List<Object> params;
		private String returnType;
		private int lineStart;
		//private boolean isStatic;
		private String declaration;
		private boolean isStatic;
		private Constructor constructor;
		private String text;
		
		public Method(String a, int b, List<Object> c, String d, int e, String f, boolean g, Constructor h, String i){
			name = a;
			numParams = b;
			params = c;
			returnType = d;
			lineStart = e;
			declaration = f;
			isStatic = g;
			constructor = h;
			text = i;
		}
		
		public Method(String a, int b, List<Object> c, String d, int e, String f, boolean g, Constructor h){
			name = a;
			numParams = b;
			params = c;
			returnType = d;
			lineStart = e;
			declaration = f;
			isStatic = g;
			constructor = h;
		}
		
		public Method(String a, int b, List<Object> c, String d, int e, String f, boolean g){
			name = a;
			numParams = b;
			params = c;
			returnType = d;
			lineStart = e;
			declaration = f;
			isStatic = g;
			
		}
		public String getName()
		{
			return name;
		}
		public int getNumParams(){
			return numParams;
		}
		public List<Object> getParams()
		{
			return params;
		}
		public String getReturnType(){
			return returnType;
		}
		public int getStart(){
			return lineStart;
		}
		public String getMethodDeclaration(){
			return declaration;
		}
		public Constructor getConstructor(){
			return constructor;
		}
		public String getText(){
			return text;
		}
	}
}
