# Run Janssen integration tests on developer machine

In this guide, we will look at steps to run the Janssen integration test suite against a locally installed Janssen server on developer machine.

## Table of Content

- Component setup
- Configure the Janssen server for tests
- Configure developer workspace
- Run tests

## Component Setup

![Component Diagram](../assets/developer/images/image-run-integration-test-from-workspace-06122022.png)


To run Janssen integration test suite, we need a Janssen server installed. Janssen server can be installed on many different platforms, and steps mentioned in this guide are applicable to most of these platforms with few platform specific tweaks that may be required. 

- OS platform: 
  
  It is recommended to use virtualization tools to bring up an Ubuntu 20.04 system. In this guide, we will assume that Janssen server has been intalled on an Ubuntu VM created using VMWare Player. Using a local VM to install the Janssen server is beneficial as it allows an easy way to start over by recreating the entire VM and get a blank slate to work on.
  
- Install Janssen: 

  Install the Janssen server using one of the methods described in [this](https://github.com/JanssenProject/jans/wiki#janssen-installation) guide. Make a note of the `host name` that you assign to the Janssen server during the installation. For the purpose of this guide, the Janssen host name would be `janssen2.op.io`

## Configure the Janssen server for tests

Once Janssen server up and running in a local VM, follow the steps below:
- Add `<IP> <host name>` pair to the developer machine's host name resolution file (i.e for Windows it would be `c:\windows\system32\drivers\etc\hosts` and for Ubuntu it would be `/etc/hosts`). `<host name>` is name of the Janssen host as assigned during installation.
- Now access the `.well-known` end-points of the Janssen server from browser to check accesssibility

  ```
  https://janssen2.op.io/jans-auth/.well-known/openid-configuration
  ```
  
  Response received should be a JSON formatted Janssen configuration details, similar to below.
  
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
  
  This acertains that the Janssen server running inside local VM is healthy and also accessible from developer's machine.

## Configure developer workspace

We are going to configure developer workspace in IntelliJIdea IDE. Using IDE, get Janssen server code from [Janssen GitHub repository](https://github.com/JanssenProject/jans). 

Janssen Server is composed of multiple modules. Below are the instructions for configuring each module for tests.

### Auth-server client module

#### setup certificate

- Update your Java cacerts 

This step is required in order to run tests from `client` module

  - extract certificate for Janssen server with name `janssen2.op.io`
  
    ```
    openssl s_client -connect test.local.jans.io:443 2>&1 |sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > /tmp/httpd.crt
    ```
    this command takes few seconds to return.
  
  - Update cacerts of your JRE which is being used by code workspace. For example if JRE being used my maven is `/usr/lib/jvm/java-11-amazon-corretto`. It will prompt for cert store password. Default is `changeit`.
  
    ```
    keytool -import -alias janssen2.op.io -keystore /usr/lib/jvm/java-11-amazon-corretto/lib/security/cacerts -file /tmp/httpd.crt
    ``` 

#### Workspace setup

Follow the steps below to configure workspace and run tests for client module.

- Under `jans-auth-server/client/profile` module, make a copy of default profile directory and name the new profile as `janssen2.op.io`
- Under `janssen2.op.io` directory, Edit `config-oxauth-test-data.properties` file and update the host name in the value of three properties: 
  - `test.server.name=<old-host>:8443` -> `test.server.name=janssen2.op.io` (Remember to remove the port)
  - `swd.resource=acct:test_user@<old-host>:8443` -> `swd.resource=acct:test_user@janssen2.op.io` (Remember to remove the port)
  - `clientKeyStoreFile=profiles/<old-profile-name>/client_keystore.p12` -> `clientKeyStoreFile=profiles/janssen2.op.io/client_keystore.p12`
- now at, `jans-auth-server/client` directory level, run following maven command

  ```
  mvn -Dcfg=janssen2.op.io -Dcvss-score=9 -Dfindbugs.skip=true -Dlog4j.default.log.level=TRACE -Ddependency.check=false -DskipTests clean install
  ```
  
  this will create new artifacts under `client/target` as per mentioned in profile `janssen2.op.io`
  
- Now run client tests by creating intellij run config as below
