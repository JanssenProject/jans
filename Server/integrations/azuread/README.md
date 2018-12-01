
This is a person authentication module for oxAuth that allows user to authenticate against Azure AD. 

Required Custom property (key/value) -
1) azure_tenant_id  (Azure AD's Tenant ID)
2) azure_client_id  
3) azure_client_secret
4) azure_ad_attributes_list = oid,given_name,family_name,upn
5) gluu_ldap_attributes_list = uid,givenName,sn,mail

Note:
An administrator of the Azure AD portal (portal.azure.com) needs to create an application for Gluu Server in the azure portal (with necessary permissions)
and configure the client id and client secret in the Custom properties of this Jython script.
