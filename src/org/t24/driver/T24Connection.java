package org.t24.driver;

import java.sql.Connection;
import java.sql.Savepoint;
import java.sql.SQLException;
import java.util.Properties;
import java.util.UUID;
import com.temenos.tocf.tcc.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.io.*;
import java.lang.*;


/**
 * Implements standard <code>java.sql.Connection</code> .
 * It passes all requests to database <code>connection</code> specified in constructor.
 * There are some features of connection managing:
 * <ul>
 * <li><code>timeout</code> property to check connection idle time
 * <li><code>isOld()</code> method to check does time of maximum inactivity passed.
 * <li><code>stackTrace</code> property that used if connection is not released to trace caller.
 * </ul>
 */
public class T24Connection implements Connection {

    /** connection parameters */
    public static final String CHANNEL = "channel";
    public static final String USER = "user";
    public static final String PASS = "password";
    public static final String CHARSET = "charset";
    private static TCCFactory tcFactory = null;
    private TCConnection tcConnection = null;
    
    private int currentChannel=0;
    private String tcChannels[];
    private long channelTimeout=10*60*1000;
    //last time in millis when connection has been established
    private long lastConnectTime=0;
    
    protected String tcUser;
    protected String tcPass;
    private String tcCharset;
    private static final String TESTCHANNEL="TESTCHANNEL";
    private boolean isTestMode=false;
    
    
	public static byte[] toByteArray(UUID uuid) {
		long msb = uuid.getMostSignificantBits();
		long lsb = uuid.getLeastSignificantBits();
		byte[] buffer = new byte[16];
		for (int i = 0; i < 8; i++) {
				buffer[i] = (byte) (msb >>> 8 * (7 - i));
		}
		for (int i = 8; i < 16; i++) {
				buffer[i] = (byte) (lsb >>> 8 * (7 - i));
		}
		return buffer;
	}
	/*
	public static byte[] toString(UUID uuid) {
		return Long.toHexString(uuid.getMostSignificantBits())+Long.toHexString(uuid.getLeastSignificantBits());
	}
	*/
	public static String toString(byte [] b){
		String s="0123456789abcdef";
		StringBuffer out=new StringBuffer(b.length*2);
		for(int i=0;i<b.length; i++){
			out.append( s.charAt( (0xF0&b[i])>>4 ) );
			out.append( s.charAt( 0x0F&b[i] ) );
		}
		return out.toString();
	}
	
    

