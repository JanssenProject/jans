'''
Project: Test Auth Client
Author: Christian Hawk
Copyright 2023 Christian Hawk

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
CLIENT_AUTH_URI = "https://your-authorize-endpoint"
TOKEN_URI = "https://your-token-endpoint"
USERINFO_URI = "https://your-userinfo-endpoint"
REDIRECT_URIS = [
    'https://localhost:9090/oidc_callback'
]
ISSUER = "https://your-server's-fqdn"

SERVER_META_URL = "https://your-openid-configuration-uri"

# Token authentication method can be
# client_secret_basic
# client_secret_post
# none

SERVER_TOKEN_AUTH_METHOD = "client_secret_post"

# for gluu
ACR_VALUES = 'inbound_saml'
PRE_SELECTED_PROVIDER = False
PRE_SELECTED_PROVIDER_ID = ''
HAS_PROVIDER_HOST = False
PROVIDER_HOST_STRING = None
# PROVIDER_HOST_STRING = 'samltest.id'

# SYSTEM SETTINGS
# use with caution, unsecure requests, for develpment environments
SSL_VERIFY = True
