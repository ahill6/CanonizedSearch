package gov.nasa.jpf.symbc.andy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class DBInteractions{

	public void nNearest(String[] testStrings, int giveTopN, String preface){
		for (String t : testStrings){
			System.out.println("/////////////////////////////////////////////");
			System.out.println(t);
			System.out.println("---------------------------------------------");
			getNNearest(preface+"." + t, giveTopN);
			System.out.println("/////////////////////////////////////////////");
		}
	}
	
	public void nNearest(String t, int giveTopN, String preface){
			System.out.println("/////////////////////////////////////////////");
			System.out.println("DB Method");
			System.out.println("---------------------------------------------");
			getNNearest(preface+"." + t, giveTopN);
			System.out.println("/////////////////////////////////////////////");
	}
	
	private void getNNearest(String name, int topN){
			
		List<String> results = new ArrayList<String>();
		
		try{
			Class.forName("com.mysql.jdbc.Driver");  
			Connection con = DriverManager.getConnection(db,"root",password); 
			// TODO - change the above so that it reads the database, username, and password from a config file
			// TODO - go back to DistanceCalculations and do the same
			
			PreparedStatement ps = con.prepareStatement("call GET_N_NEAREST(?,?)");
			ps.setString(1, name);
			ps.setInt(2, topN);
			ps.execute();
			ResultSet rs = ps.getResultSet();
			
			while(rs.next())
			{
				String pc = rs.getString(1);
				double pc2 = rs.getDouble(2);
				results.add(pc);
				System.out.println(pc+ ": " + pc2);
			}
		}
		catch (ClassNotFoundException e) {
			System.out.println("1");
			e.printStackTrace();
			System.exit(1);
			} 
		catch (SQLException e) {
			e.printStackTrace();
			System.exit(0);
			}
	}
}
