from org.jboss.seam.contexts import Context, Contexts, ServerConversationContext
from org.jboss.seam.security import Identity
from javax.faces.context import FacesContext
from org.xdi.oxauth.service.python.interfaces import ExternalAuthenticatorType
from org.xdi.oxauth.service import UserService, AttributeService
from org.xdi.oxauth.service import AuthenticationService
from org.xdi.oxauth.service.net import HttpService
from org.xdi.util.security import StringEncrypter 
from org.xdi.util import StringHelper
from org.xdi.util import ArrayHelper
from org.gluu.saml import SamlConfiguration, AuthRequest, Response
from java.util import Arrays, ArrayList
from org.xdi.oxauth.model.common import User, CustomAttribute

import java

class ExternalAuthenticator(ExternalAuthenticatorType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        print "InboundSaml initialization"

        saml_certificate_file = configurationAttributes.get("saml_certificate_file").getValue2()
        saml_idp_sso_target_url = configurationAttributes.get("saml_idp_sso_target_url").getValue2()
        saml_issuer = configurationAttributes.get("saml_issuer").getValue2()
        saml_name_identifier_format = configurationAttributes.get("saml_name_identifier_format").getValue2()

        # Load certificate from file
        f = open(saml_certificate_file, 'r')
        try:
            saml_certificate = f.read()
        except:
            return False
        finally:
            f.close()

        if (StringHelper.isEmpty(saml_certificate)):
            print "InboundSaml initialization. File with x509 certificate should be not empty"

        samlConfiguration = SamlConfiguration()

        # Set the issuer of the authentication request. This would usually be the URL of the issuing web application
        samlConfiguration.setIssuer(saml_issuer)

        # Tells the IdP to return a persistent identifier for the user
        samlConfiguration.setNameIdentifierFormat(saml_name_identifier_format)
  
        # The URL at the Identity Provider where to the authentication request should be sent
        samlConfiguration.setIdpSsoTargetUrl(saml_idp_sso_target_url)
        
        # Load x509 certificate
        samlConfiguration.loadCertificateFromString(saml_certificate)
        
        self.samlConfiguration = samlConfiguration

        print "InboundSaml initialized successfully"
        return True   
        
    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def authenticate(self, configurationAttributes, requestParameters, step):
        print "InboundSaml authenticate: " , step
        context = Contexts.getEventContext()
        authenticationService = AuthenticationService.instance()
        userService = UserService.instance()
        attributeService = AttributeService.instance()

        stringEncrypter = StringEncrypter.defaultInstance()

        saml_map_user = False
        print configurationAttributes.containsKey("saml_map_user")
        if (configurationAttributes.containsKey("saml_map_user")):
            print "saml_map_user present. Value = " + configurationAttributes.get("saml_map_user").getValue2()
            saml_map_user = StringHelper.toBoolean(configurationAttributes.get("saml_map_user").getValue2(), False)
        skip_step = requestParameters.get("skip_step")
        if(not ArrayHelper.isEmpty(skip_step)):
            skip = bool(skip_step[0])
            if(skip):
                return True
        if (step == 1):
            print "InboundSaml basic authenticate for step 1", requestParameters

            credentials = Identity.instance().getCredentials()
            user_name = credentials.getUsername()
            user_password = credentials.getPassword()

            logged_in = False
            if (StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password)):
                userService = UserService.instance()
                logged_in = userService.authenticate(user_name, user_password)

            if (not logged_in):
                conversationContext = Contexts.getSessionContext()
                conversationContext.set("mode_valid", False)
                context.set("inbound_saml_login_steps", 1)
                return False
            context.set("inbound_saml_login_steps", 1)
            return True
        elif (step == 2):
            print "InboundSaml saml authenticate for step 2", requestParameters

            saml_response_array = requestParameters.get("SAMLResponse")
            if ArrayHelper.isEmpty(saml_response_array):
                print "InboundSaml authenticate for step 2. saml_response is empty"
                return False

            saml_response = saml_response_array[0]

            print "InboundSaml authenticate for step 2. saml_response: " + saml_response

            samlResponse = Response(self.samlConfiguration)
            samlResponse.loadXmlFromBase64(saml_response)
            
            if (not samlResponse.isValid()):
                print "InboundSaml authenticate for step 2. saml_response isn't valid"

            name_id = samlResponse.getNameId()
            print "InboundSaml authenticate for step 2. name_id: " + name_id
            attributes = samlResponse.getAttributes()
            print attributes
            print attributes.keySet()
            for key in attributes.keySet():
                print key
                print attributes.get(key)
                
                
            if (StringHelper.isEmpty(name_id)):
                print "InboundSaml authenticate for step 2. name_id is invalid"
                return False

            # Use persistent Id as saml_user_uid
            saml_user_uid = name_id

            if (saml_map_user):
                print "InboundSaml authenticate for step 2. Attempting to find user by oxExternalUid: saml:" + saml_user_uid

                # Check if the is user with specified saml_user_uid
                find_user_by_uid = userService.getUserByAttribute("oxExternalUid", "saml:" + saml_user_uid)

                if (find_user_by_uid == None):
                    print "Saml authenticate for step 1. Failed to find user"
                    user = User()
                    customAttributes = java.util.ArrayList()
                    for key in attributes.keySet():
                        ldapAttributes = attributeService.getAllAttributes()
                        for ldapAttribute in ldapAttributes:
                            saml2Uri = ldapAttribute.getSaml2Uri()
                            if( saml2Uri == None ):
                                saml2Uri = attributeService.getDefaultSaml2Uri(ldapAttribute.getName())
                            if( saml2Uri == key ):
                                attribute = CustomAttribute(ldapAttribute.getName())
                                attribute.setValues(attributes.get(key))
                                customAttributes.add(attribute)
                    attribute=CustomAttribute("oxExternalUid")
                    attribute.setValue("saml:" + saml_user_uid)
                    customAttributes.add(attribute)
                    user.setCustomAttributes(customAttributes)
                    if( user.getAttribute("sn") == None ):
                        attribute=CustomAttribute("sn")
                        attribute.setValue(saml_user_uid)
                        customAttributes.add(attribute)
                    if( user.getAttribute("cn") == None ):
                        attribute=CustomAttribute("cn")
                        attribute.setValue(saml_user_uid)
                        customAttributes.add(attribute)
                    find_user_by_uid = userService.addUser(user)
                    


                found_user_name = find_user_by_uid.getUserId()
                print "InboundSaml authenticate for step 2. found_user_name: " + found_user_name

                credentials = Identity.instance().getCredentials()
                credentials.setUsername(found_user_name)
                credentials.setUser(find_user_by_uid)
            
                print "InboundSaml authenticate for step 2. Setting count steps to 2"
                context.set("saml_count_login_steps", 2)

                return True
                
    def prepareForStep(self, configurationAttributes, requestParameters, step):
        print "InboundSaml prepareForStep: " , step
        context = Contexts.getEventContext()
        authenticationService = AuthenticationService.instance()
        if (step == 1):
            print "InboundSaml prepare for step 1"

            print "InboundSaml prepare for step 1. Store current request parameters in session because Saml don't pass them via service URI"
            authenticationService.storeRequestParametersInSession()
            
            httpService = HttpService.instance();
            request = FacesContext.getCurrentInstance().getExternalContext().getRequest()
            assertionConsumerServiceUrl = httpService.constructServerUrl(request) + "/postlogin"
            print "InboundSaml prepare for step 1. Prepared assertionConsumerServiceUrl:", assertionConsumerServiceUrl

            # Generate an AuthRequest and send it to the identity provider
            samlAuthRequest = AuthRequest(self.samlConfiguration)
            saml_idp_auth_request_uri = self.samlConfiguration.getIdpSsoTargetUrl() + "?SAMLRequest=" + samlAuthRequest.getRequest(True, assertionConsumerServiceUrl)

            print "InboundSaml prepare for step 1. saml_idp_auth_request_uri: " + saml_idp_auth_request_uri
            
            context.set("saml_idp_auth_request_uri", saml_idp_auth_request_uri)
            return True
        else:
            print "InboundSaml prepare for step 2"

            print "InboundSaml prepare for step 2. Store current request parameters in session because Saml don't pass them via service URI"
            authenticationService.storeRequestParametersInSession()
            
            httpService = HttpService.instance();
            request = FacesContext.getCurrentInstance().getExternalContext().getRequest()
            assertionConsumerServiceUrl = httpService.constructServerUrl(request) + "/postlogin"
            print "InboundSaml prepare for step 2. Prepared assertionConsumerServiceUrl:", assertionConsumerServiceUrl

            # Generate an AuthRequest and send it to the identity provider
            samlAuthRequest = AuthRequest(self.samlConfiguration)
            saml_idp_auth_request_uri = self.samlConfiguration.getIdpSsoTargetUrl() + "?SAMLRequest=" + samlAuthRequest.getRequest(True, assertionConsumerServiceUrl)

            print "InboundSaml prepare for step 2. saml_idp_auth_request_uri: " + saml_idp_auth_request_uri
            
            context.set("saml_idp_auth_request_uri", saml_idp_auth_request_uri)

            return True
            
    def getExtraParametersForStep(self, configurationAttributes, step):
        return Arrays.asList("saml_user_uid")

    def getCountAuthenticationSteps(self, configurationAttributes):
        print "InboundSaml getCountAuthenticationSteps: "
        context = Contexts.getEventContext()
        conversationContext = Contexts.getSessionContext()
        
        if (context.isSet("inbound_saml_login_steps")):
            return context.get("inbound_saml_login_steps")

        return 2

    def getPageForStep(self, configurationAttributes, step):
        print "InboundSaml getPageForStep: " , step
        if (step == 1):
            return "/auth/inboundSaml/inboundsamllogin.xhtml"
        return "/auth/saml/samllogin.xhtml"

    def logout(self, configurationAttributes, requestParameters):
        return True

    def getApiVersion(self):
        return 3

    def isPassedStep1():
        credentials = Identity.instance().getCredentials()
        user_name = credentials.getUsername()
        passed_step1 = StringHelper.isNotEmptyString(user_name)

        return passed_step1