    public String t24Send(String ofs, int queryTimeout) throws SQLException {
        //maybe in the future we have to set user/password here ?
        try {
        	String ofsResp;
        	//generate pseudo unique id for logging
        	java.util.UUID uuid=java.util.UUID.randomUUID();
        	byte [] buuid=toByteArray(uuid);
        	String ofsId=Long.toHexString(uuid.getMostSignificantBits());
			long startT = System.currentTimeMillis(); 
			T24QueryFormatter.logger.info("OFS_REQ("+ofsId+"): " + ofs.replaceAll(tcPass, "\\$PASSWORD"));
        	
        	if(isTestMode){
        		URL url=null;
				if(ofs.matches("^ENQUIRY\\.SELECT.*") ){
					url=T24Connection.class.getResource("/org/t24/driver/test/enquiry_resp.txt");
				}else{
					url=T24Connection.class.getResource("/org/t24/driver/test/ofs_resp.txt");
				}
				
				BufferedReader br = new BufferedReader(new InputStreamReader( url.openStream() ));
				StringBuilder sb = new StringBuilder();
				String line = br.readLine();
			    while (line != null) {
					sb.append(line + "\n");
					line = br.readLine();
				}				
        		ofsResp = sb.toString();
        	}else{
        		for(int reconnectCount=0;;reconnectCount++){
        			this.checkConnection();
					String charsetOFS = new String(ofs.getBytes(tcCharset));
					
					TCRequest tcSendRequest = tcFactory.createOfsRequest(charsetOFS, false);
					//set IDs for the request
					((TCRequestImpl)tcSendRequest).setCorrelationId( buuid );
					((TCRequestImpl)tcSendRequest).setRequestId( new com.temenos.tocf.ofsml.RequestID( toString(buuid).getBytes() ) );
					
					InnerSend innerSend = new InnerSend(tcSendRequest, tcConnection);
					Thread th = new Thread(innerSend);
					try{
						if(queryTimeout<=0)queryTimeout=120;
						tcConnection.setMaximumRetryCount(2);
						tcConnection.setRetryInterval(queryTimeout/2); //because we have 2 retries. ??? maybe we should have just one?
						
						th.start();
						synchronized(innerSend){
							innerSend.wait((queryTimeout+1)*1000); //+1 to give chance to t24 to finish request itself.
						}
						th.stop();
					}catch (InterruptedException e){
						T24QueryFormatter.logger.warn("T24 request interrupted: "+e.getMessage());
					}
					
					TCResponse tcResponse = innerSend.getResponse();
					Exception ex = innerSend.getException();
					
					if(tcResponse == null){
						if(ex == null){
							//reset & close connection
							this.close();
							throw new T24Exception("Couldn't not send request to T24 in " + queryTimeout + " sec");
						}else{
							String errorMessage=ex.getMessage();
							
							if(errorMessage!=null){
								errorMessage=errorMessage.toLowerCase();
								if( errorMessage.indexOf("the channel is not opened") >= 0 ){
									if(reconnectCount<1){
										T24QueryFormatter.logger.warn("Try to reconnect because of error from T24: "+ex);
										this.close();
										continue; //reconnect/resend request cycle
									}else{
										this.close();
									}
								}
							}
							throw new T24Exception("Couldn't not send request to T24: " + ex.getMessage(), ex);
						}
					}
					//check IDs of the response
					try {
						if( !((TCResponseImpl)tcResponse).getResponseId().equals( toString(buuid) ) ){
							throw new T24Exception("The IDs of the OFS request and response are not equal: "+
									((TCResponseImpl)tcResponse).getResponseId()+" != "+toString(buuid));
						}
						if( !java.util.Arrays.equals( tcResponse.getCorrelationId(), buuid )  ){
							throw new T24Exception("The correlation IDs of the OFS request and response are not equal: "+
									toString(tcResponse.getCorrelationId())+" != "+toString(buuid));
						}
					}catch(Exception idce){
						throw new T24Exception("Failed to compare OFS IDs :"+idce);
					}
					//done
					ofsResp = tcResponse.getOFSString();
					//we got response so let's stop cycle
					break;
				}
        	}
			T24QueryFormatter.logger.info("OFS_RES("+ofsId+","+(System.currentTimeMillis()-startT)+"ms): " + ofsResp.replaceAll(tcPass, "\\$PASSWORD"));
            return ofsResp;
        } catch (Throwable e) {
        	if(e instanceof T24Exception)throw (T24Exception)e;
            throw new T24Exception("T24 Send Exception: " + e.getMessage(), e);
        }
    }
    public boolean isTestMode(){
    	return this.isTestMode;
    }

