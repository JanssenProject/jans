# Setup Developer Workspace for Janssen

This is a step-by-step guide to setup developer workspace for Janssen server on Ubuntu based system. Using this workspace, developers can implement code changes, deploy and test locally within development environment. 

- [Prerequisites](#prerequisites)
- [Get Code and Build](#get-code-and-build)
- [Configure Jetty](#configure-jetty)
- [Setup JSON Web Keys](#setup-json-web-keys)
- [Setup Persistance Store](#setup-persistance-store)
- [Configure Properties](#configure-properties)
- [Start Janssen Auth Server](#start-janssen-auth-server)

## Prerequisites

#### IDE

This guide uses IntellijIdea Java IDE to setup workspace. Other IDEs, like Eclipse, can also be used to perform steps mentioned in this guide. IDE should support Java version 11. Refer to Janssen documentation to find out current JDK version required by Janssen.

#### MySQL

Janssen needs persistance storage to store configuration and transactional data.
Janssen supports variety of persistance technologies including LDAP, RDBMS and cloud storage.
For this guide, we are going to use MySQL relational database as our persistance store. You can install MySql on Ubuntu using command below:

```
sudo apt-get install mysql-server
```

## Get Code and Build

- Open IntellijIdea and create a new project from version control using [Github repo](https://github.com/JanssenProject/jans-auth-server) for `jans-auth-server`

- To build `jans-auth-server`, create a new run configuration using Idea's `run/debug configurations` dialogue as below:
  - Create a new `maven` configuration named `jans-auth-server-parent` 
  - Give command line as `clean install` 
  - Under `Java options` make sure that JDK-11 is selected 
  - Add `skip tests` option by clicking `modify` on `java options`
  - `Save` configuration

> TODO: add image of run config dialogue

Running this configuration will build and install `jans-auth-server` project. Build will produce `jans-auth-server-1.0.0-SNAPSHOT.war` artifact as it'll be visible from build logs.

## Configure Jetty

Janssen uses Jetty to run application service. For the purpose of development, Janssen uses `jetty-maven-plugin` for quick deployment and testing. This plug-in is already a part of project dependencies. Following steps will configure Jetty Maven plug-in for Janssen deployment.

### Download Jetty configuration xml files 

Download files listed below. Files will be required by Jetty to enable ssl and other configuration:
- [jetty.xml](https://github.com/eclipse/jetty.project/blob/jetty-9.4.x/jetty-server/src/main/config/etc/jetty.xml)
- [jetty-http.xml](https://github.com/eclipse/jetty.project/blob/jetty-9.4.x/jetty-server/src/main/config/etc/jetty-http.xml)
- [jetty-ssl.xml](https://github.com/eclipse/jetty.project/blob/jetty-9.4.x/jetty-server/src/main/config/etc/jetty-ssl.xml)
- [jetty-ssl-context.xml](https://github.com/eclipse/jetty.project/blob/jetty-9.4.x/jetty-server/src/main/config/etc/jetty-ssl-context.xml)
- [jetty-https.xml](https://github.com/eclipse/jetty.project/blob/jetty-9.4.x/jetty-server/src/main/config/etc/jetty-https.xml)

(or you can get same files from a downloaded Jetty distribution from `<jetty-home>/etc`)

and put these files under

```
jans-auth-server/server/src/main/webapp-jetty/WEB-INF
```

- Update `<jettyXml>` and `<contextPath>` elements of `jetty-mave-plugin` under `jans-auth-server/server/pom.xml` as shown in section below:

  ```
  	<plugin>
				<groupId>org.eclipse.jetty</groupId>
				<artifactId>jetty-maven-plugin</artifactId>
				<version>9.4.31.v20200723</version>
				<configuration>
					<jettyXml>src/main/webapp-jetty/WEB-INF/jetty.xml,src/main/webapp-jetty/WEB-INF/jetty-http.xml,src/main/webapp-jetty/WEB-INF/jetty-ssl.xml,src/main/webapp-jetty/WEB-INF/jetty-ssl-context.xml,src/main/webapp-jetty/WEB-INF/jetty-https.xml</jettyXml>
					<webAppConfig>
						<descriptor>${project.build.directory}/${project.build.finalName}/WEB-INF/web.xml</descriptor>
						<contextPath>/jans-auth</contextPath>
					</webAppConfig>
					<webAppSourceDirectory>${project.build.directory}/${project.build.finalName}</webAppSourceDirectory>
					<scanIntervalSeconds>3</scanIntervalSeconds>
				</configuration>
			</plugin>
  ```

#### Assign Host Name

Assign Host Name to your local IP address by adding below entry in `/etc/hosts` file

```
127.0.0.1       test.local.jans.io
```

Here, `test.local.jans.io` can be any name of your choice. We will refer to `test.local.jans.io` as our host name for rest of this guide.

### Configure Jetty for HTTPS

To configure Jetty to work with HTTPS, we have to setup a certificate. We will use Java keytool to generate key pair. Use command given below after replacing value for `{password-of-choice}`:
    
```
keytool -genkeypair -alias jetty -keyalg EC -groupname secp256r1 -keypass secret -validity 3700 -storetype JKS -keystore keystore.test.local.jans.io.jks -storepass {password-of-choice}
```

Above command will create a `.jks` file in the same directory from where you have executed the command. Copy this keystore file to path:

```
jans-auth-server/server/src/main/webapp-jetty/WEB-INF
```

Update following properties in `jans-auth-server/server/src/main/webapp-jetty/WEB-INF/jetty-ssl-context.xml` after making appropriate replacements:
  
  ```
  <Set name="KeyStorePath">src/main/webapp-jetty/WEB-INF/keystore.test.local.jans.io.jks</Set>
  <Set name="KeyStorePassword">{replace with your keystore password}</Set>
  <Set name="KeyManagerPassword">{replace with your keystore password}</Set>
  <Set name="TrustStorePath">src/main/webapp-jetty/WEB-INF/keystore.test.local.jans.io.jks</Set>
  <Set name="TrustStorePassword">{replace with your keystore password}</Set>
  ```

## Setup JSON Web Keys

- Janssen source comes with keys that are required for running tests. Add these keys to keystore.

   ```
   keytool -importkeystore -srckeystore <auth-server-code-dir>/server/profiles/default/client_keystore.jks -destkeystore keystore.test.local.jans.io.jks
   ```

- Generate JWT

   - To generate JWT, we will use a utility Jar file. Download this file from [here](https://maven.jans.io/maven/io/jans/jans-auth-client/1.0.0-SNAPSHOT/)
   - now run command as given below. This command adds additional keys in `keystore.test.local.jans.io.jks` and creates a JSON file with web keys. We will use web keys files later to update in our persistence store.
     ```
	   java -Dlog4j.defaultInitOverride=true -cp /tmp/jans-auth-client-1.0.0-SNAPSHOT-jar-with-dependencies.jar io.jans.as.client.util.KeyGenerator -keystore './keystore.test.local.jans.io.jks' -keypasswd secret -sig_keys RS256 RS384 RS512 ES256 ES384 ES512 -enc_keys RS256 RS384 RS512 ES256 ES384 ES512 -dnname 'CN=Jans Auth CA Certificates' -expiration 365 > /tmp/keys/keys_client_keystore.json
     ```
- Move `keystore.test.local.jans.io.jks` file created above to `/etc/certs` and rename it to `jans-auth-keys.jks`. This file will be used at a later in this guide when we point Janssen server to look for certificates under `/etc/certs`.

## Setup Persistance Store

Janssen uses persistance storage to hold configuration and transactional data. 
Janssen supports variety of persistance mechanisms including LDAP, RDBMS and cloud storage.
For this guide, we are going to use MySQL relational database as a persistance store. 

As a first step, let's create a schema and a user for Janssen server.

- Log into MySQL

  ```
  sudo mysql
  ```
  
- Create new schema `jansdb`

  ```
  mysql> CREATE DATABASE jansdb;
  ```
  
- Create new db user `jans`

  ```
  CREATE USER 'jans'@'localhost' IDENTIFIED BY 'PassOfYourChoice';
  ```
  
- Grant privileges 

  ```
  GRANT ALL PRIVILEGES ON jansdb.* TO 'jans'@'localhost';
  ```
  
- Exit MySQL login 

### Load Initial Data Set

We will load basic configuration and test data into MySQL using a data import script. Janssen modules require configuration data
 at the time of start up and test data is needed to run integration tests. 

```
TODO:
Add link to script below. This script is essentially export of entire Janssen schema including test data.
Generation of this script needs to be automated. One way to do this is to have Jenkins build create 
this data dumpscript after every successful installtion on integration servers. Once this script is 
generated, it has to be made available via a link (hosted servers or stored in GH repo). Similar 
mechanism has be to established for other persistence types like LDAP, Spanner etc. 
```

[Download](TODO add link here) data import script. This script is a generic script and we have to edit certain values as per our local setup as described in steps below.

#### Update hostname
We need to replace generic host name in the script with the one that we have set for our local environment. Which is `test.local.jans.io`. 
To do this, open script in a text editor and perform following replacements.
        
  -   replace string `https://testmysql.dd.jans.io` with `https://test.local.jans.io:8443` 
  
  -   replace string `testmysql.dd.jans.io` with `test.local.jans.io`

> TODO: Replace 'testmysql.dd.jans.io' above with actual host name from final script


#### Update keystore secret in database config

- Search for string `keyStoreSecret` in the script and replace corresponding value with secret you used while creating keystore in [configuration](#configure-jetty-for-https) step.

#### Import data

Use data load script to populate `jansdb` schema

  ```
  sudo mysql -u root -p jansdb < jansdb_dump.sql;
  ```

#### Update JSON Web keys in database config

Now we need to update JSON web keys in DB with what we have generated during [Setup JSON Web Keys](#setup-json-web-keys).

- login to mysql with user `jans`

  ```
  sudo mysql -u jans -p jansdb
  ```
  
- Run the update query as below after replacing `JWKs content` with multiline content from `/tmp/keys/keys_client_keystore.json`:

  ```
  UPDATE jansdb.jansAppConf SET jansConfWebKeys = 'JWKs content' where doc_id = "jans-auth";
  ```

At this point, database is ready to support Janssen server.

## Configure Properties

Now that we have configured Jetty plug-in and persistent store, we need to update properties files in our code base so that server can connect to persistent store.

Under `jans-auth-server/server/conf`, we have two property files:
  - `jans.properties`: Properties like type of persistance to use, localtion of certificates etc.
  - `jans-sql.properties`: Properties for SQL persistence store
    
- Update `jans.properties`
  -   edit `persistence.type` to `persistence.type= sql` since we are using MySQL as our backend
  -   edit `certsDir` to `certsDir=/etc/certs`
  -   edit value of `jansAuth_ConfigurationEntryDN` to `jansAuth_ConfigurationEntryDN=ou=jans-auth,ou=configuration,o=jans` 

- Update `jans-sql.properties`
  - Set `db.schema.name` to `jansdb` as our MySql schema
  - Set `connection.uri` to `jdbc:mysql://localhost:3306/jansdb`
  - Set `connection.driver-property.serverTimezone` to `UTC`
  - Set `auth.userName` to `jans` as database user
  - Set `auth.userPassword` to passwod for `jans` db user
  - Set `password.encryption.method` to method you have selected to encrypt the password for `userPassword` property. For developer setup, since we are using plain text password, comment out this property.
    
  Other properties from `jans-sql.properies` file can be set to standard values as given below.

  ```
  # Connection pool size
  connection.pool.max-total=40
  connection.pool.max-idle=15
  connection.pool.min-idle=5

  # Max time needed to create connection pool in milliseconds
  connection.pool.create-max-wait-time-millis=20000

  # Max wait 20 seconds
  connection.pool.max-wait-time-millis=20000

  # Allow to evict connection in pool after 30 minutes
  connection.pool.min-evictable-idle-time-millis=180000
  ```

## Start Janssen Auth Server

Now we are ready to run Janssen server. You can run Janssen auth server via maven command or by creating run configuration in IntellijIdea.

### Using Maven

Run below maven command from root of `server` module

```
mvn -DskipTests -Djans.base=./target -Dlog.base=/home/dhaval/temp/logs/jans-logs jetty:run-war
```

### Using Idea run configuration

- Open IntellijIdea's `run/debug configurations` dialogue and create a copy of previously created run configuration `jans-auth-server-parent`. And update following: 
  - change working directory to `jans-auth-server` by selecting `jans-auth-server/server`
  - change the `command line` arguments to `jetty:run-war`
  - Add following `VM arguments` to `java options`
  
   ```
   -Djans.base=./target -Dlog.base=<dir-for-logs>
   ```

You can varify if the server is up by accessing: 

```
https://test.local.jans.io:8443/jans-auth/.well-known/openid-configuration
```


## Next Steps:
  - [Run unit and integration tests](https://gist.github.com/ossdhaval/f2ca2590cdbe0c11db5d58f87e13479f)
  - [Setup Debugging](https://gist.github.com/ossdhaval/11df8be8ebf9063b2ba18097efb040f9)
  - [Setup IntellijIdea](https://gist.github.com/ossdhaval/36e219c350e1120b31f803695a22e30d)
