#!/usr/bin/bash -x

config_command= /opt/jans/jans-cli/config-cli.py
scim_command=/opt/jans/jans-cli/scim-cli.py

for name in GluAttribute JSONWebKey Logging SmtpConfiguration User-Mgt:CustomUser User-Mgt:UserPatchRequest CustomScript GluuLdapConfiguration AuthenticationMethod Fido2:AppConfiguration Client Scope UmaResource SCIM:JsonPatch Admin-UI:LicenseRequest Admin-UI:LicenseSpringCredentials Admin-UI:AdminPermission Admin-UI:AdminRole Admin-UI:RolePermissionMapping

#CacheConfiguration CacheConfigurationInMemory CacheConfigurationMemcached CacheConfigurationNativePersistence CacheConfigurationRedis ConfigurationConfigApi ConfigurationProperties OrganizationConfiguration
        do
        	
                $config_command --schema $name  > /tmp/$name.json
done
cp /tmp/PatchRequest.json /tmp/patch-cache.json ;cp /tmp/PatchRequest.json /tmp/jans-auth.json;cp /tmp/PatchRequest.json /tmp/uma.json


Attribute ()
{
 echo "${FUNCNAME[0]} testing started"
$config_command --info Attribute 
$config_command --operation-id get-attributes


$config_command --operation-id get-attributes --endpoint-args limit:5
$config_command --operation-id get-attributes --endpoint-args limit:5,pattern:profile,status:ACTIVE

sed -e 's/"name": "name, displayName, birthdate, email"/"name": "testAttribute"/'\
	-e 's/"displayName": "string"/"displayName": "testAttribute"/'\
	-e 's/"description": "string"/"description": "testing post-attributes"/'\
	-e 's/"dataType": "BINARY"/"dataType": "STRING"/'\
	-e 's/"status": "INACTIVE"/"status": "REGISTER"/' \
       -e 's/"array"/["ADMIN", "OWNER"]/' /tmp/GluuAttribute.json > /tmp/postAttribute.json

$config_command --operation-id post-attributes --data /tmp/postAttribute.json >/tmp/putAttribute.json

sed -e 's/"status": "REGISTER"/"status": "ACTIVE"/' /tmp/putAttribute.json >/tmp/putAttribute_1.json
	
$config_command --operation-id put-attributes --data /tmp/putAttribute_1.json

inum=`cat /tmp/putAttribute.json | grep -i 'inum":' | cut -d'"' -f4`

$config_command --operation-id get-attributes-by-inum --url-suffix inum:$inum

$config_command --operation-id delete-attributes-by-inum --url-suffix inum:$inum

echo '
[
{
  "op": "replace",
  "path": "description",
  "value": "new path applied"
}
]' >/tmp/patch.json

$config_command --operation-id patch-attributes-by-inum --url-suffix inum:$inum --data /tmp/patch.json
echo 
}  > output.log 2>>errors.log

CacheConfiguration ()
{
echo "${FUNCNAME[0]} testing started"
$config_command --info CacheConfiguration

$config_command --operation-id get-config-cache

echo '
[
  {
  "op": "replace",
  "path": "memcachedConfiguration/bufferSize",
  "value": "32788"
  }
]' >/tmp/patch-cache.json
$config_command --operation-id patch-config-cache --data /tmp/patch-cache.json

$config_command --operation-id patch-config-cache --patch-replace nativePersistenceConfiguration/defaultPutExpiration:90

} >> output.log 2>>errors.log

CouchBaseConfiguration ()
{

echo "${FUNCNAME[0]} testing started"

$config_command --info DatabaseCouchbaseConfiguration

$config_command --operation-id get-config-database-couchbase
sed -e 's/"configId": "string"/"configId": "couchbasetest"/'\
	-e 's/"userName": "string"/"userName": "couchbasetest"/'\
	-e 's/userPassword": "string"/userPassword": "couchbasetest"/'\
	-e 's/"servers": "array"/"servers": "localhost"/'\
	-e 's/"defaultBucket": "string"/"defaultBucket": "couchbasetest"/'\
	-e 's/"buckets": "array"/"buckets": "couchbasetest"/' /tmp/CouchbaseConfiguration.json >/tmp/CouchbaseConfiguration_1.json

$config_command --operation-id post-config-database-couchbase --data /tmp/CouchbaseConfiguration_1.json

echo '
[
  {
    "op": "replace",
    "path": "userPassword",
    "value": "changedPasswd"
  }
]' >/tmp/Couchbase_patch.json

$config_command --operation-id put-config-database-couchbase --data /tmp/Couchbase_patch.json

configId=`cat /tmp/CouchbaseConfiguration_1.json | grep -i configId | cut -d'"' -f4`
$config_command --operation-id get-config-database-couchbase-by-name --url-suffix name:$configId

#$config_command –operation-id post-config-database-couchbase-test


} >> output.log 2>>errors.log

