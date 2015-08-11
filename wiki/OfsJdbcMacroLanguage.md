## introduction ##

[OFS](OFSSyntax.md) is a specific one line text string that gives a command to temenos t24 system to modify or select data.

Ofs-jdbc macro language  is a set of simple expressions that helps you to build your OFS query.

It was created to be readable, simple, and fast (I hope).

## ofs-jdbc macro language syntax ##

### common rules ###
  * one command -- one line
  * //this is a one line comment. you can comment only whole line.


### systax ###
```
SELECT 
	//comment line
	SENDOFS [TRUE|FALSE] <OFS_HEADER>
		//ofs parameters definition
		<OFS_COLUMN> = <EXPRESSION>
		...
	END
	...

	<RESULTSET_COLUMN> = <EXPRESSION_WITH_RESULTSET_AS_PARAM>
	...
	<?PARAMETER_INDEX> = <EXPRESSION_WITH_RESULTSET_AS_PARAM>
	...
```


### keywords ###
| **keyword** | **meaning** |
|:------------|:------------|
| SELECT      | means nothing just a fake keyword for tools that detects select query by SELECT keyword |
| SENDOFS     | declares the beginning of the ofs query to send to server. the whole this line processed with special `<`INLINE\_MACRO`>` |
| TRUE or FALSE | optional keyword that could cancel (in case of FALSE) sending ofs message to server  |
| END         | end of ofs definition. at this moment ofs query sent to server. must correspont to each SENDOFS. |


### `<`definitions`>` ###
| **keyword** | **meaning** |
|:------------|:------------|
| `<`OFS\_HEADER`>` | The OFS header (see [OFSSyntax](OFSSyntax.md)). could contain `<`INLINE\_MACRO`>` |
| `<`INLINE\_MACRO`>` | ofs-jdbc expression quoted with {{ }} . example: {{setIfNull ?1 0}} |
| `<`OFS\_COLUMN`>` | the name of the ofs query parameter. |
| `<`EXPRESSION`>` | ofs-jdbc expression. |
| `<`RESULTSET\_COLUMN`>`  | column that will be calculated based on each row and added into last result set |
| `<`?PARAMETER\_INDEX`>` | parameter intex started with ? . this will redefine input parameter based on last row of the last resultset. |
| `<`EXPRESSION**`>`**| ofs-jdbc expression. see below. |


### expressions ###
each expression started with function and continues with parameters.

function and parameters are separated with spaces

REF-PARAM : it's a reference to the query input parameter.
It starts with question mark "?" and continues with 1-based input parameter index.

REF-COLUMN : it's a reference to the resultset column.
It starts with question mark "?" and continues with name of the column.

When expression located inside SENDOFS+END operation:
  * then you could reference only to input parameters.

When expression located outside SENDOFS+END operation:
  * you could reference only to resultset columns in expression
  * there is a KEY on the left of the expression (before =)
    * KEY could contain column name calculated based on other resultset columns (for each row)
    * or KEY is an index of the input parameter ( ex: ?3 = const 1 ) when you want to redefine input parameter for the next OFS based on last row of resultset columns.

**functions**
| **function** | **parameters** | **description** |
|:-------------|:---------------|:----------------|
| const        | literal value  | returns literal value specified after const function |
| set          | REF            | returns value of the REF-erenced parameter or column. see explanation above. |
| decode       | REF  REPLACE\_WHAT REPLACE\_WITH REPLACE\_WHAT2 REPLACE\_WITH2 ... [DEFAULT](DEFAULT.md) | It's like an oracle sql function decode. it takes REF parameter and replaces it's value according to REPLACE\_WHAT REPLACE\_WITH pairs. If there is unpaired parameter DEFAULT, then it will be used in case when value was not replaced. |
| toCent       | REF            | returns value of the referenced column multiplied by 100. expected that REF parameter is a number. |
| fromCent     | REF            | returns value of the referenced column divided by 100. expected that REF parameter is a number. |
| split        | REF LENGTH     | splits the string into tokens by LENGTH. This function replaces a special character '**' in the KEY (on the left of =) with token index.**|
| substr       | REF START LENGTH | returns LENGTH symbols of the REF-erenced parameter beginning ftom START character. if LENGTH = -1 then means end of the string. |
| setIfNull    | REF NEWVALUE   | returns NEWVALUE if REF parameter is null or empty. |
| USER         |                | returns user name defined as jdbc connection parameter |
| PASS         |                | returns password defined as jdbc connection parameter |

**examples**

Lets assume that first parameter ( ?1 ) of the query equals ABC

```
A=decode ?1 ABC XXX YYY
```
result: A=XXX


```
A=decode ?1 AAA XXX YYY
```
result: A=YYY


```
A=decode ?1 AAA XXX
```
result: ABC


```
A=split ?1 1
```
result: A=C


```
//normally this used for OFS
A:*:=split ?1 1
```

result: A:1:=A, A:2:=B, A:3:=C