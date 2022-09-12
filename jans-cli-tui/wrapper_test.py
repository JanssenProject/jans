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

result = cli_object.process_command_by_id('get-oauth-scopes', '', 'limit:10', {})

print(result)
