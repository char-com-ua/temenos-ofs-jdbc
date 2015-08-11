## Introduction ##
The purpose of this guide is to get you started on creating and invoking a web service using WSO2 Data Service solution based on standard T24 OFS messages.

## Prerequisites ##
  * java 1.5 or 1.6
  * [temenos-ofs-jdbc library](http://code.google.com/p/temenos-ofs-jdbc/downloads/list) (take last featured version)
  * [wso2 data services server](http://wso2.org/projects/data-services-server/java) (wso2dataservices-2.2.0.zip in my case)
  * [TCClient libraries](TCClientLibraryList.md)

## Installation ##

**Extract WSO2 Data Services Server to**
```
/wso2/wso2dataservices-2.2
```

**Put the following [TCClient libraries](TCClientLibraryList.md) into**
```
/wso2/wso2dataservices-2.2/repository/components/lib
```

**Put temenos-ofs-jdbc library: t24jdbc.jar into**
```
/wso2/wso2dataservices-2.2/repository/components/lib
```

**Create [channels.xml](ChannelsXml.md) file in**
```
/wso2/wso2dataservices-2.2/conf
```

Specify correct values for host and port where your Temenos Connector Server is located.

Activate TCP listener on your TCServer.

**Create file tcclientlog.properties in**
```
/wso2/wso2dataservices-2.2/conf
```

tcclientlog.properties content:
```
#####################################
# Log4j file configuration
# Log Level = OFF, FATAL, ERROR, WARN, INFO, DEBUG
#####################################

#log4j.logger.common=INFO, file
log4j.logger.tcc=INFO, tccfile
log4j.logger.channels=INFO, tccfile
log4j.logger.common=INFO, file

log4j.appender.tccfile=org.apache.log4j.RollingFileAppender
log4j.appender.tccfile.layout=org.apache.log4j.PatternLayout
log4j.appender.tccfile.layout.ConversionPattern=%d [%t] %-5p %c %x - %m%n
log4j.appender.tccfile.File=logs/tcclient.log
log4j.appender.tccfile.MaxFileSize=1024KB
log4j.appender.tccfile.MaxBackupIndex=10
```

## Start WSO2 DS ##

**Start WSO2 DS Server**

start wso2server.bat (on windows) wso2server.sh (on linux) from
```
/wso2/wso2dataservices-2.2/bin
```

**Login to WSO2 DS Server**

Open in your browser address https://localhost:9443/carbon

Use admin/admin as default login/password to access admin console.


## Create T24 Web Service ##
**Create T24 Web Service configuration**

Create file [T24Test.dbs](Wso2DataServiceExample.md) somewhere on your local drive

For example in /wso2 directory.

Specify in this file Login and Password to the file T24 system.

**Upload `T24Test.dbs`**

Upload `T24Test.dbs` file through WSO2 DS Server admin console

Wait 10 seconds, then click on Service\List menu in admin console.

If your Temenos Connector Server (TCServer) is available at this moment, then you will see new web service in the list: `T24Test`.

If ofs-jdbc driver can't establish connection to TCServer, then you will see `Faulty Service Group` link on the top of the service list.

Click on this link to see detailed error.

Check `/wso2/wso2dataservices-2.2/logs/tcclient.log` file for more debug information from TCClient.

**Test your new web service**

If your service successfully published, you will see a link `Try this service` near it.

Click on it, and you will see a simple web interface to call T24 functionality through web service.

## External tools to test web services ##

[SoapUI](http://www.soapui.org/) (even free version) it's a powerful tool to call/test web services.




---
