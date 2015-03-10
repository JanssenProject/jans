This is SAML person authentication modules for oxAuth. It allows to configure oxAuth to use 2 factor authentication for interactive user logins.

This module has next properties:
1) saml_certificate_file - It's path to file which contains public IdP certificate in.
   Example: /etc/certs/saml.pem
   Example content of this file (Asmiba certificate):
   MIICRzCCAbCgAwIBAgIET1j1vjANBgkqhkiG9w0BAQUFADBoMQswCQYDVQQGEwJOTDEMMAoGA1UE
   CBMDT1ZSMQ8wDQYDVQQHEwZad29sbGUxDzANBgNVBAoTBkFzaW1iYTERMA8GA1UECxMIYmFzZWxp
   bmUxFjAUBgNVBAMTDUFzaW1iYSBTZXJ2ZXIwHhcNMTIwMzA4MTgwOTAyWhcNMTIwNjA2MTgwOTAy
   WjBoMQswCQYDVQQGEwJOTDEMMAoGA1UECBMDT1ZSMQ8wDQYDVQQHEwZad29sbGUxDzANBgNVBAoT
   BkFzaW1iYTERMA8GA1UECxMIYmFzZWxpbmUxFjAUBgNVBAMTDUFzaW1iYSBTZXJ2ZXIwgZ8wDQYJ
   KoZIhvcNAQEBBQADgY0AMIGJAoGBAJVFbTGTVaTvBH/F+8p27Xr/ZoC2Sr9PZBPft74fD44XLl3X
   vVGeJa6VJs7FKHjyc8I9XGWzdfokBkuDPYz1s6D/lxMMSbj96mdZ1GnihkHXRXzpl2ClAjhGpSOM
   1r5YM7pAeRVTbG4y+8HLT+2B38o3cQxwNVqTTBB30YsheErlAgMBAAEwDQYJKoZIhvcNAQEFBQAD
   gYEAH/5Nkse9YXUyaO8wLPKF/Ru0HaSjyuhFUthDMNiAy3UFjw9HuCyYWOM32+chHFCYZZv/OP79
   a2B8uWPQrAStsnzjwm6a8v6/Slka82LLhie4ZBSoHnWLhW0KjZXp3xDHCtfizPyIyHKyiVGRTFoS
   IkvCrLzoz1Okyyk8bm9OTCc=

2) saml_idp_sso_target_url - The URL at the Identity Provider where to the authentication request should be sent.
   Example: http://localhost:8080/asimba-wa/profiles/saml2/sso/web

3) saml_issuer - Set the issuer of the authentication request. This would usually be the URL of the issuing web application
   Example: localhost/oxAuth

4) saml_use_authn_context - Specify if Saml request should contains samlp:RequestedAuthnContext section.
   Allowed values: true/false
   Example: true

5) saml_name_identifier_format - Specify in which format IdP should return a name identifier for the user.
   This property isn't requred when saml_use_authn_context has value 'false'
   Example: urn:oasis:names:tc:SAML:2.0:nameid-format:persistent
   
6) saml_deployment_type - Specify deployment mode. It's optional property. If this property isn't specified Saml script
   tries to find user in local LDAP by 'Persistent Id' specified in Saml response. If this property has 'map' value Saml script map
   should map 'Persistent Id' to local user account. If this property has 'enroll' value Saml script should add new user to local LDAP
   with status 'register'. In order to map IDP attributes to local attributes it uses propertiessaml_idp_attributes_list and
   saml_local_attributes_list.
   Allowed values: map/enroll/enroll_all_attr
   Example: enroll

7) saml_validate_response - Specify if Saml script should valide Saml response signature. The path to IdP certificate should be specified in
   saml_certificate_file property. It's optional property. Default mode specify to validate Saml response.
   Allowed values: true/false
   Example: true

8) saml_client_configuration_attribute - Specify oxAuth client entry attribute name which contains Saml configuration in JSON format which
   allow to override next properties: saml_certificate_file, saml_issuer, saml_use_authn_context, saml_idp_attributes_list, saml_local_attributes_list.
   It's optional property.
   Property saml_certificate_file isn't mandatory. If the value is empty Smal script use global saml_certificate_file value.
   Example: {"saml_certificate_file": "", "saml_issuer": "https://localhost/app1", "saml_use_authn_context": "false", "saml_idp_attributes_list": "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name, http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress, http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname, http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname, http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname", "saml_local_attributes_list": "uid, mail, givenName, sn, cn"}

9) saml_idp_attributes_list - Comma separated list of attribute names. Specify list of IdP attributes which Saml scrpt should use to map to local attributes.
   It's optional property. It's manadatory only if saml_deployment_type has value 'enroll'.
   The count of attributes in this property should be equal to count attributes in saml_local_attributes_list property.
   Example: http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name, http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress, http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname, http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname, http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname

10) saml_local_attributes_list - Comma separated list of attribute names. Specify list of local attributes mapped from Saml response.
    It's optional property. It's manadatory only if saml_deployment_type has value 'enroll'.
    The count of attributes in this property should be equal to count attributes in saml_idp_attributes_list property.
    Local attributes list should contains next mandatory attributes: uid, mail, givenName, sn, cn.
    Example: uid, mail, givenName, sn, cn

11) saml_extension_module - Specify external module name. It's optional property. External module should implements 2 methods:
    def init(conf_attr):
    ...
    return True/False

    def postLogin(conf_attr, user):
    ...
    return True/False

    Saml scripts calls init method at initialization. And calls postLogin after user log in order to execute additional custom workflow.
12) saml_allow_basic_login - Specify if authentication module should allow both: basic and saml authentications
   Example: false
