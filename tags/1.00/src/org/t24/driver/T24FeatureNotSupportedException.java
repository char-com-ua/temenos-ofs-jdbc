package org.t24.driver;

import java.sql.SQLException;


class T24FeatureNotSupportedException extends SQLException{
	public T24FeatureNotSupportedException(){
		super("The rquested feature is not supported by current driver.");
	}
}
