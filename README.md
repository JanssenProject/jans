# jans-client-api

## Introduction

jans-client-api is a middleware service which can be used by web application developers to facilitate user authentication and authorization with an external [OAuth 2.0](https://tools.ietf.org/html/rfc6749) identity provider. It includes the server which is a simple REST application designed to work over the web (via https), making it possible for many apps across many servers to leverage a central jans-client-api service for [OAuth 2.0](https://tools.ietf.org/html/rfc6749) security.

## Installation

### Source Install

If you're a Java geek, you can build the jans-client-api server using Maven. The code is available in [Github](https://github.com/JanssenProject/jans-client-api).

```
git clone https://github.com/JanssenProject/jans-client-api.git
cd jans-client-api
mvn clean install -X
```

After the built is finished `jans-client-api-server-distribution.zip` is generated in `${JANS_CLIENT_API_HOME}/server/target/`. 

### Maven Repository

The distribution zip can be directly downloaded from maven repository `https://maven.jans.io/maven/io/jans/jans-client-api-server/<version>/jans-client-api-server-<version>-distribution.zip`.

#### To run jans-client-api-server:

1. Create a new directory ($JANS_CLIENT_API_SERVER_HOME) with appropriate name and unzip the downloaded `jans-client-api-server-<version>-distribution.zip` into it.

1. Change directory to `$JANS_CLIENT_API_SERVER_HOME/conf` folder and edit client-api-server.yml file to make necessary configuration changes (like setting correct absolute path of `client-api-server.keystore` in keyStorePath property etc.)

1. Now go to $JANS_CLIENT_API_SERVER_HOME/bin folder and start server using below command.

Windows:

```
client-api-start.bat
```

Linux:

```
sh oxd-start.sh
```

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

## Swagger

jans-client-api has defined swagger specification [here](https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans-client-api/master/server/src/main/resources/swagger.yaml). It is possible to generated native library in your favorite language by [Swagger Code Generator](https://swagger.io/tools/swagger-codegen/).


