This is Inbound SAML custom authentication modules for oxAuth. It allows to configure oxAuth to use 2 factor authentication for interactive user logins.

This module has few properties:
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

2) saml_IdP_sso_target_url - The URL at the Identity Provider where to the authentication request should be sent.
   Example: http://localhost:8080/asimba-wa/profiles/saml2/sso/web

3) saml_issuer - Set the issuer of the authentication request. This would usually be the URL of the issuing web application
   Example: localhost/oxAuth

4) saml_name_identifier_format - Specify in which format IdP should return a name identifier for the user.
   Example: urn:oasis:names:tc:SAML:2.0:nameid-format:persistent

5) saml_extension_module - Specify external module name. External module should implements 2 methods:
   def init(conf_attr):
   ...
   return True/False

   def postLogin(conf_attr, user):
   ...
   return True/False

   Saml scripts calls init method at initialization. And calls postLogin after user log in order to execute additional custom workflow.

6) saml_map_user - set to True to enable user mapping to the local database.