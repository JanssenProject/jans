# DevelopersApi

All URIs are relative to *https://gluu.org/oxd/4.0.0*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getAccessTokenByRefreshToken**](DevelopersApi.md#getAccessTokenByRefreshToken) | **POST** /get-access-token-by-refresh-token | Get Access Token By Refresh Token
[**getAuthorizationUrl**](DevelopersApi.md#getAuthorizationUrl) | **POST** /get-authorization-url | Get Authorization Url
[**getClientToken**](DevelopersApi.md#getClientToken) | **POST** /get-client-token | Get Client Token
[**getLogoutUri**](DevelopersApi.md#getLogoutUri) | **POST** /get-logout-uri | Get Logout URL
[**getTokensByCode**](DevelopersApi.md#getTokensByCode) | **POST** /get-tokens-by-code | Get Tokens By Code
[**getUserInfo**](DevelopersApi.md#getUserInfo) | **POST** /get-user-info | Get User Info
[**healthCheck**](DevelopersApi.md#healthCheck) | **GET** /health-check | Health Check
[**introspectAccessToken**](DevelopersApi.md#introspectAccessToken) | **POST** /introspect-access-token | Introspect Access Token
[**introspectRpt**](DevelopersApi.md#introspectRpt) | **POST** /introspect-rpt | Introspect RPT
[**registerSite**](DevelopersApi.md#registerSite) | **POST** /register-site | Register Site
[**removeSite**](DevelopersApi.md#removeSite) | **POST** /remove-site | Remove Site
[**setupClient**](DevelopersApi.md#setupClient) | **POST** /setup-client | Setup Client
[**umaRpGetClaimsGatheringUrl**](DevelopersApi.md#umaRpGetClaimsGatheringUrl) | **POST** /uma-rp-get-claims-gathering-url | UMA RP Get Claims Gathering URL
[**umaRpGetRpt**](DevelopersApi.md#umaRpGetRpt) | **POST** /uma-rp-get-rpt | UMA RP Get RPT
[**umaRsCheckAccess**](DevelopersApi.md#umaRsCheckAccess) | **POST** /uma-rs-check-access | UMA RS Check Access
[**umaRsProtect**](DevelopersApi.md#umaRsProtect) | **POST** /uma-rs-protect | UMA RS Protect Resources
[**updateSite**](DevelopersApi.md#updateSite) | **POST** /update-site | Update Site


<a name="getAccessTokenByRefreshToken"></a>
# **getAccessTokenByRefreshToken**
> GetAccessTokenByRefreshTokenResponse getAccessTokenByRefreshToken(authorization, getAccessTokenByRefreshTokenParams)

Get Access Token By Refresh Token

Get Access Token By Refresh Token

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.DevelopersApi;


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
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **authorization** | **String**|  | [optional]
 **getAccessTokenByRefreshTokenParams** | [**GetAccessTokenByRefreshTokenParams**](GetAccessTokenByRefreshTokenParams.md)|  | [optional]

### Return type

[**GetAccessTokenByRefreshTokenResponse**](GetAccessTokenByRefreshTokenResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="getAuthorizationUrl"></a>
# **getAuthorizationUrl**
> GetAuthorizationUrlResponse getAuthorizationUrl(authorization, getAuthorizationUrlParams)

Get Authorization Url

Gets authorization url

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.DevelopersApi;


DevelopersApi apiInstance = new DevelopersApi();
String authorization = "authorization_example"; // String | 
GetAuthorizationUrlParams getAuthorizationUrlParams = new GetAuthorizationUrlParams(); // GetAuthorizationUrlParams | 
try {
    GetAuthorizationUrlResponse result = apiInstance.getAuthorizationUrl(authorization, getAuthorizationUrlParams);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling DevelopersApi#getAuthorizationUrl");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **authorization** | **String**|  | [optional]
 **getAuthorizationUrlParams** | [**GetAuthorizationUrlParams**](GetAuthorizationUrlParams.md)|  | [optional]

### Return type

[**GetAuthorizationUrlResponse**](GetAuthorizationUrlResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="getClientToken"></a>
# **getClientToken**
> GetClientTokenResponse getClientToken(getClientTokenParams)

Get Client Token

Gets Client Token

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.DevelopersApi;


DevelopersApi apiInstance = new DevelopersApi();
GetClientTokenParams getClientTokenParams = new GetClientTokenParams(); // GetClientTokenParams | 
try {
    GetClientTokenResponse result = apiInstance.getClientToken(getClientTokenParams);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling DevelopersApi#getClientToken");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **getClientTokenParams** | [**GetClientTokenParams**](GetClientTokenParams.md)|  | [optional]

### Return type

[**GetClientTokenResponse**](GetClientTokenResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="getLogoutUri"></a>
# **getLogoutUri**
> GetLogoutUriResponse getLogoutUri(authorization, getLogoutUriParams)

Get Logout URL

Get Logout URL

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.DevelopersApi;


DevelopersApi apiInstance = new DevelopersApi();
String authorization = "authorization_example"; // String | 
GetLogoutUriParams getLogoutUriParams = new GetLogoutUriParams(); // GetLogoutUriParams | 
try {
    GetLogoutUriResponse result = apiInstance.getLogoutUri(authorization, getLogoutUriParams);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling DevelopersApi#getLogoutUri");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **authorization** | **String**|  | [optional]
 **getLogoutUriParams** | [**GetLogoutUriParams**](GetLogoutUriParams.md)|  | [optional]

### Return type

[**GetLogoutUriResponse**](GetLogoutUriResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="getTokensByCode"></a>
# **getTokensByCode**
> GetTokensByCodeResponse getTokensByCode(authorization, getTokensByCodeParams)

Get Tokens By Code

Get tokens by code

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.DevelopersApi;


DevelopersApi apiInstance = new DevelopersApi();
String authorization = "authorization_example"; // String | 
GetTokensByCodeParams getTokensByCodeParams = new GetTokensByCodeParams(); // GetTokensByCodeParams | 
try {
    GetTokensByCodeResponse result = apiInstance.getTokensByCode(authorization, getTokensByCodeParams);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling DevelopersApi#getTokensByCode");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **authorization** | **String**|  | [optional]
 **getTokensByCodeParams** | [**GetTokensByCodeParams**](GetTokensByCodeParams.md)|  | [optional]

### Return type

[**GetTokensByCodeResponse**](GetTokensByCodeResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="getUserInfo"></a>
# **getUserInfo**
> GetUserInfoResponse getUserInfo(authorization, getUserInfoParams)

Get User Info

Get User Info

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.DevelopersApi;


DevelopersApi apiInstance = new DevelopersApi();
String authorization = "authorization_example"; // String | 
GetUserInfoParams getUserInfoParams = new GetUserInfoParams(); // GetUserInfoParams | 
try {
    GetUserInfoResponse result = apiInstance.getUserInfo(authorization, getUserInfoParams);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling DevelopersApi#getUserInfo");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **authorization** | **String**|  | [optional]
 **getUserInfoParams** | [**GetUserInfoParams**](GetUserInfoParams.md)|  | [optional]

### Return type

[**GetUserInfoResponse**](GetUserInfoResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="healthCheck"></a>
# **healthCheck**
> healthCheck()

Health Check

Health Check endpoint is for quick check whether oxd-server is alive.

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.DevelopersApi;


DevelopersApi apiInstance = new DevelopersApi();
try {
    apiInstance.healthCheck();
} catch (ApiException e) {
    System.err.println("Exception when calling DevelopersApi#healthCheck");
    e.printStackTrace();
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: Not defined

<a name="introspectAccessToken"></a>
# **introspectAccessToken**
> IntrospectAccessTokenResponse introspectAccessToken(authorization, introspectAccessTokenParams)

Introspect Access Token

Introspect Access Token

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.DevelopersApi;


DevelopersApi apiInstance = new DevelopersApi();
String authorization = "authorization_example"; // String | 
IntrospectAccessTokenParams introspectAccessTokenParams = new IntrospectAccessTokenParams(); // IntrospectAccessTokenParams | 
try {
    IntrospectAccessTokenResponse result = apiInstance.introspectAccessToken(authorization, introspectAccessTokenParams);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling DevelopersApi#introspectAccessToken");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **authorization** | **String**|  | [optional]
 **introspectAccessTokenParams** | [**IntrospectAccessTokenParams**](IntrospectAccessTokenParams.md)|  | [optional]

### Return type

[**IntrospectAccessTokenResponse**](IntrospectAccessTokenResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="introspectRpt"></a>
# **introspectRpt**
> IntrospectRptResponse introspectRpt(authorization, introspectRptParams)

Introspect RPT

Introspect RPT

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.DevelopersApi;


DevelopersApi apiInstance = new DevelopersApi();
String authorization = "authorization_example"; // String | 
IntrospectRptParams introspectRptParams = new IntrospectRptParams(); // IntrospectRptParams | 
try {
    IntrospectRptResponse result = apiInstance.introspectRpt(authorization, introspectRptParams);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling DevelopersApi#introspectRpt");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **authorization** | **String**|  | [optional]
 **introspectRptParams** | [**IntrospectRptParams**](IntrospectRptParams.md)|  | [optional]

### Return type

[**IntrospectRptResponse**](IntrospectRptResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="registerSite"></a>
# **registerSite**
> RegisterSiteResponse registerSite(authorization, registerSiteParams)

Register Site

Registers site at oxd-server

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.DevelopersApi;


DevelopersApi apiInstance = new DevelopersApi();
String authorization = "authorization_example"; // String | 
RegisterSiteParams registerSiteParams = new RegisterSiteParams(); // RegisterSiteParams | 
try {
    RegisterSiteResponse result = apiInstance.registerSite(authorization, registerSiteParams);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling DevelopersApi#registerSite");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **authorization** | **String**|  | [optional]
 **registerSiteParams** | [**RegisterSiteParams**](RegisterSiteParams.md)|  | [optional]

### Return type

[**RegisterSiteResponse**](RegisterSiteResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="removeSite"></a>
# **removeSite**
> RemoveSiteResponse removeSite(authorization, removeSiteParams)

Remove Site

Removes site from oxd-server

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.DevelopersApi;


DevelopersApi apiInstance = new DevelopersApi();
String authorization = "authorization_example"; // String | 
RemoveSiteParams removeSiteParams = new RemoveSiteParams(); // RemoveSiteParams | 
try {
    RemoveSiteResponse result = apiInstance.removeSite(authorization, removeSiteParams);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling DevelopersApi#removeSite");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **authorization** | **String**|  | [optional]
 **removeSiteParams** | [**RemoveSiteParams**](RemoveSiteParams.md)|  | [optional]

### Return type

[**RemoveSiteResponse**](RemoveSiteResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="setupClient"></a>
# **setupClient**
> SetupClientResponse setupClient(setupClientParams)

Setup Client

Setups client is for securing communication to oxd-server

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.DevelopersApi;


DevelopersApi apiInstance = new DevelopersApi();
SetupClientParams setupClientParams = new SetupClientParams(); // SetupClientParams | 
try {
    SetupClientResponse result = apiInstance.setupClient(setupClientParams);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling DevelopersApi#setupClient");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **setupClientParams** | [**SetupClientParams**](SetupClientParams.md)|  | [optional]

### Return type

[**SetupClientResponse**](SetupClientResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="umaRpGetClaimsGatheringUrl"></a>
# **umaRpGetClaimsGatheringUrl**
> UmaRpGetClaimsGatheringUrlResponse umaRpGetClaimsGatheringUrl(authorization, umaRpGetClaimsGatheringUrlParams)

UMA RP Get Claims Gathering URL

UMA RP Get Claims Gathering URL

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.DevelopersApi;


DevelopersApi apiInstance = new DevelopersApi();
String authorization = "authorization_example"; // String | 
UmaRpGetClaimsGatheringUrlParams umaRpGetClaimsGatheringUrlParams = new UmaRpGetClaimsGatheringUrlParams(); // UmaRpGetClaimsGatheringUrlParams | 
try {
    UmaRpGetClaimsGatheringUrlResponse result = apiInstance.umaRpGetClaimsGatheringUrl(authorization, umaRpGetClaimsGatheringUrlParams);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling DevelopersApi#umaRpGetClaimsGatheringUrl");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **authorization** | **String**|  | [optional]
 **umaRpGetClaimsGatheringUrlParams** | [**UmaRpGetClaimsGatheringUrlParams**](UmaRpGetClaimsGatheringUrlParams.md)|  | [optional]

### Return type

[**UmaRpGetClaimsGatheringUrlResponse**](UmaRpGetClaimsGatheringUrlResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="umaRpGetRpt"></a>
# **umaRpGetRpt**
> UmaRpGetRptResponse umaRpGetRpt(authorization, umaRpGetRptParams)

UMA RP Get RPT

UMA RP Get RPT

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.DevelopersApi;


DevelopersApi apiInstance = new DevelopersApi();
String authorization = "authorization_example"; // String | 
UmaRpGetRptParams umaRpGetRptParams = new UmaRpGetRptParams(); // UmaRpGetRptParams | 
try {
    UmaRpGetRptResponse result = apiInstance.umaRpGetRpt(authorization, umaRpGetRptParams);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling DevelopersApi#umaRpGetRpt");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **authorization** | **String**|  | [optional]
 **umaRpGetRptParams** | [**UmaRpGetRptParams**](UmaRpGetRptParams.md)|  | [optional]

### Return type

[**UmaRpGetRptResponse**](UmaRpGetRptResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="umaRsCheckAccess"></a>
# **umaRsCheckAccess**
> UmaRsCheckAccessResponse umaRsCheckAccess(authorization, umaRsCheckAccessParams)

UMA RS Check Access

UMA RS Check Access

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.DevelopersApi;


DevelopersApi apiInstance = new DevelopersApi();
String authorization = "authorization_example"; // String | 
UmaRsCheckAccessParams umaRsCheckAccessParams = new UmaRsCheckAccessParams(); // UmaRsCheckAccessParams | 
try {
    UmaRsCheckAccessResponse result = apiInstance.umaRsCheckAccess(authorization, umaRsCheckAccessParams);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling DevelopersApi#umaRsCheckAccess");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **authorization** | **String**|  | [optional]
 **umaRsCheckAccessParams** | [**UmaRsCheckAccessParams**](UmaRsCheckAccessParams.md)|  | [optional]

### Return type

[**UmaRsCheckAccessResponse**](UmaRsCheckAccessResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="umaRsProtect"></a>
# **umaRsProtect**
> RemoveSiteResponse umaRsProtect(authorization, umaRsProtectParams)

UMA RS Protect Resources

UMA RS Protect Resources. It&#39;s important to have a single HTTP method, mentioned only once within a given path in JSON, otherwise, the operation will fail.

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.DevelopersApi;


DevelopersApi apiInstance = new DevelopersApi();
String authorization = "authorization_example"; // String | 
UmaRsProtectParams umaRsProtectParams = new UmaRsProtectParams(); // UmaRsProtectParams | 
try {
    RemoveSiteResponse result = apiInstance.umaRsProtect(authorization, umaRsProtectParams);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling DevelopersApi#umaRsProtect");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **authorization** | **String**|  | [optional]
 **umaRsProtectParams** | [**UmaRsProtectParams**](UmaRsProtectParams.md)|  | [optional]

### Return type

[**RemoveSiteResponse**](RemoveSiteResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="updateSite"></a>
# **updateSite**
> RemoveSiteResponse updateSite(authorization, updateSiteParams)

Update Site

Updates site at oxd-server

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.DevelopersApi;


DevelopersApi apiInstance = new DevelopersApi();
String authorization = "authorization_example"; // String | 
UpdateSiteParams updateSiteParams = new UpdateSiteParams(); // UpdateSiteParams | 
try {
    RemoveSiteResponse result = apiInstance.updateSite(authorization, updateSiteParams);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling DevelopersApi#updateSite");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **authorization** | **String**|  | [optional]
 **updateSiteParams** | [**UpdateSiteParams**](UpdateSiteParams.md)|  | [optional]

### Return type

[**RemoveSiteResponse**](RemoveSiteResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

