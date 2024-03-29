
from unittest import TestCase
from unittest.mock import MagicMock
import clientapp
from clientapp import create_app
from clientapp.helpers.client_handler import ClientHandler
from flask import Flask
from typing import List
import helper
import os
import builtins


class FlaskBaseTestCase(TestCase):
    def setUp(self):
        self.stashed_add_config_from_json = clientapp.add_config_from_json
        clientapp.cfg.CLIENT_ID = 'any-client-id-stub'
        clientapp.cfg.CLIENT_SECRET = 'any-client-secret-stub'
        clientapp.cfg.SERVER_META_URL = 'https://ophostname.com/server/meta/url'
        clientapp.cfg.END_SESSION_ENDPOINT = 'https://ophostname.com/end_session_endpoint'
        clientapp.add_config_from_json = MagicMock(name='add_config_from_json')
        clientapp.add_config_from_json.return_value(None)
        self.stashed_discover = ClientHandler.discover
        self.stashed_register_client = ClientHandler.register_client
        self.stashed_open = builtins.open
        builtins.open = MagicMock(name='open')
        ClientHandler.discover = MagicMock(name='discover')
        ClientHandler.discover.return_value = helper.OP_DATA_DICT_RESPONSE
        ClientHandler.register_client = MagicMock(name='register_client')
        ClientHandler.register_client.return_value = helper.REGISTER_CLIENT_RESPONSE
        self.app = create_app()
        self.app.testing = True
        self.app_context = self.app.test_request_context(
            base_url="https://chris.testingenv.org")
        self.app_context.push()
        self.client = self.app.test_client()

        #self.oauth = OAuth(self.app)
        os.environ['AUTHLIB_INSECURE_TRANSPORT'] = "1"

    def tearDown(self) -> None:
        ClientHandler.discover = self.stashed_discover
        ClientHandler.register_client = self.stashed_register_client
        builtins.open = self.stashed_open
        clientapp.add_config_from_json = self.stashed_add_config_from_json


# Helper functions
def app_endpoints(app: Flask) -> List[str]:
    """ Return all enpoints in app """
    endpoints = []
    for item in app.url_map.iter_rules():
        endpoint = item.endpoint.replace("_", "-")
        endpoints.append(endpoint)
    return endpoints


