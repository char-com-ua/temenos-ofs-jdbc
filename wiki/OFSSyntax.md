## OFS Message Syntax ##
OFS Message is one line message.

It consists of the OFS header (must present), and DATA - additional parameters.

Here are basic parts of the OFS message:

`APPLICATION,VERSION/COMMAND/SUBCOMMAND,USER/PASSWORD,ID,DATA `


**where**
| **part** | **meaning** |
|:---------|:------------|
| APPLICATION | T24 application (ex: CUSTOMER). This is something like a table in database |
| VERSION  | Application version. This is like a stored procedure through which data goes into defined APPLICATION |
| COMMAND  | I = INPUT, A = AUTHORIZE, S = SEE, D = DELETE, R = REVERS, V = VERIFY |
| SUBCOMMAND | PROCESS = process, VERIFY = verify only |
| USER     | login for Т24 |
| PASSWORD | password    |
| ID       | ID of the row of application |
| DATA     | Data. If INPUT then format: COLUMN:X:Y=VALUE, where X - multivalue, Y - subvalue. So one column could have several values. For example CUSTOMER.NAME in English and in Local language: `NAME:1:="DMITRY",NAME:2:="ДМИТРО"` |

**example:**

`UA.NB.KL.K013,UA.NB.KL.K013.IMPORT/I/PROCESS,USER/PASS,1.,TXT::="Bo oo"`

**==>**

Set field TXT:1:1 (1 if multivalue/subvalue not specified)

to value "Bo oo" (without quotation)

in the application UA.NB.KL.K013

through the version UA.NB.KL.K013.IMPORT

## Special Symbols ##

In OFS with default settings some symbols are reserved and should be replaced with other symbols.

**T24 symbol substitution**
| **symbol** | **replaced by** |
|:-----------|:----------------|
| | '_'_|
| ,          | ?               |
| '          | @               |
| "          | |               |
| (          | {               |
| )          | }               |
| /          | ^               |