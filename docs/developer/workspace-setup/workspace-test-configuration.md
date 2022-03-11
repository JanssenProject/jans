# Workspace setup for running Janssen server Tests

This guide will help you configure your Janssen workspace to run unit and integration tests. 

To successfully execute Janssen integration tests, you need a Janssen server up and running. Make sure you have followed [Janssen workspace setup guide](setup-developer-workspace.md) and you can start Janssen server from your workspace. 

- [Update Java CA Certificates](#update-java-ca-certificates)
- [Setup Test Profiles](#setup-test-profiles)

For the purpose of this guide, we will refer to local workspace code location as `auth-server-code-dir` and the host on which the Janssen server is running will be refered to as `test.local.jans.io`.

## Update Java CA Certificates 

Certificates are required in order to run some of the integration tests, for example, tests in `client` module.

- extract certificate 
  
  ```
  openssl s_client -connect test.local.jans.io:8443 2>&1 |sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > /tmp/httpd.crt
  ```
  Note: This command takes few seconds to return.
  
- Update cacerts of your JRE using command below. We are assuming the JRE being used is Amazon Corretto and it is installed at default path `/usr/lib/jvm/java-11-amazon-corretto`. This command will prompt for cert store password. Unless password has been changed, by default it is `changeit`.
  
  ```
  sudo keytool -import -alias jetty -keystore /usr/lib/jvm/java-11-amazon-corretto/lib/security/cacerts -file /tmp/httpd.crt
  ``` 

## Setup Test Profiles

In this step we will give all the essential information about target Janssen server to our local workspace . This is configured in form of `profile`. Steps below will help us create a profile in our local code workspace. 

### Create Profile for client module
- Under `<auth-server-code-dir>/client/profiles/` create directory named `test.local.jans.io`
- copy contents of `default` profile to new profile directory 
- Update following values as per local setup:
  - In file `config-oxauth-test-data.properties` update values of properties below as shown and leave other properties unchanged.
    ```
    test.server.name=test.local.jans.io:8443
    swd.resource=acct:test_user@test.local.jans.io:8443
    clientKeyStoreFile=profiles/test.local.jans.io/client_keystore.jks
    clientKeyStoreSecret=secret
    ```

- Edit Java files 
  
  > TODO: This is a hack and will be removed after appropriate configuration. For now we are hard-coding `/jans-auth` context in the code.
  
   - Edit `client/src/test/java/io/jans/as/client/BaseTest.java` where url `/.well-known/openid-configuration` is noted and add `jans-auth` to it so that it becomes `"/jans-auth/.well-known/openid-configuration"`
   - Similarly edit `client/src/main/java/io/jans/as/client/OpenIdConnectDiscoveryClient.java` file where URL `"/.well-known/webfinger"` is mentioned and make it `"/jans-auth/.well-known/webfinger"`


### Create Profile for server module
- Under `<auth-server-code-dir>/server/profiles/` create directory named `test.local.jans.io`
- copy contents of `default` profile to new profile directory 
- Edit `config-oxauth.properties`
  - Update values of properties as below:
  
    ```
    server.name=test.local.jans.io:8443
    config.jans-auth.issuer=https://test.local.jans.io:8443
    config.jans-auth.contextPath=https://test.local.jans.io:8443/jans-auth
    ```
    
    > TODO: values of these properties should not be just server name in a dev setup, it should contain port number as well. Also for property config.jans-auth.contextPath, it should have /jans-auth after server and port as context
    
  - If you are using MySql as Janssen server backend, then comment out the properties for LDAP
 
- Edit `config-oxauth-test.properties`
  - Add or update values of properties as below:
    ```
    server.name=test.local.jans.io:8443
    config.oxauth.issuer=https://test.local.jans.io:8443
    config.oxauth.contextPath=https://test.local.jans.io:8443
    config.persistence.type=sql
    config.sql.connection.driver-property.serverTimezone=UTC
    config.sql.db.schema.name=<your schema>
    config.sql.connection.uri=jdbc:mysql://<your host>:3306/<your schema>
    config.sql.auth.userName=<db username>
    config.sql.auth.userPassword=<db plain text password>
    config.sql.password.encryption.method=<comment out this property>
    ```
  - Add following properties to same file with no changes to values:

    ```
    # Connection pool size
    config.sql.connection.pool.max-total=20
    config.sql.connection.pool.max-idle=10
    config.sql.connection.pool.min-idle=5

    # Max time needed to create connection pool in milliseconds
    config.sql.connection.pool.create-max-wait-time-millis=20000

    # Max wait 20 seconds
    config.sql.connection.pool.max-wait-time-millis=20000

    # Allow to evict connection in pool after 30 minutes
    config.sql.connection.pool.min-evictable-idle-time-millis=1800000
    ```

- Now you can run test cases for each module with
   
   ```
   mvn -Dcfg=test.dd.jans.io -fae -Dcvss-score=9 -Dfindbugs.skip=true -Dlog4j.default.log.level=TRACE -Ddependency.check=false clean test
   ```


## Steps:
  - [Setup workspace](https://gist.github.com/ossdhaval/c0c82e437dcb5d5403f241e81908ec4c)	
  - [Run unit and integration tests](https://gist.github.com/ossdhaval/f2ca2590cdbe0c11db5d58f87e13479f)
  - [Setup Debugging](https://gist.github.com/ossdhaval/11df8be8ebf9063b2ba18097efb040f9)
  - [Setup IntellijIdea](https://gist.github.com/ossdhaval/36e219c350e1120b31f803695a22e30d)
