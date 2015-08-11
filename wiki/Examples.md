### ASSERT EQUALS ###
```
SELECT  SENDOFS  ENQUIRY.SELECT,,{{USER}}/{{PASS}},ENQ.UAB.CFI.CU.SRCH
	TAX.ID:EQ=set ?1
	DOC.NO:EQ=set ?2
	DATE.OF.BIRTH:EQ=set ?3
END

//evaluate customerStatusText field
customerStatusText = decode ?customer.status 1 valid 2 deleted
FILTER MATCHES "?{customer.status}" "1"
//compare first input patameter to result column TAX.ID
//if not equals, throws sql exception
ASSERT EQUALS "?{1}" "?{TAX.ID}"
```


### FILTER MATCHES ###
```
SELECT  SENDOFS  ENQUIRY.SELECT,,{{USER}}/{{PASS}},ENQ.UAB.CFI.CU.SRCH
	TAX.ID:EQ=set ?1
	DOC.NO:EQ=set ?2
	DATE.OF.BIRTH:EQ=set ?3
END

//evaluate customerStatusText field
customerStatusText = decode ?customer.status 1 valid 2 deleted
//keep in resultset rows with field customer.status that matches "1"
FILTER MATCHES "?{customer.status}" "1"
```


### Multiple requests in one query ###
**parameters**
|1|customerId| |
|:|:---------|:|
|2|taxId     | Customer tax number. Used if customerId is not provided. |
|3|newTarget |New target for the customer|

```
//search for the customer ID if it's not provided by first parameter
//the next ofs will be sent to server when first input parameter is empty
SENDOFS {{setIfNull ?1 TRUE FALSE}} ENQUIRY.SELECT,,{{USER}}/{{PASS}},ENQ.MW.CUST.LWEXISTS
	TAX.ID:EQ=			set ?2
END

//substitute first parameter with value CUST.ID from the resultset
//this will not be executed if previous ofs was not sent to server or returned 0 rows
?1 =  decode ?CUST.ID NULL ""

//get full customer information by customer ID
SENDOFS ENQUIRY.SELECT,,{{USER}}/{{PASS}},ENQ.UAB.CFI.CU.EXP
	CUSTOMER.ID:EQ=set ?1
END
		
//TARGET must be 6 or 20 (regexp)
ASSERT MATCHES "?{TARGET}" "^(6|20)$"
//SECTOR must be 2 (Private)
ASSERT EQUALS "?{SECTOR}" "2"

//modify customers TARGET to 6 if third input parameter is true and to 20 othrewise		
SENDOFS TRUE CUSTOMER,UAB.CFI.CU.PI.AMN/I/PROCESS,{{USER}}/{{PASS}}/,{{set ?1}}
	TARGET::            = decode ?3 true 6 20
END
```