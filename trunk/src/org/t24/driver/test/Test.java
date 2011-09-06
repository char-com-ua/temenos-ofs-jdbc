package org.t24.driver.test;

import java.net.*;
import java.io.*;
import java.sql.*;
import org.t24.driver.*;
import java.util.*;


public class Test {
	public static void main(String arg[]) throws Exception{
		new Test().runTest();
	}

	public void runTest() throws Exception{
		
		String driver="org.t24.driver.T24Driver";
		String url="jdbc:org:t24:TESTCHANNEL?charset=utf-8";
		String uid="TESTUID";
		String pwd="TESTPWD";
		
		
		Class.forName(driver);
		//Connect to database
		Connection con = DriverManager.getConnection(url,uid,pwd);
		T24QueryFormatter q = new T24QueryFormatter(con, 120);
		
		String query = 
			"SENDOFS ENQUIRY.SELECT,,{{USER}}/{{PASS}}/,ENQ.LWEXISTS\n" +
				"F1:EQ  = getToken ?2 , -1\n" +
			"END\n" +
			"?3 = set      ?FIELD.TEXT\n"       +
			"?2 = toCent   ?FIELD.NUM\n"        +
			"FILTER	 MATCHES   \"?{FIELD.INT}\"  \"?{1}\" \n" +
			"";
			
/*			
			"SENDOFS CUSTOMER,,{{USER}}/{{PASS}}/,ENQ.LWEXISTS\n" +
				"F1::  = set    ?2 \n" +
				"F2::  = set    ?1 \n" +
				"F3:*: = split  ?3 5 \n" +
			"END\n" +
			"";
*/			

		List param = new ArrayList();
		param.add("(1|3)");
		param.add("111,222,333,4444");
		

		T24ResultSet rs = q.execute(query,param);
		//for(int i=0;i<q.getSentOfsQueries().size();i++)
		//	System.out.println("OFS"+i+"="+q.getSentOfsQueries().get(i));

		showResult(rs);
		
		//test example
		//assertTrue(false,"user name expected");
		
		rs.close();
		con.close();
	}

	public void showResult(ResultSet rs)throws Exception{
		ResultSetMetaData rsmd = rs.getMetaData();
		String s="";
		
		for (int i=0;i<rsmd.getColumnCount();i++){
			s+=rsmd.getColumnName(i+1);
			s+="\t";
		}
		System.out.println(s);
		// print the results
		while (rs.next()) {
			s="";
			for(int i=0;i<rsmd.getColumnCount();i++){
				s+=""+rs.getObject(""+(i+1))+"\t";
			}
			System.out.println(s);
		}
	}

	//just not to import junit for while
	int failCount=0;
	Throwable trace=new Throwable();
	public void assertTrue(boolean b,String message) {
		if(!b){
			failCount++;
			System.out.println("assertTrue failed: "+message+": "+trace.fillInStackTrace().getStackTrace()[1]);
		}
	}
}
