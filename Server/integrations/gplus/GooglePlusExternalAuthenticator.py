from org.jboss.seam.contexts import Context, Contexts
from org.jboss.seam.security import Identity
from javax.faces.context import FacesContext
from org.xdi.oxauth.service.python.interfaces import ExternalAuthenticatorType
from org.xdi.oxauth.service import UserService, ClientService, AuthenticationService, AttributeService
from org.xdi.util.security import StringEncrypter 
from org.xdi.util import StringHelper
from org.xdi.util import ArrayHelper
from java.util import Arrays, ArrayList, HashMap, IdentityHashMap
from org.xdi.oxauth.model.common import User, CustomAttribute
from org.xdi.oxauth.model.jwt import Jwt, JwtClaimName

import java

try:
    import json
except ImportError:
    import simplejson as json

class ExternalAuthenticator(ExternalAuthenticatorType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis
#+
    def init(self, configurationAttributes):
        print "Google+ initialization"

        if (not configurationAttributes.containsKey("gplus_client_secrets_file")):
            print "Google+ initialization. The property gplus_client_secrets_file is empty"
            return False
            
        gplus_client_secrets_file = configurationAttributes.get("gplus_client_secrets_file").getValue2()
        self.clientSecrets = self.loadClientSecrets(gplus_client_secrets_file)
        if (self.clientSecrets == None):
            print "Google+ initialization. File with Google+ client secrets should be not empty"
            return False

        self.attributesMapping = None
        if (configurationAttributes.containsKey("gplus_remote_attributes_list") and
            configurationAttributes.containsKey("gplus_local_attributes_list")):

            gplus_remote_attributes_list = configurationAttributes.get("gplus_remote_attributes_list").getValue2()
            if (StringHelper.isEmpty(gplus_remote_attributes_list)):
                print "Google+ initialization. The property gplus_remote_attributes_list is empty"
                return False

            gplus_local_attributes_list = configurationAttributes.get("gplus_local_attributes_list").getValue2()
            if (StringHelper.isEmpty(gplus_local_attributes_list)):
                print "Google+ initialization. The property gplus_local_attributes_list is empty"
                return False

            self.attributesMapping = self.prepareAttributesMapping(gplus_remote_attributes_list, gplus_local_attributes_list)
            if (self.attributesMapping == None):
                print "Google+ initialization. The attributes mapping isn't valid"
                return False

        self.extensionModule = None
        if (configurationAttributes.containsKey("extension_module")):
            extension_module_name = configurationAttributes.get("extension_module").getValue2()
            try:
                self.extensionModule = __import__(extension_module_name)
                gplus_extension_module_init_result = self.extensionModule.init(configurationAttributes)
                if (not gplus_extension_module_init_result):
                    return False
            except ImportError, ex:
                print "Failed to load gplus_extension_module:", extension_module_name
                print "Unexpected error:", ex
                return False

        print "Google+ initialized successfully"
        return True   

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def authenticate(self, configurationAttributes, requestParameters, step):
        context = Contexts.getEventContext()
        authenticationService = AuthenticationService.instance()
        userService = UserService.instance()

        stringEncrypter = StringEncrypter.defaultInstance()

        gplus_map_user = False
        gplus_enroll_user = False
        # Use gplus_deployment_type only if there is no attributes mapping
        if (configurationAttributes.containsKey("gplus_deployment_type")):
            gplus_deployment_type = StringHelper.toLowerCase(configurationAttributes.get("gplus_deployment_type").getValue2())
            
            if (StringHelper.equalsIgnoreCase(gplus_deployment_type, "map")):
                gplus_map_user = True

            if (StringHelper.equalsIgnoreCase(gplus_deployment_type, "enroll")):
                gplus_enroll_user = True

        gplus_allow_basic_login = False
        if (configurationAttributes.containsKey("gplus_allow_basic_login")):
            gplus_allow_basic_login = StringHelper.toBoolean(configurationAttributes.get("gplus_allow_basic_login").getValue2(), False)

        use_basic_auth = False
        if (gplus_allow_basic_login):
            basic_auth = requestParameters.get("basic_auth")
            if (ArrayHelper.isNotEmpty(basic_auth)):
                use_basic_auth = StringHelper.toBoolean(basic_auth[0], False)

        if ((step == 1) and gplus_allow_basic_login and use_basic_auth):
            print "Google+ authenticate for step 1. Basic authentication"

            context.set("gplus_count_login_steps", 1)

            credentials = Identity.instance().getCredentials()
            user_name = credentials.getUsername()
            user_password = credentials.getPassword()

            logged_in = False
            if (StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password)):
                userService = UserService.instance()
                logged_in = userService.authenticate(user_name, user_password)

            if (not logged_in):
                return False

            return True

        if (step == 1):
            print "Google+ authenticate for step 1"

            currentClientSecrets = self.getCurrentClientSecrets(self.clientSecrets, configurationAttributes, requestParameters)
            if (currentClientSecrets == None):
                print "Google+ prepare for step 1. Client secrets configuration is invalid"
                return False
 
            gplus_auth_access_token_array = requestParameters.get("gplus_auth_access_token")
            if ArrayHelper.isEmpty(gplus_auth_access_token_array):
                print "Google+ authenticate for step 1. gplus_auth_access_token is empty"
                return False

            gplus_auth_access_token = gplus_auth_access_token_array[0]
            print "Google+ authenticate for step 1. gplus_response:", gplus_auth_access_token
 
            gplus_auth_id_token_array = requestParameters.get("gplus_auth_id_token")
            if ArrayHelper.isEmpty(gplus_auth_id_token_array):
                print "Google+ authenticate for step 1. gplus_auth_id_token is empty"
                return False

            gplus_auth_id_token = gplus_auth_id_token_array[0]
            print "Google+ authenticate for step 1. gplus_response:", gplus_auth_id_token
            
            jwt = Jwt.parse(gplus_auth_id_token)
            # TODO: Validate ID Token Signature  

            gplus_user_uid = jwt.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER);
            print "Google+ authenticate for step 1. Found Google user ID in the ID token: ", gplus_user_uid
            
            if (gplus_map_user):
                # Use mapping to local IDP user
                print "Google+ authenticate for step 1. Attempting to find user by oxExternalUid: gplus:", gplus_user_uid

                # Check if the is user with specified gplus_user_uid
                find_user_by_uid = userService.getUserByAttribute("oxExternalUid", "gplus:" + gplus_user_uid)

                if (find_user_by_uid == None):
                    print "Google+ authenticate for step 1. Failed to find user"
                    print "Google+ authenticate for step 1. Setting count steps to 2"
                    context.set("gplus_count_login_steps", 2)
                    context.set("gplus_user_uid", stringEncrypter.encrypt(gplus_user_uid))
                    return True

                found_user_name = find_user_by_uid.getUserId()
                print "Google+ authenticate for step 1. found_user_name:", found_user_name
                
                user_authenticated = authenticationService.authenticate(found_user_name)
                if (user_authenticated == False):
                    print "Google+ authenticate for step 1. Failed to authenticate user"
                    return False
            
                print "Google+ authenticate for step 1. Setting count steps to 1"
                context.set("gplus_count_login_steps", 1)

                post_login_result = self.extensionPostLogin(configurationAttributes, find_user_by_uid)
                print "Google+ authenticate for step 1. post_login_result:", post_login_result

                return post_login_result