CustomScript ()

{
echo "${FUNCNAME[0]} testing started"
$config_command --info CustomScripts
$config_command --operation-id get-config-scripts
sed -e 's/"name": "string"/"name": "mycustomscript"/' \
	-e 's/"description": null/"description": "my first script"/'\
	-e 's/"script": "string"/"script": "myjavascript.java"/'\
	-e 's/"level": "integer"/"level": "3"/' /tmp/CustomScript.json >/tmp/CustomScript_1.json
$config_command --operation-id post-config-scripts --data /tmp/CustomScript_1.json > /tmp/CustomScript_2.json
csinum=`cat /tmp/CustomScript_2.json |grep -i 'inum":' | cut -d'"' -f4` 

$config_command --operation-id get-config-scripts-by-inum --url-suffix inum:$csinum

$config_command --operation-id get-config-scripts-by-type --url-suffix type:CIBA_END_USER_NOTIFICATION

sed -e 's/"value1": null/"value1": "modified value"/'\
	-e 's/"value2": null/"value2": "modified value2"/'\
	-e 's/"description": "my first script"/"description": "modified desc"/' /tmp/CustomScript_2.json >/tmp/CustomScript_3.json
      

$config_command --operation-id put-config-scripts --data /tmp/CustomScript_3.json

$config_command --operation-id delete-config-scripts-by-inum --url-suffix inum:$csinum


} >> output.log 2>>errors.log

defaultMethod ()
{
echo "${FUNCNAME[0]} testing started"
$config_command --info DefaultAuthenticationMethod

$config_command --operation-id get-acrs

echo '
{
  "defaultAcr": "passwd_saml"
}
' > /tmp/AuthenticationMethod.json

$config_command --operation-id put-acrs --data /tmp/AuthenticationMethod.json


} >> output.log 2>>errors.log

jansAuth()
{
	echo "${FUNCNAME[0]} testing started"
	$config_command --info ConfigurationProperties

$config_command --operation-id get-properties

echo '[
  {
    "op": "replace",
    "path": "cibaEnabled",
    "value": true
  }
]' >/tmp/patch-jans-auth.json

$config_command --operation-id patch-properties --data /tmp/patch-jans-auth.json

} >> output.log 2>>errors.log


FIDO ()
{
echo "${FUNCNAME[0]} testing started"
$config_command --info ConfigurationFido2

$config_command --operation-id get-properties-fido2

sed -e 's/"loggingLevel": null/"loggingLevel": "INFO"/' /tmp/JansFido2DynConfiguration.json > /tmp/fido2-schema.json

$config_command --operation-id put-properties-fido2 --data /tmp/fido2-schema.json



} >> output.log 2>>errors.log


