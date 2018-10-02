
# GetClientTokenParams

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**opHost** | **String** |  | 
**opDiscoveryPath** | **String** |  |  [optional]
**scope** | **List&lt;String&gt;** |  |  [optional]
**clientId** | **String** |  | 
**clientSecret** | **String** |  | 
**authenticationMethod** | **String** | if value is missed then basic authentication is used. Otherwise it&#39;s possible to set &#x60;private_key_jwt&#x60; value for Private Key authentication. |  [optional]
**algorithm** | **String** | optional but is required if authentication_method&#x3D;private_key_jwt. Valid values are none, HS256, HS384, HS512, RS256, RS384, RS512, ES256, ES384, ES512 |  [optional]
**keyId** | **String** | optional but is required if authentication_method&#x3D;private_key_jwt. It has to be valid key id from key store. |  [optional]