#             elif (gplus_enroll_user):
#                 # Use auto enrollment to local IDP
#                 print "Google+ authenticate for step 1. Attempting to find user by oxExternalUid: gplus:", gplus_user_uid
# 
#                 # Check if the is user with specified gplus_user_uid
#                 find_user_by_uid = userService.getUserByAttribute("oxExternalUid", "gplus:" + gplus_user_uid)
# 
#                 if (find_user_by_uid == None):
#                     # Auto user enrollemnt
#                     print "Google+ authenticate for step 1. There is no user in LDAP. Adding user to local LDAP"
# 
#                     # Convert saml result attributes keys to lover case
#                     gplus_response_normalized_attributes = HashMap()
#                     for gplus_response_attribute_entry in gplus_response_attributes.entrySet():
#                         gplus_response_normalized_attributes.put(
#                             StringHelper.toLowerCase(gplus_response_attribute_entry.getKey()), gplus_response_attribute_entry.getValue())
# 
#                     currentAttributesMapping = self.getCurrentAttributesMapping(self.attributesMapping, configurationAttributes, requestParameters)
#                     print "Google+ authenticate for step 1. Using next attributes mapping", currentAttributesMapping
# 
#                     newUser = User()
#                     for attributesMappingEntry in currentAttributesMapping.entrySet():
#                         idpAttribute = attributesMappingEntry.getKey()
#                         localAttribute = attributesMappingEntry.getValue()
# 
#                         localAttributeValue = gplus_response_normalized_attributes.get(idpAttribute)
#                         if (localAttribute != None):
#                             newUser.setAttribute(localAttribute, localAttributeValue)
# 
#                     newUser.setAttribute("oxExternalUid", "gplus:" + gplus_user_uid)
#                     print "Google+ authenticate for step 1. Attempting to add user", gplus_user_uid, " with next attributes", newUser.getCustomAttributes()
# 
#                     find_user_by_uid = userService.addUser(newUser)
#                     print "Google+ authenticate for step 1. Added new user with UID", find_user_by_uid.getUserId()
#             elif (gplus_enroll_all_user_attr):
#                 print "Google+ authenticate for step 1. Attempting to find user by oxExternalUid: gplus:" + gplus_user_uid
# 
#                 # Check if the is user with specified gplus_user_uid
#                 find_user_by_uid = userService.getUserByAttribute("oxExternalUid", "gplus:" + gplus_user_uid)
# 
#                 if (find_user_by_uid == None):
#                     print "Google+ authenticate for step 1. Failed to find user"
# 
#                     user = User()
#                     customAttributes = ArrayList()
#                     for key in attributes.keySet():
#                         ldapAttributes = attributeService.getAllAttributes()
#                         for ldapAttribute in ldapAttributes:
#                             saml2Uri = ldapAttribute.getSaml2Uri()
#                             if(saml2Uri == None):
#                                 saml2Uri = attributeService.getDefaultSaml2Uri(ldapAttribute.getName())
#                             if(saml2Uri == key):
#                                 attribute = CustomAttribute(ldapAttribute.getName())
#                                 attribute.setValues(attributes.get(key))
#                                 customAttributes.add(attribute)
# 
#                     attribute = CustomAttribute("oxExternalUid")
#                     attribute.setValue("gplus:" + gplus_user_uid)
#                     customAttributes.add(attribute)
#                     user.setCustomAttributes(customAttributes)
# 
#                     if(user.getAttribute("sn") == None):
#                         attribute = CustomAttribute("sn")
#                         attribute.setValue(gplus_user_uid)
#                         customAttributes.add(attribute)
# 
#                     if(user.getAttribute("cn") == None):
#                         attribute = CustomAttribute("cn")
#                         attribute.setValue(gplus_user_uid)
#                         customAttributes.add(attribute)
# 
#                     find_user_by_uid = userService.addUser(user)
#                     print "Google+ authenticate for step 1. Added new user with UID", find_user_by_uid.getUserId()
# 
#                 found_user_name = find_user_by_uid.getUserId()
#                 print "Google+ authenticate for step 1. found_user_name:", found_user_name
# 
#                 user_authenticated = authenticationService.authenticate(found_user_name)
#                 if (user_authenticated == False):
#                     print "Google+ authenticate for step 1. Failed to authenticate user"
#                     return False
# 
#                 print "Google+ authenticate for step 1. Setting count steps to 1"
#                 context.set("gplus_count_login_steps", 1)
# 
#                 post_login_result = self.extensionPostLogin(configurationAttributes, find_user_by_uid)
#                 print "Google+ authenticate for step 1. post_login_result:", post_login_result
# 
#                 return post_login_result
            else:
                # Check if the is user with specified gplus_user_uid
                print "Google+ authenticate for step 1. Attempting to find user by uid:", gplus_user_uid

                find_user_by_uid = userService.getUser(gplus_user_uid)
                if (find_user_by_uid == None):
                    print "Google+ authenticate for step 1. Failed to find user"
                    return False

                found_user_name = find_user_by_uid.getUserId()
                print "Google+ authenticate for step 1. found_user_name:", found_user_name

                user_authenticated = authenticationService.authenticate(found_user_name)
                if (user_authenticated == False):
                    print "Google+ authenticate for step 1. Failed to authenticate user"
                    return False

                print "Google+ authenticate for step 1. Setting count steps to 1"
                context.set("gplus_count_login_steps", 1)

                post_login_result = self.extensionPostLogin(configurationAttributes, find_user_by_uid)
                print "Google+ authenticate for step 1. post_login_result:", post_login_result

                return post_login_result
        elif (step == 2):
            print "Google+ authenticate for step 2"
            
            gplus_user_uid_array = requestParameters.get("gplus_user_uid")
            if ArrayHelper.isEmpty(gplus_user_uid_array):
                print "Google+ authenticate for step 2. gplus_user_uid is empty"
                return False

            gplus_user_uid = stringEncrypter.decrypt(gplus_user_uid_array[0])
            passed_step1 = StringHelper.isNotEmptyString(gplus_user_uid)
            if (not passed_step1):
                return False

            credentials = Identity.instance().getCredentials()
            user_name = credentials.getUsername()
            user_password = credentials.getPassword()

            logged_in = False
            if (StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password)):
                logged_in = userService.authenticate(user_name, user_password)

            if (not logged_in):
                return False

            # Check if there is user which has gplus_user_uid
            # Avoid mapping Google account to more than one IDP account
            find_user_by_uid = userService.getUserByAttribute("oxExternalUid", "gplus:" + gplus_user_uid)

            if (find_user_by_uid == None):
                # Add gplus_user_uid to user one id UIDs
                find_user_by_uid = userService.addUserAttribute(user_name, "oxExternalUid", "gplus:" + gplus_user_uid)
                if (find_user_by_uid == None):
                    print "Google+ authenticate for step 2. Failed to update current user"
                    return False

                post_login_result = self.extensionPostLogin(configurationAttributes, find_user_by_uid)
                print "Google+ authenticate for step 2. post_login_result:", post_login_result

                return post_login_result
            else:
                found_user_name = find_user_by_uid.getUserId()
                print "Google+ authenticate for step 2. found_user_name:", found_user_name
    
                if StringHelper.equals(user_name, found_user_name):
                    post_login_result = self.extensionPostLogin(configurationAttributes, find_user_by_uid)
                    print "Google+ authenticate for step 2. post_login_result:", post_login_result
    
                    return post_login_result
        
            return False
        else:
            return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        context = Contexts.getEventContext()
        authenticationService = AuthenticationService.instance()

        if (step == 1):
            print "Google+ prepare for step 1"
            
            currentClientSecrets = self.getCurrentClientSecrets(self.clientSecrets, configurationAttributes, requestParameters)
            if (currentClientSecrets == None):
                print "Google+ prepare for step 1. Google+ client configuration is invalid"
                return False
            
            context.set("gplus_client_id", currentClientSecrets["web"]["client_id"])
            context.set("gplus_client_secret", currentClientSecrets["web"]["client_secret"])

            return True
        elif (step == 2):
            print "Google+ prepare for step 2"

            return True
        else:
            return False

    def getExtraParametersForStep(self, configurationAttributes, step):
        if (step == 2):
            return Arrays.asList("gplus_user_uid")

        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        context = Contexts.getEventContext()
        if (context.isSet("gplus_count_login_steps")):
            return context.get("gplus_count_login_steps")
        
        return 2

    def getPageForStep(self, configurationAttributes, step):
        if (step == 1):
            return "/auth/gplus/gpluslogin.xhtml"

        return "/auth/gplus/gpluspostlogin.xhtml"

    def logout(self, configurationAttributes, requestParameters):
        return True

    def getApiVersion(self):
        return 3

    def isPassedStep1():
        credentials = Identity.instance().getCredentials()
        user_name = credentials.getUsername()
        passed_step1 = StringHelper.isNotEmptyString(user_name)

        return passed_step1

    def loadClientSecrets(self, gplus_client_secrets_file):
        gplus_client_secrets = None

        # Load certificate from file
        f = open(gplus_client_secrets_file, 'r')
        try:
            gPlusClientSecrets = json.loads(f.read())
        except:
            print "Failed to load Google+ client secrets from file:", gPlusClientSecrets
            return None
        finally:
            f.close()
        
        return gPlusClientSecrets

    def getClientConfiguration(self, configurationAttributes, requestParameters):
        # Get client configuration
        if (configurationAttributes.containsKey("gplus_client_configuration_attribute")):
            gplus_client_configuration_attribute = configurationAttributes.get("gplus_client_configuration_attribute").getValue2()
            print "Google+ GetClientConfiguration. Using client attribute:", gplus_client_configuration_attribute

            if (requestParameters == None):
                return None

            client_id = None
            client_id_array = requestParameters.get("client_id")
            if (ArrayHelper.isNotEmpty(client_id_array) and StringHelper.isNotEmptyString(client_id_array[0])):
                client_id = client_id_array[0]

            if (client_id == None):
                eventContext = Contexts.getEventContext()
                if (eventContext.isSet("stored_request_parameters")):
                    client_id = eventContext.get("stored_request_parameters").get("client_id")

            if (client_id == None):
                print "Google+ GetClientConfiguration. client_id is empty"
                return None

            clientService = ClientService.instance()
            client = clientService.getClient(client_id)
            if (client == None):
                print "Google+ GetClientConfiguration. Failed to find client", client_id, " in local LDAP"
                return None

            gplus_client_configuration = clientService.getCustomAttribute(client, gplus_client_configuration_attribute)
            if ((gplus_client_configuration == None) or StringHelper.isEmpty(gplus_client_configuration.getValue())):
                print "Google+ GetClientConfiguration. Client", client_id, " attribute", gplus_client_configuration_attribute, " is empty"
            else:
                print "Google+ GetClientConfiguration. Client", client_id, " attribute", gplus_client_configuration_attribute, " is", gplus_client_configuration
                return gplus_client_configuration

        return None

    def getCurrentClientSecrets(self, currentClientSecrets, configurationAttributes, requestParameters):
        clientConfiguration = self.getClientConfiguration(configurationAttributes, requestParameters)
        if (clientConfiguration == None):
            return currentClientSecrets
        
        clientConfigurationValue = json.loads(clientConfiguration.getValue())

        return clientConfigurationValue["gplus"]

    def getCurrentAttributesMapping(self, currentAttributesMapping, configurationAttributes, requestParameters):
        clientConfiguration = self.getClientConfiguration(configurationAttributes, requestParameters)
        if (clientConfiguration == None):
            return currentAttributesMapping

        clientConfigurationValue = json.loads(clientConfiguration.getValue())

        clientAttributesMapping = self.prepareAttributesMapping(clientConfigurationValue["gplus_remote_attributes_list"], clientConfigurationValue["gplus_local_attributes_list"])
        if (clientAttributesMapping == None):
            print "Google+ GetCurrentAttributesMapping. Client attributes mapping is invalid. Using default one"
            return currentAttributesMapping

        return clientAttributesMapping

    def prepareAttributesMapping(self, remote_attributes_list, local_attributes_list):
        remote_attributes_list_array = StringHelper.split(remote_attributes_list, ",")
        if (ArrayHelper.isEmpty(remote_attributes_list_array)):
            print "Google+ PrepareAttributesMapping. There is no attributes specified in remote_attributes_list property"
            return None
        
        local_attributes_list_array = StringHelper.split(local_attributes_list, ",")
        if (ArrayHelper.isEmpty(local_attributes_list_array)):
            print "Google+ PrepareAttributesMapping. There is no attributes specified in local_attributes_list property"
            return None

        if (len(remote_attributes_list_array) != len(local_attributes_list_array)):
            print "Google+ PrepareAttributesMapping. The number of attributes in remote_attributes_list and local_attributes_list isn't equal"
            return None
        
        attributeMapping = IdentityHashMap()
        containsUid = False
        i = 0
        count = len(remote_attributes_list_array)
        while (i < count):
            idpAttribute = StringHelper.toLowerCase(remote_attributes_list_array[i])
            localAttribute = StringHelper.toLowerCase(local_attributes_list_array[i])
            attributeMapping.put(idpAttribute, localAttribute)

            if (StringHelper.equalsIgnoreCase(localAttribute, "uid")):
                containsUid = True

            i = i + 1

        if (not containsUid):
            print "Google+ PrepareAttributesMapping. There is no mapping to mandatory 'uid' attribute"
            return None
        
        return attributeMapping

    def extensionPostLogin(self, configurationAttributes, user):
        if (self.extensionModule != None):
            try:
                post_login_result = self.extensionModule.postLogin(configurationAttributes, user)
                print "Google+ post login result:", post_login_result

                return post_login_result
            except Exception, ex:
                print "Failed to execute postLogin method"
                print "Unexpected error:", ex
                return False
            except java.lang.Throwable, ex:
                print "Failed to execute postLogin method"
                ex.printStackTrace() 
                return False
                    
        return True