    /**
     * Stack trace where connection was allocated.
     */
    //    protected SQLException stackTrace;
    /**
     * Creates <code>DBConnection</code>.
     * @param connection The real database connection.
     * @param pool connection pool this connection is related to.
     */
    protected T24Connection(Properties info) throws SQLException {
        super();
        tcChannels = info.getProperty(CHANNEL,"").split(",");
        if(tcChannels.length==0)throw new T24Exception("T24 channel not defined in connection url.");
        tcUser = info.getProperty(USER);
        tcPass = info.getProperty(PASS);
        tcCharset = info.getProperty(CHARSET);
        if (tcCharset == null || tcCharset.length() == 0) {
            tcCharset = java.nio.charset.Charset.defaultCharset().name();
        }
        this.checkConnection();
    }
    
    
/*    
    protected void reconnect(boolean switchChannel) throws SQLException {
       	this.close();
       	
       	//manage channels if we have more then one
       	if( switchChannel && tcChannels.length>1 ){
       		currentChannel++;
       		if(currentChannel>=tcChannels.length){
       			currentChannel=0;
       		}
       	}
       	
        try {
        	if(TESTCHANNEL.equals(tcChannels[currentChannel])) {
        		//don't connect ! it's just a test channel
        		isTestMode=true;
        	}else{
        		isTestMode=false;
				if (tcFactory == null) {
					tcFactory = TCCFactory.getInstance();
					tcFactory.setDefaultCharSet(tcCharset);
				}
				
				tcConnection = tcFactory.createTCConnection(tcChannels[currentChannel]);
        	}
        } catch (Exception e) {
            throw new T24Exception("T24 Connection Error: " + e.getMessage());
        }
    }
*/    
    protected void checkConnection()throws SQLException{
    	Exception conEx=null;
    	
		isTestMode=false;
    	if(TESTCHANNEL.equals(tcChannels[currentChannel])) {
    		//don't connect ! it's just a test channel
    		isTestMode=true;
    		return;
    	}
    	
    	if( lastConnectTime+channelTimeout<System.currentTimeMillis() && currentChannel>0 ){
			T24QueryFormatter.logger.warn("Disconnect from secondary channel because of timeout.");
    		currentChannel=0;
    		this.close();
    	}
    	
		if (tcFactory == null) {
			tcFactory = TCCFactory.getInstance();
			tcFactory.setDefaultCharSet(tcCharset);
		}
    	
    	if( tcConnection==null ){
			for(int i=0;i<tcChannels.length;i++){
				try {
					conEx=null;
					currentChannel=i;
					tcConnection = tcFactory.createTCConnection(tcChannels[currentChannel]);
					ping();
					lastConnectTime=System.currentTimeMillis();
					if(currentChannel==0)T24QueryFormatter.logger.warn("Connected to primary channel: "+tcChannels[currentChannel]);
					if(currentChannel>0)T24QueryFormatter.logger.warn("Connected to secondary channel: "+tcChannels[currentChannel]);
				} catch (Exception e) {
					T24QueryFormatter.logger.warn("Failed to connect channel: "+tcChannels[currentChannel]);
					conEx=e;
					this.close();
				}
			}
    	}
    	
    	
    	if(tcConnection==null){
    		throw new T24Exception("T24 Connection Error: " + conEx.getMessage(),conEx);
    	}
    	
    	
    }
    
    private void ping()throws SQLException{
		InnerPing r = new InnerPing();
		r.tcConnection=this.tcConnection;
		
		Thread th = new Thread(r);
		try{
			th.start();
			synchronized(r){
				r.wait(600);
			}
			th.stop();
		}catch (InterruptedException e){
			throw new T24Exception("T24 Ping timeout in 600 ms.",e);
		}
		if(r.ex!=null)throw new T24Exception("T24 Ping failed.",r.ex);
    }
    
    
    


    //Connection interface methods
    //All methods below this line are described in java.sql.Connection class
    /**
     * Method clearWarnings
     * standard <code>JDBC Connection</code> method
     * @throws SQLException
     */
    public void clearWarnings() throws SQLException {
        //nothing to do
    }

    /**
     * Method close
     * returns this connection to ConnectionPool
     * @throws SQLException
     */
    public void close() throws SQLException {
        if (tcConnection != null) {
            try {
                tcConnection.close();
            } catch (Exception e) {
				T24QueryFormatter.logger.warn("Can't close T24 connection: "+e);
            }
            try {
            	TCConnectionPool.releaseConnection(tcChannels[currentChannel],(TCConnectionImpl)tcConnection);
            }catch(Exception e){
				T24QueryFormatter.logger.warn("Can't release T24 connection: "+e);
            }
            tcConnection = null;
        }
    }

    /**
     * Method commit
     * standard <code>JDBC Connection</code> method
     * @throws SQLException
     */
    public void commit() throws SQLException {
        //no commit in t24 ofs
    }

    /**
     * Method createStatement
     * standard <code>JDBC Connection</code> method
     *
     * @return
     * @throws SQLException
     */
    public java.sql.Statement createStatement() throws SQLException {
        throw new T24FeatureNotSupportedException();
    }

    /**
     * Method createStatement
     * standard <code>JDBC Connection</code> method
     * @param resultSetType
     * @param resultSetConcurrency
     * @return
     * @throws SQLException
     */
    public java.sql.Statement createStatement(
            int resultSetType, int resultSetConcurrency) throws SQLException {
        throw new T24FeatureNotSupportedException();
    }

    /**
     * Method createStatement
     * standard <code>JDBC Connection</code> method
     * @param resultSetType
     * @param resultSetConcurrency
     * @return
     * @throws SQLException
     */
    public java.sql.Statement createStatement(
            int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        throw new T24FeatureNotSupportedException();
    }

