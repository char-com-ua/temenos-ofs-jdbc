/*
 *
 * Copyright 2006 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.t24.driver;


import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.text.*;

/**
 * An object that represents a precompiled SQL statement.
 * <P>A SQL statement is precompiled and stored in a
 * <code>PreparedStatement</code> object. This object can then be used to
 * efficiently execute this statement multiple times. 
 *
 * <P><B>Note:</B> The setter methods (<code>setShort</code>, <code>setString</code>,
 * and so on) for setting IN parameter values
 * must specify types that are compatible with the defined SQL type of
 * the input parameter. For instance, if the IN parameter has SQL type
 * <code>INTEGER</code>, then the method <code>setInt</code> should be used.
 *
 * <p>If arbitrary parameter type conversions are required, the method
 * <code>setObject</code> should be used with a target SQL type.
 * <P>
 * In the following example of setting a parameter, <code>con</code> represents
 * an active connection:  
 * <PRE>
 *   PreparedStatement pstmt = con.prepareStatement("UPDATE EMPLOYEES
 *                                     SET SALARY = ? WHERE ID = ?");
 *   pstmt.setBigDecimal(1, 153833.00)
 *   pstmt.setInt(2, 110592)
 * </PRE>
 *
 * @see Connection#prepareStatement
 * @see ResultSet 
 */

public class T24PreparedStatement extends T24Statement implements PreparedStatement {
	//!!!!!    Prepared Statement   !!!!!!	
	//original sql statement
	private String sql=null;
	//private boolean isEnquiry=false;
	private ParamList param = new ParamList();
    private static SimpleDateFormat sdfDate = new SimpleDateFormat("yyyyMMdd");
    private static SimpleDateFormat sdfDateTime = new SimpleDateFormat("yyyyMMddHHmm");
	       
	protected T24PreparedStatement(T24Connection con, String sql)throws SQLException{
		super(con);
		this.sql=sql;
	}

    public int getQueryType() {
        return queryType;
    }
	
	protected String quote(String s)throws SQLException{
		//do simple value quotation.
		
		//if sql is enquiry, then we don't need to quote, and we can't support coma
		if(queryType==QUERY_TYPE_ENQ){
			//maybe we have to put into a function detection of the ofs type
			if(s==null)return "";
			//maybe we should throw exception if wrong search symbol appeared...
			if(s.indexOf('"')!=-1)throw new T24Exception("T24 does not support double quotes (\").");
			if(s.indexOf(',')!=-1)throw new T24Exception("T24 does not support coma in enquiry parameter.");
			return s.replace('"','\'').replace(',','.');
		}else{
			if(s==null)return "\"\"";
			//right now T24 does not support double quote (") symbol.
			//so replace double quote by single one, add quotation, without escaping
			//in the future maybe escaping required.
			return "\""+s.replace('"','\'')+"\"";
		}
	}
	/**
	 * Internal method
	 * Returns a prepared sql statement. all question marks replaced by values.
	 */
    @Override
    protected void prepare(String sql) throws SQLException {
        super.prepare(sql);
        sql=this.preparedOfs;
        System.out.println("--- preparedOfs0   = " + this.preparedOfs);
        System.out.println("--- postProcessSql = " + this.postProcessSql);
        
        if (queryType == QUERY_TYPE_ENQ) {
            //prepared sql
            StringBuffer pSql = new StringBuffer((int) (1.75 * sql.length()));
            int parmNo = 0;

            int state = 0; //0: normal; 1: in "string";

            for (int i = 0; i < sql.length(); i++) {
                char c = sql.charAt(i);
                switch (state) {
                    case 0: {
                        if (c == '"') {
                            state = 1;
                            pSql.append(c);
                        } else if (c == '?') {
                            parmNo++;
                            //all stored parameters must be converted to string on setXXX
                            pSql.append(quote((String) param.get(parmNo-1)));
                        } else {
                            pSql.append(c);
                        }
                        break;
                    }
                    case 1: {
                        pSql.append(c);
                        if (c == '"') {
                            state = 0;
                        }
                        break;
                    }
                    default:
                        throw new T24Exception("wrong state during sql prepare.");
                }
            }
            preparedOfs = pSql.toString();
        } else if (queryType == QUERY_TYPE_ENQ_FMT) {
            preparedOfs = queryFormatter.prepare(sql, param);
        } else if (queryType == QUERY_TYPE_FMT) {
            preparedOfs = queryFormatter.prepare(sql, param);
        } else {
            throw new T24Exception("Not supported query type: " + sql);
        }
    }
	
