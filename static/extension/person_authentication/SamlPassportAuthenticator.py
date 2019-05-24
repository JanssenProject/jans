# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2019, Gluu
#
# Author: Jose Gonzalez
# Author: Yuriy Movchan
#
from org.gluu.jsf2.service import FacesService
from org.gluu.jsf2.message import FacesMessages

from org.gluu.oxauth.model.common import User, WebKeyStorage
from org.gluu.oxauth.model.config import ConfigurationFactory
from org.gluu.oxauth.model.configuration import AppConfiguration
from org.gluu.oxauth.model.crypto import CryptoProviderFactory
from org.gluu.oxauth.model.jwt import Jwt, JwtClaimName
from org.gluu.oxauth.model.util import Base64Util
from org.gluu.oxauth.service import AppInitializer, AuthenticationService, UserService
from org.gluu.oxauth.model.authorize import AuthorizeRequestParam
from org.gluu.oxauth.service.net import HttpService
from org.gluu.oxauth.security import Identity
from org.gluu.oxauth.util import ServerUtil
from org.gluu.config.oxtrust import LdapOxPassportConfiguration
from org.gluu.model.custom.script.type.auth import PersonAuthenticationType
from org.gluu.service.cdi.util import CdiUtil
from org.gluu.util import StringHelper
from java.util import ArrayList, Arrays, Collections, HashSet

from javax.faces.application import FacesMessage
from javax.faces.context import FacesContext

