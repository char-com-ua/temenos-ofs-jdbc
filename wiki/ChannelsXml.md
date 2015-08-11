## Introduction ##
The channels.xml file describes T24 TCServer locations.

You should use this file to configure T24 TCClient API.

This file statically loaded during T24 libraries initialisation.

Here is a simple channel.xml example:
```
<?xml version="1.0" ?>
<CHANNELS>
	<CHANNEL>
		<NAME>MYCHANNEL</NAME>
		<TIMEOUT>60</TIMEOUT>
		<ADAPTER>
			<TYPE>tcp</TYPE>
			<PORT>8004</PORT>
			<SUPPLIER>
				<INITIATOR>
					<HOSTNAME>my-t24-host</HOSTNAME>
				</INITIATOR>
			</SUPPLIER>
		</ADAPTER>
	</CHANNEL>
</CHANNELS>
```

After this you could reffer `MYCHANNEL` in your java code and use it in jdbc url:
```
String url="jdbc:org:t24:MYCHANNEL";
```

## channels.xml file location ##
If you set "tc.home" java system property, then channel.xml will be loaded from this location:

```
String channelsLocation = System.getProperty("tc.home") + "conf/channel.xml" ;
```

If you connecting T24 through temenos-ofs-jdbc (t24jdbc.jar) and "tc.home" was not set,
then "tc.home" will be set to current directory.