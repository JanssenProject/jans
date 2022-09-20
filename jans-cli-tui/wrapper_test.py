import os
import json
from cli import config_cli
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

# result = cli_object.process_command_by_id(
#         'get-oauth-uma-resources', '', 'limit:10', {})


result = cli_object.process_command_by_id(
        operation_id='get-oauth-uma-resources-by-clientid',
        url_suffix='clientId:1800.f33615dd-907d-45d5-9494-f358aa1a9912',
        endpoint_args='',
        data_fn=None,
        data={}
        )
print(result.text)

'''
{'dn': 'jansId=5635e18b-67b9-4997-a786-a6b2cdb84355,ou=resources,ou=uma,o=jans',
 'id': '5635e18b-67b9-4997-a786-a6b2cdb84355', 
'name': '[GET] /document',
 'scopes': ['inum=40a48740-4892-4fce-b30f-81c5c45670f4,ou=scopes,o=jans'],
  'clients': ['inum=e218bef8-a3cb-4b01-b1c6-e3514d98fb68,ou=clients,o=jans'],
   'rev': '1',
    'creationDate': '2022-09-15T09:03:14',
     'expirationDate': '2022-10-05T09:03:14', 
     'deletable': True}, 
     
{'dn': 'jansId=9b473b72-496b-4414-828c-a4d2bebca97b,ou=resources,ou=uma,o=jans', 
'id': '9b473b72-496b-4414-828c-a4d2bebca97b',
 'name': '[GET] /photo',
  'scopes': ['inum=40a48740-4892-4fce-b30f-81c5c45670f4,ou=scopes,o=jans'], 
  'clients': ['inum=e218bef8-a3cb-4b01-b1c6-e3514d98fb68,ou=clients,o=jans'],
   'rev': '1',
    'creationDate': '2022-09-15T09:03:13',
     'expirationDate': '2022-10-05T09:03:13', 'deletable': True},
     
{'dn': 'jansId=095a7fd8-e92f-43a2-a7d1-e52c123d3f02,ou=resources,ou=uma,o=jans',
 'id': '095a7fd8-e92f-43a2-a7d1-e52c123d3f02', 
 'name': '[PUT, POST] /photo', 
 'scopes': ['inum=514b71e9-b12b-42b5-9b9f-2453ae24ed08,ou=scopes,o=jans',
  'inum=19927467-d607-4c69-b145-f8752bd63e8c,ou=scopes,o=jans'], 
  'clients': ['inum=ac1ebdf4-a794-4fef-8bcq-33291221c4a5,ou=clients,o=jans'],
   'rev': '1', 
   'creationDate': '2022-09-15T09:03:14', 
   'expirationDate': '2022-10-05T09:03:14',
    'deletable': True}
    ]

'''
