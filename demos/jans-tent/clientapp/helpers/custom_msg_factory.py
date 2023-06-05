"""
Custom message factory required by pyoic to add scope param
Overrides RegistrationRequest, RegistrationResponse
and use them to create CustomMessageFactory
"""

from oic.oic.message import OIDCMessageFactory, RegistrationRequest, RegistrationResponse, MessageTuple, OPTIONAL_LOGICAL
from oic.oauth2.message import OPTIONAL_LIST_OF_STRINGS, REQUIRED_LIST_OF_STRINGS, SINGLE_OPTIONAL_STRING, SINGLE_OPTIONAL_INT


class MyRegistrationRequest(RegistrationRequest):
    c_param = {
        "redirect_uris": REQUIRED_LIST_OF_STRINGS,
        "response_types": OPTIONAL_LIST_OF_STRINGS,
        "grant_types": OPTIONAL_LIST_OF_STRINGS,
        "application_type": SINGLE_OPTIONAL_STRING,
        "contacts": OPTIONAL_LIST_OF_STRINGS,
        "client_name": SINGLE_OPTIONAL_STRING,
        "logo_uri": SINGLE_OPTIONAL_STRING,
        "client_uri": SINGLE_OPTIONAL_STRING,
        "policy_uri": SINGLE_OPTIONAL_STRING,
        "tos_uri": SINGLE_OPTIONAL_STRING,
        "jwks": SINGLE_OPTIONAL_STRING,
        "jwks_uri": SINGLE_OPTIONAL_STRING,
        "sector_identifier_uri": SINGLE_OPTIONAL_STRING,
        "subject_type": SINGLE_OPTIONAL_STRING,
        "id_token_signed_response_alg": SINGLE_OPTIONAL_STRING,
        "id_token_encrypted_response_alg": SINGLE_OPTIONAL_STRING,
        "id_token_encrypted_response_enc": SINGLE_OPTIONAL_STRING,
        "userinfo_signed_response_alg": SINGLE_OPTIONAL_STRING,
        "userinfo_encrypted_response_alg": SINGLE_OPTIONAL_STRING,
        "userinfo_encrypted_response_enc": SINGLE_OPTIONAL_STRING,
        "request_object_signing_alg": SINGLE_OPTIONAL_STRING,
        "request_object_encryption_alg": SINGLE_OPTIONAL_STRING,
        "request_object_encryption_enc": SINGLE_OPTIONAL_STRING,
        "token_endpoint_auth_method": SINGLE_OPTIONAL_STRING,
        "token_endpoint_auth_signing_alg": SINGLE_OPTIONAL_STRING,
        "default_max_age": SINGLE_OPTIONAL_INT,
        "require_auth_time": OPTIONAL_LOGICAL,
        "default_acr_values": OPTIONAL_LIST_OF_STRINGS,
        "initiate_login_uri": SINGLE_OPTIONAL_STRING,
        "request_uris": OPTIONAL_LIST_OF_STRINGS,
        "post_logout_redirect_uris": OPTIONAL_LIST_OF_STRINGS,
        "frontchannel_logout_uri": SINGLE_OPTIONAL_STRING,
        "frontchannel_logout_session_required": OPTIONAL_LOGICAL,
        "backchannel_logout_uri": SINGLE_OPTIONAL_STRING,
        "backchannel_logout_session_required": OPTIONAL_LOGICAL,
        "scope": OPTIONAL_LIST_OF_STRINGS,  # added
    }
    c_default = {"application_type": "web", "response_types": ["code"]}
    c_allowed_values = {
        "application_type": ["native", "web"],
        "subject_type": ["public", "pairwise"],
    }


class CustomMessageFactory(OIDCMessageFactory):
    registration_endpoint = MessageTuple(MyRegistrationRequest, RegistrationResponse)

