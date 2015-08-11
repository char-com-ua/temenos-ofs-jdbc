## Wso2 Data Service configuration example ##
This is a simple [Data Service](http://wso2.org/downloads/data-services-server) configuration to expose web service named `T24Test` with three operations:
  * getCustomerById
  * getUserList
  * modifyCustomer


**`T24test.dbs`** file:
```
<?xml version="1.0" encoding="UTF-8"?>
<data name="T24Test" baseURI="http://my.namespace">
<description>Test service</description>

<config id="t24">
	<property name="org.wso2.ws.dataservice.driver">org.t24.driver.T24Driver</property>
	<!-- in your channel.xml there should be channel named MYCHANNEL -->
	<property name="org.wso2.ws.dataservice.protocol">jdbc:org:t24:MYCHANNEL</property>
	<property name="org.wso2.ws.dataservice.user">T24_USER</property>
	<property name="org.wso2.ws.dataservice.password">T24_PASS</property>
	<property name="org.wso2.ws.dataservice.minpoolsize">0</property>
	<property name="org.wso2.ws.dataservice.maxpoolsize">3</property>
</config>


<!--================================= getCustomerById ======================================-->
<operation name="getCustomerById">
    <description>The operation that gets information from a standard</description>
    <call-query href="getCustomerById" />
</operation>

<query id="getCustomerById" useConfig="t24">
	<sql>SELECT SENDOFS ENQUIRY.SELECT,,{{USER}}/{{PASS}},%CUSTOMER
		 	@ID:EQ  =   set ?1
		 END
	</sql>

	<result element="getCustomerByIdResponse" rowName="customer" >
		<element name="id"              column="@ID" />
		<element name="mnemonic"        column="MNEMONIC"/>
		<element name="shortName"       column="SHORT.NAME"/>
		<element name="nationality"     column="NATIONALITY"/>
	</result>

	<param name="id"  sqlType="STRING" type="IN"  ordinal="1" />
</query>



<!--================================= getUserList ======================================-->
<operation name="getUserList">
    <description>Get User List</description>
    <call-query href="getUserList" />
</operation>

<query id="getUserList" useConfig="t24">
	<sql>SELECT SENDOFS ENQUIRY.SELECT,,{{USER}}/{{PASS}},%USER
		 END
	</sql>

	<result element="getUserListResponse" rowName="user" >
		<element name="id"               column="@ID" />
		<element name="login"            column="SIGN.ON.NAME"/>
		<element name="firstName"        column="USER.NAME"/>
		<element name="lastName"         column="LAST.NAME"/>
		<element name="language"         column="LANGUAGE"/>
	</result>
</query>



<!--================================= modifyCustomer ======================================-->
<operation name="modifyCustomer">
    <description>modify customer Last Name. You must specify CUSTOMER ID and new LAST-NAME</description>
    <call-query href="modifyCustomer" />
</operation>

<query id="modifyCustomer" useConfig="t24">
    <!--  Normally you will have a specific version name in the next line -->
    <!--  example: ... CUSTOMER,MY.VERSION.NAME/I/PROCESS, ...            -->
	<sql>SELECT SENDOFS CUSTOMER,/I/PROCESS,{{USER}}/{{PASS}}/,{{set ?1}}
			LAST.NAME:1:        = set ?2
		 END
	</sql>
	<!-- application always returns @id -->
	<result element="modifyCustomerResponse" rowName="return" >
		<element name="id"               column="@ID" />
	</result>
	
	<param name="id"        sqlType="STRING" type="IN"  ordinal="1" />
	<param name="lastName"  sqlType="STRING" type="IN"  ordinal="2" />
</query>

</data>

```