JWK ()
{
echo "${FUNCNAME[0]} testing started"
$config_command --info ConfigurationJWKJSONWebKeyJWK

$config_command --operation-id get-config-jwks

echo '
{
"kid": "dd550214-7969-41b9-b919-2a0cfa36047b_enc_rsa1_5",
"kty": "RSA",
"use": "enc",
"alg": "RSA-OAEP",
"crv": "",
"exp": 1622245655163,
"x5c": [
  "MIIDCjCCAfKgAwIBAgIhANYLiviUTmgOsf9Bf+6N/pr6H4Mis5ku1VXNj7VW/CMbMA0GCSqGSIb3DQEBCwUAMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjEwNTI2MjM0NzI5WhcNMjEwNTI4MjM0NzM1WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArlD19ib3J2bKYr2iap1d/gCmbXocMJTk5o7o3h9jJKXbh9pdf2gd3ZOE6wc5XwGx/CfHSgdEmACCXMiG7sQt80DPM67dlbtv/pEnWrHk4fwwst83OF+HXTSi4Sd9QWhDtBvaUu8Rp8ir+x2D0RK8YNGs0prA+qGR8O/h6Y+ascz4VNbbDlbJ+w7DJYeWU1HVp/5Lt8O5i4Q6I8KZEAytwvspF5y8m8DCrfYXF6Kz14vXgqr08hj0l0Aj4O3y/9i8kf2pmznpu5QEDimj1yxEB+G5WEYuHD/+qRTV85OXDIQJz6fgNM4kEimv7pmspcDfk/KKB7/KT0rEOn7T2rXW9QIDAQABoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwDQYJKoZIhvcNAQELBQADggEBAKrtlIPhvDBxBfcqS9Xy39QqE1WOPiNQooa/FVVOsCROdRZrHhFcP27HpxO9e6genQSJ6nBRaJ4ykEf0oM535Ker5jZcDWzCwPIyt+5Kc6qeacZI5FxEHRldYkSd4lF1OTzQNvGLOPKnNWnYnXwj48ZxO50lJUsRFspVbP79E6llVNOPexrZ2GOzWghyY1E74f4uGr6fzcXQk2aFaIfLusoJlvbROPTnDu68Jt+IW4WZcO4F0tl0JIcuaqSmLS6McJW0Mpmu4wqEPV6E45zRAuX0kJUkKDMzM/lYW1MZ8QaSTt/pCmlknX1+KTgb6Sf9zZJEya8AyKML/NCpc4sfn8g="
],
"n": "rlD19ib3J2bKYr2iap1d_gCmbXocMJTk5o7o3h9jJKXbh9pdf2gd3ZOE6wc5XwGx_CfHSgdEmACCXMiG7sQt80DPM67dlbtv_pEnWrHk4fwwst83OF-HXTSi4Sd9QWhDtBvaUu8Rp8ir-x2D0RK8YNGs0prA-qGR8O_h6Y-ascz4VNbbDlbJ-w7DJYeWU1HVp_5Lt8O5i4Q6I8KZEAytwvspF5y8m8DCrfYXF6Kz14vXgqr08hj0l0Aj4O3y_9i8kf2pmznpu5QEDimj1yxEB-G5WEYuHD_-qRTV85OXDIQJz6fgNM4kEimv7pmspcDfk_KKB7_KT0rEOn7T2rXW9Q",
"e": "AQAB",
"x": null,
"y": null
}' >/tmp/JsonWebKey.json

$config_command --operation-id post-config-jwks-key --data /tmp/JsonWebKey.json

sed  -e 's/"crv": ""/"crv": "SHA"/' /tmp/JsonWebKey.json >/tmp/path-jwk.json

$config_command --operation-id put-config-jwks --data /tmp/path-jwk.json

kid=`cat /tmp/path-jwk.json |grep -i 'kid":' | cut -d'"' -f4`
$config_command --operation-id put-config-jwk-kid --url-suffix kid:$kid
echo '
[
{
  "op": "replace",
  "path": "use",
  "value": "sig"
}
]' >/tmp/path-jwk1.json
$config_command --operation-id patch-config-jwk-kid --url-suffix kid:$kid --data /tmp/path-jwk1.json

$config_command --operation-id delete-config-jwk-kid --url-suffix kid:$kid

} >> output.log 2>>errors.log

