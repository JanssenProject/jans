# Developing for Janssen Project

## Remote Debugging

Janssen Server modules run as a Java process. Hence, like any other Java process the module
JVMs can be configured to open a debug port where a remote debugger can be attached. The steps below will show how to 
configure `auth-server` module for remote debugging.

1. Pass the command-line options to the JVM

   On the Janssen Server host, open the service config file `/etc/default/jans-auth` and add the following JVM 
   parameters to as `JAVA_OPTIONS`
    ```
    -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=6001
    ```
   This will open port 6001 for the remote debugger. Any other port can also be used based on availability.

2. Restart `jans-auth` services
    ```
    systemctl restart jans-auth.service
    ```

3. Check if the port is open and accessible from within the Janssen Server host
   Use the `jdb` tool from JDK to test if the JVM port has been opened
   ```
   ./<path-to-JDK>/bin/jdb -attach 6001
   ```
   if the port is open, it'll give you output like the below:
   ```
   Set uncaught java.lang.Throwable
   Set deferred uncaught java.lang.Throwable
   Initializing jdb ...
   >
   ```
   press ctrl+c to come out of it.

4. Ensure that the port is accessible from outside the host VM as well and firewalls are configured accordingly

5. Connect to the remote port on the Janssen Server host from the Janssen workspace. Use any IDE (Intellij, Eclipse, 
   etc.) to create and run a remote debugging profile providing IP and debug port of the Janssen Server host.

   For IntelliJIdea, create a debug configuration as below:

   ![](../assets/image-jans-remote-debug-intellij.png)

## Run Integration Tests with a Janssen Server VM

In this guide, we will look at steps to run the Janssen integration test suite against a locally installed Janssen 
server on the developer machine.

### Component Setup

![Component Diagram](../assets/image-run-integration-test-from-workspace-06122022.png)

### Install Janssen Server

Install the Janssen server using one of the methods described in installation guide. Note the points below when
following installation instructions.

- Make a note 
of the `host name` that you assign to the Janssen server during the installation. For this guide, the Janssen host name 
would be `janssen2.op.io`
- Install with test data load. This can be achieved by using `-t` switch when invoking the setup script.

Use the [VM installation guide](../admin/install/vm-install/README.md) for complete set of instructions.

Once the installation is successfully complete, check if the `.well-known` end-points (sample below) of the 
Janssen server from the browser to ascertain that the 
Janssen server running inside local VM is healthy and also accessible from the developer's machine. 

!!! Note
    Based on developer setup it may be necessary to add appropriate IP-HOST mapping to the developer workstation. For
    instance, on a Linux based developer workstation, this means adding a mapping to `/etc/hosts` file. Make sure that 
    VM's IP is mapped to a FQDN like `janssen2.op.io`. Refering to VM with `localhost` or just IP will not work.

  ```
  https://janssen2.op.io/jans-auth/.well-known/openid-configuration
  ```