import json
import sys

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        print "Passport. init called"

        self.extensionModule = self.loadExternalModule(configurationAttributes.get("extension_module"))
        extensionResult = self.extensionInit(configurationAttributes)
        if extensionResult != None:
            return extensionResult

        print "Passport. init. Behaviour is inbound SAML"
        success = self.processKeyStoreProperties(configurationAttributes)
        
        if success:
            self.providerKey = "provider"
            self.customAuthzParameter = self.getCustomAuthzParameter(configurationAttributes.get("authz_req_param_provider"))
            print "Passport. init. Initialization success"
        else:
            print "Passport. init. Initialization failed"
        return success


    def destroy(self, configurationAttributes):
        print "Passport. destroy called"
        return True


    def getApiVersion(self):
        return 2


    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True


    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None


    def authenticate(self, configurationAttributes, requestParameters, step):

        extensionResult = self.extensionAuthenticate(configurationAttributes, requestParameters, step)
        if extensionResult != None:
            return extensionResult

        print "Passport. authenticate for step %s called" % str(step)
        identity = CdiUtil.bean(Identity)

        if step == 1:
            jwt_param = None
            if self.isInboundFlow(identity):
                print "Passport. authenticate for step 1. Detected idp-initiated inbound Saml flow"
                jwt_param = identity.getSessionId().getSessionAttributes().get(AuthorizeRequestParam.STATE)

            if jwt_param == None:
                jwt_param = ServerUtil.getFirstValue(requestParameters, "user")
                
            if jwt_param != None:
                print "Passport. authenticate for step 1. JWT user profile token found"

                # Parse JWT and validate
                jwt = Jwt.parse(jwt_param)
                if not self.validSignature(jwt):
                    return False

                if self.jwtHasExpired(jwt):
                    return False

                (user_profile, json) = self.getUserProfile(jwt)
                if user_profile == None:
                    return False

                return self.attemptAuthentication(identity, user_profile, json)

            #See passportlogin.xhtml
            provider = ServerUtil.getFirstValue(requestParameters, "loginForm:provider")
            if StringHelper.isEmpty(provider):

                #it's username + passw auth
                print "Passport. authenticate for step 1. Basic authentication detected"
                logged_in = False

                credentials = identity.getCredentials()
                user_name = credentials.getUsername()
                user_password = credentials.getPassword()

                if StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password):
                    authenticationService = CdiUtil.bean(AuthenticationService)
                    logged_in = authenticationService.authenticate(user_name, user_password)

                print "Passport. authenticate for step 1. Basic authentication returned: %s" % logged_in
                return logged_in

            elif provider in self.registeredProviders:
                #it's a recognized external IDP
                identity.setWorkingParameter("selectedProvider", provider)
                print "Passport. authenticate for step 1. Retrying step 1"
                #see prepareForStep (step = 1)
                return True

        if step == 2:
            mail = ServerUtil.getFirstValue(requestParameters, "loginForm:email")
            json = identity.getWorkingParameter("passport_user_profile")

            if mail == None:
                self.setEmailMessageError()
            elif json != None:
                # Completion of profile takes place
                user_profile = self.getProfileFromJson(json)
                user_profile["mail"] = mail

                return self.attemptAuthentication(identity, user_profile, json)

            print "Passport. authenticate for step 2. Failed: expected mail value in HTTP request and json profile in session"
            return False


    def prepareForStep(self, configurationAttributes, requestParameters, step):
        
        extensionResult = self.extensionPrepareForStep(configurationAttributes, requestParameters, step)
        if extensionResult != None:
            return extensionResult

        print "Passport. prepareForStep called %s"  % str(step)
        identity = CdiUtil.bean(Identity)

        if step == 1:
            #re-read the strategies config (for instance to know which strategies have enabled the email account linking)
            self.parseProviderConfigs()
            identity.setWorkingParameter("externalProviders", json.dumps(self.registeredProviders))

            providerParam = self.customAuthzParameter
            url = None

            sessionAttributes = identity.getSessionId().getSessionAttributes()
            self.skipProfileUpdate = StringHelper.equalsIgnoreCase(sessionAttributes.get("skipPassportProfileUpdate"), "true")

            #this param could have been set previously in authenticate step if current step is being retried
            provider = identity.getWorkingParameter("selectedProvider")
            if provider != None:
                url = self.getPassportRedirectUrl(provider)
                identity.setWorkingParameter("selectedProvider", None)

            elif providerParam != None:
                paramValue = sessionAttributes.get(providerParam)

                if paramValue != None:
                    print "Passport. prepareForStep. Found value in custom param of authorization request: %s" % paramValue
                    provider = self.getProviderFromJson(paramValue)

                    if provider == None:
                        print "Passport. prepareForStep. A provider value could not be extracted from custom authorization request parameter"
                    elif not provider in self.registeredProviders:
                        print "Passport. prepareForStep. Provider '%s' not part of known configured IDPs/OPs" % provider
                    else:
                        url = self.getPassportRedirectUrl(provider)

            if url == None:
                print "Passport. prepareForStep. A page to manually select an identity provider will be shown"
            else:
                facesService = CdiUtil.bean(FacesService)
                facesService.redirectToExternalURL(url)

        return True


    def getExtraParametersForStep(self, configurationAttributes, step):
        print "Passport. getExtraParametersForStep called"
        if step == 1:
            return Arrays.asList("selectedProvider", "externalProviders")
        elif step == 2:
            return Arrays.asList("passport_user_profile")
        return None


    def getCountAuthenticationSteps(self, configurationAttributes):
        print "Passport. getCountAuthenticationSteps called"
        identity = CdiUtil.bean(Identity)
        if identity.getWorkingParameter("passport_user_profile") != None:
            return 2
        return 1


    def getPageForStep(self, configurationAttributes, step):
        print "Passport. getPageForStep called"

        extensionResult = self.extensionGetPageForStep(configurationAttributes, step)
        if extensionResult != None:
            return extensionResult

        if step == 1:
            identity = CdiUtil.bean(Identity)
            if self.isInboundFlow(identity):
                print "Passport. getPageForStep for step 1. Detected inbound Saml flow"
                return "/postlogin.xhtml"

            return "/auth/passport/passportlogin.xhtml"

        return "/auth/passport/passportpostlogin.xhtml"


    def getNextStep(self, configurationAttributes, requestParameters, step):
        if step == 1:
            identity = CdiUtil.bean(Identity)
            provider = identity.getWorkingParameter("selectedProvider")
            if provider != None:
                return 1

        return -1


    def logout(self, configurationAttributes, requestParameters):
        return True

# Extension module related functions

    def extensionInit(self, configurationAttributes):

        if self.extensionModule == None:
            return None
        return self.extensionModule.init(configurationAttributes)


    def extensionAuthenticate(self, configurationAttributes, requestParameters, step):

        if self.extensionModule == None:
            return None
        return self.extensionModule.authenticate(configurationAttributes, requestParameters, step)


    def extensionPrepareForStep(self, configurationAttributes, requestParameters, step):

        if self.extensionModule == None:
            return None
        return self.extensionModule.prepareForStep(configurationAttributes, requestParameters, step)


    def extensionGetPageForStep(self, configurationAttributes, step):

        if self.extensionModule == None:
            return None
        return self.extensionModule.getPageForStep(configurationAttributes, step)

