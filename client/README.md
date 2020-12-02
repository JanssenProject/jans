SCIM-Client
===========

A Java client for consumption of Jans-SCIM endpoints.

# How to start

If you use maven, add the following to your pom.xml:

```
<properties>
	<scim.client.version>5.0.0-SNAPSHOT</scim.client.version>
</properties>
...
<repositories>
  <repository>
    <id>gluu</id>
    <name>Janssen repository</name>
    <url>https://maven.jans.io/maven</url>
  </repository>
</repositories>
...
<dependency>
  <groupId>io.jans</groupId>
  <artifactId>jans-scim-client</artifactId>
  <version>${scim.client.version}</version>
</dependency>
```

Alternatively you can grab the library jar from [here](https://maven.jans.io/maven/io/jans/jans-scim-client/) and manually add all other dependant jars. 

## Sample code

TODO
