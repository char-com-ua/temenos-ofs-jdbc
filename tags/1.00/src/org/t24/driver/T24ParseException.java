package org.t24.driver;


import java.sql.SQLException;


class T24ParseException extends T24Exception 
{

	T24ParseException(String s){
    	super(s);
	}

	T24ParseException(String s,Throwable cause){
    	super(s,cause);
	}


} 