    /**
     * Method getAutoCommit
     * standard <code>JDBC Connection</code> method
     * @return
     * @throws SQLException
     */
    public boolean getAutoCommit() throws SQLException {
        //no transactions in OFS
        return true;
    }

    /**
     * Method getCatalog
     * @return
     * @throws SQLException
     */
    public String getCatalog() throws SQLException {
        return null; //no catalogs, so current catalog is null

    }

    /**
     * Method getMetaData
     * standard <code>JDBC Connection</code> method
     * @return
     * @throws SQLException
     */
    public java.sql.DatabaseMetaData getMetaData() throws SQLException {
        //not supported yet. maybe in the future.
        return new T24DatabaseMetaData(this);
    }

    /**
     * Method getTransactionIsolation
     * standard <code>JDBC Connection</code> method
     * @return
     * @throws SQLException
     */
    public int getTransactionIsolation() throws SQLException {
        return 0;
    }

    /**
     * Method getTypeMap
     * standard <code>JDBC Connection</code> method
     * @return
     * @throws SQLException
     */
    public java.util.Map getTypeMap() throws SQLException {
        return new java.util.HashMap();
    }

    /**
     * Method getWarnings
     * standard <code>JDBC Connection</code> method
     * @return
     * @throws SQLException
     */
    public java.sql.SQLWarning getWarnings() throws SQLException {
        return null;
    }

    /**
     * Method isClosed
     * standard <code>JDBC Connection</code> method
     * @return
     * @throws SQLException
     */
    public boolean isClosed() throws SQLException {
        return tcConnection == null && isTestMode==false;
    }

    /**
     * Method isReadOnly
     * standard <code>JDBC Connection</code> method
     * @return
     * @throws SQLException
     */
    public boolean isReadOnly() throws SQLException {
        return false;
    }

    /**
     * Method nativeSQL
     * standard <code>JDBC Connection</code> method
     * @param sql
     * @return
     * @throws SQLException
     */
    public String nativeSQL(String sql) throws SQLException {
        //???  not supported yet. should be   ???
        throw new T24FeatureNotSupportedException();
    }

    /**
     * Method prepareCall
     * standard <code>JDBC Connection</code> method
     * @param sql
     * @return
     * @throws SQLException
     */
    public java.sql.CallableStatement prepareCall(String sql) throws SQLException {
        throw new T24FeatureNotSupportedException();
    }

    /**
     * Method prepareCall
     * standard <code>JDBC Connection</code> method
     * @param sql
     * @param resultSetType
     * @param resultSetConcurrency
     * @return
     * @throws SQLException
     */
    public java.sql.CallableStatement prepareCall(
            String sql, int resultSetType, int resultSetConcurrency)
            throws SQLException {
        //not supported
        throw new T24FeatureNotSupportedException();
    }

    /**
     * Method prepareCall
     * standard <code>JDBC Connection</code> method
     * @param sql
     * @param resultSetType
     * @param resultSetConcurrency
     * @return
     * @throws SQLException
     */
    public java.sql.CallableStatement prepareCall(
            String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        //not supported
        throw new T24FeatureNotSupportedException();
    }

    /**
     * Method prepareStatement
     * standard <code>JDBC Connection</code> method
     * @param sql
     * @return
     * @throws SQLException
     */
    public java.sql.PreparedStatement prepareStatement(String sql)
            throws SQLException {
        return new T24PreparedStatement(this, sql);
    }

    /**
     * Method prepareStatement
     * standard <code>JDBC Connection</code> method
     * @param sql
     * @return
     * @throws SQLException
     */
    public java.sql.PreparedStatement prepareStatement(String sql, String[] columnNames)
            throws SQLException {
        //maybe for the future
        throw new T24FeatureNotSupportedException();
    }

    /**
     * Method prepareStatement
     * standard <code>JDBC Connection</code> method
     * @param sql
     * @return
     * @throws SQLException
     */
    public java.sql.PreparedStatement prepareStatement(String sql, int[] columnIndexes)
            throws SQLException {
        //later
        throw new T24FeatureNotSupportedException();
    }

