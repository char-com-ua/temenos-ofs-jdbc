package org.t24.driver;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.ArrayList;

/**
 *
 * @author avityuk
 */
public class T24QueryFormatter {

    //private static SimpleDateFormat sdfDateParse = new SimpleDateFormat("yyyy-MM-dd");
    //private static SimpleDateFormat sdfDate = new SimpleDateFormat("yyyyMMdd");
    private T24Connection con;    //connection
    //private String query;         //original query
    //private List<String> param;   //input parameters
    
    private static int STATE_DEF=0;
    private static int STATE_HEAD=1;
    private static int STATE_POST=3;

    public T24QueryFormatter(T24Connection con) {
        this.con = con;
    }
    
	public T24ResultSet execute(String query, List<String> queryParam){
		//cut off the optional SELECT keyword
		if( query.matches("^SELECT\\s"))query=query.substring(7).trim();
		
		String ofsHeader;
		T24ResultSet result;
		Map<String,String> ofsParam=new LinkedHashMap<String,String>();
		
		int state=0; 
		StringTokenizer st = new StringTokenizer(query, "\r\n");
		while(st.hasMoreElements()){
			String line = st.nextToken().trim();
			//skip empty and commented lines
			if (line.length() < 1 || line.startsWith("//")) {
				continue;
			}
			
			if(line.matches("^SENDOFS\\s")) {
				switch(state){
					case STATE_DEF: 
						break;
					case STATE_HEAD: 
						result=executeOfs(ofsHeader,ofsParam,result);
						break;
					case STATE_POST:
						break;
					default:
						throw new T24Exception("Wrong parser status: "+state);
				}
				state=STATE_HEAD;
				ofsHeader=prepareHeader(line);
			}else if(line.equals("POSTPROCESS")){
				switch(state){
					case STATE_HEAD: 
						result=executeOfs(ofsHeader,ofsParam,result);
						break;
					default:
						throw new T24Exception("Wrong parser status: "+state);
				}
				state=STATE_POST;
			}else{
				//usual evaluate lines here
				switch(state){
					case STATE_HEAD:
						/* we don't have query parameter names */
						evaluate(line, null, queryParam, ofsParam);
						break;
					case STATE_POST:
						postEvaluate(line,result,queryParam);
						break;
					default:
						throw new T24Exception("Wrong parser status: "+state);
				}
			}
		}
		//finally
		if(state==STATE_HEAD){
			result=executeOfs(ofsHeader,ofsParam,result);
		}
		if(state==STATE_DEF)throw new T24Exception("Wrong parser status: empty query");
		//return result fron last ofs
		return result;
	}
	
	/**
	* apply post evaluated parameters to resultset and to query parameters
	* " ?[0-9] = expression " must go to queryParam
	* " ?xxx = expression " must throw not supported exception (maybe in the future we will use it)
	* " xxx = expression " should go to the result (new column evaluated for each row)
	*/
	protected void postEvaluate(String line, T24ResultSet result, List<String> queryParam) throws SQLException{
		Map<String,String> postParam=new HashMap<String,String>(2); //initial count = 2
		
		for(int i=1; i <= result.getRowCount(); i++ ) {
			evaluate(line, ((T24ResultSet)result.getMetaData()).getColumnNames(), getDataRow(i), postParam);
			//now go through all key/values
			for( Map.Entry<String,String> entry : postParam.entrySet() ) {
				String key=entry.getKey();
				if(key.startsWith("?")) {
					//key started with ? so set the value for query parameter
					try {
						int index=Integer.parseInt(key.substring(1))-1;
						queryParam.set(index,entry.getValue());
					} catch ( Exception e ) {
						throw new T24Exception("Wrong post process index in expression: "+line,e);
					}
				} else {
					//usual key, so add column for the resultset
					result.setValue(i,key,entry.getValue());
				}
			}
			
		}
	}
	
