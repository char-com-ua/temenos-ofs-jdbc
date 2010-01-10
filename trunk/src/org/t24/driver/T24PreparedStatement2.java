package org.t24.driver;


import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.text.*;


public class T24PreparedStatement2 implements PreparedStatement {
	
	//original sql statement
	private String sql=null;
	private T24Connection con;
	private ParamList param = new ParamList();
    private T24ResultSet result = null;
    private T24QueryFormatter queryFormatter;

	
    private static SimpleDateFormat sdfDate = new SimpleDateFormat("yyyyMMdd");
    private static SimpleDateFormat sdfDateTime = new SimpleDateFormat("yyyyMMddHHmm");
	       
	protected T24PreparedStatement2(T24Connection con, String sql)throws SQLException{
		this.con=con;
		this.sql=sql.trim();
		
	    queryFormatter = new T24QueryFormatter(con);
	}

    public boolean execute() throws SQLException{
    	//!!!!!!!TODO!!!!!!!
    	this.result=null;
    	return true;
    }

    public ResultSet executeQuery() throws SQLException{
    	execute();
    	return getResultSet();
    }
    
    public int executeUpdate() throws SQLException{
    	execute();
    	return 1;
    }
    
    public void close() throws SQLException {
        if (this.result != null) {
            this.result.close();
        }
        result = null;
    }
    
    public ResultSet getResultSet() throws SQLException {
    	ResultSet r=this.result;
    	this.result=null;
        return r;
    }
    
    public boolean getMoreResults() throws SQLException {
        return this.result!=null;
    }

    public int getUpdateCount() throws SQLException {
        return -1;
    }
    