LDAP ()
{
echo "${FUNCNAME[0]} testing started"
$config_command --info DatabaseLDAPConfiguration

$config_command --operation-id get-config-database-ldap

sed -e 's/"configId": "auth_ldap_server"/"configId": "auth_ldap_server1"/'\
	-e 's/"bindDN": "string"/"bindDN": "cn=jans"/'\
	-e 's/"bindPassword": "string"/"bindPassword": "ldappasswd"/'\
	-e 's/"servers": "array"/"servers": "localhost"/'\
	-e 's/"baseDNs": "array"/"baseDNs": "auth_ldap_server1"/'\
	-e 's/primaryKey": "SAMAccountName,uid, email"/primaryKey": "auth_ldap, imanojs@gmail1.com"/'\
	-e 's/"localPrimaryKey": "uid, email"/"localPrimaryKey": "auth_ldap, imanojs@gmail1.com"/' /tmp/LdapConfiguration.json >/tmp/LdapConfiguration_1.json

$config_command --operation-id post-config-database-ldap --data /tmp/LdapConfiguration_1.json >/tmp/LdapConfiguration.json

sed -e 's/"maxConnections": 2/"maxConnections": 1000/' /tmp/LdapConfiguration.json > /tmp/LdapConfiguration_1.json
$config_command --operation-id put-config-database-ldap --data /tmp/LdapConfiguration_1.json

$config_command --operation-id get-config-database-ldap-by-name --url-suffix name:auth_ldap_server1

echo '
[
  {
    "op": "replace",
    "path": "level",
    "value": "100"
  }
]' >/tmp/ldappatch.json


$config_command --operation-id patch-config-database-ldap-by-name --url-suffix name:auth_ldap_server1 --data /tmp/ldappatch.json
$config_command --operation-id delete-config-database-ldap-by-name --url-suffix name:auth_ldap_server1

} >> output.log 2>>errors.log

Logging ()
{
echo "${FUNCNAME[0]} testing started"
$config_command --info ConfigurationLogging

$config_command --operation-id get-config-logging
sed -e 's/"loggingLevel": "WARN"/"loggingLevel": "TRACE"/'\
	-e 's/"loggingLayout": "text"/"loggingLayout": "json"/' /tmp/LoggingConfiguration.json > /tmp/LoggingConfiguration_1.json

$config_command --operation-id put-config-logging --data /tmp/LoggingConfiguration_1.json

} >> output.log 2>>errors.log


OAuthScope ()
{
echo "${FUNCNAME[0]} testing started"
 $config_command --info OauthScopes

 $config_command --operation-id get-oauth-scopes --endpoint-args limit:3,pattern:view,type:openid

sed -e 's/"displayName": null/"displayName": "myScope"/'\
	-e 's/"id": "string"/"id": "myScope"/'\
	-e 's/"description": null/"description": "myScope desc"/' /tmp/Scope.json >/tmp/Scope_1.json

$config_command --operation-id post-oauth-scopes --data /tmp/Scope_1.json
sed -e 's/"scopeType": "openid"/"scopeType": "dynamic"/' /tmp/Scope_1.json >/tmp/Scope.json
$config_command --operation-id put-oauth-scopes --data /tmp/Scope.json
oauthScopeinum=`cat /tmp/Scope.json | grep -i 'inum":' | cut -d'"' -f4`
echo '
[
        {
    "op": "replace",
    "path": "iconUrl",
    "value": "https://jans.io/icon.png"
  }

]'  >/tmp/scope-patch.json

$config_command --operation-id patch-oauth-scopes-by-id --url-suffix inum:$oauthScopeinum --data /tmp/scope-patch.json

$config_command --operation-id get-oauth-scopes-by-inum --url-suffix inum:C4F6

$config_command --operation-id delete-oauth-scopes-by-inum --url-suffix inum:$oauthScopeinum



} >> output.log 2>>errors.log

UMA ()

{
echo "${FUNCNAME[0]} testing started"
opt/jans/jans-cli/config-cli.py --info OAuthUMAResources
$config_command --operation-id get-oauth-uma-resources --endpoint-args limit:5

 $config_command --operation-id get-oauth-uma-resources --endpoint-args limit:1,pattern:"Jans Cofig Api Uma Resource /jans-config-api/api/v1/config/cache/native-persistence"
sed -e 's/"name": "string"/"name": "myuma"/'\
	-e 's/"description": "string"/"description": "myuma resource"/'  /tmp/UmaResource.json >/tmp/UmaResource_1.json

$config_command --operation-id post-oauth-uma-resources --data /tmp/UmaResource_1.json

umaid=`cat /tmp/Scope.json | grep -i 'id":' | cut -d'"' -f4`
$config_command --operation-id get-oauth-uma-resources-by-id --url-suffix id:$umaid

echo '
[
  {
    "op": "replace",
    "path": "deletable",
    "value": false
  }
]' >/tmp/patch-uma.json

$config_command --operation-id patch-oauth-uma-resources-by-id --url-suffix id:$umaid --data /tmp/patch-uma.json


}  >> output.log 2>>errors.log


