# jans-client-api

## Introduction

jans-client-api is a middleware service which can be used by web application developers to facilitate user authentication and authorization with an external [OAuth 2.0](https://tools.ietf.org/html/rfc6749) identity provider. It includes the server which is a simple REST application designed to work over the web (via https), making it possible for many apps across many servers to leverage a central jans-client-api service for [OAuth 2.0](https://tools.ietf.org/html/rfc6749) security.

## Packaging and running the application
### Prerequisites
- A working installation of jans-auth-server
### Verify jans-auth-server necessary configuration
1. Verify configurationEntryDN in jans.properties
```
clientApi_ConfigurationEntryDN=ou=jans-client-api,ou=configuration,o=jans
```
2. Verify `clientApi_ConfigurationEntryDN` in DB configuration, if not exist execute respective insert. 

- [MySql](https://github.com/JanssenProject/jans-client-api/blob/master/server/scripts/mysql/clientApi_ConfigurationEntryDN.sql)
- [Ldap](https://github.com/JanssenProject/jans-client-api/blob/master/server/scripts/ldap/clientApi_ConfigurationEntryDN.ldif)

3. Verify file route of next parameters in `clientApi_ConfigurationEntryDN` field:`jansConfDyn` configuration json:
```
  "keyStorePath"
  "cryptProviderKeyStorePath"
  "mtlsClientKeyStorePath"
  "storageConfiguration"  
```

### Source Packaging

You can build the jans-client-api server using [Maven](https://maven.apache.org). The code is available in [Github](https://github.com/JanssenProject/jans-client-api).

Create a folder to clone ${PATH_REPOSITORY}, and clone inside.
```
cd ${PATH_REPOSITORY}
git clone https://github.com/JanssenProject/jans-client-api.git
cd jans-client-api
mvn clean install -Dmaven.test.skip=true -Dcompile.jans.base={JANS_AUTH_SERVER_CONFIG_PATH, example: /etc/jans}
```

After the built is finished `jans-client-api-server.war` is generated in `${PATH_REPOSITORY}/jans-client-api/server/target/`. 

### Jetty 11 Deploy

Download jetty 11 zip, here a link:
https://repo1.maven.org/maven2/org/eclipse/jetty/jetty-home/11.0.9/jetty-home-11.0.9.zip

Create a folder to unzip Jetty ${PATH_FOLDER_JETTY}, and unzip Jetty inside.
```
cd ${PATH_FOLDER_JETTY}
export JETTY_HOME=${PATH_FOLDER_JETTY}/jetty-home-11.0.9
mkdir jetty-base
export JETTY_BASE=${PATH_FOLDER_JETTY}/jetty-base/
cd jetty-base
java -jar $JETTY_HOME/start.jar --add-module=server,deploy,annotations,webapp,servlet,resources,http,http-forwarded,threadpool,jsp,websocket,logging/slf4j,logging-jetty
cp ${PATH_REPOSITORY}/jans-client-api/server/target/jans-client-api-server.war $JETTY_BASE/webapps/
java -jar  $JETTY_HOME/start.jar jetty.http.port=9999
```
After `jans-client-api` server is started, status can be checked using `health-check` url: http://localhost:9999/jans-client-api-server/health-check.

## Api Description

jans-client-api offers an easy API for [OAuth 2.0](https://tools.ietf.org/html/rfc6749), [OpenID Connect](http://openid.net/specs/openid-connect-core-1_0.html), and [UMA 2.0](https://docs.kantarainitiative.org/uma/wg/oauth-uma-grant-2.0-05.html).

HTTP request | Method | Description
------------ | ------------- | ------------- 
/health-check | GET | Quick check whether jans-client-api-server is alive.
/register-site | POST | Register client with jans-client-api-server.
/get-client-token | POST | Gets Client Token.
/introspect-access-token | POST | Introspect Access Token.
/update-site | POST | Updates client. If something changes in a pre-registered client, you can use this API to update your client in the OP.
/remove-site | POST | Removes site from jans-client-api-server.
/get-authorization-url | POST | Gets Authorization Url.
/get-tokens-by-code | POST | Get tokens by code.
/get-user-info | POST | Get User Info.
/get-access-token-by-refresh-token | POST | Get Access Token By Refresh Token.
/uma-rs-protect | POST | UMA RS Protect Resources.
/uma-rs-modify | POST | This end-point can be used to modify one resource at a time from whole set of UMA resources of cient.
/uma-rs-check-access | POST | UMA RS Check Access.
/introspect-rpt | POST | Introspect RPT.
/uma-rp-get-rpt | POST | UMA RP Get RPT.
/uma-rp-get-claims-gathering-url | POST | UMA RP Get Claims Gathering URL.
/get-jwks | POST | Get JSON Web Key Set.
/get-issuer | POST | Get Issuer.
/get-discovery | POST | Get OP Discovery Configuration.
/get-rp-jwks | GET | Get Rp JWKS.
/get-request-object-uri | POST | Get Request Object Uri.
/get-request-object/{request_object_id} | GET | Get Request Object.