	protected String prepareHeader(String ofsHeader,List<String> queryParam){
		//evaluate and replace all {{expression}}
		//no special formatting here put result as is
		int p1=0,p2=0;
		Map<String,String> eval=new HashMap<String,String>(2); //initial count = 2
		StringBuilder out=new StringBuilder();
		
		p1=ofsHeader.indexOf("{{");
		while ( p1>=0 ) {
			out.append( ofsHeader.substring(p2,p1) );
			p2=ofsHeader.indexOf("}}",p1);
			
			if(p2<0)throw new Exception("Can't find close tag for header expression: "+ofsHeader);
			String expression=ofsHeader.substring(p1+2,p2);
			
			evaluate("x="+expression,null,queryParam,eval);
			out.append( eval.get("x") );
			
			p1=ofsHeader.indexOf("{{",p2);
			p2+=2;
		}
		if(p2<ofsHeader.length())out.append(ofsHeader.substring(p2));
		return out.toString();
	}

    
	
	protected T24ResultSet executeOfs(String ofsHeader,Map<String,String> ofsParam, T24ResultSet oldResult){
		//do final prepare of the ofs
		
		///ofsHeader:
		///remove SENDOFS
		///if next keyword separated by spaces is FALSE then don't execute and just return oldResult
		///if next keyword separated by spaces is TRUE just remove it
		///so, [TRUE|FALSE] are optional keywords
		
		//detect query type (ENQUIRY|OFS)
		//add parameters into query
		
		//execute query
		//convert it into resultset
		...
		//clear ofs Parameters
		ofsParam.clear();
		
		//return new resultset
		...
	}
    
    /** prepares one single OFS query */
    protected String prepare(String sql, List<String> param) throws SQLException {
        HashMap<String, String> result = new HashMap<String, String>();
        String ofsBody = "";
        String ofsHeader = "";
        String ofs = "";

        StringTokenizer st = new StringTokenizer(sql, "\r\n");
        int lineCount = 0;
        while (st.hasMoreElements()) {
            String line = st.nextToken().trim();
            if (line.length() < 1 || line.startsWith("//")) {
                continue;
            }
            lineCount++;
            if (lineCount == 1) {
                ofsHeader = line;
                try {
                    if (ofsHeader.matches("^.*(\\?\\d).*$")) {
                        int index = Integer.parseInt(ofsHeader.replaceAll("^.*\\?(\\d+).*$", "$1"));
                        ofsHeader = ofsHeader.replaceAll("\\?\\d+", param.get(index));
                    }
                } catch (Exception e) {
                    String paramNum = ofsHeader.replaceAll("\\?\\d+", "$1");
                    throw new T24Exception("mandatory parameter not found" + paramNum);
                }
            } else {
                evaluate(line, null, param, result);
                ofs = ofsHeader;
                for (String columnName : result.keySet()) {
                    String columnValue = result.get(columnName);
                    ofsBody = prepareField(columnName, columnValue);
                    ofs += ofsBody;
                }
            }
        }
        return ofs;
    }
    
    //deprecated?
    ...
    protected HashMap<String, String> postProcesing(T24ResultSetMetaData metaData, ArrayList data, String postProcesParam) throws SQLException {
        HashMap<String, String> result = new HashMap<String, String>();
        StringTokenizer st = new StringTokenizer(postProcesParam, "\r\n");
        ArrayList<String> list = new ArrayList();

        while (st.hasMoreElements()) {
            String line = st.nextToken();
            evaluate(line, metaData.getHeaderList(), data, result);
        }
        return result;
    }

