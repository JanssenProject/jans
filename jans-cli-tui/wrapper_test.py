import os
import json
from cli_tui.cli import config_cli
test_client = config_cli.client_id if config_cli.test_client else None
config_cli.debug = True
cli_object = config_cli.JCA_CLI(
                host=config_cli.host, 
                client_id=config_cli.client_id,
                client_secret=config_cli.client_secret, 
                access_token=config_cli.access_token, 
                test_client=test_client
            )

#print(config_cli.host, config_cli.client_id, config_cli.client_secret, config_cli.access_token, cli_object.use_test_client)
status = cli_object.check_connection()

print(status)
"""
response = cli_object.get_device_verification_code()

result = response.json()
print(result)

input()

cli_object.get_jwt_access_token(result)

print(result)
"""

client_data = {
    "displayName": "Test 3",
    "clientSecret": "TopSecret",
    "redirectUris": ["https://www.jans.io/cb"],
    "applicationType": "web",
    "grantTypes": ["implicit", "refresh_token"]
}
"""
result = cli_object.process_command_by_id(
        operation_id='post-oauth-openid-clients',
        url_suffix='',
        endpoint_args='',
        data_fn='',
        data=client_data
        )


result = cli_object.process_command_by_id(
        operation_id='delete-oauth-openid-clients-by-inum',
        url_suffix='inum:7710112a-ce34-445b-8d85-fd18bec56ce5',
        endpoint_args='',
        data_fn='',
        data={}
        )

result = cli_object.process_command_by_id(
        operation_id='get-oauth-openid-clients',
        url_suffix='',
        endpoint_args='',
        data_fn='',
        data={}
        )

"""


# endpoint_args ='pattern:2B29' ### limit the responce and wrong pattern
data = {}
inum = '40a48740-4892-4fce-b30f-81c5c45670f4'  ## this user has 3 UMA
result = cli_object.process_command_by_id(
                        operation_id='get-all-attribute',
                        url_suffix='',
                        endpoint_args="pattern:3B47\,3692\,E7BC\,11AA",
                        data_fn=None,
                        data={}
                        )

x = result.json()  
print(x)
# print(len(x["entries"]))
# print(x["entries"])
# for i in x["entries"]:
#         if i['inum'] == '08E2':
#                 print(i['claimName'])



# endpoint_args ='limit:1,pattern:hasdaeat'
# endpoint_args ='limit:5,startIndex:0,pattern:sagasg'

# result = cli_object.process_command_by_id(
#                         operation_id='get-oauth-openid-clients',
#                         url_suffix='',
#                         endpoint_args=endpoint_args,
#                         data_fn=None,
#                         data={}
#                         )
# print(result.text)









'''
[
{"dn":"jansId=5635e18b-67b9-4997-a786-a6b2cdb84355,ou=resources,ou=uma,o=jans",
"id":"5635e18b-67b9-4997-a786-a6b2cdb84355",
"name":"[GET] /document",
"scopes":["inum=40a48740-4892-4fce-b30f-81c5c45670f4,ou=scopes,o=jans"],
"clients":["inum=a5f45938-97d6-408b-965a-229afb0552fa,ou=clients,o=jans"],
"creationDate":"2022-09-15T09:03:14",
"expirationDate":"2022-10-05T09:03:14",
"deletable":true},

{"dn":"jansId=9b473b72-496b-4414-828c-a4d2bebca97b,ou=resources,ou=uma,o=jans"
,"id":"9b473b72-496b-4414-828c-a4d2bebca97b",
"name":"[GET] /photo",
"scopes":["inum=40a48740-4892-4fce-b30f-81c5c45670f4,ou=scopes,o=jans"],
"clients":["inum=a5f45938-97d6-408b-965a-229afb0552fa,ou=clients,o=jans"],
"creationDate":"2022-09-15T09:03:13",
"expirationDate":"2022-10-05T09:03:13",
"deletable":true}
]
'''


