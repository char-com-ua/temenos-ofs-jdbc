package org.t24.driver.test;

import java.net.*;
import java.io.*;
import java.sql.*;
import org.t24.driver.*;
import java.util.*;


public class Test {
	public static void main(String arg[]) throws Exception{
		String driver="org.t24.driver.T24Driver";
		String url="jdbc:org:t24:TESTCHANNEL?charset=utf-8";
		String uid="TESTUID";
		String pwd="TESTPWD";
		
		
		Class.forName(driver);
		//Connect to database
		Connection con = DriverManager.getConnection(url,uid,pwd);
		
	    String queryEnq2 = "SENDOFS {{decode ?1 NULL TRUE FALSE}} ENQUIRY.SELECT,,${USER}/${PASS}/,ENQ.MW.CUST.LWEXISTS\n" +
			"TAX.ID:EQ = set ?2\n" +
			"SECTOR:EQ = const 2\n" +
			"DOC.NO:EQ = set ?3\n" +
			"DATE.OF.BIRTH:EQ = set ?4\n" +
			"RESIDENCE:EQ  = set ?5\n" +
			"END\n"+
			"?1 = decode ?T24.ID NULL 0 ?T24.ID\n"+

			"SENDOFS TRUE CUSTOMER,UAB.CFI.CU.PI..{{decode ?1 NULL INP AMN}}/I/PROCESS,${USER}/${PASS}/,\n" +
			"TAX.ID:1: = set ?2\n" +
			"LAST.NAME:1: = set ?3\n" +
			"FIRST.NAME:1: = set ?4\n" +
			"MIDDLE.NAME:1: = set ?5\n" +
			"DOC.TYPE:1:1  = const 1\n" +
			"END\n";

		String [] paramApp = {"asdfasdasd","1999-01-01"};
		List paramEnq = new ArrayList();
		paramEnq.add("111111");
		paramEnq.add("3200701925");
		paramEnq.add("99");
		paramEnq.add("");
		paramEnq.add("");

		execute(con, queryEnq2, paramEnq);
		
		con.close();
	}




	public static void execute(Connection con, String query,List param)throws Exception{

        T24QueryFormatter queryFormatter = new T24QueryFormatter(con);

		T24ResultSet rs = queryFormatter.execute(query,param);

                               
        ResultSetMetaData rsmd = rs.getMetaData();
		String s="";
        
        for (int i=0;i<rsmd.getColumnCount();i++){
        	s+=rsmd.getColumnName(i+1);
        	s+="\t";
        }
        s+="\n";
        // print the results
        while (rs.next()) {
            for(int i=0;i<rsmd.getColumnCount();i++){
            	s+=""+rs.getObject(""+(i+1))+"\t";
            }
            s+="\n";
        }

        System.out.println(s);
		rs.close();
		
	}


}
