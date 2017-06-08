/*
 * Copyright (C) 2014, United States Government, as represented by the
 * Administrator of the National Aeronautics and Space Administration.
 * All rights reserved.
 *
 * Symbolic Pathfinder (jpf-symbc) is licensed under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0. 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */
import java.util.Arrays;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Z3Exception;

import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.IntegerConstant;
import gov.nasa.jpf.symbc.numeric.RealConstant;
import gov.nasa.jpf.symbc.numeric.SymbolicInteger;
import gov.nasa.jpf.symbc.numeric.SymbolicReal;

/**
 * example to demonstrate creation of test suites for path coverage
 */
public class TestPaths {

	public static void main (String[] args){
		// testMe(42, false);
		//System.out.println("!!!!!!!!!!!!!!! Start Testing! ");
		//(new TestPaths()).testMe2(0,false);
		(new TestPaths()).testMe4(0,false,(float)0, 0.0, (long)0, (long)1, true);
		(new TestPaths()).testMe2(0, true, 2);
		//(new TestPaths()).testMe2(0,false, 1);
		double w = (new TestPaths()).median1a(0, 1, 2);
		//Debug.printPC("");
		//double y = (new TestPaths()).median1b(0, 1, 2);
		//Debug.printPC("");
		double x = (new TestPaths()).median1b(0, 1, 2);
		double y = (new TestPaths()).median1c(0, 1, 2);
		double z = (new TestPaths()).median1d(0, 1, 2);
		double a = (new TestPaths()).buggyMedian1ResultChange(0, 1, 2);
		double e = (new TestPaths()).buggyMedian2ResultChange(0, 1, 2);
		double f = (new TestPaths()).buggyMedian1StructuralChange(0, 1, 2);
		double g = (new TestPaths()).buggyMedian2StructuralChange(0, 1, 2);
		double v = (new TestPaths()).arrayMedian(0, 1, 2);
		double u = (new TestPaths()).notMedian(0, 1, 2);
		String h = (new TestPaths()).forLoop("test string!");

		//double b = (new TestPaths()).manualSwitch(0, 1, 2);
		//String c = (new TestPaths()).addOneConstraint("0", "<", "1");
		//Expr d = (new TestPaths()).makeClause(new SymbolicInteger("c"));
		//double y = (new TestPaths()).median1b(0, 1, 2);
		//Debug.printPC("");
		//System.out.println(Debug.getSolvedPC());
	}

	// how many tests do we need to cover all paths?
	public static void testMe (int x, boolean b) {
		System.out.println("x = " + x);

		if (x <= 1200){
			System.out.println("  <= 1200");
		}
		if(x >= 1200){
			System.out.println("  >= 1200");
		}
	}

	public void testMe2 (int x, boolean b, int c) {
		//System.out.println("!!!!!!!!!!!!!!! First step! ");
		//System.out.println("x = " + x);
		int a = -1;
		boolean yes = false;
		if (b && c < 10) {
			a = c;
		}
		else{
			if (2*(x + 1) <= 1200){
				a = x;
				yes = true;
			}
			else if(x >= 1200 - c){
				a = c;
			}
		}
	}
	
	public double notMedian(int a, int b, int c)
	{
		double medi = 0;
		if ((a <= b) && (b <= c)){
			medi = c;
		}
		else if ((a <= c) && (c <= b)){
				medi = c;
			}
			else if ((b <= a) && (a <= c)){
					medi = c;
				}
				else if ((b <= c) && (c <= a)){
						medi = c;
					}
					else if ((c <= a) && (a <= b)){
							medi = c;
						}
						else if ((c <= b) && (b <= a)){
								medi = c;
							}
		return medi;
	}

	public double median1a(int a, int b, int c)
	{
		double medi = 0;
		if ((a <= b) && (b <= c)){
			medi = b;
		}
		else if ((a <= c) && (c <= b)){
				medi = c;
			}
			else if ((b <= a) && (a <= c)){
					medi = a;
				}
				else if ((b <= c) && (c <= a)){
						medi = c;
					}
					else if ((c <= a) && (a <= b)){
							medi = a;
						}
						else if ((c <= b) && (b <= a)){
								medi = b;
							}
		return medi;
	}
	
	public double median1b(int a, int b, int c)
	{
		double medi = 0;
		if (a <=b){
			if (b <=c){
				medi = b;
			}
			else if (c <=a){
				medi = a;
			}
			else if (c < b){
				medi = c;
			}
		}
		else{
			if (a <=c){
				medi = a;
			}
			else if (c <= b){
				medi = b;
			}
			else if (b < c){
				medi = c;
			}
		}
		
		return medi;
	}
	
	
	public double buggyMedian1ResultChange(int a, int b, int c)
	{
		double medi = 0;
		if (a <=b){
			if (b <=c){
				medi = b;
			}
			else if (c <=a){
				medi = b;
			}
			else if (c < b){
				medi = c;
			}
		}
		else{
			if (a <=c){
				medi = a;
			}
			else if (c <= b){
				medi = b;
			}
			else if (b < c){
				medi = c;
			}
		}
		
		return medi;
	}
	
	public double buggyMedian2ResultChange(int a, int b, int c)
	{
		double medi = 0;
		if (a <=b){
			if (b <=c){
				medi = b;
			}
			else if (c <=a){
				medi = b;
			}
			else if (c < b){
				medi = c;
			}
		}
		else{
			if (a <=c){
				medi = a;
			}
			else if (c <= b){
				medi = b;
			}
			else if (b < c){
				medi = b;
			}
		}
		return medi;
	}
	