    /** 
     * evaluate the one-line command
     * @param line one-line command ( CR & LF not expected )
     * @param colName list of column names (could be null)
     * @param colValue list of column values
     * @param result a Map where to store result : column - bdec pair evaluated from line. Better to use LinkedHashMap, so order will be preserved.
     */
    private void evaluate(String line, List<String> colName, List<String> colValue, Map<String, String> result) throws SQLException {
        line = line.trim();
        if (line.length() == 0) {
            return;
        }
        if (line.startsWith("//")) {
            return;
        }
        int position;
        position = line.indexOf('=');
        if (position < 0) {
            throw new T24Exception("Syntax error: '=' expected in line: " + line);
        }
        String fieldName = line.substring(0, position).trim();
        String expression = line.substring(position + 1).trim();

        String command = expression.replaceAll("^(\\w+)\\s.*$", "$1");
        expression = expression.replaceAll("^(\\w+)\\s(.*)$", "$2").trim();
        List<String> commandParams = getCommandParams(expression);

        if ("const".equals(command)) {
            evaluateConst(fieldName, commandParams, colName, colValue, result);
        } else if ("decode".equals(command)) {
            evaluateDecode(fieldName, commandParams, colName, colValue, result);
        } else if ("toCent".equals(command)) {
            evaluateToCent(fieldName, commandParams, colName, colValue, result);
        } else if ("set".equals(command)) {
            evaluateSet(fieldName, commandParams, colName, colValue, result);
        } else if ("fromCent".equals(command)) {
            evaluateFromCent(fieldName, commandParams, colName, colValue, result);
        } else if ("split".equals(command)) {
            evaluateSplit(fieldName, commandParams, colName, colValue, result);
        } else if ("substr".equals(command)) {
            evaluateSubstr(fieldName, commandParams, colName, colValue, result);
        } else if ("setIfNull".equals(command)) {
            evaluateSetIfNull(fieldName, commandParams, colName, colValue, result);
        } else {
            throw new T24Exception("Unknown command : " + command);
        }
    }

    private List<String> getCommandParams(String expression) {
        List<String> commnadParams = new ArrayList();
        StringTokenizer st = new StringTokenizer(expression);
        while (st.hasMoreElements()) {
            commnadParams.add(st.nextToken());
        }
        return commnadParams;
    }

    private void evaluateToCent(String fieldName, List<String> commandParams, List<String> colName, List<String> colValue, Map<String, String> result) throws T24Exception {
        if (commandParams == null || colName == null || colValue == null) {
            throw new T24Exception("Incorrect parameters or ResultSet: ");
        }
        if (!commandParams.get(0).startsWith("?")) {
            throw new T24Exception("Incorrect parameter: " + commandParams.get(0));
        } else {
            int valueIndex = colName.indexOf(commandParams.get(0).substring(1));
            if (valueIndex != -1) {
                if ("".equals(colValue.get(valueIndex).trim())) {
                    result.put(fieldName, "");
                } else {
                    try {
                        BigDecimal value = new BigDecimal(colValue.get(valueIndex).trim());
                        value = value.multiply(new BigDecimal("100")).setScale(0);
                        result.put(fieldName, value.toString());
                    } catch (Exception e) {
                        result.put(fieldName, "");
                    }
                }
            } else {
                throw new T24Exception("Incorrect parameter: " + commandParams.get(0));
            }
        }
    }

    private void evaluateDecode(String fieldName, List<String> commandParams, List<String> colName, List<String> colValue, Map<String, String> result) throws T24Exception {
        String value = "";
        boolean changed = false;

        if (commandParams == null || colName == null || colValue == null) {
            throw new T24Exception("Incorrect parameters or ResultSet: ");
        }
        if (!commandParams.get(0).startsWith("?")) {
            throw new T24Exception("Incorrect parameter: " + commandParams.get(0));
        } else {
            int valueIndex = colName.indexOf(commandParams.get(0).substring(1));
            if (valueIndex == -1) {
                throw new T24Exception("Incorect result Set parameter : " + commandParams.get(0).substring(1));
            }
            value = colValue.get(valueIndex).trim();
        }
        for (int i = 1; i < commandParams.size() - 1; i += 2) {
            if (value.equals(commandParams.get(i).trim())) {
                value = commandParams.get(i + 1);
                changed = true;
            }
        }
        if (!changed && commandParams.size() % 2 == 0) {
            value = commandParams.get(commandParams.size() - 1);
        }
        result.put(fieldName, value);
    }

    private void evaluateConst(String fieldName, List<String> commandParams, List<String> colName, List<String> colValue, Map<String, String> result) {
        result.put(fieldName, commandParams.get(0));
    }