OAuthopenID ()
{
echo "${FUNCNAME[0]} testing started"
$config_command --info OAuthOpenIDConnectClients

$config_command --operation-id get-oauth-openid-clients

$config_command --operation-id get-oauth-openid-clients --endpoint-args limit:2

sed -e 's/"displayName": null/"displayName": "myopenid"/'\
	-e 's/"frontChannelLogoutSessionRequired": true/"frontChannelLogoutSessionRequired": false/' /tmp/Client.json >/tmp/Client_1.json
	

$config_command --operation-id post-oauth-openid-clients --data /tmp/Client_1.json
sed -e 's/"includeClaimsInIdToken": false/"includeClaimsInIdToken": true/' /tmp/Client_1.json > /tmp/Client.json
$config_command --operation-id put-oauth-openid-clients --data /tmp/Client.json

oauthinum=`cat /tmp/Client.json | grep -i 'id":' | cut -d'"' -f4`
$config_command --operation-id get-oauth-openid-clients-by-inum url-suffix: inum:$oauthinum
$config_command --operation-id patch-oauth-openid-clients-by-inum url-suffix: inum:$oauthinum
$config_command --operation-id delete-oauth-openid-clients-by-inum url-suffix: inum:$oauthinum


} >> output.log 2>>errors.log

ConfigurationConfigApi ()
{

$config_command --info
$config_command --operation-id get-config-api-properties
$config_command --operation-id patch-config-api-properties


} >> output.log 2>>errors.log

SMTP ()

{

echo "${FUNCNAME[0]} testing started"

$config_command --info ConfigurationSMTP

$config_command --operation-id get-config-smtp

sed -e 's/"host": null/"host": "localhost"/'\
	-e 's/"port": null/"port": 3039/'\
	-e 's/from_name": null/from_name": "admin"/'\
	-e 's/"from_email_address": null/"from_email_address": "admin@gmail.com"/'\
	-e 's/"user_name": null/"user_name": "admin"/'\
	-e 's/"password": null/"password": "Admin@123"/' /tmp/SmtpConfiguration.json >/tmp/SmtpConfiguration_1.json 
$config_command --operation-id post-config-smtp --data /tmp/SmtpConfiguration_1.json
sed -e 's/"password": "Admin@123"/"password": "Admin@1234"/' /tmp/SmtpConfiguration_1.json >/tmp/SmtpConfiguration.json
$config_command --operation-id put-config-smtp --data /tmp/SmtpConfiguration.json

#$config_command --operation-id delete-config-smtp 

} >> output.log 2>>errors.log

UserResource ()
{
echo "${FUNCNAME[0]} testing started"

$scim_command --info User

$scim_command --operation-id get-users
$scim_command --operation-id get-users --endpoint-args count:1
sed -e 's/"userName": null/"userName": "user1"/'\
	-e 's/"familyName": null/"familyName": "user1"/'\
	-e 's/"givenName": null/"givenName": "user1"/'\
	-e 's/"middleName": null/"middleName": "user1"/'\
	-e 's/"displayName": null/"displayName": "user1"/'\
	-e 's/"password": null/"password": "user1"/'\
       -e 's/"urn:ietf:params:scim:schemas:extension:gluu:2.0:User": {}/"urn:ietf:params:scim:schemas:extension:gluu:2.0:User": null/'\
	-e 's/"schemas": []/"schemas": null/'       /tmp/UserResource.json > /tmp/UserResource_1.json

$scim_command --operation-id create-user --data /tmp/UserResource_1.json >/tmp/UserResource.json


$scim_command --operation-id get-user-by-id url-suffix: id

$scim_command --operation-id patch-user-by-id url-suffix: id


}   >> output.log 2>>errors.log




GroupResource ()
{
echo "${FUNCNAME[0]} testing started"


$scim_command --info Group

$scim_command --operation-id get-users

$scim_command --operation-id create-group --data /tmp/GroupResource.json

$scim_command --operation-id get-group-by-id --url-suffix id:766ffd8c-88a8-4aa8-a430-a5b3ae809c21

$scim_command --operation-id update-group-by-id --data /tmp/GroupResource.json --url-suffix id:56030854-2784-408e-8fa7-e11835804ac7

$scim_command --operation-id delete-group-by-id --url-suffix id:56030854-2784-408e-8fa7-e11835804ac7


}  >> output.log 2>>errors.log

