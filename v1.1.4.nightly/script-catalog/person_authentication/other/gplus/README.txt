This is a person authentication module for oxAuth that enables [Google+ Authentication](https://www.google.com) for user authentication.

The module has a few properties:

1) gplus_client_secrets_file - It's mandatory property. It's path to application configuration file downloaded from Google console for application.
Example: `/etc/certs/gplus_client_secrets.json`
These are steps needed to get it:
    a) Log into: https://console.developers.google.com/project
    b) Click "Create project" and enter project name
    c) Open new project "API & auth -> Credentials" menu item in configuration navigation tree
    d) Click "Add credential" with type "OAuth 2.0 client ID"
    e) Select "Web application" application type
    f) Enter "Authorized JavaScript origins". It should be CE server DNS name. Example: https://gluu.info
    g) Click "Create" and Click "OK" in next dialog
    h) Click "Download JSON" in order to download gplus_client_secrets.json file
Also it's mandatory to enable Google+ API:
    a) Log into: https://console.developers.google.com/project
    b) Select project and enter project name
    c) Open new project "API & auth -> API" menu item in configuration navigation tree
    d) Click "Google+ API"
    e) Click "Enable API" button

2) gplus_deployment_type - Specify deployment mode. It's optional property. If this property isn't specified script
   tries to find user in local LDAP by 'subject_identifier' claim specified in id_token. If this property has 'map' value script
   allow to map 'subject_identifier' to local user account. If this property has 'enroll' value script should add new user to local LDAP
   with status 'acrtive'. In order to map IDP attributes to local attributes it uses properties gplus_remote_attributes_list and
   gplus_local_attributes_list.
   Allowed values: map/enroll
   Example: enroll

3) gplus_remote_attributes_list - Comma separated list of attribute names. Specify list of Google+ claims(attributes) which script should use to map to local attributes.
   It's optional property. It's mandatory only if gplus_deployment_type has value 'enroll'.
   The count of attributes in this property should be equal to count attributes in gplus_local_attributes_list property.
   Example: email, email, name, family_name, given_name, locale

4) gplus_local_attributes_list - Comma separated list of attribute names. Specify list of local attributes mapped from Google+ userInfo OpenId response.
    It's optional property. It's mandatory only if gplus_deployment_type has value 'enroll'.
    The count of attributes in this property should be equal to count attributes in gplus_remote_attributes_list property.
    Local attributes list should contains next mandatory attributes: uid, mail, givenName, sn, cn.
    Example: uid, mail, givenName, sn, cn, preferredLanguage

5) extension_module - Specify external module name. It's optional property. External module should implements 2 methods:
    def init(conf_attr):
    ...
    return True/False

    def postLogin(conf_attr, user):
    ...
    return True/False

    Scripts calls init method at initialization. And calls postLogin after user log in order to execute additional custom workflow.

 6) gplus_client_configuration_attribute - Specify client entry attribute name which can override gplus_client_secrets_file file content. It's optional property.
    It can be used in cases when all clients should use separate gplus_client_secrets.json configuration.