    /**
     * Executes the SQL query in this <code>PreparedStatement</code> object
     * and returns the <code>ResultSet</code> object generated by the query.
     *
     * @return a <code>ResultSet</code> object that contains the data produced by the
     *         query; never <code>null</code>
     * @exception SQLException if a database access error occurs;
     * this method is called on a closed  <code>PreparedStatement</code> or the SQL
     *            statement does not return a <code>ResultSet</code> object
     */
    public ResultSet executeQuery() throws SQLException{
    	execute();
    	ResultSet lrs=getResultSet();
    	return lrs;
    }

    /**
     * Executes the SQL statement in this <code>PreparedStatement</code> object,
     * which must be an SQL Data Manipulation Language (DML) statement, such as <code>INSERT</code>, <code>UPDATE</code> or
     * <code>DELETE</code>; or an SQL statement that returns nothing, 
     * such as a DDL statement.
     *
     * @return either (1) the row count for SQL Data Manipulation Language (DML) statements
     *         or (2) 0 for SQL statements that return nothing
     * @exception SQLException if a database access error occurs;
     * this method is called on a closed  <code>PreparedStatement</code> 
     * or the SQL
     *            statement returns a <code>ResultSet</code> object
     */
    public int executeUpdate() throws SQLException{
    	executeQuery();
    	return rs.getRowCount();
    }

    /**
     * Sets the designated parameter to SQL <code>NULL</code>.
     *
     * <P><B>Note:</B> You must specify the parameter's SQL type.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param sqlType the SQL type code defined in <code>java.sql.Types</code>
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or 
     * this method is called on a closed <code>PreparedStatement</code>
     * @exception T24FeatureNotSupportedException if <code>sqlType</code> is
     * a <code>ARRAY</code>, <code>BLOB</code>, <code>CLOB</code>, 
     * <code>DATALINK</code>, <code>JAVA_OBJECT</code>, <code>NCHAR</code>, 
     * <code>NCLOB</code>, <code>NVARCHAR</code>, <code>LONGNVARCHAR</code>,
     *  <code>REF</code>, <code>ROWID</code>, <code>SQLXML</code>
     * or  <code>STRUCT</code> data type and the JDBC driver does not support
     * this data type
     */
    public void setNull(int parameterIndex, int sqlType) throws SQLException{
    	param.set(parameterIndex-1,"");
    }

