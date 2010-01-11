package org.t24.driver.test;

import java.net.*;
import java.io.*;
import java.sql.*;
import org.t24.driver.*;

public class Test {
	public static void main(String arg[]) throws Exception{
		String driver="org.t24.driver.T24Driver";
		String url="jdbc:org:t24:TESTCHANNEL?charset=utf-8";
		String uid="TESTUID";
		String pwd="TESTPWD";
		
		
		Class.forName(driver);
		//Connect to database
		Connection conn = DriverManager.getConnection(url,uid,pwd);
		
		System.out.println(((T24Connection)conn).t24Send(""));
		
		
		conn.close();
	}
}
