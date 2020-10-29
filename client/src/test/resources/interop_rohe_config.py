#!/usr/bin/env python

import json

info = {
    "interaction": [{
        "matches": {
            "url": "https://${test.server.name}/oxauth/login.seam"
        },
        "page-type": "login",
        "control": {
            "type": "form",
            "set": {
                "loginForm:username": "${auth.user.uid}",
                "loginForm:password": "${auth.user.password}"
            }
        }
    }, {
        "matches": {
            "url": "https://${test.server.name}/oxauth/authorize.seam"
        },
        "page-type": "user-consent",
        "control": {
            "type": "form",
            "click": "authorizeForm:allowButton"
        }
    }],
    "provider": {
        "version": {
            "oauth": "2.0",
            "openid": "3.0"
        },
        "dynamic": "https://${test.server.name}"
    },
    "features": {
        "registration": True,
        "discovery": True,
        "session_management": False,
        "key_export": True
    },
    "client": {
        "redirect_uris": ["https://${test.server.name}/oxauth-rp/home.htm?foo=bar"],
        "contact": ["yuriy@gluu.com"],
        "application_type": "web",
        "application_name": "OIC test tool",
        "key_export_url": "https://${test.server.name}/jans-auth-client/test/resources/jwks.json",
        "keys": {
            "RSA": {
                "key": "keys/pyoidc",
                "use": ["enc", "sig"]
            }
        },
        "preferences": {
            "subject_type": ["pairwise", "public"],
            "request_object_signing_alg": ["RS256", "RS384", "RS512",
                                           "HS512", "HS384", "HS256"],
            "token_endpoint_auth_methods_supported": ["client_secret_basic",
                                                      "client_secret_post",
                                                      "client_secret_jwt",
                                                      "private_key_jwt"],
            "id_token_signed_response_alg": ["RS256", "RS384", "RS512",
                                             "HS512", "HS384", "HS256"],
            "default_max_age": 3600,
            "require_auth_time": True,
            "default_acr": ["2", "1"]
        }
    }
}

print json.dumps(info)