    /**
     * Method prepareStatement
     * standard <code>JDBC Connection</code> method
     * @param sql
     * @return
     * @throws SQLException
     */
    public java.sql.PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
            throws SQLException {
        //no such a keys.
        throw new T24FeatureNotSupportedException();
    }

    /**
     * Method prepareStatement
     * standard <code>JDBC Connection</code> method
     * @param sql
     * @param resultSetType
     * @param resultSetConcurrency
     * @return
     * @throws SQLException
     */
    public java.sql.PreparedStatement prepareStatement(
            String sql, int resultSetType, int resultSetConcurrency)
            throws SQLException {
        //not supported.
        throw new T24FeatureNotSupportedException();
    }

    /**
     * Method prepareStatement
     * standard <code>JDBC Connection</code> method
     * @param sql
     * @param resultSetType
     * @param resultSetConcurrency
     * @return
     * @throws SQLException
     */
    public java.sql.PreparedStatement prepareStatement(
            String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        //no support.
        throw new T24FeatureNotSupportedException();
    }

    /**
     * Method rollback
     * standard <code>JDBC Connection</code> method
     * @throws SQLException
     */
    public void rollback() throws SQLException {
        //nothing to do
    }

    /**
     * Method rollback
     * standard <code>JDBC Connection</code> method
     * @throws SQLException
     */
    public void rollback(Savepoint savepoint) throws SQLException {
        //no support.
        throw new T24FeatureNotSupportedException();
    }

    /**
     * Changes the holdability of <code>ResultSet</code> objects
     * created using this <code>Connection</code> object to the given
     * holdability.
     *
     * @param holdability a <code>ResultSet</code> holdability constant; one of
     *        <code>ResultSet.HOLD_CURSORS_OVER_COMMIT</code> or
     *        <code>ResultSet.CLOSE_CURSORS_AT_COMMIT</code>
     * @throws SQLException if a database access occurs, the given parameter
     *         is not a <code>ResultSet</code> constant indicating holdability,
     *         or the given holdability is not supported
     * @see #getHoldability
     * @see ResultSet
     * @since 1.4
     */
    public void setHoldability(int holdability) throws SQLException {
        //no support.
        throw new T24FeatureNotSupportedException();
    }

    /**
     * Retrieves the current holdability of <code>ResultSet</code> objects
     * created using this <code>Connection</code> object.
     *
     * @return the holdability, one of
     *        <code>ResultSet.HOLD_CURSORS_OVER_COMMIT</code> or
     *        <code>ResultSet.CLOSE_CURSORS_AT_COMMIT</code>
     * @throws SQLException if a database access occurs
     * @see #setHoldability
     * @see ResultSet
     * @since 1.4
     */
    public int getHoldability() throws SQLException {
        //no support.
        throw new T24FeatureNotSupportedException();
    }

    /**
     * Creates an unnamed savepoint in the current transaction and 
     * returns the new <code>Savepoint</code> object that represents it.
     *
     * @return the new <code>Savepoint</code> object
     * @exception SQLException if a database access error occurs
     *            or this <code>Connection</code> object is currently in
     *            auto-commit mode
     * @see Savepoint
     * @since 1.4
     */
    public Savepoint setSavepoint() throws SQLException {
        //no support.
        throw new T24FeatureNotSupportedException();
    }

    /**
     * Creates a savepoint with the given name in the current transaction
     * and returns the new <code>Savepoint</code> object that represents it.
     *
     * @param name a <code>String</code> containing the name of the savepoint
     * @return the new <code>Savepoint</code> object
     * @exception SQLException if a database access error occurs
     *            or this <code>Connection</code> object is currently in
     *            auto-commit mode
     * @see Savepoint
     * @since 1.4
     */
    public Savepoint setSavepoint(String name) throws SQLException {
        //no support.
        throw new T24FeatureNotSupportedException();
    }