	/*!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/
	/*!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!===PREPARED STATEMENT===!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/

    public void setNull(int parameterIndex, int sqlType) throws SQLException{
    	param.set(parameterIndex-1,null);
    }
    
    public void setObject(int parameterIndex, Object x) throws SQLException{
    	setString(parameterIndex,x==null?null:""+x);
    }

    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException{
    	setObject(parameterIndex,x);
    }
    
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException{
    	 setObject( parameterIndex, x);
    }
    
    public void setString(int parameterIndex, String x) throws SQLException{
    	param.set(parameterIndex-1,x);
    }

    public void setShort(int parameterIndex, short x) throws SQLException{
    	param.set(parameterIndex-1,Short.toString(x));
    }

    public void setInt(int parameterIndex, int x) throws SQLException{
    	param.set(parameterIndex-1,Integer.toString(x));
    }

    public void setLong(int parameterIndex, long x) throws SQLException{
    	param.set(parameterIndex-1,Float.toString(x));
    }
    
    public void setFloat(int parameterIndex, float x) throws SQLException{
    	param.set(parameterIndex-1,Float.toString(x));
    }

    public void setDouble(int parameterIndex, double x) throws SQLException{
    	param.set(parameterIndex-1,Double.toString(x));
    }

    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException{
    	param.set(parameterIndex-1, x==null?null:x.toString());
    }

    public void setDate(int parameterIndex, java.sql.Date x) throws SQLException{
		param.set( parameterIndex-1, x==null?null:sdfDate.format(x) );
    }

    public void clearParameters() throws SQLException{
    	param.clear();
    }

    public ResultSetMetaData getMetaData() throws SQLException{
    	//TODO later
    	return null;
    }

    public void setDate(int parameterIndex, java.sql.Date x, Calendar cal) throws SQLException{
    	setDate(parameterIndex, x);
    }


	public void setNull (int parameterIndex, int sqlType, String typeName) throws SQLException{
		setNull(parameterIndex,sqlType);
    }

    public void clearWarnings() throws SQLException {
    }
    
    public SQLWarning getWarnings() throws SQLException {
        return null;
    }
    
    public int getQueryTimeout() throws SQLException {
        return 0;
    }
    
    public int getMaxFieldSize() throws SQLException {
        return 0;
    }

    public int getMaxRows() throws SQLException {
        return 0;
    }


    /*!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!====NOT SUPPORTED====!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/

    public void setMaxFieldSize(int max) throws SQLException {
        throw new T24FeatureNotSupportedException();
    }
    public void setMaxRows(int max) throws SQLException {
        throw new T24FeatureNotSupportedException();
    }
    public void setEscapeProcessing(boolean enable) throws SQLException {
        throw new T24FeatureNotSupportedException();
    }
    public void setQueryTimeout(int seconds) throws SQLException {
        throw new T24FeatureNotSupportedException();
    }
    public void cancel() throws SQLException {
        throw new T24FeatureNotSupportedException();
    }
    public ResultSet executeQuery(String sql) throws SQLException {
    	throw new T24FeatureNotSupportedException();
    }
    public int executeUpdate(String sql) throws SQLException {
    	throw new T24FeatureNotSupportedException();
    }
    public boolean execute(String sql) throws SQLException {
    	throw new T24FeatureNotSupportedException();
    }
    public void setCursorName(String name) throws SQLException {
        throw new T24FeatureNotSupportedException();
    }
    
    public void setBytes(int parameterIndex, byte x[]) throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }

    public void setBoolean(int parameterIndex, boolean x) throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }

    public void setByte(int parameterIndex, byte x) throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }

    public void setTime(int parameterIndex, java.sql.Time x) throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }

    public void setTimestamp(int parameterIndex, java.sql.Timestamp x) throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }

    public void setAsciiStream(int parameterIndex, java.io.InputStream x, int length) throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }

    public void setUnicodeStream(int parameterIndex, java.io.InputStream x, int length) throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }

    public void setBinaryStream(int parameterIndex, java.io.InputStream x, int length) throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }
    public void setTime(int parameterIndex, java.sql.Time x, Calendar cal) 
	    throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }
    public void setTimestamp(int parameterIndex, java.sql.Timestamp x, Calendar cal)
	    throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }
    public void setURL(int parameterIndex, java.net.URL x) throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }
    public ParameterMetaData getParameterMetaData() throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }
    public void addBatch() throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }
    public void setCharacterStream(int parameterIndex, java.io.Reader reader, int length) throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }
    public void setRef (int parameterIndex, Ref x) throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }
    public void setBlob (int parameterIndex, Blob x) throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }
    public void setClob (int parameterIndex, Clob x) throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }
    public void setArray (int parameterIndex, Array x) throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }


    //--------------------------JDBC 2.0-----------------------------
    public void setFetchDirection(int direction) throws SQLException {
        throw new T24FeatureNotSupportedException();
    }
    
    public int getFetchDirection() throws SQLException {
        return ResultSet.FETCH_FORWARD;
    }

    public void setFetchSize(int rows) throws SQLException {
        throw new T24FeatureNotSupportedException();
    }

    public int getFetchSize() throws SQLException {
        return 0;
    }

    public int getResultSetConcurrency() throws SQLException {
        return ResultSet.CONCUR_READ_ONLY;
    }

    public int getResultSetType() throws SQLException {
        return ResultSet.TYPE_FORWARD_ONLY;
    }

    public void addBatch(String sql) throws SQLException {
        throw new T24FeatureNotSupportedException();
    }

    public void clearBatch() throws SQLException {
        throw new T24FeatureNotSupportedException();
    }

    public int[] executeBatch() throws SQLException {
        throw new T24FeatureNotSupportedException();
    }

    public Connection getConnection() throws SQLException {
        return con;
    }

    public boolean getMoreResults(int current) throws SQLException {
        return getMoreResults();
    }

    public ResultSet getGeneratedKeys() throws SQLException {
        throw new T24FeatureNotSupportedException();
    }

    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        throw new T24FeatureNotSupportedException();
    }

    public int executeUpdate(String sql, int columnIndexes[]) throws SQLException {
        throw new T24FeatureNotSupportedException();
    }

    public int executeUpdate(String sql, String columnNames[]) throws SQLException {
        throw new T24FeatureNotSupportedException();
    }

    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        throw new T24FeatureNotSupportedException();
    }

    public boolean execute(String sql, int columnIndexes[]) throws SQLException {
        throw new T24FeatureNotSupportedException();
    }

    public boolean execute(String sql, String columnNames[]) throws SQLException {
        throw new T24FeatureNotSupportedException();
    }

    public int getResultSetHoldability() throws SQLException {
        throw new T24FeatureNotSupportedException();
    }
    
    public boolean isClosed() throws SQLException {
        return result == null;
    }

    public void setPoolable(boolean poolable) throws SQLException {
        throw new T24FeatureNotSupportedException();
    }

    public boolean isPoolable() throws SQLException {
        return false;
    }

    //!!!!!  Wrapper    !!!!!!
    <T> T unwrap(java.lang.Class<T> iface) throws java.sql.SQLException {
        throw new T24FeatureNotSupportedException();
    }

    public boolean isWrapperFor(java.lang.Class<?> iface) throws java.sql.SQLException {
        throw new T24FeatureNotSupportedException();
    }  
    
    
    
    
    
    
    

/*
    public void setRowId(int parameterIndex, RowId x) throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }

 
    public void setNString(int parameterIndex, String value) throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }


    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }

    public void setNClob(int parameterIndex, NClob value) throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }


    public void setClob(int parameterIndex, Reader reader, long length)
        throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }


    public void setBlob(int parameterIndex, InputStream inputStream, long length)
        throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }

    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }


    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }


    public void setAsciiStream(int parameterIndex, java.io.InputStream x, long length)
	    throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }

    public void setBinaryStream(int parameterIndex, java.io.InputStream x, 
			 long length) throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }

    public void setCharacterStream(int parameterIndex,
       			  java.io.Reader reader,
			  long length) throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }

    public void setAsciiStream(int parameterIndex, java.io.InputStream x)
	    throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }

    public void setBinaryStream(int parameterIndex, java.io.InputStream x)
    throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }

    public void setCharacterStream(int parameterIndex,
       			  java.io.Reader reader) throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }

    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }


    public void setClob(int parameterIndex, Reader reader)
       throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }


    public void setBlob(int parameterIndex, InputStream inputStream)
        throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }

    public void setNClob(int parameterIndex, Reader reader)
       throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }
*/
       
}
