"""Per-script expectations for the catalog load check. Keys are paths relative to docs/script-catalog."""

# AIO service that loads each script type (mirrors each service's AppInitializer).
AUTH_EXCLUDED = {"link_interception", "scim", "idp", "config_api_auth", "fido2_extension", "lock_extension"}
NON_AUTH_LOADERS = {
    "scim": "jans-scim",
    "config_api_auth": "jans-config-api",
    "fido2_extension": "jans-fido2",
}
NOT_IN_AIO = {"idp", "link_interception", "lock_extension", "shib_post_authn_approval"}

# Loaded (or compiled) with a global effect on every request, so each one runs alone after the shared
# batch. client_authn answers 401 to any request without Basic auth, so it always runs last.
ISOLATED_TYPES = ["persistence_extension", "client_authn"]

# Scripts that do not load on the current server. A test reports them skipped; if one starts loading,
# drop it from here so a later regression fails.
KNOWN_BROKEN = {
    "authorization_challenge/multi_step/AuthorizationChallenge.java": "references undefined variable authorizationChallengeSessionObject",
    "introspection/introspection-custom-parameters/introspection_custom_params.java": "imports io.jans.as.server.model.common.SessionId (moved to io.jans.as.common.model.session)",
    "application_session/sample-script/SampleScript.py": "imports io.jans.as.model.ldap.TokenEntity, which no longer exists",
    "ciba/end-user-notification/end_user_notification.py": "imports io.jans.as.client.fcm, not on the auth-server classpath",
    "client_registration/OpenBanking/Registration.py": "imports classes removed from jans-auth-server",
    "client_registration/sample-script/SampleScript.py": "imports classes removed from jans-auth-server",
    "person_authentication/apple-external-authenticator/AppleExternalAuthenticator.py": "imports io.jans.as.server.service.net.HttpService (now HttpService2)",
    "person_authentication/duo-external-authenticator/DuoExternalAuthenticator.py": "imports io.jans.as.server.service.net.HttpService (now HttpService2)",
    "person_authentication/github-external-authenticator/GithubAuthenticatorForADS.py": "imports io.jans.as.server.service.net.HttpService (now HttpService2)",
    "person_authentication/github-external-authenticator/GithubExternalAuthenticator.py": "imports io.jans.as.server.service.net.HttpService (now HttpService2)",
    "person_authentication/google-external-authenticator/GoogleExternalAuthenticator.py": "needs google-api-client jackson2, not shipped in the AIO",
    "person_authentication/oidc/oidc-jans-script.py": "imports io.jans.as.server.service.net.HttpService (now HttpService2)",
    "person_authentication/other/allowed_countries/allowed_countries.py": "imports io.jans.as.server.service.net.HttpService (now HttpService2)",
    "person_authentication/other/azuread/AzureADAuthenticationForGluu.py": "legacy org.gluu imports",
    "person_authentication/other/basic.one_session/BasicOneSessionExternalAuthenticator.py": "legacy org.gluu imports",
    "person_authentication/other/cas2/Cas2ExternalAuthenticator.py": "imports io.jans.as.server.service.net.HttpService (now HttpService2)",
    "person_authentication/other/cas2_duo/Cas2DuoExternalAuthenticator.py": "imports the Cas2ExternalAuthenticator module, unavailable to the server",
    "person_authentication/other/ciba/FirebaseEndUserNotification.py": "legacy org.gluu imports",
    "person_authentication/other/custom_registration/register.py": "legacy org.gluu imports",
    "person_authentication/other/gplus/GooglePlusExternalAuthenticator.py": "legacy org.gluu imports",
    "person_authentication/other/inwebo/inwebo.py": "imports io.jans.as.server.service.net.HttpService (now HttpService2)",
    "person_authentication/other/obconnect/UpdateToken.py": "legacy org.gluu.oxauth imports",
    "person_authentication/other/obconnect/obconnectExternalAuthentication.py": "legacy org.gluu.oxauth imports",
    "person_authentication/other/passport/PassportExternalAuthenticator.py": "legacy org.gluu imports",
    "person_authentication/other/registration/register.py": "legacy org.gluu imports",
    "person_authentication/other/saml-passport/SamlPassportAuthenticator.py": "legacy org.gluu imports",
    "person_authentication/other/saml/SamlExternalAuthenticator.py": "legacy org.gluu imports",
    "person_authentication/other/uaf/UafExternalAuthenticator.py": "imports io.jans.as.server.service.common, which no longer exists",
    "person_authentication/other/wwpass/wwpassauth.py": "legacy org.gluu imports",
    "person_authentication/passport/passport-social-jans-script.py": "imports io.jans.as.server.service.net.HttpService (now HttpService2)",
    "person_authentication/super-gluu-external-authenticator/SuperGluuExternalAuthenticator.py": "needs the Super Gluu android notification libs, not shipped in the AIO",
    "person_authentication/user-cert-external-authenticator/UserCertExternalAuthenticator.py": "imports io.jans.as.server.service.net.HttpService (now HttpService2)",
    "revoke_token/revoke-token/revoke_token.py": "legacy org.gluu imports",
}

