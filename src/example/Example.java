import java.util.*;
import java.sql.*;
import java.io.*;


public class Example{

		static String driver="org.t24.driver.T24Driver";
		static String url="jdbc:org:t24:t24?charset=utf-8";
		//password actually not used now
		static String uid="MWARE_B2";
		static String pwd="MiddleWare31";


	public static void main(String arg[])throws Exception{

	    //Pool and connection settings
		//are presented here in order to show 
		//how they can be written.
		//These properties can be loaded from external source.

	
//	    String queryEnq = "SELECT ENQUIRY.SELECT,,MWARE_CFI/MiddleWare31,ENQ.UAB.MW.CU.PI.LIST,TAX.ID:EQ=?,DOC.NO:EQ=,DATE.OF.BIRTH:EQ=,LAST.NAME:EQ=";
	    String queryEnq = "SELECT  FORMATTER   ENQUIRY.SELECT,,MWARE_CFI/MiddleWare31,ENQ.UAB.MW.CU.PI.LIST\n"+
			"TAX.ID:EQ=set ?1\n"+
			"DOC.NO:EQ=fromCent ?2\n"+
			"DATE.OF.BIRTH:EQ= set ?3\n"+
			"LAST.NAME:EQ= set ?4\n"+
			"-POSTFORMATING-\n"+
			"CUST.ID1=const 111999 \n"+
			"CUST.ID2=toCent ?cust.id \n"+
			"CUST.ID3=decode ?cust.id 2520171 000000 25201771 111111\n";

//			"CUST.ID1=set ?CUST.ID \n";
//			"CUST.ID2=decode ?CUST.ID AAA BBB DEF\n";

		
/*		String queryApp = "SELECT FORMATTER  UA.NB.KL.K013,UA.NB.KL.K013.IMPORT/I/PROCESS,${USER}/${PASS},1.\n"+
			"TXT::    = const ..\n"+
			"TXT:1:   = set ?1\n"+
			"D.OPEN:: = date ?2\n"+
			"ACTIVE.FLAG:: = const Y\n"+
			"-POSTFORMATING-\n"+
			"XXX = const Y \n";
*/		
		String [] paramApp = {"asdfasdasd","1999-01-01"};
		String [] paramEnq = {"3200701925","99","",""};
//		String [] paramEnq = {"2647208341"};

		execute(queryEnq, paramEnq);

	}


	public static void execute(String query,String[] param)throws Exception{
		//Loads specified driver.
		//All drivers must support self registration by DriverManager.
		Class.forName(driver);
		
		//Connect to database
		Connection conn = DriverManager.getConnection(url,uid,pwd);

		String s="";
		PreparedStatement st = conn.prepareStatement(query);
               
		for (int i=0;i<param.length;i++){
			System.out.println(param[i]);
			st.setString(i+1,param[i]);
		}

		ResultSet rs = st.executeQuery();
                               
        ResultSetMetaData rsmd = rs.getMetaData();
        
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
		conn.close();
		
	}

}