Response received should be JSON formatted Janssen configuration details, similar to those below.

  ```
  {
  "request_parameter_supported" : true,
  "pushed_authorization_request_endpoint" : "https://janssen2.op.io/jans-auth/restv1/par",
  "introspection_endpoint" : "https://janssen2.op.io/jans-auth/restv1/introspection",
  "claims_parameter_supported" : false,
  "issuer" : "https://janssen2.op.io",
  "userinfo_encryption_enc_values_supported" : [ "RSA1_5", "RSA-OAEP", "A128KW", "A256KW" ],
  "id_token_encryption_enc_values_supported" : [ "A128CBC+HS256", "A256CBC+HS512", "A128GCM", "A256GCM" ],
  "authorization_endpoint" : "https://janssen2.op.io/jans-auth/restv1/authorize",
  "service_documentation" : "http://jans.org/docs",
  "authorization_encryption_alg_values_supported" : [ "RSA1_5", "RSA-OAEP", "A128KW", "A256KW" ],
  "id_generation_endpoint" : "https://janssen2.op.io/jans-auth/restv1/id",
  "claims_supported" : [ "street_address", "country", "zoneinfo", "birthdate", "role", "gender", "user_name", "formatted", "phone_mobile_number", "preferred_username", "inum", "locale", "updated_at", "post_office_box", "nickname", "preferred_language", "email", "website", "email_verified", "profile", "locality", "room_number", "phone_number_verified", "given_name", "middle_name", "picture", "name", "phone_number", "postal_code", "region", "family_name", "jansAdminUIRole" ],
  "scope_to_claims_mapping" : [ {
    "user_name" : [ "user_name" ]
  }, {
    "https://jans.io/scim/users.write" : [ ]
  }, {
    "https://jans.io/scim/groups.read" : [ ]
  }, {
    "https://jans.io/scim/all-resources.search" : [ ]
  }, {
    "https://jans.io/scim/fido.write" : [ ]
  }, {
    "https://jans.io/scim/groups.write" : [ ]
  }, {
    "https://jans.io/scim/fido2.read" : [ ]
  }, {
    "https://jans.io/scim/fido.read" : [ ]
  }, {
    "https://jans.io/scim/fido2.write" : [ ]
    
  ```

### Configure developer workspace

Now that we have Janssen Server running in a VM and it is accessible from developer workstation as well, we will
create and configure the developer workspace in order to run integration tests.

We are going to configure the developer workspace in IntelliJIdea IDE. Developer can use any IDE for this purpose. 

Using an IDE, get Janssen server code from 
[Janssen GitHub repository](https://github.com/JanssenProject/jans).

Janssen Server is composed of multiple modules. Each module have it's own set of tests. 
Below are the instructions for configuring each module for tests.

#### Configuring the server and the client modules

##### setup certificate

- Update cacerts

  extract certificate for Janssen server with name `janssen2.op.io`

  ```
  openssl s_client -connect test.local.jans.io:443 2>&1 |sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > /tmp/httpd.crt
  ```
  this command takes few seconds to return.

Update cacerts of your JRE which is being used by the code workspace. For example,  
`/usr/lib/jvm/java-11-amazon-corretto`. When running the command below, it will prompt for cert store password. Provide
the correct password. The default password is `changeit`.

  ```
  keytool -import -alias janssen2.op.io -keystore /usr/lib/jvm/java-11-amazon-corretto/lib/security/cacerts -file /tmp/httpd.crt
  ``` 

##### Profile setup

Since Janssen Server has been installed with test data load, it also creates profile files required to run the test. 
These files are kept on the VM under `/opt/jans/jans-setup/output/test/jans-auth` directory. Copy this directory to
developer workstation.

Follow the steps below to configure the profile for the client module. Same steps should be followed for
setting up profile for server module.

1. Under `jans-auth-server/client/profile` module, create a new directory and name it as `janssen2.op.io`
2. Copy the contents of `jans-auth/client` directory in the newly created `janssen2.op.io` directory
3. Copy keystore file `/client/profiles/default/client_keystore.p12` from `default` directory to 
   the `janssen2.op.io` directory

#### Configuring the Agama Module

Agama module code resides under `jans/agama` directory.

Follow the steps below from `agama` directory to configure the module to run the integration tests.

1. Remove existing profile if any by deleting and recreating the directory `engine/profiles/janssen2.op.io`
2. Copy the file `jans-auth/config-agama-test.properties` to the `engine/profiles/janssen2.op.io/` directory

Once above steps have been followed, the local copy of `jans-auth` directory that was copied
from `janssen2.op.io` can be deleted.

### Running The Tests

Each module in Janssen Server has it's own tests that has to be executed separately.
For example, to run integration tests for `jans-auth-server` module, run the following maven command at the directory
level:

  ```
  mvn -Dcfg=janssen2.op.io test
  ```

