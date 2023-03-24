from flask import Flask
from typing import List

# Helper functions
def app_endpoints(app: Flask) -> List[str]:
    """ Return all enpoints in app """
    endpoints = []
    for item in app.url_map.iter_rules():
        endpoint = item.endpoint.replace("_","-")
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
  'claims_supported': ['street_address', 'country', 'zoneinfo', 'birthdate', 'role', 'gender', 'formatted', 'user_name', 'phone_mobile_number', 'preferred_username', 'locale', 'inum', 'updated_at', 'nickname', 'email', 'website', 'email_verified', 'profile', 'locality', 'phone_number_verified', 'given_name', 'middle_name', 'picture', 'name', 'phone_number', 'postal_code', 'region', 'family_name'],
  'scope_to_claims_mapping': [{
    'profile': ['name', 'family_name', 'given_name', 'middle_name', 'nickname', 'preferred_username', 'profile', 'picture', 'website', 'gender', 'birthdate', 'zoneinfo', 'locale', 'updated_at']
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
  'token_endpoint_auth_methods_supported': ['client_secret_basic', 'client_secret_post', 'client_secret_jwt', 'private_key_jwt', 'tls_client_auth', 'self_signed_tls_client_auth'],
  'tls_client_certificate_bound_access_tokens': True,
  'response_modes_supported': ['query', 'form_post', 'fragment'],
  'backchannel_logout_session_supported': True,
  'token_endpoint': 'https://t1.techno24x7.com/oxauth/restv1/token',
  'response_types_supported': ['code id_token', 'code', 'id_token', 'token', 'code token', 'code id_token token', 'id_token token'],
  'request_uri_parameter_supported': True,
  'backchannel_user_code_parameter_supported': False,
  'grant_types_supported': ['implicit', 'refresh_token', 'client_credentials', 'authorization_code', 'password', 'urn:ietf:params:oauth:grant-type:uma-ticket'],
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
  'request_object_signing_alg_values_supported': ['none', 'HS256', 'HS384', 'HS512', 'RS256', 'RS384', 'RS512', 'ES256', 'ES384', 'ES512'],
  'request_object_encryption_alg_values_supported': ['RSA1_5', 'RSA-OAEP', 'A128KW', 'A256KW'],
  'session_revocation_endpoint': 'https://t1.techno24x7.com/oxauth/restv1/revoke_session',
  'check_session_iframe': 'https://t1.techno24x7.com/oxauth/opiframe.htm',
  'scopes_supported': ['address', 'openid', 'clientinfo', 'user_name', 'profile', 'https://t1.techno24x7.com/oxauth/restv1/uma/scopes/scim_access', 'uma_protection', 'permission', 'revoke_session', 'oxtrust-api-write', 'oxtrust-api-read', 'phone', 'mobile_phone', 'oxd', 'super_gluu_ro_session', 'email', 'https://t1.techno24x7.com/oxauth/restv1/uma/scopes/passport_access'],
  'backchannel_logout_supported': True,
  'acr_values_supported': ['simple_password_auth', 'passport_saml', 'urn:oasis:names:tc:SAML:2.0:ac:classes:Password', 'urn:oasis:names:tc:SAML:2.0:ac:classes:InternetProtocol', 'urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport', 'passport_social'],
  'request_object_encryption_enc_values_supported': ['A128CBC+HS256', 'A256CBC+HS512', 'A128GCM', 'A256GCM'],
  'display_values_supported': ['page', 'popup'],
  'userinfo_signing_alg_values_supported': ['HS256', 'HS384', 'HS512', 'RS256', 'RS384', 'RS512', 'ES256', 'ES384', 'ES512'],
  'claim_types_supported': ['normal'],
  'userinfo_encryption_alg_values_supported': ['RSA1_5', 'RSA-OAEP', 'A128KW', 'A256KW'],
  'end_session_endpoint': 'https://t1.techno24x7.com/oxauth/restv1/end_session',
  'revocation_endpoint': 'https://t1.techno24x7.com/oxauth/restv1/revoke',
  'backchannel_authentication_endpoint': 'https://t1.techno24x7.com/oxauth/restv1/bc-authorize',
  'token_endpoint_auth_signing_alg_values_supported': ['HS256', 'HS384', 'HS512', 'RS256', 'RS384', 'RS512', 'ES256', 'ES384', 'ES512'],
  'frontchannel_logout_supported': True,
  'jwks_uri': 'https://t1.techno24x7.com/oxauth/restv1/jwks',
  'subject_types_supported': ['public', 'pairwise'],
  'id_token_signing_alg_values_supported': ['none', 'HS256', 'HS384', 'HS512', 'RS256', 'RS384', 'RS512', 'ES256', 'ES384', 'ES512'],
  'registration_endpoint': 'https://t1.techno24x7.com/oxauth/restv1/register',
  'id_token_token_binding_cnf_values_supported': ['tbh']
}

REGISTER_CLIENT_RESPONSE = {
  'web': {
    'client_id': 'a22a0cd6-8af1-406a-a790-e71e936bce35',
    'client_secret': '649d9f34-1e35-467c-b2bb-d2735dfaa4ef',
    'auth_uri': 'https://t1.techno24x7.com/oxauth/restv1/authorize',
    'token_uri': 'https://t1.techno24x7.com/oxauth/restv1/token',
    'userinfo_uri': 'https://t1.techno24x7.com/oxauth/restv1/userinfo',
    'redirect_uris': ['https://mock.test.com/oidc_callback'],
    'issuer': 'https://t1.techno24x7.com'
  }
}