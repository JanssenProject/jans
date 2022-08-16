from cli import config_cli
test_client = config_cli.client_id if config_cli.test_client else None
cli_object = config_cli.JCA_CLI(
                host=config_cli.host, 
                client_id=config_cli.client_id,
                client_secret=config_cli.client_secret, 
                access_token=config_cli.access_token, 
                test_client=test_client
            )

print(config_cli.host, config_cli.client_id, config_cli.client_secret, config_cli.access_token, cli_object.use_test_client)
status = cli_object.check_connection()

print(status)

response = cli_object.get_device_verification_code()

result = response.json()
print(result)

input()

cli_object.get_jwt_access_token(result)

print(result)