# Mocks
OP_DATA_DICT_RESPONSE = {
    'request_parameter_supported': True,
    'token_revocation_endpoint': 'https://t1.techno24x7.com/oxauth/restv1/revoke',
    'introspection_endpoint': 'https://t1.techno24x7.com/oxauth/restv1/introspection',
    'claims_parameter_supported': False,
    'issuer': 'https://t1.techno24x7.com',
    'userinfo_encryption_enc_values_supported': ['RSA1_5', 'RSA-OAEP', 'A128KW', 'A256KW'],
    'id_token_encryption_enc_values_supported': ['A128CBC+HS256', 'A256CBC+HS512', 'A128GCM', 'A256GCM'],
    'authorization_endpoint': 'https://t1.techno24x7.com/oxauth/restv1/authorize',
    'service_documentation': 'http://gluu.org/docs',
    'id_generation_endpoint': 'https://t1.techno24x7.com/oxauth/restv1/id',
    'claims_supported': ['street_address', 'country', 'zoneinfo', 'birthdate', 'role', 'gender', 'formatted',
                         'user_name', 'phone_mobile_number', 'preferred_username', 'locale', 'inum', 'updated_at',
                         'nickname', 'email', 'website', 'email_verified', 'profile', 'locality',
                         'phone_number_verified', 'given_name', 'middle_name', 'picture', 'name', 'phone_number',
                         'postal_code', 'region', 'family_name'],
    'scope_to_claims_mapping': [{
        'profile': ['name', 'family_name', 'given_name', 'middle_name', 'nickname', 'preferred_username', 'profile',
                    'picture', 'website', 'gender', 'birthdate', 'zoneinfo', 'locale', 'updated_at']
    }, {
        'openid': []
    }, {
        'https://t1.techno24x7.com/oxauth/restv1/uma/scopes/scim_access': []
    }, {
        'permission': ['role']
    }, {
        'super_gluu_ro_session': []
    }, {
        'https://t1.techno24x7.com/oxauth/restv1/uma/scopes/passport_access': []
    }, {
        'phone': ['phone_number_verified', 'phone_number']
    }, {
        'revoke_session': []
    }, {
        'address': ['formatted', 'postal_code', 'street_address', 'locality', 'country', 'region']
    }, {
        'clientinfo': ['name', 'inum']
    }, {
        'mobile_phone': ['phone_mobile_number']
    }, {
        'email': ['email_verified', 'email']
    }, {
        'user_name': ['user_name']
    }, {
        'oxtrust-api-write': []
    }, {
        'oxd': []
    }, {
        'uma_protection': []
    }, {
        'oxtrust-api-read': []
    }],
    'op_policy_uri': 'http://ox.gluu.org/doku.php?id=oxauth:policy',
    'token_endpoint_auth_methods_supported': ['client_secret_basic', 'client_secret_post', 'client_secret_jwt',
                                              'private_key_jwt', 'tls_client_auth', 'self_signed_tls_client_auth'],
    'tls_client_certificate_bound_access_tokens': True,
    'response_modes_supported': ['query', 'form_post', 'fragment'],
    'backchannel_logout_session_supported': True,
    'token_endpoint': 'https://t1.techno24x7.com/oxauth/restv1/token',
    'response_types_supported': ['code id_token', 'code', 'id_token', 'token', 'code token', 'code id_token token',
                                 'id_token token'],
    'request_uri_parameter_supported': True,
    'backchannel_user_code_parameter_supported': False,
    'grant_types_supported': ['implicit', 'refresh_token', 'client_credentials', 'authorization_code', 'password',
                              'urn:ietf:params:oauth:grant-type:uma-ticket'],
    'ui_locales_supported': ['en', 'bg', 'de', 'es', 'fr', 'it', 'ru', 'tr'],
    'userinfo_endpoint': 'https://t1.techno24x7.com/oxauth/restv1/userinfo',
    'op_tos_uri': 'http://ox.gluu.org/doku.php?id=oxauth:tos',
    'auth_level_mapping': {
        '-1': ['simple_password_auth'],
        '60': ['passport_saml'],
        '40': ['passport_social']
    },
    'require_request_uri_registration': False,
    'id_token_encryption_alg_values_supported': ['RSA1_5', 'RSA-OAEP', 'A128KW', 'A256KW'],
    'frontchannel_logout_session_supported': True,
    'claims_locales_supported': ['en'],
    'clientinfo_endpoint': 'https://t1.techno24x7.com/oxauth/restv1/clientinfo',
    'request_object_signing_alg_values_supported': ['none', 'HS256', 'HS384', 'HS512', 'RS256', 'RS384', 'RS512',
                                                    'ES256', 'ES384', 'ES512'],
    'request_object_encryption_alg_values_supported': ['RSA1_5', 'RSA-OAEP', 'A128KW', 'A256KW'],
    'session_revocation_endpoint': 'https://t1.techno24x7.com/oxauth/restv1/revoke_session',
    'check_session_iframe': 'https://t1.techno24x7.com/oxauth/opiframe.htm',
    'scopes_supported': ['address', 'openid', 'clientinfo', 'user_name', 'profile',
                         'https://t1.techno24x7.com/oxauth/restv1/uma/scopes/scim_access', 'uma_protection',
                         'permission', 'revoke_session', 'oxtrust-api-write', 'oxtrust-api-read', 'phone',
                         'mobile_phone', 'oxd', 'super_gluu_ro_session', 'email',
                         'https://t1.techno24x7.com/oxauth/restv1/uma/scopes/passport_access'],
    'backchannel_logout_supported': True,
    'acr_values_supported': ['simple_password_auth', 'passport_saml', 'urn:oasis:names:tc:SAML:2.0:ac:classes:Password',
                             'urn:oasis:names:tc:SAML:2.0:ac:classes:InternetProtocol',
                             'urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport', 'passport_social'],
    'request_object_encryption_enc_values_supported': ['A128CBC+HS256', 'A256CBC+HS512', 'A128GCM', 'A256GCM'],
    'display_values_supported': ['page', 'popup'],
    'userinfo_signing_alg_values_supported': ['HS256', 'HS384', 'HS512', 'RS256', 'RS384', 'RS512', 'ES256', 'ES384',
                                              'ES512'],
    'claim_types_supported': ['normal'],
    'userinfo_encryption_alg_values_supported': ['RSA1_5', 'RSA-OAEP', 'A128KW', 'A256KW'],
    'end_session_endpoint': 'https://t1.techno24x7.com/oxauth/restv1/end_session',
    'revocation_endpoint': 'https://t1.techno24x7.com/oxauth/restv1/revoke',
    'backchannel_authentication_endpoint': 'https://t1.techno24x7.com/oxauth/restv1/bc-authorize',
    'token_endpoint_auth_signing_alg_values_supported': ['HS256', 'HS384', 'HS512', 'RS256', 'RS384', 'RS512', 'ES256',
                                                         'ES384', 'ES512'],
    'frontchannel_logout_supported': True,
    'jwks_uri': 'https://t1.techno24x7.com/oxauth/restv1/jwks',
    'subject_types_supported': ['public', 'pairwise'],
    'id_token_signing_alg_values_supported': ['none', 'HS256', 'HS384', 'HS512', 'RS256', 'RS384', 'RS512', 'ES256',
                                              'ES384', 'ES512'],
    'registration_endpoint': 'https://t1.techno24x7.com/oxauth/restv1/register',
    'id_token_token_binding_cnf_values_supported': ['tbh']
}

REGISTER_CLIENT_RESPONSE = {'allow_spontaneous_scopes': False, 'application_type': 'web', 'rpt_as_jwt': False,
                            'registration_client_uri': 'https://t1.techno24x7.com/jans-auth/restv1/register?client_id'
                                                       '=079f3682-3d60-4bca-8ff7-bbc7dbc18cd7',
                            'run_introspection_script_before_jwt_creation': False,
                            'registration_access_token': '89c51fd6-34ec-497e-a4ae-85e21b7e725b',
                            'client_id': '079f3682-3d60-4bca-8ff7-bbc7dbc18cd7',
                            'token_endpoint_auth_method': 'client_secret_post',
                            'scope': 'online_access device_sso openid permission uma_protection offline_access',
                            'client_secret': '8f53c454-f6ee-4181-8581-9f1ee120b878', 'client_id_issued_at': 1680038429,
                            'backchannel_logout_session_required': False, 'client_name': 'Jans Tent',
                            'par_lifetime': 600, 'spontaneous_scopes': [], 'id_token_signed_response_alg': 'RS256',
                            'access_token_as_jwt': False, 'grant_types': ['authorization_code'],
                            'subject_type': 'pairwise', 'additional_token_endpoint_auth_methods': [],
                            'keep_client_authorization_after_expiration': False, 'require_par': False,
                            'redirect_uris': ['https://localhost:9090/oidc_callback'], 'additional_audience': [],
                            'frontchannel_logout_session_required': False, 'client_secret_expires_at': 0,
                            'access_token_signing_alg': 'RS256', 'response_types': ['code']}


