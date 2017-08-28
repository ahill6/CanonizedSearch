import java.util.Arrays;

/**
 * example to demonstrate creation of test suites for path coverage
 */
public class ToyTests {

	public static void main (String[] args){

		(new ToyTests()).median(0, 1, 2);
		(new ToyTests()).arrayMedian(0, 1, 2);
		(new ToyTests()).sumOrDifference(0, 1, 2);
		(new ToyTests()).sumOrDifference2(0, 1, 2);
		(new ToyTests()).grade(0, 1, 2);
		(new ToyTests()).grade2(0, 1, 2);
		(new ToyTests()).returnMin(0, 1, 2);
		(new ToyTests()).returnMin2(0, 1, 2);
		(new ToyTests()).copies(0, 1, 2);
		(new ToyTests()).copies2(0, 1, 2);
		(new ToyTests()).makeChange(0, 1, 2);
		(new ToyTests()).makeChange2(0, 1, 2);
		(new ToyTests()).xor(0, 1, 2);
		(new ToyTests()).xor2(0, 1, 2);
		
	}

		public double median(int a, int b, int c)
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

	// add odds, subtract evens
	public int sumOrDifference(int a, int b, int c){
	  if(a%2 == 1){
	    return b + c;
	  }
	  else{
	    return b - c;
	  }
	}
	
	// add odds, subtract evens
	public int sumOrDifference2(int a, int b, int c){
	  if(a%2 == 1){
	    return b - c;
	  }
	  else{
	    return b - c;
	  }
	}
	
	
	//some complicated testMe type combination of the three numbers determines output
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
				a = 3;
			}
		}
	}
	
	// return letter grade from number grade (we don't give no 'D's)
	public char grade2 (double a, int b, int c){
	  if (a < 70){
	    return 'A';
	  }
	  else if (a < 80){
	    return 'B';
	  }
	  else if (a < 90){
	    return 'C';
	  }
	  else if (a <= 100){
	    return 'F';
	  }
	  else{
	    return 'N';
	  }
	}
	
	
	// return letter grade from number grade (we don't give no 'D's)
	public char grade (double a, int b, int c){
	  if (a < 70){
	    return 'F';
	  }
	  else if (a < 80){
	    return 'C';
	  }
	  else if (a < 90){
	    return 'B';
	  }
	  else if (a <= 100){
	    return 'A';
	  }
	  else{
	    return 'N';
	  }
	}
	
	
	public int returnMin(int a, int b, int c){
	  if ( a <= b){
		    if (a <= c){
		      return a;
		    }
		    else{
		      return c;
		    }
		  }
		  else{
		    if (b <= c){
		      return b;
		    }
		    else{
		      return c;
		    }
		  }
	  }
	
	public int returnMin2(int a, int b, int c){
		if (a <= b && a <= c){
			return a;
		}
		else if (b <= a && b <= c){
			return b;
		}
		else{
			return c;
		}
	}

	
	//check whether two of the numbers passed are duplicates
	public boolean copies(int a, int b, int c){
	  if (a ==b){
	    return true;
	  }
	  else if (b ==c){
	    return true;
	  }
	  else if (a == c){
	    return true;
	  }
	  return false;
	}
	
	//check whether two of the numbers passed are duplicates
	public boolean copies2(int a, int b, int c){
	  if (a == b || a == c || b == c){
	    return true;
	  }
	  else{
		  return false;
	  }
	 }
	
	//blackjack (return sum, conditionals for face cards)
		public int blackjack(int a, int b, int c){
		  if (a > 10){
		    a = 10;
		  }
		  if (b > 10){
		    b = 10;
		  }
		  if ((a+b <= 11 && a == 1) || (a+b < 11 && b ==1))
		  {
		    if (a == 1 && b == 1){
		      return 12;
		    }
		    else{
		      return a + b + 9;
		    }
		  }
		  return a + b;
		}

	
	// can make correct change without loops (input is #nickels, #pennies, sum) output is true/false (can/can't)
	public boolean makeChange(int nickels, int pennies, int sum){
	  if (sum%5 > pennies){
	    return false;
	  }
	  else if (sum / 5 > nickels){
	    return false;
	  }
	  return true;
	}
	
	// can make correct change without loops (input is #nickels, #pennies, sum) output is true/false (can/can't)
	public boolean makeChange2(int nickels, int pennies, int sum){
	  if ( 5* nickels + pennies < sum){
		  return false;
	  }
	  else if (sum - 5*(sum/5) > pennies){
		  return false;
	  }
	  return true;
	}


	// XOR first and second arguments
	public boolean xor(int a, int b, int c){
	  // it will be interesting to see if these ternaries trip the PCs
	  boolean first = a < 0.5 ? false : true;
	  boolean second = b < 0.5 ? false : true;

	  if (first && second){
	    return false;
	  }
	  else{
	    if (first || second){
	      return true;
	    }
	    else{
	      return false;
	    }
	  }
	}
	

	// XOR first and second arguments
	public boolean xor2(int a, int b, int c){
	  // it will be interesting to see if these ternaries trip the PCs
	  boolean first = a < 0.5 ? false : true;
	  boolean second = b < 0.5 ? false : true;

	  if (!(first && second) && (first || second)){
		  return true;
	  }
	  else{
		  return false;
	  }
	}
	

}
