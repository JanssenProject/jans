'''
Project: Test Auth Client
Author: Christian Hawk

Licensed under the Apache License, Version 2.0 (the 'License');
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an 'AS IS' BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
'''

CLIENT_ID = ""
CLIENT_SECRET = ""
SERVER_META_URL = 'https://op-hostname/.well-known/openid-configuration'
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