AdminUILicense ()
{
echo "${FUNCNAME[0]} testing started"
$config_command --info AdminUILicense
$config_command --operation-id activate-adminui-license
$config_command --operation-id get-adminui-license
$config_command --operation-id save-license-api-credentials
$config_command --operation-id is-license-active

}  >> output.log 2>>errors.log

AdminUiPermission ()
{
echo "${FUNCNAME[0]} testing started"
$config_command --info AdminUiPermission
$config_command --operation-id get-all-adminui-permissions

$config_command --operation-id edit-adminui-permission
$config_command --operation-id add-adminui-permission
$config_command --operation-id get-adminui-permission
$config_command --operation-id delete-adminui-permission

}  >> output.log 2>>errors.log
AuthServerHealthCheck ()
{
echo "${FUNCNAME[0]} testing started"

$config_command --info AuthServerHealthCheck
$config_command --operation-id get-auth-server-health
} >> output.log 2>>errors.log

AuthSessionManagement ()
{
  echo "${FUNCNAME[0]} testing started"
$config_command --operation-id get-sessions
$config_command --operation-id revoke-user-session

}  >> output.log 2>>errors.log

StatisticsUser  ()
{
echo "${FUNCNAME[0]} testing started"
$config_command --info StatisticsUser

$config_command --operation-id get-stat
}  >> output.log 2>>errors.log


ConfigurationAgamaFlow ()
{
echo "${FUNCNAME[0]} testing started"
 $config_command --info ConfigurationAgamaFlow
 $config_command --operation-id  get-agama-flows
 $config_command --operation-id post-agama-flow
 $config_command --operation-id get-agama-flow
 $config_command --operation-id post-agama-flow-from-source
 $config_command --operation-id  patch-agama-flow
 $config_command --operation-id  delete-agama-flow
 $config_command --operation-id  put-agama-flow-from-source
}  >> output.log 2>>errors.log

AdminUIPermission ()
{
echo "${FUNCNAME[0]} testing started"
 $config_command --operation-id get-adminui-permissions
 $config_command --operation-id add-adminui-permission
 $config_command --operation-id edit-adminui-permission
 $config_command --operation-id delete-adminui-permission

} >> output.log 2>>errors.log

AdminUIRole()
{
echo "${FUNCNAME[0]} testing started"
 $config_command --operation-id get-adminui-roles
 $config_command --operation-id get-all-adminui-roles
 $config_command --operation-id add-adminui-role
 $config_command --operation-id edit-adminui-role
 $config_command --operation-id delete-adminui-role

} >> output.log 2>>errors.log
AdminUIRolePermissionsMapping()
{
echo "${FUNCNAME[0]} testing started"
$config_command --operation-id get-all-adminui-role-permissions
 $config_command --operation-id get-adminui-role-permissions
 $config_command --operation-id add-role-permissions-mapping
 $config_command --operation-id map-permissions-to-role
 $config_command --operation-id remove-role-permissions-permission

  
} >> output.log 2>>errors.log

AdminUILicense
AdminUIPermission
AdminUIRole
AdminUIRolePermissionsMapping
Attribute 
CacheConfiguration 
CouchBaseConfiguration 
CustomScript 
defaultMethod 
jansAuth 
FIDO 
JWK 
LDAP 
Logging 
OAuth 
UMA
OAuthopenID
SMTP
UserResource
GroupResource
StatisticsUser
AuthSessionManagement
AuthServerHealthCheck
HealthCheck
#CacheConfigurationInMemory 
#CacheConfigurationMemcached 
#CacheConfigurationNativePersistence 
#CacheConfigurationRedis 
ConfigurationAgamaFlow 
ConfigurationJWKJSONWebKeyJWK
ConfigurationLogging
ConfigurationProperties
ConfigurationSMTP
ConfigurationUserManagement
ConfigurationConfigApi
CustomScripts
DatabaseLDAPConfiguration
DefaultAuthenticationMethod
Fido2Configuration
HealthCheck
OAuthOpenIDConnectClients
OAuthScopes
OAuthUMAResources
OrganizationConfiguration
SCIMConfigManagement
ServerStats
StatisticsUser