# Initalization routines

    def loadExternalModule(self, simpleCustProperty):

        if simpleCustProperty != None:
            print "Passport. loadExternalModule. Loading passport extension module..."
            moduleName = simpleCustProperty.getValue2()
            try:
                module = __import__(moduleName)
                return module
            except:
                print "Passport. loadExternalModule. Failed to load module %s" % moduleName
                print "Exception: ", sys.exc_info()[1]
                print "Passport. loadExternalModule. Flow will be driven entirely by routines of main passport script"
        return None


    def processKeyStoreProperties(self, attrs):
        file = attrs.get("key_store_file")
        password = attrs.get("key_store_password")

        if file != None and password != None:
            file = file.getValue2()
            password = password.getValue2()

            if StringHelper.isNotEmpty(file) and StringHelper.isNotEmpty(password):
                self.keyStoreFile = file
                self.keyStorePassword = password
                return True

        print "Passport. readKeyStoreProperties. Properties key_store_file or key_store_password not found or empty"
        return False


    def getCustomAuthzParameter(self, simpleCustProperty):

        customAuthzParameter = None
        if simpleCustProperty != None:
            prop = simpleCustProperty.getValue2()
            if StringHelper.isNotEmpty(prop):
                customAuthzParameter = prop

        if customAuthzParameter == None:
            print "Passport. getCustomAuthzParameter. No custom param for OIDC authz request in script properties"
            print "Passport. getCustomAuthzParameter. Passport flow cannot be initiated by doing an OpenID connect authorization request"
        else:
            print "Passport. getCustomAuthzParameter. Custom param for OIDC authz request in script properties: %s" % customAuthzParameter

        return customAuthzParameter

# Configuration parsing

    def parseAllProviders(self):

        registeredProviders = {}
        print "Passport. parseAllProviders. Adding providers"
        passportDN = CdiUtil.bean(ConfigurationFactory).getPersistenceConfiguration().getConfiguration().getString("oxpassport_ConfigurationEntryDN")
        entryManager = CdiUtil.bean(AppInitializer).createPersistenceEntryManager()
        
        config = LdapOxPassportConfiguration()
        config = entryManager.find(config.getClass(), passportDN).getPassportConfiguration()
        config = config.getProviders() if config != None else config
            
        if config != None and len(config) > 0:      
            for prvdetails in config:
                if prvdetails.isEnabled():              
                    registeredProviders[prvdetails.getId()] = {     
                        "emailLinkingSafe": prvdetails.isEmailLinkingSafe(), 
                        "requestForEmail" : prvdetails.isRequestForEmail(),
                        "logo_img": prvdetails.getLogoImg(),
                        "displayName": prvdetails.getDisplayName(),
                        "type": prvdetails.getType()
                    }
        
        return registeredProviders


    def parseProviderConfigs(self):

        registeredProviders = {}
        try:
            registeredProviders = self.parseAllProviders()
            toRemove = []
            
            for provider in registeredProviders:                
                if registeredProviders[provider]["type"] != "saml":
                    toRemove.append(provider)
                else:
                    registeredProviders[provider]["saml"] = True
                
            for provider in toRemove:
                registeredProviders.pop(provider)
                
            if len(registeredProviders.keys()) > 0:
                print "Passport. parseProviderConfigs. Configured providers:", registeredProviders    
            else:
                print "Passport. parseProviderConfigs. No providers registered yet"
        except:
            print "Passport. parseProviderConfigs. An error occurred while building the list of supported authentication providers", sys.exc_info()[1]
            
        self.registeredProviders = registeredProviders
        
