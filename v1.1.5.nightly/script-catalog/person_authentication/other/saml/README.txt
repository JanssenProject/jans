This authentication interception script configures oxAuth to use an external SAML identity provider. This approach enables you to dynamically enroll users in your Gluu Server after they authenticate at their home IDP--meaning you can create a local ldap entry for each new inbound SAML user. This is handy because it enables you to use either SAML or OpenID Connect API's that connect to the Gluu Server. Confusing? Yes! Handy... yes, yes! 

This module has next properties:
1) asimba_saml_certificate_file - It's path to file which contains public IdP certificate in.
   Example: /etc/certs/saml.pem

2) saml_idp_sso_target_url - The URL at the Identity Provider where to the authentication request should be sent.
   Example: https://test.gluu.org/asimba/profiles/saml2/sso/web

3) asimba_entity_id - Set the issuer of the authentication request. This would usually be the URL of the issuing web application
   Example: https://test.gluu.info/saml

4) saml_use_authn_context - Specify if Saml request should contains samlp:RequestedAuthnContext section.
   Allowed values: true/false
   Example: true

5) saml_name_identifier_format - Specify in which format IdP should return a name identifier for the user.
   This property isn't required when saml_use_authn_context has value 'false'
   Example: urn:oasis:names:tc:SAML:2.0:nameid-format:persistent
   
6) saml_deployment_type - Specify deployment mode. It's optional property. If this property isn't specified Saml script
   tries to find user in local LDAP by 'Persistent Id' specified in Saml response. If this property has 'map' value Saml script map
   should map 'Persistent Id' to local user account. If this property has 'enroll' value Saml script should add new user to local LDAP
   with status 'register'. In order to map IDP attributes to local attributes it uses propertiessaml_idp_attributes_list and
   saml_local_attributes_list.
   Allowed values: map/enroll/enroll_all_attr
   Example: enroll

7) saml_validate_response - Specify if Saml script should valide Saml response signature. The path to IdP certificate should be specified in
   asimba_saml_certificate_file property. It's optional property. Default mode specify to validate Saml response.
   Allowed values: true/false
   Example: true

8) saml_client_configuration_attribute - Specify oxAuth client entry attribute name which contains Saml configuration in JSON format which
   allow to override next properties: asimba_saml_certificate_file, asimba_entity_id, saml_use_authn_context, saml_idp_attributes_list, saml_local_attributes_list.
   It's optional property.
   Property asimba_saml_certificate_file isn't mandatory. If the value is empty Smal script use global asimba_saml_certificate_file value.
   Example: oxAuthExtraConf
   Example content of client oxAuthExtraConf attribute: {"saml_certificate_file": "", "asimba_entity_id": "https://localhost/app1", "saml_use_authn_context": "false", "saml_idp_attributes_list": "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name, http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress, http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname, http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname, http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname", "saml_local_attributes_list": "uid, mail, givenName, sn, cn"}

9) saml_idp_attributes_list - Comma separated list of attribute names. Specify list of IdP attributes which Saml scrpt should use to map to local attributes.
   It's optional property. It's manadatory only if saml_deployment_type has value 'enroll'.
   The count of attributes in this property should be equal to count attributes in saml_local_attributes_list property.
   Example: http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name, http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress, http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname, http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname, http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname

10) saml_local_attributes_list - Comma separated list of attribute names. Specify list of local attributes mapped from Saml response.
    It's optional property. It's manadatory only if saml_deployment_type has value 'enroll'.
    The count of attributes in this property should be equal to count attributes in saml_idp_attributes_list property.
    Local attributes list should contains next mandatory attributes: uid, mail, givenName, sn, cn.
    Example: uid, mail, givenName, sn, cn
   
11) user_object_classes - Specify custom list of LDAP user object classes
   Example: eduPerson

12) enforce_uniqueness_attr_list - Specify list of unique user attributes to validate if there is no user with simular attributes already
   Example: uid, mail

13) saml_extension_module - Specify external module name. It's optional property. External module should implements 2 methods:
    def init(conf_attr):
    ...
    return True/False

    def postLogin(conf_attr, user):
    ...
    return True/False

    SAML scripts calls init method at initialization, and calls postLogin after to execute additional custom workflow.
14) saml_allow_basic_login - Specify if authentication module should allow both: basic and saml authentications
   Example: false