	public double buggyMedian4ResultChange(int a, int b, int c)
	{
		double medi = 0;
		if (a <=b){
			if (b <=c){
				medi = a;
			}
			else if (c <=a){
				medi = a;
			}
			else if (c < b){
				medi = a;
			}
		}
		else{
			if (a <=c){
				medi = a;
			}
			else if (c <= b){
				medi = a;
			}
			else if (b < c){
				medi = a;
			}
		}
		return medi;
	}
	
	public double buggyMedian1StructuralChange(int a, int b, int c)
	{
		double medi = 0;
		if (a <=b){
			if (c <=a){
				medi = a;
			}
			else if (c < b){
				medi = c;
			}
		}
		else{
			if (a <=c){
				medi = a;
			}
			else if (c <= b){
				medi = b;
			}
			else if (b < c){
				medi = c;
			}
		}
		
		return medi;
	}	
	
	public double buggyMedian2StructuralChange(int a, int b, int c)
	{
		double medi = 0;
		if (a <=b ){
			if (c <=a){
				medi = a;
			}
			else if (c < b){
				medi = c;
			}
		}
		else{
			if (c <= b){
				medi = b;
			}
			else if (b < c){
				medi = c;
			}
		}
		
		return medi;
	}
	public double median1c(int x, int y, int z)
	{
		double medi = 0;
		if ((x <= y) && (y <= z)){
			medi = y;
		}
		else if ((x <= z) && (z <= y)){
				medi = z;
			}
			else if ((y <= x) && (x <= z)){
					medi = x;
				}
				else if ((y <= z) && (z <= x)){
						medi = z;
					}
					else if ((z <= x) && (x <= y)){
							medi = x;
						}
						else if ((z <= y) && (y <= x)){
								medi = y;
							}
		return medi;
	}
	
	public double median1d(int a, int b, int c)
	{
		double medi = 0;
		if (a <=b){
			if (b <=c){
				medi = b;
			}
			else if (c <=a){
				medi = a;
			}
			else{
				medi = c;
			}
		}
		else{
			if (a <=c){
				medi = a;
			}
			else if (c <= b){
				medi = b;
			}
			else{
				medi = c;
			}
		}
		
		return medi;
	}
	

	public double median3(int a, int b, int c)
	{
		return c;
		
	}

	public double arrayMedian(int a, int b, int c){
		int[] lst = {a,b,c};
		Arrays.sort(lst);
		if (lst.length%2 == 1){
			return lst[lst.length/2];
		}
		else{
			return ((lst[lst.length/2] + lst[(lst.length/2) + 1])* 0.5);
		}
	}
	
	public boolean testMe4 (int z, boolean b, float c, double d, long e, long f, boolean g) {
		//System.out.println("!!!!!!!!!!!!!!! First step! ");
		//System.out.println("x = " + x);
		double output = 0;
		boolean output2 = false;
		
		if (b) {
			if (c <= 25.3){
				if(z > 1200){
					//System.out.println("Case 2");
					output = (long)d;
					output2 = true;
				}
				else if (z == 1200){
					//System.out.println("Case 1");
					YoMamma yo = new YoMamma();
					yo.tester(z, d,  c,  b);
				}
				else{
					//System.out.println("Case 6");
					output = -3;
				}
			}

			else if (g && d == 4.239)
			{
				if (z <= 1200){
					//System.out.println("Case 3");
					z = (int) c + 4;
					output = z;
					output2 = true;
				}
				else {
					//System.out.println("Case 4");
					output = e + f;
				}
			}

			else{
				//System.out.println("Case 5");
				//output = Math.max((double)2.3456*c, (double)d);
				output = (double) (e + d);
			}
		}
		else{
			//System.out.println("Case 7");
		}
		//return new Integer((int) output);
		return output2;
	}
	
	public Expr makeClause(Expression expression){
		/* 
		 * Takes the pieces of a constraint and makes them into Z3 variables of the appropriate type.
		 * All sorts of stylistic problems by Java standards in this.
		 */
		Context ctx = new Context(); // What to do about these kind of definitions that do not take place in the method?
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
		else {
			System.out.println("Need to add something to makeClause.  Namely");
			System.out.println(expression.getClass());
		}
		return null;
	}
	
	public String forLoop(String a){
		System.out.println(a);
		String c = "";
		for (int i=0; i< a.length(); i++){
			char b = a.charAt(i);
			char bPrime = (char)(b + 1);
			System.out.println(i + " " + b + " " + bPrime);
			c += (char) (b+1);
		}
		System.out.println(c);
		
		return c;
		
	}
	
	public String addOneConstraint(String l, String r, String c)
	{
		String tmp = null;
		try{
			switch(c){
				case "=": 
					tmp = l + "=" + r;
					break;
				case "!=": 
					tmp = l + "!=" + r;
					break;
				case "<": 
					tmp = l + "<" + r;
					break;
				case ">":
					tmp = l+">"+r;
					break;
				case "<=": 
					tmp = l+"<="+r;
					break;
				case ">=":  
					tmp = l+">="+r;
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


public class YoMamma{
	public int tester (int a, double b, float c, boolean d){
		if(d){
			if(a == 10){
				return (int)c;
			}
		}
		else{
			if (b>4){
				//return (int)Math.sin(b);
				return 1;
			}
		}
		return a;
	}
}
}