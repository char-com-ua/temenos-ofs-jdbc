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

    private static SimpleDateFormat sdfDateParse = new SimpleDateFormat("yyyy-MM-dd");
    private static SimpleDateFormat sdfDate = new SimpleDateFormat("yyyyMMdd");
    private T24Statement statement;

    public T24QueryFormatter(T24Statement st) {
        this.statement = st;
    }

    public String prepare(String sql, List<String> param) throws SQLException {
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
        } else if ("date".equals(command)) {
            evaluateDate(fieldName, commandParams, colName, colValue, result);
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

    private void evaluateDate(String fieldName, List<String> commandParams, List<String> colName, List<String> colValue, Map<String, String> result) throws T24Exception {
        String value;
        if (!commandParams.get(0).startsWith("?")) {
            throw new T24Exception("Incorrect parameters");
        } else {
            value = colValue.get(Integer.parseInt(commandParams.get(0).substring(1)) - 1);
            try {
                value = sdfDate.format(sdfDateParse.parse(value));
            } catch (ParseException parseException) {
                throw new T24Exception(parseException.getMessage());
            }
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