# Auxiliary routines

    def getProviderFromJson(self, providerJson):

        provider = None
        try:
            obj = json.loads(Base64Util.base64urldecodeToString(providerJson))
            provider = obj[self.providerKey]
        except:
            print "Passport. getProviderFromJson. Could not parse provided Json string. Returning None"

        return provider


    def getPassportRedirectUrl(self, provider):

        # provider is assumed to exist in self.registeredProviders
        url = None
        try:
            facesContext = CdiUtil.bean(FacesContext)
            tokenEndpoint = "https://%s/passport/token" % facesContext.getExternalContext().getRequest().getServerName()

            httpService = CdiUtil.bean(HttpService)
            httpclient = httpService.getHttpsClient()

            print "Passport. getPassportRedirectUrl. Obtaining token from passport at %s" % tokenEndpoint
            resultResponse = httpService.executeGet(httpclient, tokenEndpoint, Collections.singletonMap("Accept", "text/json"))
            httpResponse = resultResponse.getHttpResponse()
            bytes = httpService.getResponseContent(httpResponse)

            response = httpService.convertEntityToString(bytes)
            print "Passport. getPassportRedirectUrl. Response was %s" % httpResponse.getStatusLine().getStatusCode()

            tokenObj = json.loads(response)
            url = "/passport/auth/%s/%s" % (provider, tokenObj["token_"])
        except:
            print "Passport. getPassportRedirectUrl. Error building redirect URL: ", sys.exc_info()[1]

        return url


    def validSignature(self, jwt):

        print "Passport. validSignature. Checking JWT token signature"
        valid = False

        try:
            appConfiguration = AppConfiguration()
            appConfiguration.setWebKeysStorage(WebKeyStorage.KEYSTORE)
            appConfiguration.setKeyStoreFile(self.keyStoreFile)
            appConfiguration.setKeyStoreSecret(self.keyStorePassword)

            cryptoProvider = CryptoProviderFactory.getCryptoProvider(appConfiguration)
            valid = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), jwt.getHeader().getKeyId(),
                                                        None, None, jwt.getHeader().getAlgorithm())
        except:
            print "Exception: ", sys.exc_info()[1]

        print "Passport. validSignature. Validation result was %s" % valid
        return valid

    def jwtHasExpired(self, jwt):
        # Check if jwt has expired
        jwt_claims = jwt.getClaims()
        try:
            exp_date = jwt_claims.getClaimAsDate(JwtClaimName.EXPIRATION_TIME)
            hasExpired = exp_date < datetime.now()
        except:
            print "Exception: The JWT does not have '%s' attribute" % JwtClaimName.EXPIRATION_TIME
            return False

        return hasExpired

    def getUserProfile(self, jwt):
        # Check if there is user profile
        jwt_claims = jwt.getClaims()
        user_profile_json = jwt_claims.getClaimAsString("data")
        if StringHelper.isEmpty(user_profile_json):
            print "Passport. getUserProfile. User profile missing in JWT token"
            user_profile = None
        else:
            user_profile = self.getProfileFromJson(user_profile_json)

        return (user_profile, user_profile_json)


    def getProfileFromJson(self, user_profile_json):
        data = json.loads(user_profile_json)
        user_profile = {}
        for key in data.keys():
            user_profile[key.lower()] = data[key]
        return user_profile


    def attemptAuthentication(self, identity, user_profile, user_profile_json):

        uidKey = "uid"
        if not self.checkRequiredAttributes(user_profile, [uidKey, self.providerKey]):
            return False

        provider = user_profile[self.providerKey]
        if not provider in self.registeredProviders:
            print "Passport. attemptAuthentication. Identity Provider %s not recognized" % provider
            return False

        uid = user_profile[uidKey][0]
        externalUid = "passport-%s:%s:%s" % ("saml", provider, uid)

        userService = CdiUtil.bean(UserService)
        userByUid = self.getUserByExternalUid(uid, provider, userService)

        email = None
        if "mail" in user_profile:
            email = user_profile["mail"]
            if len(email) == 0:
                email = None
            else:
                email = email[0]
                user_profile["mail"] = [ email ]

        if email == None and self.registeredProviders[provider]["requestForEmail"]:
            print "Passport. attemptAuthentication. Email was not received"

            if userByUid != None:
                # This avoids asking for the email over every login attempt
                email = userByUid.getAttribute("mail")
                if email != None:
                    print "Passport. attemptAuthentication. Filling missing email value with %s" % email
                    user_profile["mail"] = [ email ]

            if email == None:
                # Store user profile in session and abort this routine
                identity.setWorkingParameter("passport_user_profile", user_profile_json)
                return True

        userByMail = None if email == None else userService.getUserByAttribute("mail", email)

        # Determine if we should add entry, update existing, or deny access
        doUpdate = False
        doAdd = False
        if userByUid != None:
            print "User with externalUid '%s' already exists" % externalUid
            if userByMail == None:
                doUpdate = True
            else:
                if userByMail.getUserId() == userByUid.getUserId():
                    doUpdate = True
                else:
                    print "Users with externalUid '%s' and mail '%s' are different. Access will be denied. Impersonation attempt?" % (externalUid, email)
        else:
            if userByMail == None:
                doAdd = True
            elif self.registeredProviders[provider]["emailLinkingSafe"]:

                tmpList = userByMail.getAttributeValues("oxExternalUid")
                tmpList = ArrayList() if tmpList == None else ArrayList(tmpList)
                tmpList.add(externalUid)
                userByMail.setAttribute("oxExternalUid", tmpList)

                userByUid = userByMail
                print "External user supplying mail %s will be linked to existing account '%s'" % (email, userByMail.getUserId())
                doUpdate = True
            else:
                print "An attempt to supply an email of an existing user was made. Turn on 'emailLinkingSafe' if you want to enable linking"

        username = None
        try:
            if doUpdate:
                username = userByUid.getUserId()
                print "Passport. attemptAuthentication. Updating user %s" % username
                self.updateUser(userByUid, user_profile, userService)
            elif doAdd:
                print "Passport. attemptAuthentication. Creating user %s" % externalUid
                newUser = self.addUser(externalUid, user_profile, userService)
                username = newUser.getUserId()
        except:
            print "Exception: ", sys.exc_info()[1]
            print "Passport. attemptAuthentication. Authentication failed"
            return False

        if username == None:
            print "Passport. attemptAuthentication. Authentication attempt was rejected"
            return False
        else:
            logged_in = CdiUtil.bean(AuthenticationService).authenticate(username)
            print "Passport. attemptAuthentication. Authentication for %s returned %s" % (username, logged_in)
            return logged_in


    def getUserByExternalUid(self, uid, provider, userService):
        newFormat = "passport-%s:%s:%s" % ("saml", provider, uid)
        user = userService.getUserByAttribute("oxExternalUid", newFormat)

        if user == None:
            oldFormat = "passport-%s:%s" % ("saml", uid)
            user = userService.getUserByAttribute("oxExternalUid", oldFormat)

            if user != None:
                # Migrate to newer format
                list = HashSet(user.getAttributeValues("oxExternalUid"))
                list.remove(oldFormat)
                list.add(newFormat)
                user.setAttribute("oxExternalUid", ArrayList(list))
                print "Migrating user's oxExternalUid to newer format 'passport-saml:provider:uid'"
                userService.updateUser(user)

        return user


    def setEmailMessageError(self):
        facesMessages = CdiUtil.bean(FacesMessages)
        facesMessages.setKeepMessages()
        facesMessages.clear()
        facesMessages.add(FacesMessage.SEVERITY_ERROR, "Email was missing in user profile")


    def checkRequiredAttributes(self, profile, attrs):

        for attr in attrs:
            if (not attr in profile) or len(profile[attr]) == 0:
                print "Passport. checkRequiredAttributes. Attribute '%s' is missing in profile" % attr
                return False
        return True


    def addUser(self, externalUid, profile, userService):

        newUser = User()
        #Fill user attrs
        newUser.setAttribute("oxExternalUid", externalUid)
        self.fillUser(newUser, profile)
        newUser = userService.addUser(newUser, True)
        return newUser


    def updateUser(self, foundUser, profile, userService):
        # when this is false, there might still some updates taking place (e.g. not related to profile attrs released by external provider)
        if (not self.skipProfileUpdate):
            self.fillUser(foundUser, profile)
        userService.updateUser(foundUser)


    def fillUser(self, foundUser, profile):

        for attr in profile:
            # "provider" is disregarded if part of mapping
            if attr != self.providerKey:
                values = profile[attr]
                print "%s = %s" % (attr, values)
                foundUser.setAttribute(attr, values)

                if attr == "mail":
                    oxtrustMails = []
                    for mail in values:
                        oxtrustMails.append('{"value":"%s","primary":false}' % mail)
                    foundUser.setAttribute("oxTrustEmail", oxtrustMails)

# IDP-initiated flow routines

    def isInboundFlow(self, identity):
        sessionId = identity.getSessionId()
        if sessionId == None:
            # Detect mode if there is no session yet. It's needed for getPageForStep method
            facesContext = CdiUtil.bean(FacesContext)
            requestParameters = facesContext.getExternalContext().getRequestParameterMap()

            authz_state = requestParameters.get(AuthorizeRequestParam.STATE)
        else:
            authz_state = identity.getSessionId().getSessionAttributes().get(AuthorizeRequestParam.STATE)

        if self.isInboundJwt(authz_state):
            return True

        return False


    def isInboundJwt(self, value):
        if value == None:
            return False
        
        try:
            jwt = Jwt.parse(value)
            user_profile_json = jwt.getClaims().getClaimAsString("data")
            if StringHelper.isEmpty(user_profile_json):
                return False
        except:
            return False

        return True