# Scripts whose init() must succeed with the configuration below. Every other script may legitimately
# need credentials or files the AIO does not have, so a failed init is reported but does not gate.
INIT_REQUIRED = {
    "application_session/jans-session-audit/jans-session-audit.py",
    "client_authn/ClientAuthn.java",
    "introspection/introspection-custom-parameters/introspection_custom_params.py",
    "introspection/introspection-github-claims/introspection_github_claims.py",
    "person_authentication/basic-external-authenticator/BasicExternalAuthenticator.py",
    "person_authentication/basic-lock-account-external-authenticator/BasicLockAccountExternalAuthenticator.py",
    "person_authentication/forgot_password/forgot_password.py",
    "person_authentication/passkey/PasskeyInterceptionScript.py",
    "person_authentication/twilio-2fa/twilio2FA.py",
    "resource_owner_password_credentials/resource-owner-password-credentials-2fa/resource-owner-password-credentials-2fa.py",
    "token_exchange/TokenExchange.java",
    "uma_claims_gathering/uma-claims-gathering/uma_claims_gathering.py",
}

# Not a script: helper module imported by another catalog script.
LIBRARIES = {"person_authentication/other/wwpass/wwpass.py"}


def config_properties(rel, fqdn):
    """Placeholder configuration so init() runs without real third-party credentials."""
    twilio = {"twilio_sid": "AC00000000000000000000000000000000", "twilio_token": "ci-placeholder",
              "from_number": "+15005550006"}
    props = {
        "person_authentication/twilio-2fa/twilio2FA.py": twilio,
        "resource_owner_password_credentials/resource-owner-password-credentials-2fa/resource-owner-password-credentials-2fa.py": {
            "twilio_account_sid": twilio["twilio_sid"], "twilio_auth_token": twilio["twilio_token"],
            "twilio_from_number": twilio["from_number"]},
        "person_authentication/passkey/PasskeyInterceptionScript.py": {
            "fido2_server_uri": f"https://{fqdn}", "fido2_domain": fqdn},
        "person_authentication/otp-external-authenticator/OtpExternalAuthenticator.py": {
            "otp_type": "totp", "issuer": "Janssen CI", "otp_conf_file": "/etc/certs/otp_configuration.json"},
        "person_authentication/basic-lock-account-external-authenticator/BasicLockAccountExternalAuthenticator.py": {
            "invalid_login_count_attribute": "jansCountInvalidLogin", "maximum_invalid_login_attemps": "3",
            "lock_expiration_time": "600"},
    }.get(rel, {})
    return [{"value1": k, "value2": v, "hide": False} for k, v in props.items()]


def loader_service(script_type):
    if script_type in NOT_IN_AIO:
        return None
    if script_type in NON_AUTH_LOADERS:
        return NON_AUTH_LOADERS[script_type]
    return "jans-auth" if script_type not in AUTH_EXCLUDED else None
