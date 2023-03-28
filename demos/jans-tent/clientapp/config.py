# Replace op_hostname
ISSUER = 'https://op_hostname'

REDIRECT_URIS = [
    'https://localhost:9090/oidc_callback'
]

# Token authentication method can be
# client_secret_basic
# client_secret_post
# none
SERVER_TOKEN_AUTH_METHOD = "client_secret_post"

# ACR VALUES
# Examples:
# ACR_VALUES = "agama"
# ACR_VALUES = 'simple_password_auth'
ACR_VALUES = None

# ADDITIONAL PARAMS TO CALL AUTHORIZE ENDPOINT, WITHOUT BASE64 ENCODING. USE DICT {'param': 'value'}
# ADDITIONAL_PARAMS = {'paramOne': 'valueOne', 'paramTwo': 'valueTwo'}
ADDITIONAL_PARAMS = None

# SYSTEM SETTINGS
# use with caution, unsecure requests, for development environments
SSL_VERIFY = False
