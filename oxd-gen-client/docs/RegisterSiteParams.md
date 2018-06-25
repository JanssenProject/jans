
# RegisterSiteParams

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**authorizationRedirectUri** | **String** |  | 
**opHost** | **String** | If missing, must be present in defaults |  [optional]
**postLogoutRedirectUri** | **String** |  |  [optional]
**applicationType** | **String** |  |  [optional]
**responseTypes** | **List&lt;String&gt;** |  |  [optional]
**grantTypes** | **List&lt;String&gt;** |  |  [optional]
**scope** | **List&lt;String&gt;** |  |  [optional]
**acrValues** | **List&lt;String&gt;** |  |  [optional]
**clientName** | **String** | oxd will generate its own non-human readable name by defaultif client_name is not specified |  [optional]
**clientJwksUri** | **String** |  |  [optional]
**clientTokenEndpointAuthMethod** | **String** |  |  [optional]
**clientRequestUris** | **List&lt;String&gt;** |  |  [optional]
**clientFrontchannelLogoutUris** | **List&lt;String&gt;** |  |  [optional]
**clientSectorIdentifierUri** | **List&lt;String&gt;** |  |  [optional]
**contacts** | **List&lt;String&gt;** |  |  [optional]
**uiLocales** | **List&lt;String&gt;** |  |  [optional]
**claimsLocales** | **List&lt;String&gt;** |  |  [optional]
**claimsRedirectUri** | **List&lt;String&gt;** |  |  [optional]
**clientId** | **String** | client id of existing client, ignores all other parameters and skips new client registration forcing to use existing client (client_secret is required if this parameter is set) |  [optional]
**clientSecret** | **String** | client secret of existing client, must be used together with client_id |  [optional]



