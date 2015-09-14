from org.jboss.seam.contexts import Context, Contexts
from org.jboss.seam.security import Identity
from javax.faces.context import FacesContext
from org.xdi.model.custom.script.type.auth import PersonAuthenticationType
from org.xdi.oxauth.service import UserService, ClientService, AuthenticationService, AttributeService
from org.xdi.oxauth.service.net import HttpService
from org.xdi.util import StringHelper
from org.xdi.util import ArrayHelper
from org.gluu.saml import SamlConfiguration, AuthRequest, Response
from java.util import Arrays, ArrayList, HashMap, IdentityHashMap
from org.xdi.oxauth.model.common import User, CustomAttribute

import java

try:
    import json
except ImportError:
    import simplejson as json

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        print "Saml. Initialization"

        saml_certificate_file = configurationAttributes.get("saml_certificate_file").getValue2()
        saml_idp_sso_target_url = configurationAttributes.get("saml_idp_sso_target_url").getValue2()
        saml_issuer = configurationAttributes.get("saml_issuer").getValue2()
        saml_use_authn_context = StringHelper.toBoolean(configurationAttributes.get("saml_use_authn_context").getValue2(), True)
        if (saml_use_authn_context):
            saml_name_identifier_format = configurationAttributes.get("saml_name_identifier_format").getValue2()
        else:
            saml_name_identifier_format = None

        saml_certificate = self.loadCeritificate(saml_certificate_file)
        if (StringHelper.isEmpty(saml_certificate)):
            print "Saml. Initialization. File with x509 certificate should be not empty"
            return False

        samlConfiguration = SamlConfiguration()

        # Set the issuer of the authentication request. This would usually be the URL of the issuing web application
        samlConfiguration.setIssuer(saml_issuer)

        # Tells the IdP to return a persistent identifier for the user
        samlConfiguration.setNameIdentifierFormat(saml_name_identifier_format)
  
        # The URL at the Identity Provider where to the authentication request should be sent
        samlConfiguration.setIdpSsoTargetUrl(saml_idp_sso_target_url)

        # Enablediable RequestedAuthnContext
        samlConfiguration.setUseRequestedAuthnContext(saml_use_authn_context)
        
        # Load x509 certificate
        samlConfiguration.loadCertificateFromString(saml_certificate)
        
        self.samlConfiguration = samlConfiguration

        self.attributesMapping = None
        if (configurationAttributes.containsKey("saml_idp_attributes_list") and
            configurationAttributes.containsKey("saml_local_attributes_list")):

            saml_idp_attributes_list = configurationAttributes.get("saml_idp_attributes_list").getValue2()
            if (StringHelper.isEmpty(saml_idp_attributes_list)):
                print "Saml. Initialization. The property saml_idp_attributes_list is empty"
                return False

            saml_local_attributes_list = configurationAttributes.get("saml_local_attributes_list").getValue2()
            if (StringHelper.isEmpty(saml_local_attributes_list)):
                print "Saml. Initialization. The property saml_local_attributes_list is empty"
                return False

            self.attributesMapping = self.prepareAttributesMapping(saml_idp_attributes_list, saml_local_attributes_list)
            if (self.attributesMapping == None):
                print "Saml. Initialization. The attributes mapping isn't valid"
                return False

        self.samlExtensionModule = None
        if (configurationAttributes.containsKey("saml_extension_module")):
            saml_extension_module_name = configurationAttributes.get("saml_extension_module").getValue2()
            try:
                self.samlExtensionModule = __import__(saml_extension_module_name)
                saml_extension_module_init_result = self.samlExtensionModule.init(configurationAttributes)
                if (not saml_extension_module_init_result):
                    return False
            except ImportError, ex:
                print "Saml. Initialization. Failed to load saml_extension_module:", saml_extension_module_name
                print "Saml. Initialization. Unexpected error:", ex
                return False

        print "Saml. Initialized successfully"
        return True   

    def destroy(self, configurationAttributes):
        print "Saml. Destroy"
        print "Saml. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 1

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def authenticate(self, configurationAttributes, requestParameters, step):
        context = Contexts.getEventContext()
        authenticationService = AuthenticationService.instance()
        userService = UserService.instance()

        saml_map_user = False
        saml_enroll_user = False
        saml_enroll_all_user_attr = False
        # Use saml_deployment_type only if there is no attributes mapping
        if (configurationAttributes.containsKey("saml_deployment_type")):
            saml_deployment_type = StringHelper.toLowerCase(configurationAttributes.get("saml_deployment_type").getValue2())
            
            if (StringHelper.equalsIgnoreCase(saml_deployment_type, "map")):
                saml_map_user = True

            if (StringHelper.equalsIgnoreCase(saml_deployment_type, "enroll")):
                saml_enroll_user = True

            if (StringHelper.equalsIgnoreCase(saml_deployment_type, "enroll_all_attr")):
                saml_enroll_all_user_attr = True

        saml_allow_basic_login = False
        if (configurationAttributes.containsKey("saml_allow_basic_login")):
            saml_allow_basic_login = StringHelper.toBoolean(configurationAttributes.get("saml_allow_basic_login").getValue2(), False)

        use_basic_auth = False
        if (saml_allow_basic_login):
            # Detect if user used basic authnetication method
            credentials = Identity.instance().getCredentials()

            user_name = credentials.getUsername()
            user_password = credentials.getPassword()
            if (StringHelper.isNotEmpty(user_name) and StringHelper.isNotEmpty(user_password)):
                use_basic_auth = True

        if ((step == 1) and saml_allow_basic_login and use_basic_auth):
            print "Saml. Authenticate for step 1. Basic authentication"

            context.set("saml_count_login_steps", 1)

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
            print "Saml. Authenticate for step 1"

            currentSamlConfiguration = self.getCurrentSamlConfiguration(self.samlConfiguration, configurationAttributes, requestParameters)
            if (currentSamlConfiguration == None):
                print "Saml. Prepare for step 1. Client saml configuration is invalid"
                return False

            saml_response_array = requestParameters.get("SAMLResponse")
            if ArrayHelper.isEmpty(saml_response_array):
                print "Saml. Authenticate for step 1. saml_response is empty"
                return False

            saml_response = saml_response_array[0]

            print "Saml. Authenticate for step 1. saml_response:", saml_response

            samlResponse = Response(currentSamlConfiguration)
            samlResponse.loadXmlFromBase64(saml_response)
            
            saml_validate_response = True
            if (configurationAttributes.containsKey("saml_validate_response")):
                saml_validate_response = StringHelper.toBoolean(configurationAttributes.get("saml_validate_response").getValue2(), False)

            if (saml_validate_response):
                if (not samlResponse.isValid()):
                    print "Saml. Authenticate for step 1. saml_response isn't valid"

            saml_response_name_id = samlResponse.getNameId()
            if (StringHelper.isEmpty(saml_response_name_id)):
                print "Saml. Authenticate for step 1. saml_response_name_id is invalid"
                return False

            print "Saml. Authenticate for step 1. saml_response_name_id:", saml_response_name_id

            saml_response_attributes = samlResponse.getAttributes()
            print "Saml. Authenticate for step 1. attributes: ", saml_response_attributes

            # Use persistent Id as saml_user_uid
            saml_user_uid = saml_response_name_id
            
            if (saml_map_user):
                # Use mapping to local IDP user
                print "Saml. Authenticate for step 1. Attempting to find user by oxExternalUid: saml:", saml_user_uid

                # Check if the is user with specified saml_user_uid
                find_user_by_uid = userService.getUserByAttribute("oxExternalUid", "saml:" + saml_user_uid)

                if (find_user_by_uid == None):
                    print "Saml. Authenticate for step 1. Failed to find user"
                    print "Saml. Authenticate for step 1. Setting count steps to 2"
                    context.set("saml_count_login_steps", 2)
                    context.set("saml_user_uid", saml_user_uid)
                    return True

                found_user_name = find_user_by_uid.getUserId()
                print "Saml. Authenticate for step 1. found_user_name:", found_user_name
                
                user_authenticated = authenticationService.authenticate(found_user_name)
                if (user_authenticated == False):
                    print "Saml. Authenticate for step 1. Failed to authenticate user"
                    return False
            
                print "Saml. Authenticate for step 1. Setting count steps to 1"
                context.set("saml_count_login_steps", 1)

                post_login_result = self.samlExtensionPostLogin(configurationAttributes, find_user_by_uid)
                print "Saml. Authenticate for step 1. post_login_result:", post_login_result

                return post_login_result
            elif (saml_enroll_user):
                # Use auto enrollment to local IDP
                print "Saml. Authenticate for step 1. Attempting to find user by oxExternalUid: saml:", saml_user_uid

                # Check if the is user with specified saml_user_uid
                find_user_by_uid = userService.getUserByAttribute("oxExternalUid", "saml:" + saml_user_uid)

                if (find_user_by_uid == None):
                    # Auto user enrollemnt
                    print "Saml. Authenticate for step 1. There is no user in LDAP. Adding user to local LDAP"

                    # Convert saml result attributes keys to lover case
                    saml_response_normalized_attributes = HashMap()
                    for saml_response_attribute_entry in saml_response_attributes.entrySet():
                        saml_response_normalized_attributes.put(
                            StringHelper.toLowerCase(saml_response_attribute_entry.getKey()), saml_response_attribute_entry.getValue())

                    currentAttributesMapping = self.prepareCurrentAttributesMapping(self.attributesMapping, configurationAttributes, requestParameters)
                    print "Saml. Authenticate for step 1. Using next attributes mapping", currentAttributesMapping

                    newUser = User()
                    for attributesMappingEntry in currentAttributesMapping.entrySet():
                        idpAttribute = attributesMappingEntry.getKey()
                        localAttribute = attributesMappingEntry.getValue()

                        localAttributeValue = saml_response_normalized_attributes.get(idpAttribute)
                        if (localAttribute != None):
                            newUser.setAttribute(localAttribute, localAttributeValue)

                    newUser.setAttribute("oxExternalUid", "saml:" + saml_user_uid)
                    print "Saml. Authenticate for step 1. Attempting to add user", saml_user_uid, " with next attributes", newUser.getCustomAttributes()

                    find_user_by_uid = userService.addUser(newUser, True)
                    print "Saml. Authenticate for step 1. Added new user with UID", find_user_by_uid.getUserId()

                found_user_name = find_user_by_uid.getUserId()
                print "Saml. Authenticate for step 1. found_user_name:", found_user_name

                user_authenticated = authenticationService.authenticate(found_user_name)
                if (user_authenticated == False):
                    print "Saml. Authenticate for step 1. Failed to authenticate user"
                    return False

                print "Saml. Authenticate for step 1. Setting count steps to 1"
                context.set("saml_count_login_steps", 1)

                post_login_result = self.samlExtensionPostLogin(configurationAttributes, find_user_by_uid)
                print "Saml. Authenticate for step 1. post_login_result:", post_login_result

                return post_login_result
            elif (saml_enroll_all_user_attr):
                print "Saml. Authenticate for step 1. Attempting to find user by oxExternalUid: saml:" + saml_user_uid

                # Check if the is user with specified saml_user_uid
                find_user_by_uid = userService.getUserByAttribute("oxExternalUid", "saml:" + saml_user_uid)

                if (find_user_by_uid == None):
                    print "Saml. Authenticate for step 1. Failed to find user"

                    user = User()
                    customAttributes = ArrayList()
                    for key in attributes.keySet():
                        ldapAttributes = attributeService.getAllAttributes()
                        for ldapAttribute in ldapAttributes:
                            saml2Uri = ldapAttribute.getSaml2Uri()
                            if(saml2Uri == None):
                                saml2Uri = attributeService.getDefaultSaml2Uri(ldapAttribute.getName())
                            if(saml2Uri == key):
                                attribute = CustomAttribute(ldapAttribute.getName())
                                attribute.setValues(attributes.get(key))
                                customAttributes.add(attribute)

                    attribute = CustomAttribute("oxExternalUid")
                    attribute.setValue("saml:" + saml_user_uid)
                    customAttributes.add(attribute)
                    user.setCustomAttributes(customAttributes)

                    if(user.getAttribute("sn") == None):
                        attribute = CustomAttribute("sn")
                        attribute.setValue(saml_user_uid)
                        customAttributes.add(attribute)

                    if(user.getAttribute("cn") == None):
                        attribute = CustomAttribute("cn")
                        attribute.setValue(saml_user_uid)
                        customAttributes.add(attribute)

                    find_user_by_uid = userService.addUser(user, True)
                    print "Saml. Authenticate for step 1. Added new user with UID", find_user_by_uid.getUserId()

                found_user_name = find_user_by_uid.getUserId()
                print "Saml. Authenticate for step 1. found_user_name:", found_user_name

                user_authenticated = authenticationService.authenticate(found_user_name)
                if (user_authenticated == False):
                    print "Saml. Authenticate for step 1. Failed to authenticate user"
                    return False

                print "Saml. Authenticate for step 1. Setting count steps to 1"
                context.set("saml_count_login_steps", 1)

                post_login_result = self.samlExtensionPostLogin(configurationAttributes, find_user_by_uid)
                print "Saml. Authenticate for step 1. post_login_result:", post_login_result

                return post_login_result
            else:
                # Check if the is user with specified saml_user_uid
                print "Saml. Authenticate for step 1. Attempting to find user by uid:", saml_user_uid

                find_user_by_uid = userService.getUser(saml_user_uid)
                if (find_user_by_uid == None):
                    print "Saml. Authenticate for step 1. Failed to find user"
                    return False

                found_user_name = find_user_by_uid.getUserId()
                print "Saml. Authenticate for step 1. found_user_name:", found_user_name

                user_authenticated = authenticationService.authenticate(found_user_name)
                if (user_authenticated == False):
                    print "Saml. Authenticate for step 1. Failed to authenticate user"
                    return False

                print "Saml. Authenticate for step 1. Setting count steps to 1"
                context.set("saml_count_login_steps", 1)

                post_login_result = self.samlExtensionPostLogin(configurationAttributes, find_user_by_uid)
                print "Saml. Authenticate for step 1. post_login_result:", post_login_result

                return post_login_result
        elif (step == 2):
            print "Saml. Authenticate for step 2"

            sessionAttributes = context.get("sessionAttributes")
            if (sessionAttributes == None) or not sessionAttributes.containsKey("saml_user_uid"):
                print "Saml. Authenticate for step 2. saml_user_uid is empty"
                return False

            saml_user_uid = sessionAttributes.get("saml_user_uid")
            passed_step1 = StringHelper.isNotEmptyString(saml_user_uid)
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

            # Check if there is user which has saml_user_uid
            # Avoid mapping Saml account to more than one IDP account
            find_user_by_uid = userService.getUserByAttribute("oxExternalUid", "saml:" + saml_user_uid)

            if (find_user_by_uid == None):
                # Add saml_user_uid to user one id UIDs
                find_user_by_uid = userService.addUserAttribute(user_name, "oxExternalUid", "saml:" + saml_user_uid)
                if (find_user_by_uid == None):
                    print "Saml. Authenticate for step 2. Failed to update current user"
                    return False

                post_login_result = self.samlExtensionPostLogin(configurationAttributes, find_user_by_uid)
                print "Saml. Authenticate for step 2. post_login_result:", post_login_result

                return post_login_result
            else:
                found_user_name = find_user_by_uid.getUserId()
                print "Saml. Authenticate for step 2. found_user_name:", found_user_name
    
                if StringHelper.equals(user_name, found_user_name):
                    post_login_result = self.samlExtensionPostLogin(configurationAttributes, find_user_by_uid)
                    print "Saml. Authenticate for step 2. post_login_result:", post_login_result
    
                    return post_login_result
        
            return False
        else:
            return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        context = Contexts.getEventContext()
        authenticationService = AuthenticationService.instance()

        if (step == 1):
            print "Saml. Prepare for step 1"
            
            httpService = HttpService.instance();
            request = FacesContext.getCurrentInstance().getExternalContext().getRequest()
            assertionConsumerServiceUrl = httpService.constructServerUrl(request) + "/postlogin"
            print "Saml. Prepare for step 1. Prepared assertionConsumerServiceUrl:", assertionConsumerServiceUrl
            
            currentSamlConfiguration = self.getCurrentSamlConfiguration(self.samlConfiguration, configurationAttributes, requestParameters)
            if (currentSamlConfiguration == None):
                print "Saml. Prepare for step 1. Client saml configuration is invalid"
                return False

            # Generate an AuthRequest and send it to the identity provider
            samlAuthRequest = AuthRequest(currentSamlConfiguration)
            external_auth_request_uri = currentSamlConfiguration.getIdpSsoTargetUrl() + "?SAMLRequest=" + samlAuthRequest.getRequest(True, assertionConsumerServiceUrl)

            print "Saml. Prepare for step 1. external_auth_request_uri:", external_auth_request_uri
            
            context.set("external_auth_request_uri", external_auth_request_uri)

            return True
        elif (step == 2):
            print "Saml. Prepare for step 2"

            return True
        else:
            return False

    def getExtraParametersForStep(self, configurationAttributes, step):
        if (step == 2):
            return Arrays.asList("saml_user_uid")

        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        context = Contexts.getEventContext()
        if (context.isSet("saml_count_login_steps")):
            return context.get("saml_count_login_steps")
        
        return 2

    def getPageForStep(self, configurationAttributes, step):
        if (step == 1):
            saml_allow_basic_login = False
            if (configurationAttributes.containsKey("saml_allow_basic_login")):
                saml_allow_basic_login = StringHelper.toBoolean(configurationAttributes.get("saml_allow_basic_login").getValue2(), False)

            if (saml_allow_basic_login):
                return "/login.xhtml"
            else:
                return "/auth/saml/samllogin.xhtml"

        return "/auth/saml/samlpostlogin.xhtml"

    def logout(self, configurationAttributes, requestParameters):
        return True

    def isPassedStep1():
        credentials = Identity.instance().getCredentials()
        user_name = credentials.getUsername()
        passed_step1 = StringHelper.isNotEmptyString(user_name)

        return passed_step1

    def loadCeritificate(self, saml_certificate_file):
        saml_certificate = None

        # Load certificate from file
        f = open(saml_certificate_file, 'r')
        try:
            saml_certificate = f.read()
        except:
            print "Failed to load certificate from file:", saml_certificate_file
            return None
        finally:
            f.close()
        
        return saml_certificate

    def getClientConfiguration(self, configurationAttributes, requestParameters):
        # Get client configuration
        if (configurationAttributes.containsKey("saml_client_configuration_attribute")):
            saml_client_configuration_attribute = configurationAttributes.get("saml_client_configuration_attribute").getValue2()
            print "Saml. GetClientConfiguration. Using client attribute:", saml_client_configuration_attribute

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
                print "Saml. GetClientConfiguration. client_id is empty"
                return None

            clientService = ClientService.instance()
            client = clientService.getClient(client_id)
            if (client == None):
                print "Saml. GetClientConfiguration. Failed to find client", client_id, " in local LDAP"
                return None

            saml_client_configuration = clientService.getCustomAttribute(client, saml_client_configuration_attribute)
            if ((saml_client_configuration == None) or StringHelper.isEmpty(saml_client_configuration.getValue())):
                print "Saml. GetClientConfiguration. Client", client_id, " attribute", saml_client_configuration_attribute, " is empty"
            else:
                print "Saml. GetClientConfiguration. Client", client_id, " attribute", saml_client_configuration_attribute, " is", saml_client_configuration
                return saml_client_configuration

        return None

    def getCurrentSamlConfiguration(self, currentSamlConfiguration, configurationAttributes, requestParameters):
        saml_client_configuration = self.getClientConfiguration(configurationAttributes, requestParameters)
        if (saml_client_configuration == None):
            return currentSamlConfiguration
        
        saml_client_configuration_value = json.loads(saml_client_configuration.getValue())

        client_saml_certificate = None      
        client_saml_certificate_file = saml_client_configuration_value["saml_certificate_file"]
        if (StringHelper.isNotEmpty(client_saml_certificate_file)):
            client_saml_certificate = self.loadCeritificate(client_saml_certificate_file)
            if (StringHelper.isEmpty(client_saml_certificate)):
                print "Saml. BuildClientSamlConfiguration. File with x509 certificate should be not empty. Using default configuration"
                return currentSamlConfiguration

        clientSamlConfiguration = currentSamlConfiguration.clone()
        
        if (client_saml_certificate != None):
            clientSamlConfiguration.loadCertificateFromString(client_saml_certificate)

        client_saml_issuer = saml_client_configuration_value["saml_issuer"]
        clientSamlConfiguration.setIssuer(client_saml_issuer)
        
        saml_use_authn_context = saml_client_configuration_value["saml_use_authn_context"]
        client_use_saml_use_authn_context = StringHelper.toBoolean(saml_use_authn_context, True)
        clientSamlConfiguration.setUseRequestedAuthnContext(client_use_saml_use_authn_context)

        return clientSamlConfiguration

    def prepareAttributesMapping(self, saml_idp_attributes_list, saml_local_attributes_list):
        saml_idp_attributes_list_array = StringHelper.split(saml_idp_attributes_list, ",")
        if (ArrayHelper.isEmpty(saml_idp_attributes_list_array)):
            print "Saml. PrepareAttributesMapping. There is no attributes specified in saml_idp_attributes_list property"
            return None
        
        saml_local_attributes_list_array = StringHelper.split(saml_local_attributes_list, ",")
        if (ArrayHelper.isEmpty(saml_local_attributes_list_array)):
            print "Saml. PrepareAttributesMapping. There is no attributes specified in saml_local_attributes_list property"
            return None

        if (len(saml_idp_attributes_list_array) != len(saml_local_attributes_list_array)):
            print "Saml. PrepareAttributesMapping. The number of attributes in saml_idp_attributes_list and saml_local_attributes_list isn't equal"
            return None
        
        attributeMapping = IdentityHashMap()
        containsUid = False
        i = 0
        count = len(saml_idp_attributes_list_array)
        while (i < count):
            idpAttribute = StringHelper.toLowerCase(saml_idp_attributes_list_array[i])
            localAttribute = StringHelper.toLowerCase(saml_local_attributes_list_array[i])
            attributeMapping.put(idpAttribute, localAttribute)

            if (StringHelper.equalsIgnoreCase(localAttribute, "uid")):
                containsUid = True

            i = i + 1

        if (not containsUid):
            print "Saml. PrepareAttributesMapping. There is no mapping to mandatory 'uid' attribute"
            return None
        
        return attributeMapping

    def prepareCurrentAttributesMapping(self, currentAttributesMapping, configurationAttributes, requestParameters):
        saml_client_configuration = self.getClientConfiguration(configurationAttributes, requestParameters)
        if (saml_client_configuration == None):
            return currentAttributesMapping

        saml_client_configuration_value = json.loads(saml_client_configuration.getValue())

        clientAttributesMapping = self.prepareAttributesMapping(saml_client_configuration_value["saml_idp_attributes_list"], saml_client_configuration_value["saml_local_attributes_list"])
        if (clientAttributesMapping == None):
            print "Saml. PrepareCurrentAttributesMapping. Client attributes mapping is invalid. Using default one"
            return currentAttributesMapping

        return clientAttributesMapping

    def samlExtensionPostLogin(self, configurationAttributes, user):
        if (self.samlExtensionModule != None):
            try:
                post_login_result = self.samlExtensionModule.postLogin(configurationAttributes, user)
                print "Saml. ExtensionPostlogin result:", post_login_result

                return post_login_result
            except Exception, ex:
                print "Saml. ExtensionPostlogin. Failed to execute postLogin method"
                print "Saml. ExtensionPostlogin. Unexpected error:", ex
                return False
            except java.lang.Throwable, ex:
                print "Saml. ExtensionPostlogin. Failed to execute postLogin method"
                ex.printStackTrace() 
                return False
                    
        return True