'''
[
{"dn":"inum=1800.671CF9,ou=scopes,o=jans","inum":"1800.671CF9","displayName":"Config API scope https://jans.io/oauth/config/database/couchbase.readonly","id":"https://jans.io/oauth/config/database/couchbase.readonly","description":"View Couchbase database information","scopeType":"oauth","defaultScope":false,"attributes":{"showInConfigurationEndpoint":false},"creationDate":"2022-09-21T11:15:15","umaType":false,"baseDn":"inum=1800.671CF9,ou=scopes,o=jans"},
{"dn":"inum=F0C4,ou=scopes,o=jans","inum":"F0C4","displayName":"authenticate_openid_connect","id":"openid","description":"Authenticate using OpenID Connect.","scopeType":"openid","defaultScope":true,"attributes":{"showInConfigurationEndpoint":true},"creationDate":"2022-09-24T07:07:19","umaType":false,"baseDn":"inum=F0C4,ou=scopes,o=jans"},
{"dn":"inum=43F1,ou=scopes,o=jans","inum":"43F1","displayName":"view_profile","id":"profile","description":"View your basic profile info.","scopeType":"openid","claims":["inum=2B29,ou=attributes,o=jans","inum=0C85,ou=attributes,o=jans","inum=B4B0,ou=attributes,o=jans","inum=A0E8,ou=attributes,o=jans","inum=5EC6,ou=attributes,o=jans","inum=B52A,ou=attributes,o=jans","inum=64A0,ou=attributes,o=jans","inum=EC3A,ou=attributes,o=jans","inum=3B47,ou=attributes,o=jans","inum=3692,ou=attributes,o=jans","inum=98FC,ou=attributes,o=jans","inum=A901,ou=attributes,o=jans","inum=36D9,ou=attributes,o=jans","inum=BE64,ou=attributes,o=jans","inum=6493,ou=attributes,o=jans","inum=4CF1,ou=attributes,o=jans","inum=29DA,ou=attributes,o=jans"],"defaultScope":false,"attributes":{"showInConfigurationEndpoint":true},"creationDate":"2022-09-24T07:07:19","umaType":false,"baseDn":"inum=43F1,ou=scopes,o=jans"},
{"dn":"inum=1800.A92509,ou=scopes,o=jans","inum":"1800.A92509","displayName":"Config API scope https://jans.io/oauth/config/attributes.write","id":"https://jans.io/oauth/config/attributes.write","description":"Manage attribute related information","scopeType":"oauth","defaultScope":false,"attributes":{"showInConfigurationEndpoint":false},"creationDate":"2022-09-21T11:15:14","umaType":false,"baseDn":"inum=1800.A92509,ou=scopes,o=jans"},
{"dn":"inum=C4F5,ou=scopes,o=jans","inum":"C4F5","displayName":"view_user_permissions_roles","id":"permission","description":"View your user permission and roles.","scopeType":"dynamic","defaultScope":true,"dynamicScopeScripts":["inum=CB5B-3211,ou=scripts,o=jans"],"attributes":{"showInConfigurationEndpoint":true},"creationDate":"2022-09-24T07:07:19","umaType":false,"baseDn":"inum=C4F5,ou=scopes,o=jans"},
{"dn":"inum=1200.2B53F9,ou=scopes,o=jans","inum":"1200.2B53F9","displayName":"Scim users.read","id":"https://jans.io/scim/users.read","description":"Query user resources","scopeType":"oauth","attributes":{"showInConfigurationEndpoint":true},"creationDate":"2022-09-24T07:07:19","umaType":false,"baseDn":"inum=1200.2B53F9,ou=scopes,o=jans"},
{"dn":"inum=1800.642032,ou=scopes,o=jans","inum":"1800.642032","displayName":"Config API scope https://jans.io/oauth/config/fido2.write","id":"https://jans.io/oauth/config/fido2.write","description":"Manage FIDO2 related information","scopeType":"oauth","defaultScope":false,"attributes":{"showInConfigurationEndpoint":false},"creationDate":"2022-09-21T11:15:15","umaType":false,"baseDn":"inum=1800.642032,ou=scopes,o=jans"},
{"dn":"inum=1800.831F68,ou=scopes,o=jans","inum":"1800.831F68","displayName":"Config API scope https://jans.io/oauth/config/acrs.write","id":"https://jans.io/oauth/config/acrs.write","description":"Manage ACRS related information","scopeType":"oauth","defaultScope":false,"attributes":{"showInConfigurationEndpoint":false},"creationDate":"2022-09-21T11:15:14","umaType":false,"baseDn":"inum=1800.831F68,ou=scopes,o=jans"},
{"dn":"inum=1800.2BFAC6,ou=scopes,o=jans","inum":"1800.2BFAC6","displayName":"Config API scope https://jans.io/oauth/jans-auth-server/config/properties.write","id":"https://jans.io/oauth/jans-auth-server/config/properties.write","description":"Manage Auth Server properties related information","scopeType":"oauth","defaultScope":false,"attributes":{"showInConfigurationEndpoint":false},"creationDate":"2022-09-21T11:15:14","umaType":false,"baseDn":"inum=1800.2BFAC6,ou=scopes,o=jans"},
{"dn":"inum=1800.A9D2B6,ou=scopes,o=jans","inum":"1800.A9D2B6","displayName":"Config API scope https://jans.io/oauth/config/database/ldap.readonly","id":"https://jans.io/oauth/config/database/ldap.readonly","description":"View LDAP database related information","scopeType":"oauth","defaultScope":false,"attributes":{"showInConfigurationEndpoint":false},"creationDate":"2022-09-21T11:15:15","umaType":false,"baseDn":"inum=1800.A9D2B6,ou=scopes,o=jans"},
{"dn":"inum=1800.7DA888,ou=scopes,o=jans","inum":"1800.7DA888","displayName":"Config API scope https://jans.io/oauth/config/acrs.readonly","id":"https://jans.io/oauth/config/acrs.readonly","description":"View ACRS related information","scopeType":"oauth","defaultScope":false,"attributes":{"showInConfigurationEndpoint":false},"creationDate":"2022-09-21T11:15:14","umaType":false,"baseDn":"inum=1800.7DA888,ou=scopes,o=jans"},
{"dn":"inum=1800.E99751,ou=scopes,o=jans","inum":"1800.E99751","displayName":"Config API scope https://jans.io/oauth/config/scripts.write","id":"https://jans.io/oauth/config/scripts.write","description":"Manage scripts related information","scopeType":"oauth","defaultScope":false,"attributes":{"showInConfigurationEndpoint":false},"creationDate":"2022-09-21T11:15:15","umaType":false,"baseDn":"inum=1800.E99751,ou=scopes,o=jans"},
{"dn":"inum=1800.EB8C51,ou=scopes,o=jans","inum":"1800.EB8C51","displayName":"Config API scope https://jans.io/oauth/config/attributes.readonly","id":"https://jans.io/oauth/config/attributes.readonly","description":"View attribute related information","scopeType":"oauth","defaultScope":false,"attributes":{"showInConfigurationEndpoint":false},"creationDate":"2022-09-21T11:15:14","umaType":false,"baseDn":"inum=1800.EB8C51,ou=scopes,o=jans"},
{"dn":"inum=1800.6B63B0,ou=scopes,o=jans","inum":"1800.6B63B0","displayName":"Config API scope https://jans.io/oauth/config/smtp.delete","id":"https://jans.io/oauth/config/smtp.delete","description":"Delete SMTP related information","scopeType":"oauth","defaultScope":false,"attributes":{"showInConfigurationEndpoint":false},"creationDate":"2022-09-21T11:15:14","umaType":false,"baseDn":"inum=1800.6B63B0,ou=scopes,o=jans"},
{"dn":"inum=6D99,ou=scopes,o=jans","inum":"6D99","displayName":"UMA Protection","id":"uma_protection","description":"Obtain UMA PAT.","scopeType":"openid","defaultScope":true,"attributes":{"showInConfigurationEndpoint":true},"creationDate":"2022-09-24T07:07:19","umaType":false,"baseDn":"inum=6D99,ou=scopes,o=jans"},
{"dn":"inum=C4F7,ou=scopes,o=jans","inum":"C4F7","id":"jans_stat","description":"This scope is required for calling Statistic Endpoint","scopeType":"openid","defaultScope":false,"attributes":{"showInConfigurationEndpoint":false},"creationDate":"2022-09-24T07:07:19","umaType":false,"baseDn":"inum=C4F7,ou=scopes,o=jans"},
{"dn":"inum=1800.AA3FFB,ou=scopes,o=jans","inum":"1800.AA3FFB","displayName":"Config API scope https://jans.io/oauth/config/uma/resources.readonly","id":"https://jans.io/oauth/config/uma/resources.readonly","description":"View UMA Resource related information","scopeType":"oauth","defaultScope":false,"attributes":{"showInConfigurationEndpoint":false},"creationDate":"2022-09-21T11:15:15","umaType":false,"baseDn":"inum=1800.AA3FFB,ou=scopes,o=jans"},
{"dn":"inum=1800.5E2668,ou=scopes,o=jans","inum":"1800.5E2668","displayName":"Config API scope https://jans.io/oauth/config/logging.write","id":"https://jans.io/oauth/config/logging.write","description":"Manage logging related information","scopeType":"oauth","defaultScope":false,"attributes":{"showInConfigurationEndpoint":false},"creationDate":"2022-09-21T11:15:15","umaType":false,"baseDn":"inum=1800.5E2668,ou=scopes,o=jans"},
{"dn":"inum=1800.E9EE2A,ou=scopes,o=jans","inum":"1800.E9EE2A","displayName":"Config API scope https://jans.io/oauth/config/openid/clients.readonly","id":"https://jans.io/oauth/config/openid/clients.readonly","description":"View clients related information","scopeType":"oauth","defaultScope":false,"attributes":{"showInConfigurationEndpoint":false},"creationDate":"2022-09-21T11:15:14","umaType":false,"baseDn":"inum=1800.E9EE2A,ou=scopes,o=jans"},
{"dn":"inum=1800.5D8461,ou=scopes,o=jans","inum":"1800.5D8461","displayName":"Config API scope https://jans.io/oauth/config/scopes.write","id":"https://jans.io/oauth/config/scopes.write","description":"Manage scope related information","scopeType":"oauth","defaultScope":false,"attributes":{"showInConfigurationEndpoint":false},"creationDate":"2022-09-21T11:15:15","umaType":false,"baseDn":"inum=1800.5D8461,ou=scopes,o=jans"},
{"dn":"inum=D491,ou=scopes,o=jans","inum":"D491","displayName":"view_phone_number","id":"phone","description":"View your phone number.","scopeType":"openid","claims":["inum=B17A,ou=attributes,o=jans","inum=0C18,ou=attributes,o=jans"],"defaultScope":false,"attributes":{"showInConfigurationEndpoint":true},"creationDate":"2022-09-24T07:07:19","umaType":false,"baseDn":"inum=D491,ou=scopes,o=jans"},
{"dn":"inum=C17A,ou=scopes,o=jans","inum":"C17A","displayName":"view_address","id":"address","description":"View your address.","scopeType":"openid","claims":["inum=27DB,ou=attributes,o=jans","inum=2A3D,ou=attributes,o=jans","inum=6609,ou=attributes,o=jans","inum=6EEB,ou=attributes,o=jans","inum=BCE8,ou=attributes,o=jans","inum=D90B,ou=attributes,o=jans","inum=E6B8,ou=attributes,o=jans","inum=E999,ou=attributes,o=jans"],"defaultScope":false,"groupClaims":true,"attributes":{"showInConfigurationEndpoint":true},"creationDate":"2022-09-24T07:07:19","umaType":false,"baseDn":"inum=C17A,ou=scopes,o=jans"},
{"dn":"inum=1800.31BA7D,ou=scopes,o=jans","inum":"1800.31BA7D","displayName":"Config API scope https://jans.io/oauth/config/attributes.delete","id":"https://jans.io/oauth/config/attributes.delete","description":"Delete attribute related information","scopeType":"oauth","defaultScope":false,"attributes":{"showInConfigurationEndpoint":false},"creationDate":"2022-09-21T11:15:14","umaType":false,"baseDn":"inum=1800.31BA7D,ou=scopes,o=jans"},
{"dn":"inum=1200.BAC91C,ou=scopes,o=jans","inum":"1200.BAC91C","displayName":"Scim users.write","id":"https://jans.io/scim/users.write","description":"Modify user resources","scopeType":"oauth","attributes":{"showInConfigurationEndpoint":true},"creationDate":"2022-09-24T07:07:19","umaType":false,"baseDn":"inum=1200.BAC91C,ou=scopes,o=jans"},
{"dn":"inum=1800.F0B5D0,ou=scopes,o=jans","inum":"1800.F0B5D0","displayName":"Config API scope https://jans.io/oauth/config/database/couchbase.delete","id":"https://jans.io/oauth/config/database/couchbase.delete","description":"Delete Couchbase database related information","scopeType":"oauth","defaultScope":false,"attributes":{"showInConfigurationEndpoint":false},"creationDate":"2022-09-21T11:15:15","umaType":false,"baseDn":"inum=1800.F0B5D0,ou=scopes,o=jans"},
{"dn":"inum=1800.2928DA,ou=scopes,o=jans","inum":"1800.2928DA","displayName":"Config API scope https://jans.io/oauth/config/database/couchbase.write","id":"https://jans.io/oauth/config/database/couchbase.write","description":"Manage Couchbase database related information","scopeType":"oauth","defaultScope":false,"attributes":{"showInConfigurationEndpoint":false},"creationDate":"2022-09-21T11:15:15","umaType":false,"baseDn":"inum=1800.2928DA,ou=scopes,o=jans"},
{"dn":"inum=1800.7382A8,ou=scopes,o=jans","inum":"1800.7382A8","displayName":"Config API scope https://jans.io/oauth/config/cache.readonly","id":"https://jans.io/oauth/config/cache.readonly","description":"View cache related information","scopeType":"oauth","defaultScope":false,"attributes":{"showInConfigurationEndpoint":false},"creationDate":"2022-09-21T11:15:14","umaType":false,"baseDn":"inum=1800.7382A8,ou=scopes,o=jans"},
{"dn":"inum=8A01,ou=scopes,o=jans","inum":"8A01","displayName":"view_mobile_phone_number","id":"mobile_phone","description":"View your mobile phone number.","scopeType":"openid","claims":["inum=6DA6,ou=attributes,o=jans"],"defaultScope":false,"attributes":{"showInConfigurationEndpoint":true},"creationDate":"2022-09-24T07:07:19","umaType":false,"baseDn":"inum=8A01,ou=scopes,o=jans"},
{"dn":"inum=7D90,ou=scopes,o=jans","inum":"7D90","displayName":"revoke_session","id":"revoke_session","description":"revoke_session scope which is required to be able call /revoke_session endpoint","scopeType":"openid","defaultScope":false,"attributes":{"showInConfigurationEndpoint":true},"creationDate":"2022-09-24T07:07:19","umaType":false,"baseDn":"inum=7D90,ou=scopes,o=jans"},
{"dn":"inum=10B2,ou=scopes,o=jans","inum":"10B2","displayName":"view_username","id":"user_name","description":"View your local username in the Janssen Server.","scopeType":"openid","claims":["inum=42E0,ou=attributes,o=jans"],"defaultScope":false,"attributes":{"showInConfigurationEndpoint":true},"creationDate":"2022-09-24T07:07:19","umaType":false,"baseDn":"inum=10B2,ou=scopes,o=jans"},
{"dn":"inum=764C,ou=scopes,o=jans","inum":"764C","displayName":"view_email_address","id":"email","description":"View your email address.","scopeType":"openid","claims":["inum=8F88,ou=attributes,o=jans","inum=CAE3,ou=attributes,o=jans"],"defaultScope":false,"attributes":{"showInConfigurationEndpoint":true},"creationDate":"2022-09-24T07:07:19","umaType":false,"baseDn":"inum=764C,ou=scopes,o=jans"},
{"dn":"inum=1800.F4D6ED,ou=scopes,o=jans","inum":"1800.F4D6ED","displayName":"Config API scope https://jans.io/oauth/config/openid/clients.delete","id":"https://jans.io/oauth/config/openid/clients.delete","description":"Delete clients related information","scopeType":"oauth","defaultScope":false,"attributes":{"showInConfigurationEndpoint":false},"creationDate":"2022-09-21T11:15:14","umaType":false,"baseDn":"inum=1800.F4D6ED,ou=scopes,o=jans"},
{"dn":"inum=1800.F74870,ou=scopes,o=jans","inum":"1800.F74870","displayName":"Config API scope https://jans.io/oauth/config/scripts.readonly","id":"https://jans.io/oauth/config/scripts.readonly","description":"View cache scripts information","scopeType":"oauth","defaultScope":false,"attributes":{"showInConfigurationEndpoint":false},"creationDate":"2022-09-21T11:15:15","umaType":false,"baseDn":"inum=1800.F74870,ou=scopes,o=jans"},
{"dn":"inum=1800.26E74A,ou=scopes,o=jans","inum":"1800.26E74A","displayName":"Config API scope https://jans.io/oauth/config/jwks.write","id":"https://jans.io/oauth/config/jwks.write","description":"Manage JWKS related information","scopeType":"oauth","defaultScope":false,"attributes":{"showInConfigurationEndpoint":false},"creationDate":"2022-09-21T11:15:15","umaType":false,"baseDn":"inum=1800.26E74A,ou=scopes,o=jans"},
{"dn":"inum=1800.238093,ou=scopes,o=jans","inum":"1800.238093","displayName":"Config API scope https://jans.io/oauth/config/cache.write","id":"https://jans.io/oauth/config/cache.write","description":"Manage cache related information","scopeType":"oauth","defaultScope":false,"attributes":{"showInConfigurationEndpoint":false},"creationDate":"2022-09-21T11:15:14","umaType":false,"baseDn":"inum=1800.238093,ou=scopes,o=jans"},
{"dn":"inum=341A,ou=scopes,o=jans","inum":"341A","displayName":"view_client","id":"clientinfo","description":"View the client info.","scopeType":"openid","claims":["inum=2B29,ou=attributes,o=jans","inum=29DA,ou=attributes,o=jans"],"defaultScope":false,"attributes":{"showInConfigurationEndpoint":true},"creationDate":"2022-09-24T07:07:19","umaType":false,"baseDn":"inum=341A,ou=scopes,o=jans"},
{"dn":"inum=1800.E3A23B,ou=scopes,o=jans","inum":"1800.E3A23B","displayName":"Config API scope https://jans.io/oauth/config/jwks.readonly","id":"https://jans.io/oauth/config/jwks.readonly","description":"View JWKS related information","scopeType":"oauth","defaultScope":false,"attributes":{"showInConfigurationEndpoint":false},"creationDate":"2022-09-21T11:15:15","umaType":false,"baseDn":"inum=1800.E3A23B,ou=scopes,o=jans"},
{"dn":"inum=1800.9A234E,ou=scopes,o=jans","inum":"1800.9A234E","displayName":"Config API scope https://jans.io/oauth/config/logging.readonly","id":"https://jans.io/oauth/config/logging.readonly","description":"View logging related information","scopeType":"oauth","defaultScope":false,"attributes":{"showInConfigurationEndpoint":false},"creationDate":"2022-09-21T11:15:15","umaType":false,"baseDn":"inum=1800.9A234E,ou=scopes,o=jans"},
{"dn":"inum=1800.C86963,ou=scopes,o=jans","inum":"1800.C86963","displayName":"Config API scope https://jans.io/oauth/config/fido2.readonly","id":"https://jans.io/oauth/config/fido2.readonly","description":"View FIDO2 related information","scopeType":"oauth","defaultScope":false,"attributes":{"showInConfigurationEndpoint":false},"creationDate":"2022-09-21T11:15:15","umaType":false,"baseDn":"inum=1800.C86963,ou=scopes,o=jans"},
{"dn":"inum=1800.63BC87,ou=scopes,o=jans","inum":"1800.63BC87","displayName":"Config API scope https://jans.io/oauth/config/smtp.readonly","id":"https://jans.io/oauth/config/smtp.readonly","description":"View SMTP related information","scopeType":"oauth","defaultScope":false,"attributes":{"showInConfigurationEndpoint":false},"creationDate":"2022-09-21T11:15:14","umaType":false,"baseDn":"inum=1800.63BC87,ou=scopes,o=jans"},
{"dn":"inum=1800.1101C3,ou=scopes,o=jans","inum":"1800.1101C3","displayName":"Config API scope https://jans.io/oauth/config/database/ldap.write","id":"https://jans.io/oauth/config/database/ldap.write","description":"Manage LDAP database related information","scopeType":"oauth","defaultScope":false,"attributes":{"showInConfigurationEndpoint":false},"creationDate":"2022-09-21T11:15:15","umaType":false,"baseDn":"inum=1800.1101C3,ou=scopes,o=jans"},
{"dn":"inum=1800.487E8C,ou=scopes,o=jans","inum":"1800.487E8C","displayName":"Config API scope https://jans.io/oauth/config/database/ldap.delete","id":"https://jans.io/oauth/config/database/ldap.delete","description":"Delete LDAP database related information","scopeType":"oauth","defaultScope":false,"attributes":{"showInConfigurationEndpoint":false},"creationDate":"2022-09-21T11:15:15","umaType":false,"baseDn":"inum=1800.487E8C,ou=scopes,o=jans"},
{"dn":"inum=1800.8BA54D,ou=scopes,o=jans","inum":"1800.8BA54D","displayName":"Config API scope https://jans.io/oauth/config/scripts.delete","id":"https://jans.io/oauth/config/scripts.delete","description":"Delete scripts related information","scopeType":"oauth","defaultScope":false,"attributes":{"showInConfigurationEndpoint":false},"creationDate":"2022-09-21T11:15:15","umaType":false,"baseDn":"inum=1800.8BA54D,ou=scopes,o=jans"},
{"dn":"inum=6D90,ou=scopes,o=jans","inum":"6D90","displayName":"jans_client_api","id":"jans_client_api","description":"jans_client_api scope which is required to call jans_client_api API","scopeType":"openid","defaultScope":true,"attributes":{"showInConfigurationEndpoint":true},"creationDate":"2022-09-24T07:07:19","umaType":false,"baseDn":"inum=6D90,ou=scopes,o=jans"},
{"dn":"inum=1800.FA84EE,ou=scopes,o=jans","inum":"1800.FA84EE","displayName":"Config API scope https://jans.io/oauth/config/scopes.delete","id":"https://jans.io/oauth/config/scopes.delete","description":"Delete scope related information","scopeType":"oauth","defaultScope":false,"attributes":{"showInConfigurationEndpoint":false},"creationDate":"2022-09-21T11:15:15","umaType":false,"baseDn":"inum=1800.FA84EE,ou=scopes,o=jans"},
{"dn":"inum=1800.41F22A,ou=scopes,o=jans","inum":"1800.41F22A","displayName":"Config API scope https://jans.io/oauth/config/smtp.write","id":"https://jans.io/oauth/config/smtp.write","description":"Manage SMTP related information","scopeType":"oauth","defaultScope":false,"attributes":{"showInConfigurationEndpoint":false},"creationDate":"2022-09-21T11:15:14","umaType":false,"baseDn":"inum=1800.41F22A,ou=scopes,o=jans"},
{"dn":"inum=1800.C95FD0,ou=scopes,o=jans","inum":"1800.C95FD0","displayName":"Config API scope https://jans.io/oauth/jans-auth-server/config/properties.readonly","id":"https://jans.io/oauth/jans-auth-server/config/properties.readonly","description":"View Auth Server properties related information","scopeType":"oauth","defaultScope":false,"attributes":{"showInConfigurationEndpoint":false},"creationDate":"2022-09-21T11:15:14","umaType":false,"baseDn":"inum=1800.C95FD0,ou=scopes,o=jans"},
{"dn":"inum=1800.55DAE0,ou=scopes,o=jans","inum":"1800.55DAE0","displayName":"Config API scope https://jans.io/oauth/config/openid/clients.write","id":"https://jans.io/oauth/config/openid/clients.write","description":"Manage clients related information","scopeType":"oauth","defaultScope":false,"attributes":{"showInConfigurationEndpoint":false},"creationDate":"2022-09-21T11:15:14","umaType":false,"baseDn":"inum=1800.55DAE0,ou=scopes,o=jans"},
{"dn":"inum=1800.446159,ou=scopes,o=jans","inum":"1800.446159","displayName":"Config API scope https://jans.io/oauth/config/scopes.readonly","id":"https://jans.io/oauth/config/scopes.readonly","description":"View scope related information","scopeType":"oauth","defaultScope":false,"attributes":{"showInConfigurationEndpoint":false},"creationDate":"2022-09-21T11:15:15","umaType":false,"baseDn":"inum=1800.446159,ou=scopes,o=jans"},
{"dn":"inum=C4F6,ou=scopes,o=jans","inum":"C4F6","displayName":"refresh_token","id":"offline_access","description":"This scope value requests that an OAuth 2.0 Refresh Token be issued.","scopeType":"openid","defaultScope":true,"attributes":{"showInConfigurationEndpoint":true},"creationDate":"2022-09-24T07:07:19","umaType":false,"baseDn":"inum=C4F6,ou=scopes,o=jans"}]

'''

