This is a simple jdbc driver wrapper over Temenos T24 OFS protocol.

## Info ##
We don't plan to map OFS to SQL.
We just want to wrap an OFS into jdbs.

## Idea ##
There is a very simple way to expose web service based on jdbc driver:

[WSO2 Data Services](http://wso2.org/projects/data-services-server/java)

The main target of this project is to provide a simple ability to create web services based on temenos ofs requests.


## Quick Start Web Service Based on T24 OFS Messages ##
[QuickStartWebService](QuickStartWebService.md)

## For java developers ##
This driver is not full featured jdbc driver. There is no t24 metadata (no way to take it through OFS).

Only following jdbc classes are implemented:
```
 * T24Driver
 * T24Connection
 * T24PreparedStatement
 * T24ResultSet
```

The data received through OFS is a String.

When you call `T24ResultSet.getDate()`, we just parse string in specified column according to following mask "yyyyMMdd".




## If you have questions ##
If you have any questions please contact [temenos-ofs-jdbc google group](http://groups.google.com/group/temenos-ofs-jdbc)

<wiki:gadget url="http://jubic.googlecode.com/svn/rssreader.xml" up\_feeds="http://groups.google.com/group/temenos-ofs-jdbc/feed/rss\_v2\_0\_msgs.xml" height="300" width="700" border="1" up\_title="" up\_setup="false"/>