    /**
     * Removes the given <code>Savepoint</code> object from the current 
     * transaction. Any reference to the savepoint after it have been removed 
     * will cause an <code>SQLException</code> to be thrown.
     *
     * @param savepoint the <code>Savepoint</code> object to be removed
     * @exception SQLException if a database access error occurs or
     *            the given <code>Savepoint</code> object is not a valid 
     *            savepoint in the current transaction
     * @since 1.4
     */
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        //no support.
        throw new T24FeatureNotSupportedException();
    }

    /**
     * Method setAutoCommit
     * standard <code>JDBC Connection</code> method
     * @param b
     * @throws SQLException
     */
    public void setAutoCommit(boolean b) throws SQLException {
        //always autocommit
    }

    /**
     * Method setCatalog
     * standard <code>JDBC Connection</code> method
     * @param catalog
     * @throws SQLException
     */
    public void setCatalog(String catalog) throws SQLException {
        //no support.
        throw new T24FeatureNotSupportedException();
    }

    /**
     * Method setReadOnly
     * standard <code>JDBC Connection</code> method
     * @param b
     * @throws SQLException
     */
    public void setReadOnly(boolean b) throws SQLException {
        //no support.
        throw new T24FeatureNotSupportedException();
    }

    /**
     * Method setTransactionIsolation
     * standard <code>JDBC Connection</code> method
     * @param i
     * @throws SQLException
     */
    public void setTransactionIsolation(int i) throws SQLException {
        //no support.
        throw new T24FeatureNotSupportedException();
    }

    /**
     * Method setTypeMap
     * standard <code>JDBC Connection</code> method
     * @param map
     * @throws SQLException
     */
    public void setTypeMap(java.util.Map map) throws SQLException {
        //no support.
        throw new T24FeatureNotSupportedException();
    }

    public java.sql.Clob createClob() throws SQLException {
        //no support.
        throw new T24FeatureNotSupportedException();
    }

    public java.sql.Blob createBlob() throws SQLException {
        //no support.
        throw new T24FeatureNotSupportedException();
    }

    public boolean isValid(int timeout) throws SQLException {
        //no support.
        throw new T24FeatureNotSupportedException();
    }

    public java.sql.Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        //no support.
        throw new T24FeatureNotSupportedException();
    }

    public java.sql.Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        //no support.
        throw new T24FeatureNotSupportedException();
    }

    public <T> T unwrap(java.lang.Class<T> iface) throws java.sql.SQLException {
        //no support.
        throw new T24FeatureNotSupportedException();
    }

    public boolean isWrapperFor(java.lang.Class<?> iface) throws java.sql.SQLException {
        //no support.
        throw new T24FeatureNotSupportedException();
    }


    //finalization
    protected void finalize() {
    	try{
	    	this.close();
    	}catch(Exception e){}
    }


        
	private class InnerSend implements Runnable{
		TCConnection tcConnection = null;
		TCRequest tcSendRequest = null;
		TCResponse tcResponse = null;
		private Exception ex = null;
		
		public TCResponse getResponse(){
			return tcResponse;
		}	
		public Exception getException(){
			return ex;
		}	
		
		InnerSend(TCRequest tcSendReq, TCConnection tcConn){
			this.tcConnection = tcConn;
			this.tcSendRequest = tcSendReq;
		}
		
		public void run(){
			try{
				tcResponse = tcSendRequest.send(tcConnection);
			}catch (Exception e){
				ex = e;
			}
			synchronized(this){
				this.notify();
			}
		}		
	}
    
	private class InnerPing implements Runnable{
		TCConnection tcConnection = null;
		Exception ex = null;
		
		public void run(){
			try{
				if( !this.tcConnection.ping() )throw new Exception("T24 ping failed.");
			}catch (Exception e){
				ex = e;
			}
			synchronized(this){
				this.notify();
			}
		}		
	}
    
    
    
    //JAVA 6 VERSION
    
	public java.sql.NClob createNClob() throws SQLException{
		//no support.
		throw new T24FeatureNotSupportedException();
	}


	public java.sql.SQLXML createSQLXML() throws SQLException{
		//no support.
		throw new T24FeatureNotSupportedException();
	}

	public String getClientInfo(String name) throws SQLException{
		//no support.
		throw new T24FeatureNotSupportedException();
	}


	public java.util.Properties getClientInfo() throws SQLException{
		//no support.
		throw new T24FeatureNotSupportedException();
	}

	public void setClientInfo(String name, String value) throws java.sql.SQLClientInfoException{
		//no support.
	}

	public void setClientInfo(java.util.Properties properties) throws java.sql.SQLClientInfoException{
		//no support.
	}
    
    
}
