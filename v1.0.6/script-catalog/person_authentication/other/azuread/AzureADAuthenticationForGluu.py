# Author: Naveen Kumar Gopi

from io.jans.service.cdi.util import CdiUtil
from io.jans.as.server.security import Identity
from io.jans.model.custom.script.type.auth import PersonAuthenticationType
from io.jans.as.server.service import AuthenticationService, UserService
from org.gluu.oxauth.model.common import User
from io.jans.util import StringHelper, ArrayHelper
from java.util import IdentityHashMap
from org.apache.commons.codec.binary import Base64
from java.lang import String
import httplib
import urllib
import json

import java


class PersonAuthentication(PersonAuthenticationType):

    def __init__(self, current_time_millis):
        self.currentTimeMillis = current_time_millis

    def init(self, customScript,  configuration_attributes):
        print "AzureAD. Initialization"

        global azure_tenant_id
        azure_tenant_id = configuration_attributes.get("azure_tenant_id").getValue2()
        print "AzureAD. Initialization. Value of azure_tenant_id is %s" % azure_tenant_id

        global azure_client_id
        azure_client_id = configuration_attributes.get("azure_client_id").getValue2()
        print "AzureAD. Initialization. Value of azure_client_id is %s" % azure_client_id

        global azure_client_secret
        azure_client_secret = configuration_attributes.get("azure_client_secret").getValue2()

        global MICROSOFT_AUTHORITY_URL
        MICROSOFT_AUTHORITY_URL = 'login.microsoftonline.com'

        global AZURE_AD_GRAPH_RESOURCE_ENDPOINT
        AZURE_AD_GRAPH_RESOURCE_ENDPOINT = 'https://graph.windows.net'

        global azure_user_uuid
        azure_user_uuid = "oid"

        global gluu_ldap_uuid
        gluu_ldap_uuid = "uid"

        global ADMIN
        ADMIN = 'admin'

        global attributes_mapping

        if (configuration_attributes.containsKey("azure_ad_attributes_list") and
                configuration_attributes.containsKey("gluu_ldap_attributes_list")):

            azure_ad_attributes_list = configuration_attributes.get("azure_ad_attributes_list").getValue2()
            if StringHelper.isEmpty(azure_ad_attributes_list):
                print "AzureAD: Initialization. The property azure_ad_attributes_list is empty"
                return False

            gluu_ldap_attributes_list = configuration_attributes.get("gluu_ldap_attributes_list").getValue2()
            if StringHelper.isEmpty(gluu_ldap_attributes_list):
                print "AzureAD: Initialization. The property gluu_ldap_attributes_list is empty"
                return False

            attributes_mapping = self.attribute_mapping_function(azure_ad_attributes_list, gluu_ldap_attributes_list)
            if attributes_mapping is None:
                print "AzureAD: Initialization. The attributes mapping isn't valid"
                return False

        print "AzureAD. Initialized successfully"
        return True

    @staticmethod
    def attribute_mapping_function(azure_ad_attributes_list, gluu_ldap_attributes_list):
        try:
            azure_ad_attributes_list_array = StringHelper.split(azure_ad_attributes_list, ",")
            if ArrayHelper.isEmpty(azure_ad_attributes_list_array):
                print("AzureAD: There is no attributes specified in azure_ad_attributes_list property")
                return None

            gluu_ldap_attributes_list_array = StringHelper.split(gluu_ldap_attributes_list, ",")
            if ArrayHelper.isEmpty(gluu_ldap_attributes_list_array):
                print("AzureAD: There is no attributes specified in gluu_ldap_attributes_list property")
                return None

            if len(azure_ad_attributes_list_array) != len(gluu_ldap_attributes_list_array):
                print("AzureAD: The number of attributes isn't equal")
                return None

            attributes_map = IdentityHashMap()
            i = 0
            count = len(azure_ad_attributes_list_array)
            while i < count:
                azure_ad_attribute = StringHelper.toLowerCase(azure_ad_attributes_list_array[i])
                gluu_ldap_attribute = StringHelper.toLowerCase(gluu_ldap_attributes_list_array[i])
                attributes_map.put(azure_ad_attribute, gluu_ldap_attribute)
                i = i + 1

            return attributes_map
        except Exception, err:
            print("AzureAD: Exception inside prepareAttributesMapping " + str(err))

    def prepareForStep(self, configuration_attributes, request_parameters, step):
        if step == 1:
            print "AzureAD. Prepare for Step 1"
            return True
        else:
            return False

    def authenticate(self, configuration_attributes, request_parameters, step):
        print "AzureAD. Inside authenticate. Step %d" % step
        authentication_service = CdiUtil.bean(AuthenticationService)
        identity = CdiUtil.bean(Identity)

        if step == 1:
            print "AzureAD. Authenticate for step 1"
            logged_in = self.authenticate_user_credentials(identity, authentication_service)
            print "AzureAD. Status of User Credentials based Authentication : %r" % logged_in
            if not logged_in:
                return False
            print "AzureAD. Authenticate successful for step %d" % step
            return True
        else:
            return False

    def authenticate_user_credentials(self, identity, authentication_service):
        credentials = identity.getCredentials()
        user_name = credentials.getUsername()
        user_password = credentials.getPassword()
        print "AzureAD. user_name: %s" % user_name
        logged_in = False
        if StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password):

            # Special condition to allow for Gluu admin login
            if StringHelper.equals(user_name, ADMIN):
                return self.authenticate_user_in_gluu_ldap(authentication_service, user_name, user_password)

            # Authenticate user credentials with Azure AD non-interactively
            azure_auth_response = self.authenticate_user_in_azure(azure_tenant_id, user_name, user_password, azure_client_id, azure_client_secret)
            print "AzureAD. Value of azure_auth_response is %s" % azure_auth_response
            azure_auth_response_json = json.loads(azure_auth_response)
            if azure_user_uuid in azure_auth_response_json:
                # Azure authentication has succeeded. User needs to be enrolled in Gluu LDAP
                user = self.enroll_azure_user_in_gluu_ldap(azure_auth_response_json)
                if user is None:
                    # User Enrollment in Gluu LDAP has failed
                    logged_in = False
                else:
                    # Authenticating the user within Gluu
                    user_authenticated_in_gluu = authentication_service.authenticate(user.getUserId())
                    print "AzureAD: Authentication status of the user enrolled in Gluu LDAP %r " % user_authenticated_in_gluu
                    return user_authenticated_in_gluu
            else:
                # Azure authentication has failed.
                logged_in = False
        return logged_in

    @staticmethod
    def authenticate_user_in_azure(tenant_id, user_name, pwd, client_id, client_secret):
        post_params_json = {'resource': AZURE_AD_GRAPH_RESOURCE_ENDPOINT, 'client_id': client_id,
                            'client_secret': client_secret, 'username': user_name, 'password': pwd,
                            'grant_type': 'password', 'scope': 'openid'}
        post_params_url_encoded = urllib.urlencode(post_params_json)
        headers_json = {'Content-type': 'application/x-www-form-urlencoded',
                        'Accept': 'application/json'}
        conn = httplib.HTTPSConnection(MICROSOFT_AUTHORITY_URL + ':443')
        relative_url = '/' + tenant_id + '/oauth2/token'
        conn.request('POST', relative_url, post_params_url_encoded, headers_json)
        response = conn.getresponse()
        # print response.status, response.reason
        azure_response = response.read()
        conn.close()
        # print "Response Data: %s" % azure_response
        azure_response_json = json.loads(azure_response)
        if 'id_token' in azure_response_json:
            id_token = azure_response_json['id_token']
            id_token_array = String(id_token).split("\\.")
            id_token_payload = id_token_array[1]
            id_token_payload_str = String(Base64.decodeBase64(id_token_payload), 'UTF-8')
            return str(id_token_payload_str)
        else:
            return azure_response

    def enroll_azure_user_in_gluu_ldap(self, azure_auth_response_json):
        user_service = CdiUtil.bean(UserService)
        azure_user_uuid_value = azure_auth_response_json[azure_user_uuid]
        found_user = self.find_user_from_gluu_ldap_by_attribute(user_service, gluu_ldap_uuid, azure_user_uuid_value)
        print "AzureAD. Value of found_user is %s" % found_user
        if found_user is None:
            new_user = User()
            self.populate_user_obj_with_azure_user_data(new_user, azure_auth_response_json)
            try:
                # Add azure user in Gluu LDAP
                found_user = user_service.addUser(new_user, True)
                found_user_id = found_user.getUserId()
                print("AzureAD: Azure User added successfully in Gluu LDAP " + found_user_id)
            except Exception, err:
                print("AzureAD: Error in adding azure user to Gluu LDAP:" + str(err))
                return None
        else:
            self.populate_user_obj_with_azure_user_data(found_user, azure_auth_response_json)
            try:
                # Update the user in Gluu LDAP with latest values from Azure AD
                found_user = user_service.updateUser(found_user)
                found_user_id = found_user.getUserId()
                print("AzureAD: Azure User updated successfully in Gluu LDAP " + found_user_id)
            except Exception, err:
                print("AzureAD: Error in updating azure user to Gluu LDAP:" + str(err))
                return None

        return found_user

    @staticmethod
    def populate_user_obj_with_azure_user_data(user, azure_auth_response_json):
        # attributes_mapping = ["oid:uid", "given_name:givenName", "family_name:sn", "upn:mail"]
        for attributesMappingEntry in attributes_mapping.entrySet():
            azure_ad_attribute_key = attributesMappingEntry.getKey()
            gluu_ldap_attribute_key = attributesMappingEntry.getValue()
            gluu_ldap_attribute_value = "undefined"
            if azure_ad_attribute_key in azure_auth_response_json:
                gluu_ldap_attribute_value = azure_auth_response_json[azure_ad_attribute_key]
            print gluu_ldap_attribute_key + ' : ' + gluu_ldap_attribute_value
            if (gluu_ldap_attribute_key is not None) & (gluu_ldap_attribute_value != "undefined"):
                user.setAttribute(gluu_ldap_attribute_key, gluu_ldap_attribute_value)
        return None

    @staticmethod
    def authenticate_user_in_gluu_ldap(authentication_service, user_name, user_password):
        return authentication_service.authenticate(user_name, user_password)

    @staticmethod
    def find_user_from_gluu_ldap_by_attribute(user_service, attribute_name, attribute_value):
        return user_service.getUserByAttribute(attribute_name, attribute_value)

    def getExtraParametersForStep(self, configuration_attributes, step):
        return None

    def getCountAuthenticationSteps(self, configuration_attributes):
        return 1

    def getPageForStep(self, configuration_attributes, step):
        return ""

    def destroy(self, configuration_attributes):
        print "AzureAD. Destroy"
        return True

    def getAuthenticationMethodClaims(self, requestParameters):
        return None

    def getApiVersion(self):
        return 11

    def isValidAuthenticationMethod(self, usage_type, configuration_attributes):
        return True

    def getAlternativeAuthenticationMethod(self, usage_type, configuration_attributes):
        return None

    def logout(self, configuration_attributes, request_parameters):
        return True

    def getNextStep(self, configurationAttributes, requestParameters, step):
        return -1

    def getLogoutExternalUrl(self, configurationAttributes, requestParameters):
        print "Get external logout URL call"
        return None