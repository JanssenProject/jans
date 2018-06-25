# swagger-java-client

## Requirements

Building the API client library requires [Maven](https://maven.apache.org/) to be installed.

## Installation

To install the API client library to your local Maven repository, simply execute:

```shell
mvn install
```

To deploy it to a remote Maven repository instead, configure the settings of the repository and execute:

```shell
mvn deploy
```

Refer to the [official documentation](https://maven.apache.org/plugins/maven-deploy-plugin/usage.html) for more information.

### Maven users

Add this dependency to your project's POM:

```xml
<dependency>
    <groupId>io.swagger</groupId>
    <artifactId>swagger-java-client</artifactId>
    <version>1.0.0</version>
    <scope>compile</scope>
</dependency>
```

### Gradle users

Add this dependency to your project's build file:

```groovy
compile "io.swagger:swagger-java-client:1.0.0"
```

### Others

At first generate the JAR by executing:

    mvn package

Then manually install the following JARs:

* target/swagger-java-client-1.0.0.jar
* target/lib/*.jar

## Getting Started

Please follow the [installation](#installation) instruction and execute the following Java code:

```java

import io.swagger.client.*;
import io.swagger.client.auth.*;
import io.swagger.client.model.*;
import io.swagger.client.api.DevelopersApi;

import java.io.File;
import java.util.*;

public class DevelopersApiExample {

    public static void main(String[] args) {
        
        DevelopersApi apiInstance = new DevelopersApi();
        String authorization = "authorization_example"; // String | 
        GetAccessTokenByRefreshTokenParams getAccessTokenByRefreshTokenParams = new GetAccessTokenByRefreshTokenParams(); // GetAccessTokenByRefreshTokenParams | 
        try {
            GetAccessTokenByRefreshTokenResponse result = apiInstance.getAccessTokenByRefreshToken(authorization, getAccessTokenByRefreshTokenParams);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling DevelopersApi#getAccessTokenByRefreshToken");
            e.printStackTrace();
        }
    }
}

```

## Documentation for API Endpoints

All URIs are relative to *https://gluu.org/oxd/4.0.0*

Class | Method | HTTP request | Description
------------ | ------------- | ------------- | -------------
*DevelopersApi* | [**getAccessTokenByRefreshToken**](docs/DevelopersApi.md#getAccessTokenByRefreshToken) | **POST** /get-access-token-by-refresh-token | Get Access Token By Refresh Token
*DevelopersApi* | [**getAuthorizationUrl**](docs/DevelopersApi.md#getAuthorizationUrl) | **POST** /get-authorization-url | Get Authorization Url
*DevelopersApi* | [**getClientToken**](docs/DevelopersApi.md#getClientToken) | **POST** /get-client-token | Get Client Token
*DevelopersApi* | [**getLogoutUri**](docs/DevelopersApi.md#getLogoutUri) | **POST** /get-logout-uri | Get Logout URL
*DevelopersApi* | [**getTokensByCode**](docs/DevelopersApi.md#getTokensByCode) | **POST** /get-tokens-by-code | Get Tokens By Code
*DevelopersApi* | [**getUserInfo**](docs/DevelopersApi.md#getUserInfo) | **POST** /get-user-info | Get User Info
*DevelopersApi* | [**healthCheck**](docs/DevelopersApi.md#healthCheck) | **GET** /health-check | Health Check
*DevelopersApi* | [**introspectAccessToken**](docs/DevelopersApi.md#introspectAccessToken) | **POST** /introspect-access-token | Introspect Access Token
*DevelopersApi* | [**introspectRpt**](docs/DevelopersApi.md#introspectRpt) | **POST** /introspect-rpt | Introspect RPT
*DevelopersApi* | [**registerSite**](docs/DevelopersApi.md#registerSite) | **POST** /register-site | Register Site
*DevelopersApi* | [**removeSite**](docs/DevelopersApi.md#removeSite) | **POST** /remove-site | Remove Site
*DevelopersApi* | [**setupClient**](docs/DevelopersApi.md#setupClient) | **POST** /setup-client | Setup Client
*DevelopersApi* | [**umaRpGetClaimsGatheringUrl**](docs/DevelopersApi.md#umaRpGetClaimsGatheringUrl) | **POST** /uma-rp-get-claims-gathering-url | UMA RP Get Claims Gathering URL
*DevelopersApi* | [**umaRpGetRpt**](docs/DevelopersApi.md#umaRpGetRpt) | **POST** /uma-rp-get-rpt | UMA RP Get RPT
*DevelopersApi* | [**umaRsCheckAccess**](docs/DevelopersApi.md#umaRsCheckAccess) | **POST** /uma-rs-check-access | UMA RS Check Access
*DevelopersApi* | [**umaRsProtect**](docs/DevelopersApi.md#umaRsProtect) | **POST** /uma-rs-protect | UMA RS Protect Resources
*DevelopersApi* | [**updateSite**](docs/DevelopersApi.md#updateSite) | **POST** /update-site | Update Site


## Documentation for Models

 - [GetAccessTokenByRefreshTokenParams](docs/GetAccessTokenByRefreshTokenParams.md)
 - [GetAccessTokenByRefreshTokenResponse](docs/GetAccessTokenByRefreshTokenResponse.md)
 - [GetAccessTokenByRefreshTokenResponseData](docs/GetAccessTokenByRefreshTokenResponseData.md)
 - [GetAuthorizationUrlParams](docs/GetAuthorizationUrlParams.md)
 - [GetAuthorizationUrlResponse](docs/GetAuthorizationUrlResponse.md)
 - [GetAuthorizationUrlResponseData](docs/GetAuthorizationUrlResponseData.md)
 - [GetClientTokenParams](docs/GetClientTokenParams.md)
 - [GetClientTokenResponse](docs/GetClientTokenResponse.md)
 - [GetClientTokenResponseData](docs/GetClientTokenResponseData.md)
 - [GetLogoutUriParams](docs/GetLogoutUriParams.md)
 - [GetLogoutUriResponse](docs/GetLogoutUriResponse.md)
 - [GetLogoutUriResponseClaims](docs/GetLogoutUriResponseClaims.md)
 - [GetTokensByCodeParams](docs/GetTokensByCodeParams.md)
 - [GetTokensByCodeResponse](docs/GetTokensByCodeResponse.md)
 - [GetTokensByCodeResponseData](docs/GetTokensByCodeResponseData.md)
 - [GetTokensByCodeResponseDataIdTokenClaims](docs/GetTokensByCodeResponseDataIdTokenClaims.md)
 - [GetUserInfoParams](docs/GetUserInfoParams.md)
 - [GetUserInfoResponse](docs/GetUserInfoResponse.md)
 - [GetUserInfoResponseClaims](docs/GetUserInfoResponseClaims.md)
 - [GetauthorizationurlCustomParameters](docs/GetauthorizationurlCustomParameters.md)
 - [IntrospectAccessTokenParams](docs/IntrospectAccessTokenParams.md)
 - [IntrospectAccessTokenReponse](docs/IntrospectAccessTokenReponse.md)
 - [IntrospectAccessTokenReponseData](docs/IntrospectAccessTokenReponseData.md)
 - [IntrospectRptParams](docs/IntrospectRptParams.md)
 - [IntrospectRptResponse](docs/IntrospectRptResponse.md)
 - [IntrospectRptResponseData](docs/IntrospectRptResponseData.md)
 - [IntrospectaccesstokenParams](docs/IntrospectaccesstokenParams.md)
 - [RegisterSiteParams](docs/RegisterSiteParams.md)
 - [RegisterSiteResponse](docs/RegisterSiteResponse.md)
 - [RegisterSiteResponseData](docs/RegisterSiteResponseData.md)
 - [RemoveSiteParams](docs/RemoveSiteParams.md)
 - [SetupClientParams](docs/SetupClientParams.md)
 - [SetupClientResponse](docs/SetupClientResponse.md)
 - [SetupClientResponseData](docs/SetupClientResponseData.md)
 - [UmaRpGetClaimsGatheringUrlParams](docs/UmaRpGetClaimsGatheringUrlParams.md)
 - [UmaRpGetClaimsGatheringUrlResponse](docs/UmaRpGetClaimsGatheringUrlResponse.md)
 - [UmaRpGetClaimsGatheringUrlResponseData](docs/UmaRpGetClaimsGatheringUrlResponseData.md)
 - [UmaRpGetRptParams](docs/UmaRpGetRptParams.md)
 - [UmaRpGetRptResponse](docs/UmaRpGetRptResponse.md)
 - [UmaRpGetRptResponseData](docs/UmaRpGetRptResponseData.md)
 - [UmaRsCheckAccessParams](docs/UmaRsCheckAccessParams.md)
 - [UmaRsCheckAccessResponse](docs/UmaRsCheckAccessResponse.md)
 - [UmaRsCheckAccessResponseData](docs/UmaRsCheckAccessResponseData.md)
 - [UmaRsProtectParams](docs/UmaRsProtectParams.md)
 - [UpdateSiteParams](docs/UpdateSiteParams.md)
 - [UpdateSiteResponse](docs/UpdateSiteResponse.md)
 - [UpdateSiteResponseData](docs/UpdateSiteResponseData.md)


## Documentation for Authorization

All endpoints do not require authorization.
Authentication schemes defined for the API:

## Recommendation

It's recommended to create an instance of `ApiClient` per thread in a multithreaded environment to avoid any potential issues.

## Author

yuriyz@gluu.org