    /**
     * Sets the designated parameter to the given Java <code>boolean</code> value.
     * The driver converts this
     * to an SQL <code>BIT</code> or <code>BOOLEAN</code> value when it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; 
     * if a database access error occurs or 
     * this method is called on a closed <code>PreparedStatement</code>
     */
    public void setBoolean(int parameterIndex, boolean x) throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }


    /**
     * Sets the designated parameter to the given Java <code>byte</code> value.  
     * The driver converts this
     * to an SQL <code>TINYINT</code> value when it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or 
     * this method is called on a closed <code>PreparedStatement</code>
     */
    public void setByte(int parameterIndex, byte x) throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }


    /**
     * Sets the designated parameter to the given Java <code>short</code> value. 
     * The driver converts this
     * to an SQL <code>SMALLINT</code> value when it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or
     * this method is called on a closed <code>PreparedStatement</code>
     */
    public void setShort(int parameterIndex, short x) throws SQLException{
    	param.set(parameterIndex-1,Short.toString(x));
    }


    /**
     * Sets the designated parameter to the given Java <code>int</code> value.  
     * The driver converts this
     * to an SQL <code>INTEGER</code> value when it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or 
     * this method is called on a closed <code>PreparedStatement</code>
     */
    public void setInt(int parameterIndex, int x) throws SQLException{
    	param.set(parameterIndex-1,Integer.toString(x));
    }

    /**
     * Sets the designated parameter to the given Java <code>long</code> value. 
     * The driver converts this
     * to an SQL <code>BIGINT</code> value when it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or 
     * this method is called on a closed <code>PreparedStatement</code>
     */
    public void setLong(int parameterIndex, long x) throws SQLException{
    	param.set(parameterIndex-1,Float.toString(x));
    }

    /**
     * Sets the designated parameter to the given Java <code>float</code> value. 
     * The driver converts this
     * to an SQL <code>REAL</code> value when it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or 
     * this method is called on a closed <code>PreparedStatement</code>
     */
    public void setFloat(int parameterIndex, float x) throws SQLException{
    	param.set(parameterIndex-1,Float.toString(x));
    }

    /**
     * Sets the designated parameter to the given Java <code>double</code> value.  
     * The driver converts this
     * to an SQL <code>DOUBLE</code> value when it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or 
     * this method is called on a closed <code>PreparedStatement</code>
     */
    public void setDouble(int parameterIndex, double x) throws SQLException{
    	param.set(parameterIndex-1,Double.toString(x));
    }

    /**
     * Sets the designated parameter to the given <code>java.math.BigDecimal</code> value.  
     * The driver converts this to an SQL <code>NUMERIC</code> value when
     * it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or 
     * this method is called on a closed <code>PreparedStatement</code>
     */
    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException{
    	if(x==null)return;
    	param.set(parameterIndex-1, x.toString());
    }

    /**
     * Sets the designated parameter to the given Java <code>String</code> value. 
     * The driver converts this
     * to an SQL <code>VARCHAR</code> or <code>LONGVARCHAR</code> value
     * (depending on the argument's
     * size relative to the driver's limits on <code>VARCHAR</code> values)
     * when it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or 
     * this method is called on a closed <code>PreparedStatement</code>
     */
    public void setString(int parameterIndex, String x) throws SQLException{
    	//if(x==null)return;
    	param.set(parameterIndex-1,x);
    }

    /**
     * Sets the designated parameter to the given Java array of bytes.  The driver converts
     * this to an SQL <code>VARBINARY</code> or <code>LONGVARBINARY</code>
     * (depending on the argument's size relative to the driver's limits on
     * <code>VARBINARY</code> values) when it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value 
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or 
     * this method is called on a closed <code>PreparedStatement</code>
     */
    public void setBytes(int parameterIndex, byte x[]) throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }

    /**
     * Sets the designated parameter to the given <code>java.sql.Date</code> value
     * using the default time zone of the virtual machine that is running
     * the application. 
     * The driver converts this
     * to an SQL <code>DATE</code> value when it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or 
     * this method is called on a closed <code>PreparedStatement</code>
     */
    public void setDate(int parameterIndex, java.sql.Date x) throws SQLException{
    	if(x==null)return;
    	String value=sdfDateTime.format(x);
    	if(value.endsWith("0000") )value=value.substring(0,8);
    	param.set(parameterIndex-1, value);
    }

    /**
     * Sets the designated parameter to the given <code>java.sql.Time</code> value.  
     * The driver converts this
     * to an SQL <code>TIME</code> value when it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or 
     * this method is called on a closed <code>PreparedStatement</code>
     */
    public void setTime(int parameterIndex, java.sql.Time x) throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }

    /**
     * Sets the designated parameter to the given <code>java.sql.Timestamp</code> value.  
     * The driver
     * converts this to an SQL <code>TIMESTAMP</code> value when it sends it to the
     * database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value 
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or 
     * this method is called on a closed <code>PreparedStatement</code>     */
    public void setTimestamp(int parameterIndex, java.sql.Timestamp x) throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }


    /**
     * Sets the designated parameter to the given input stream, which will have 
     * the specified number of bytes.
     * When a very large ASCII value is input to a <code>LONGVARCHAR</code>
     * parameter, it may be more practical to send it via a
     * <code>java.io.InputStream</code>. Data will be read from the stream
     * as needed until end-of-file is reached.  The JDBC driver will
     * do any necessary conversion from ASCII to the database char format.
     * 
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the Java input stream that contains the ASCII parameter value
     * @param length the number of bytes in the stream 
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or 
     * this method is called on a closed <code>PreparedStatement</code>
     */
    public void setAsciiStream(int parameterIndex, java.io.InputStream x, int length) throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }


    /**
     * Sets the designated parameter to the given input stream, which 
     * will have the specified number of bytes. 
     *
     * When a very large Unicode value is input to a <code>LONGVARCHAR</code>
     * parameter, it may be more practical to send it via a
     * <code>java.io.InputStream</code> object. The data will be read from the 
     * stream as needed until end-of-file is reached.  The JDBC driver will
     * do any necessary conversion from Unicode to the database char format.
     *
     *The byte format of the Unicode stream must be a Java UTF-8, as defined in the 
     *Java Virtual Machine Specification.
     * 
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...  
     * @param x a <code>java.io.InputStream</code> object that contains the
     *        Unicode parameter value 
     * @param length the number of bytes in the stream 
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or 
     * this method is called on a closed <code>PreparedStatement</code>
     * @exception T24FeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @deprecated
     */
    public void setUnicodeStream(int parameterIndex, java.io.InputStream x, int length) throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }


    /**
     * Sets the designated parameter to the given input stream, which will have 
     * the specified number of bytes.
     * When a very large binary value is input to a <code>LONGVARBINARY</code>
     * parameter, it may be more practical to send it via a
     * <code>java.io.InputStream</code> object. The data will be read from the 
     * stream as needed until end-of-file is reached.
     * 
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the java input stream which contains the binary parameter value
     * @param length the number of bytes in the stream 
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or 
     * this method is called on a closed <code>PreparedStatement</code>
     */
    public void setBinaryStream(int parameterIndex, java.io.InputStream x, int length) throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }


    /**
     * Clears the current parameter values immediately.
     * <P>In general, parameter values remain in force for repeated use of a
     * statement. Setting a parameter value automatically clears its
     * previous value.  However, in some cases it is useful to immediately
     * release the resources used by the current parameter values; this can
     * be done by calling the method <code>clearParameters</code>.
     *
     * @exception SQLException if a database access error occurs or 
     * this method is called on a closed <code>PreparedStatement</code>
     */
    public void clearParameters() throws SQLException{
    	param.clear();
    }

    //----------------------------------------------------------------------
    // Advanced features:

   /**
    * Sets the value of the designated parameter with the given object.
    * This method is like the method <code>setObject</code>
    * above, except that it assumes a scale of zero.
    *
    * @param parameterIndex the first parameter is 1, the second is 2, ...
    * @param x the object containing the input parameter value
    * @param targetSqlType the SQL type (as defined in java.sql.Types) to be 
    *                      sent to the database
    * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or 
    * this method is called on a closed <code>PreparedStatement</code>
    * @exception T24FeatureNotSupportedException if <code>targetSqlType</code> is
    * a <code>ARRAY</code>, <code>BLOB</code>, <code>CLOB</code>, 
    * <code>DATALINK</code>, <code>JAVA_OBJECT</code>, <code>NCHAR</code>, 
    * <code>NCLOB</code>, <code>NVARCHAR</code>, <code>LONGNVARCHAR</code>,
    *  <code>REF</code>, <code>ROWID</code>, <code>SQLXML</code>
    * or  <code>STRUCT</code> data type and the JDBC driver does not support
    * this data type
    * @see Types
    */
    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException{
    	setString(parameterIndex,x==null?null:""+x);
    }


    /**
     * <p>Sets the value of the designated parameter using the given object. 
     * The second parameter must be of type <code>Object</code>; therefore, the
     * <code>java.lang</code> equivalent objects should be used for built-in types.
     *
     * <p>The JDBC specification specifies a standard mapping from
     * Java <code>Object</code> types to SQL types.  The given argument 
     * will be converted to the corresponding SQL type before being
     * sent to the database.
     *
     * <p>Note that this method may be used to pass datatabase-
     * specific abstract data types, by using a driver-specific Java
     * type.
     *
     * If the object is of a class implementing the interface <code>SQLData</code>,
     * the JDBC driver should call the method <code>SQLData.writeSQL</code>
     * to write it to the SQL data stream.
     * If, on the other hand, the object is of a class implementing
     * <code>Ref</code>, <code>Blob</code>, <code>Clob</code>,  <code>NClob</code>,
     *  <code>Struct</code>, <code>java.net.URL</code>, <code>RowId</code>, <code>SQLXML</code>  
     * or <code>Array</code>, the driver should pass it to the database as a 
     * value of the corresponding SQL type.
     * <P>
     *<b>Note:</b> Not all databases allow for a non-typed Null to be sent to 
     * the backend. For maximum portability, the <code>setNull</code> or the
     * <code>setObject(int parameterIndex, Object x, int sqlType)</code> 
     * method should be used 
     * instead of <code>setObject(int parameterIndex, Object x)</code>.
     *<p>
     * <b>Note:</b> This method throws an exception if there is an ambiguity, for example, if the
     * object is of a class implementing more than one of the interfaces named above.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the object containing the input parameter value 
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs; 
     *  this method is called on a closed <code>PreparedStatement</code> 
     * or the type of the given object is ambiguous
     */
    public void setObject(int parameterIndex, Object x) throws SQLException{
    	setString(parameterIndex,x==null?null:""+x);
    }

    /**
     * Executes the SQL statement in this <code>PreparedStatement</code> object,
     * which may be any kind of SQL statement.
     * Some prepared statements return multiple results; the <code>execute</code>
     * method handles these complex statements as well as the simpler
     * form of statements handled by the methods <code>executeQuery</code>
     * and <code>executeUpdate</code>.
     * <P>
     * The <code>execute</code> method returns a <code>boolean</code> to
     * indicate the form of the first result.  You must call either the method
     * <code>getResultSet</code> or <code>getUpdateCount</code>
     * to retrieve the result; you must call <code>getMoreResults</code> to
     * move to any subsequent result(s).
     *
     * @return <code>true</code> if the first result is a <code>ResultSet</code>
     *         object; <code>false</code> if the first result is an update
     *         count or there is no result
     * @exception SQLException if a database access error occurs; 
     * this method is called on a closed <code>PreparedStatement</code> 
     * or an argument is supplied to this method
     * @see Statement#execute
     * @see Statement#getResultSet
     * @see Statement#getUpdateCount
     * @see Statement#getMoreResults

     */
    public boolean execute() throws SQLException{
//    	String preparedSql=prepare();    
    	return execute(sql);
    }


    //--------------------------JDBC 2.0-----------------------------

    /**
     * Adds a set of parameters to this <code>PreparedStatement</code>
     * object's batch of commands.
     * 
     * @exception SQLException if a database access error occurs or 
     * this method is called on a closed <code>PreparedStatement</code>
     * @see Statement#addBatch
     * @since 1.2
     */
    public void addBatch() throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }


    /**
     * Sets the designated parameter to the given <code>Reader</code>
     * object, which is the given number of characters long.
     * When a very large UNICODE value is input to a <code>LONGVARCHAR</code>
     * parameter, it may be more practical to send it via a
     * <code>java.io.Reader</code> object. The data will be read from the stream
     * as needed until end-of-file is reached.  The JDBC driver will
     * do any necessary conversion from UNICODE to the database char format.
     * 
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param reader the <code>java.io.Reader</code> object that contains the 
     *        Unicode data
     * @param length the number of characters in the stream 
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or 
     * this method is called on a closed <code>PreparedStatement</code>
     * @since 1.2
     */
    public void setCharacterStream(int parameterIndex, java.io.Reader reader, int length) throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }


    /**
     * Sets the designated parameter to the given
     *  <code>REF(&lt;structured-type&gt;)</code> value.
     * The driver converts this to an SQL <code>REF</code> value when it
     * sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x an SQL <code>REF</code> value
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or 
     * this method is called on a closed <code>PreparedStatement</code>
     * @throws T24FeatureNotSupportedException  if the JDBC driver does not support this method
     * @since 1.2
     */
    public void setRef (int parameterIndex, Ref x) throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }


    /**
     * Sets the designated parameter to the given <code>java.sql.Blob</code> object.
     * The driver converts this to an SQL <code>BLOB</code> value when it
     * sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x a <code>Blob</code> object that maps an SQL <code>BLOB</code> value
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or 
     * this method is called on a closed <code>PreparedStatement</code>
     * @throws T24FeatureNotSupportedException  if the JDBC driver does not support this method
     * @since 1.2
     */
    public void setBlob (int parameterIndex, Blob x) throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }


    /**
     * Sets the designated parameter to the given <code>java.sql.Clob</code> object.
     * The driver converts this to an SQL <code>CLOB</code> value when it
     * sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x a <code>Clob</code> object that maps an SQL <code>CLOB</code> value
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or 
     * this method is called on a closed <code>PreparedStatement</code>
     * @throws T24FeatureNotSupportedException  if the JDBC driver does not support this method
     * @since 1.2
     */
    public void setClob (int parameterIndex, Clob x) throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }


    /**
     * Sets the designated parameter to the given <code>java.sql.Array</code> object.
     * The driver converts this to an SQL <code>ARRAY</code> value when it
     * sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x an <code>Array</code> object that maps an SQL <code>ARRAY</code> value
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or 
     * this method is called on a closed <code>PreparedStatement</code>
     * @throws T24FeatureNotSupportedException  if the JDBC driver does not support this method
     * @since 1.2
     */
    public void setArray (int parameterIndex, Array x) throws SQLException{
    	//maybe will be used for multivalues
    	throw new T24FeatureNotSupportedException();
    }


    /**
     * Retrieves a <code>ResultSetMetaData</code> object that contains
     * information about the columns of the <code>ResultSet</code> object
     * that will be returned when this <code>PreparedStatement</code> object 
     * is executed.
     * <P>
     * Because a <code>PreparedStatement</code> object is precompiled, it is
     * possible to know about the <code>ResultSet</code> object that it will
     * return without having to execute it.  Consequently, it is possible
     * to invoke the method <code>getMetaData</code> on a
     * <code>PreparedStatement</code> object rather than waiting to execute
     * it and then invoking the <code>ResultSet.getMetaData</code> method
     * on the <code>ResultSet</code> object that is returned.
     * <P>
     * <B>NOTE:</B> Using this method may be expensive for some drivers due
     * to the lack of underlying DBMS support.
     *
     * @return the description of a <code>ResultSet</code> object's columns or
     *         <code>null</code> if the driver cannot return a
     *         <code>ResultSetMetaData</code> object
     * @exception SQLException if a database access error occurs or 
     * this method is called on a closed <code>PreparedStatement</code>
     * @exception T24FeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    public ResultSetMetaData getMetaData() throws SQLException{
    	//TODO later
    	return null;
    }

    /**
     * Sets the designated parameter to the given <code>java.sql.Date</code> value,
     * using the given <code>Calendar</code> object.  The driver uses
     * the <code>Calendar</code> object to construct an SQL <code>DATE</code> value,
     * which the driver then sends to the database.  With 
     * a <code>Calendar</code> object, the driver can calculate the date
     * taking into account a custom timezone.  If no
     * <code>Calendar</code> object is specified, the driver uses the default
     * timezone, which is that of the virtual machine running the application.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @param cal the <code>Calendar</code> object the driver will use
     *            to construct the date
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or 
     * this method is called on a closed <code>PreparedStatement</code>
     * @since 1.2
     */
    public void setDate(int parameterIndex, java.sql.Date x, Calendar cal)
	    throws SQLException{
    	setDate(parameterIndex, x);
    }


    /**
     * Sets the designated parameter to the given <code>java.sql.Time</code> value,
     * using the given <code>Calendar</code> object.  The driver uses
     * the <code>Calendar</code> object to construct an SQL <code>TIME</code> value,
     * which the driver then sends to the database.  With 
     * a <code>Calendar</code> object, the driver can calculate the time
     * taking into account a custom timezone.  If no
     * <code>Calendar</code> object is specified, the driver uses the default
     * timezone, which is that of the virtual machine running the application.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @param cal the <code>Calendar</code> object the driver will use
     *            to construct the time
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or 
     * this method is called on a closed <code>PreparedStatement</code>
     * @since 1.2
     */
    public void setTime(int parameterIndex, java.sql.Time x, Calendar cal) 
	    throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }


    /**
     * Sets the designated parameter to the given <code>java.sql.Timestamp</code> value,
     * using the given <code>Calendar</code> object.  The driver uses
     * the <code>Calendar</code> object to construct an SQL <code>TIMESTAMP</code> value,
     * which the driver then sends to the database.  With a
     *  <code>Calendar</code> object, the driver can calculate the timestamp
     * taking into account a custom timezone.  If no
     * <code>Calendar</code> object is specified, the driver uses the default
     * timezone, which is that of the virtual machine running the application.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value 
     * @param cal the <code>Calendar</code> object the driver will use
     *            to construct the timestamp
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or 
     * this method is called on a closed <code>PreparedStatement</code>
     * @since 1.2
     */
    public void setTimestamp(int parameterIndex, java.sql.Timestamp x, Calendar cal)
	    throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }


    /**
     * Sets the designated parameter to SQL <code>NULL</code>.
     * This version of the method <code>setNull</code> should
     * be used for user-defined types and REF type parameters.  Examples
     * of user-defined types include: STRUCT, DISTINCT, JAVA_OBJECT, and 
     * named array types.
     *
     * <P><B>Note:</B> To be portable, applications must give the
     * SQL type code and the fully-qualified SQL type name when specifying
     * a NULL user-defined or REF parameter.  In the case of a user-defined type 
     * the name is the type name of the parameter itself.  For a REF 
     * parameter, the name is the type name of the referenced type.  If 
     * a JDBC driver does not need the type code or type name information, 
     * it may ignore it.     
     *
     * Although it is intended for user-defined and Ref parameters,
     * this method may be used to set a null parameter of any JDBC type.
     * If the parameter does not have a user-defined or REF type, the given
     * typeName is ignored.
     *
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param sqlType a value from <code>java.sql.Types</code>
     * @param typeName the fully-qualified name of an SQL user-defined type;
     *  ignored if the parameter is not a user-defined type or REF 
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or 
     * this method is called on a closed <code>PreparedStatement</code>
     * @exception T24FeatureNotSupportedException if <code>sqlType</code> is
     * a <code>ARRAY</code>, <code>BLOB</code>, <code>CLOB</code>, 
     * <code>DATALINK</code>, <code>JAVA_OBJECT</code>, <code>NCHAR</code>, 
     * <code>NCLOB</code>, <code>NVARCHAR</code>, <code>LONGNVARCHAR</code>,
     *  <code>REF</code>, <code>ROWID</code>, <code>SQLXML</code>
     * or  <code>STRUCT</code> data type and the JDBC driver does not support
     * this data type or if the JDBC driver does not support this method
     * @since 1.2
     */
	public void setNull (int parameterIndex, int sqlType, String typeName) 
		throws SQLException{
		setNull(parameterIndex,sqlType);
    }


    //------------------------- JDBC 3.0 -----------------------------------

    /**
     * Sets the designated parameter to the given <code>java.net.URL</code> value. 
     * The driver converts this to an SQL <code>DATALINK</code> value
     * when it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the <code>java.net.URL</code> object to be set
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or 
     * this method is called on a closed <code>PreparedStatement</code>
     * @throws T24FeatureNotSupportedException  if the JDBC driver does not support this method
     * @since 1.4
     */ 
    public void setURL(int parameterIndex, java.net.URL x) throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }


    /**
     * Retrieves the number, types and properties of this 
     * <code>PreparedStatement</code> object's parameters.
     *
     * @return a <code>ParameterMetaData</code> object that contains information
     *         about the number, types and properties for each 
     *  parameter marker of this <code>PreparedStatement</code> object
     * @exception SQLException if a database access error occurs or 
     * this method is called on a closed <code>PreparedStatement</code>
     * @see ParameterMetaData
     * @since 1.4
     */
    public ParameterMetaData getParameterMetaData() throws SQLException{
    	throw new T24FeatureNotSupportedException();
    }


    //------------------------- JDBC 4.0 -----------------------------------
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength)
            throws SQLException{
    	 setObject( parameterIndex, x);
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