    private void evaluateSet(String fieldName, List<String> commandParams, List<String> colName, List<String> colValue, Map<String, String> result) throws T24Exception {
        String value;
        if (!commandParams.get(0).startsWith("?")) {
            throw new T24Exception("Incorrect parameter: " + commandParams.get(0));
        } else {
            value = colValue.get(Integer.parseInt(commandParams.get(0).substring(1)) - 1);
        }
        result.put(fieldName, value);
    }

    private void evaluateSetIfNull(String fieldName, List<String> commandParams, List<String> colName, List<String> colValue, Map<String, String> result) throws T24Exception {
        int paramIndex = Integer.parseInt(commandParams.get(0).substring(1));

        if (colValue.get(paramIndex - 1) == null || "".equals(colValue.get(paramIndex - 1))) {
            result.put(fieldName, commandParams.get(1));
        } else {
            result.put(fieldName, colValue.get(paramIndex - 1));
        }
    }

    private void evaluateFromCent(String fieldName, List<String> commandParams, List<String> colName, List<String> colValue, Map<String, String> result) throws T24Exception {
        String value = "";
        if (!commandParams.get(0).startsWith("?")) {
            throw new T24Exception("Incorrect parameters");
        } else {
            value = colValue.get(Integer.parseInt(commandParams.get(0).substring(1)) - 1);
            BigDecimal bdec = new BigDecimal(value);
            bdec = bdec.multiply(new BigDecimal("0.01"));
            value = bdec.toString();
        }
        result.put(fieldName, value);
    }

    private void evaluateSplit(String fieldName, List<String> commandParams, List<String> colName, List<String> colValue, Map<String, String> result) throws T24Exception {
        String value = "";
        if (!commandParams.get(0).startsWith("?")) {
            throw new T24Exception("Incorrect parameters");
        } else {
            String str = colValue.get(Integer.parseInt(commandParams.get(0).substring(1)) - 1);
            int counter = 1;
            int length = Integer.parseInt(commandParams.get(1));
            while (str.length() > 0) {
                value = substr(str, 0, length);
                result.put(fieldName.replaceAll("\\*", Integer.toString(counter)), value);
                str = substr(str, length, str.length());
                counter++;
            }
        }
    }

    private void evaluateSubstr(String fieldName, List<String> commandParams, List<String> colName, List<String> colValue, Map<String, String> result) throws T24Exception {
        String value = "";
        if (!commandParams.get(0).toString().startsWith("?")) {
            throw new T24Exception("Incorrect parameters");
        } else {
            String str = colValue.get(Integer.parseInt(commandParams.get(0).substring(1)) - 1);
            int indexStart = Integer.parseInt(commandParams.get(1));
            int length = Integer.parseInt(commandParams.get(2));

            value = substr(str, indexStart, length);
        //bdec = prepareField(fieldName, bdec);
        }
        result.put(fieldName, value);
    }

    private String substr(String str, int start, int length) {
        String res;
        int indexBegin;
        int indexEnd;
        if (str == null || str.length() == 0) {
            res = "";
        } else {
            indexBegin = Math.min(start, str.length());
            if (length < 0) {
                indexEnd = str.length();
            } else {
                indexEnd = Math.min(start + length, str.length());
            }
            res = str.substring(indexBegin, indexEnd);
        }
        return res;
    }

    private String prepareField(String fieldName, String value, int counter) {
        String res;
        if (value == null || value.length() == 0) {
            res = "";
        } else {
            value = value.replace('\"', '\'');
            if (statement.getQueryType() == T24Statement.QUERY_TYPE_FMT) {
                value = value.replaceAll("_", "'_'");
                value = "\"" + value + "\"";
            }
            fieldName = fieldName.replaceAll("\\*", Integer.toString(counter));
            res = "," + fieldName + "=" + value;
        }
        return res;
    }

    private String prepareField(String fieldName, String value) {
        return prepareField(fieldName, value, 1);
    }
}
