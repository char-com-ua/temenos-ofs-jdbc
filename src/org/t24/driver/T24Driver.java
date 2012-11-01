/*
 * Author: TMS Team
 * Creation Date        : 02-28-2001
 * Modification Date    : 02-28-2001
 * List of Modifications:
 * Date       Version Author               Description/Purpose
 * ---------- ------- -------------------- ------------------------------------
 * 02-28-2001    1.00 TMS Team             initial release
 */
package org.t24.driver;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.Connection;
import java.sql.SQLException;

import java.util.Hashtable;
import java.util.Properties;
import java.util.Enumeration;

import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.File;

import java.util.Date;
import java.util.StringTokenizer;

/**
 * A DBProxyDriver class provides all <code>java.sql.Driver</code> requirements.
 * <P>
 * @author TMS team
 */
public class T24Driver implements Driver{

	private static final String myUrl="jdbc:org:t24:";
	public Properties conInfo;
	

	private static T24Driver m_defaultDrvr=null;
	static{
		try {
			if(m_defaultDrvr==null){
				m_defaultDrvr=new T24Driver();
				DriverManager.registerDriver(m_defaultDrvr);
			}
		}catch(Exception e){
			System.err.println("org.t24.driver.T24Driver Exception:");
			e.printStackTrace(System.err);
		}
		try {
			if(System.getProperty("tc.home",null)==null){
				System.setProperty( "tc.home",(new File(".")).getCanonicalPath() );
			}
		}catch(Exception e){
			System.err.println("Can't set tc.home java sustem property: "+e);
		}
	}

	/**
	 * Constructs a new T24Driver.
	 * @throws SQLException if any sql exception occured
	 */
	public T24Driver() throws Exception{
	}


	/**
	 * Returns true if the driver thinks that it can open
	 * a connection to the given URL.
	 * @param url the database connection url to check.
	 * @throws SQLException if any sql exception occured
	 */
	public boolean acceptsURL(String url) throws SQLException{
		if(url==null)return false;
		return url.startsWith(myUrl);
	}

	private void getUrlProperties(String strURL,Properties prop){
		StringTokenizer st      =new StringTokenizer(strURL, "&");
		int             j       =0;
		String          strTemp="";
		while(st.hasMoreTokens()){
			strTemp=st.nextToken();
			j       =strTemp.indexOf("=");
			prop.setProperty(strTemp.substring(0, j), strTemp.substring(j+1));
		}
	}

	/**
	 * Attempts to make a database connection to the given URL
	 * @param url database connection url.
	 * @param info database connection parameters (user, password, etc.).
	 * @return a new database connection
	 * @throws SQLException if any sql exception occured
	 * @see java.sql.Driver
	 */
	public Connection connect(String url, Properties info) throws SQLException{
		if(!acceptsURL(url))return null;
		url=url.substring(myUrl.length());
		String tcserver;
		int i=url.indexOf("?");
		if(i==-1){
			tcserver=url;
		}else{
			tcserver=url.substring(0,i);
			getUrlProperties(url.substring(i+1),info);
		}
		info.setProperty(T24Connection.CHANNEL,tcserver);
		conInfo = info; 
		
		InnerCon r = new InnerCon();
		Thread th = new Thread(r);
		try{
			th.start();
			synchronized(r){
				r.wait(30000);
			}
			th.stop();
		}catch (InterruptedException e){
			e.getMessage();
		}
		        
		Connection con = r.getCon();
		Exception ex = r.getException();
		if(con == null){
			if(ex == null){
				throw new T24Exception("Couldn't not establish connection to T24 in 30 sec");
			}else{
				throw new T24Exception("Couldn't not establish connection to T24: " + ex.getMessage(), ex);
			}
		}
		return con;
	}

	/**
	 * Gets the driver's major version number
	 * @return the driver's major version number
	 */
	public int getMajorVersion(){
		return 2;
	}

	/**
	 * Gets the driver's minor version number
	 * @return the driver's minor version number
	 */
	public int getMinorVersion(){
		return 0;
	}

	public Properties getConnInfo(){
		return conInfo;
	}
	
	
	/**
	 * Gets information about the possible properties for this driver
	 * @param url database connection url.
	 * @param info database connection parameters.
	 * @see java.sql.Driver
	 */
	public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException{
		return new DriverPropertyInfo[0];
	}

	/**
	 * Reports whether this driver is a genuine JDBC COMPLIANT driver
	 * @see java.sql.Driver
	 */
	public boolean jdbcCompliant(){
		return true;
	}
	
	private class InnerCon implements Runnable{
		private Connection con;
		private Exception ex = null;
		
		public Connection getCon(){
			return con;
		}	
		public Exception getException(){
			return ex;
		}	
		
		public void run(){
			try{
				con = new T24Connection(conInfo);
			}catch (SQLException e){
				ex = e;
			}
			synchronized(this){
				this.notify();
			}
		}		
	}
	
	

}

