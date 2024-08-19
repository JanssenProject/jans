# Janssen Project software is available under the Apache 2.0 License (2004). See http://www.apache.org/licenses/ for full text.
# Copyright (c) 2020, Janssen Project
#
# Inbound OAuth and OpenID Connect authentication Passport Script for Janssen
#
# Author: Jose Gonzalez
# Author: Yuriy Movchan
# Author: Kiran Mali
# 
from io.jans.as.common.model.common import User
from io.jans.as.model.common import WebKeyStorage
from io.jans.as.model.configuration import AppConfiguration
from io.jans.as.model.crypto import CryptoProviderFactory
from io.jans.as.model.jwt import Jwt, JwtClaimName
from io.jans.as.model.util import Base64Util
from io.jans.as.server.service import AppInitializer, AuthenticationService
from io.jans.as.common.service.common import UserService, EncryptionService
from io.jans.as.server.service.net import HttpService
from io.jans.as.server.security import Identity
from io.jans.as.server.util import ServerUtil
from io.jans.model.custom.script.type.auth import PersonAuthenticationType
from io.jans.service.cdi.util import CdiUtil
from io.jans.util import StringHelper

from io.jans.jsf2.service import FacesService
from jakarta.faces.application import FacesMessage
from jakarta.faces.context import FacesContext
from java.util import ArrayList, Arrays, Collections

import json
import sys
import datetime

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Passport Social. init"
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
        return 11
        
    def getAuthenticationMethodClaims(self, requestParameters):
        return None
        
    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def authenticate(self, configurationAttributes, requestParameters, step):
        print "Passport. authenticate for step %s called" % str(step)
        identity = CdiUtil.bean(Identity)

        # Loading self.registeredProviders in case passport destroyed
        if not hasattr(self,'registeredProviders'):
            print "Passport. Fetching registered providers."
            self.parseProviderConfigs()

        if step == 1:
            # Get JWT token
            jwt_param = ServerUtil.getFirstValue(requestParameters, "user")

            if jwt_param != None:
                print "Passport. authenticate for step 1. JWT user profile token found"

                # Parse JWT and validate
                jwt = Jwt.parse(jwt_param)
                if not self.validSignature(jwt):
                    return False

                if self.jwtHasExpired(jwt):
                    return False

                (user_profile, jsonp) = self.getUserProfile(jwt)
                if user_profile == None:
                    return False

                sessionAttributes = identity.getSessionId().getSessionAttributes()
                self.skipProfileUpdate = StringHelper.equalsIgnoreCase(sessionAttributes.get("skipPassportProfileUpdate"), "true")

                return self.attemptAuthentication(identity, user_profile, jsonp)

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
            jsonp = identity.getWorkingParameter("passport_user_profile")

            if mail == None:
                self.setMessageError(FacesMessage.SEVERITY_ERROR, "Email was missing in user profile")
            elif jsonp != None:
                # Completion of profile takes place
                user_profile = json.loads(jsonp)
                user_profile["mail"] = [ mail ]

                return self.attemptAuthentication(identity, user_profile, jsonp)

            print "Passport. authenticate for step 2. Failed: expected mail value in HTTP request and json profile in session"
            return False


    def prepareForStep(self, configurationAttributes, requestParameters, step):
        print "Passport. prepareForStep called %s"  % str(step)
        identity = CdiUtil.bean(Identity)

        faces_context = CdiUtil.bean(FacesContext)
        request_parameters = faces_context.getExternalContext().getRequestParameterMap()

        passport_strategy_failed = None
        try:
            passport_strategy_failed = request_parameters['failure']
            print("Passport. failure return from passport: %s, Check Passport logs " % passport_strategy_failed)
        except Exception as _:
            pass

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
                    elif passport_strategy_failed != None:
                        print("Passport. passport strategy failed : %s, Check Passport logs" % passport_strategy_failed)
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

        if step == 1:
            return "/auth/passport/passportlogin.xhtml"
        return "/auth/passport/passportpostlogin.xhtml"


    def getNextStep(self, configurationAttributes, requestParameters, step):

        if step == 1:
            identity = CdiUtil.bean(Identity)
            provider = identity.getWorkingParameter("selectedProvider")
            if provider != None:
                return 1

        return -1

    def getLogoutExternalUrl(self, configurationAttributes, requestParameters):
        print "Get external logout URL call"
        return None

    def logout(self, configurationAttributes, requestParameters):
        return True

    def processKeyStoreProperties(self, attrs):
        file = attrs.get("key_store_file")
        password = attrs.get("key_store_password")
        providersJsonFile = attrs.get("providers_json_file")
        passportFQDN = attrs.get("passport_fqdn")
        passportTokenEndpointPath = attrs.get("passport_token_endpoint_path")
        passportAuthEndpointPath = attrs.get("passport_auth_endpoint_path")

        if file != None and password != None:
            file = file.getValue2()
            password = password.getValue2()
            providersJsonFile = providersJsonFile.getValue2()
            passportFQDN = passportFQDN.getValue2()
            passportTokenEndpointPath = passportTokenEndpointPath.getValue2()
            passportAuthEndpointPath = passportAuthEndpointPath.getValue2()

            if StringHelper.isNotEmpty(file) and StringHelper.isNotEmpty(password):
                self.keyStoreFile = file
                self.keyStorePassword = password
                self.providersJsonFile = providersJsonFile
                self.passportFQDN = passportFQDN
                self.passportTokenEndpointPath = passportTokenEndpointPath
                self.passportAuthEndpointPath = passportAuthEndpointPath
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

    def parseAllProviders(self):
        registeredProviders = {}
        print "Passport. parseAllProviders. Adding providers"
        
        jsonData = json.loads(open(self.providersJsonFile).read())
        print(jsonData)
        
        for provider in jsonData:
          print(provider["id"])
          registeredProviders[provider["id"]] = {
            "emailLinkingSafe": provider["emailLinkingSafe"],
            "requestForEmail": provider["requestForEmail"],
            "logo_img": provider["logoImg"],
            "displayName": provider["displayName"]
          }

        print(registeredProviders)

        return registeredProviders


    def parseProviderConfigs(self):

        registeredProviders = {}
        try:
            registeredProviders = self.parseAllProviders()
            
            if len(registeredProviders.keys()) > 0:
                print "Passport. parseProviderConfigs. Configured providers:", registeredProviders
            else:
                print "Passport. parseProviderConfigs. No providers registered yet"
        except Exception as e:
            print(e)
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
            tokenEndpoint = "%s%s" % (self.passportFQDN, self.passportTokenEndpointPath)

            httpService = CdiUtil.bean(HttpService)
            httpclient = httpService.getHttpsClient()

            print "Passport. getPassportRedirectUrl. Obtaining token from passport at %s" % tokenEndpoint
            resultResponse = httpService.executeGet(httpclient, tokenEndpoint, Collections.singletonMap("Accept", "text/json"))
            httpResponse = resultResponse.getHttpResponse()
            bytes = httpService.getResponseContent(httpResponse)

            response = httpService.convertEntityToString(bytes)
            print "Passport. getPassportRedirectUrl. Response was %s" % httpResponse.getStatusLine().getStatusCode()

            tokenObj = json.loads(response)
            url = "%s%s/%s/%s" % (self.passportFQDN, self.passportAuthEndpointPath, provider, tokenObj["token_"])
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
            appConfiguration.setKeyRegenerationEnabled(False)

            cryptoProvider = CryptoProviderFactory.getCryptoProvider(appConfiguration)
            valid = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), jwt.getHeader().getKeyId(),
                                                        None, None, jwt.getHeader().getSignatureAlgorithm())
        except:
            print "Exception: ", sys.exc_info()[1]

        print "Passport. validSignature. Validation result was %s" % valid
        return valid


    def jwtHasExpired(self, jwt):
        # Check if jwt has expired
        jwt_claims = jwt.getClaims()
        try:
            exp_date_timestamp = float(jwt_claims.getClaimAsString(JwtClaimName.EXPIRATION_TIME))
            exp_date = datetime.datetime.fromtimestamp(exp_date_timestamp)
            hasExpired = exp_date < datetime.datetime.now()
        except:
            print "Exception: The JWT does not have '%s' attribute" % JwtClaimName.EXPIRATION_TIME
            return False

        return hasExpired


    def getUserProfile(self, jwt):
        jwt_claims = jwt.getClaims()
        user_profile_json = None
        user_profile = None

        try:
            user_profile_json = CdiUtil.bean(EncryptionService).decrypt(jwt_claims.getClaimAsString("data"))
            user_profile = json.loads(user_profile_json)
        except Exception as e:
            print e
            print "Passport. getUserProfile. Problem obtaining user profile json representation"

        return (user_profile, user_profile_json)


    def attemptAuthentication(self, identity, user_profile, user_profile_json):

        uidKey = "uid"
        if not self.checkRequiredAttributes(user_profile, [uidKey, self.providerKey]):
            return False

        provider = user_profile[self.providerKey]
        if not provider in self.registeredProviders:
            print "Passport. attemptAuthentication. Identity Provider %s not recognized" % provider
            return False

        uid = user_profile[uidKey][0]
        externalUid = "passport-%s:%s" % (provider, uid)

        userService = CdiUtil.bean(UserService)
        userByUid = userService.getUserByAttribute("jansExtUid", externalUid, True)

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
                    self.setMessageError(FacesMessage.SEVERITY_ERROR, "Email value corresponds to an already existing provisioned account")
        else:
            if userByMail == None:
                doAdd = True
            elif self.registeredProviders[provider]["emailLinkingSafe"]:

                tmpList = userByMail.getAttributeValues("jansExtUid")
                tmpList = ArrayList() if tmpList == None else ArrayList(tmpList)
                tmpList.add(externalUid)
                userByMail.setAttribute("jansExtUid", tmpList, True)

                userByUid = userByMail
                print "External user supplying mail %s will be linked to existing account '%s'" % (email, userByMail.getUserId())
                doUpdate = True
            else:
                print "An attempt to supply an email of an existing user was made. Turn on 'emailLinkingSafe' if you want to enable linking"
                self.setMessageError(FacesMessage.SEVERITY_ERROR, "Email value corresponds to an already existing account. If you already have a username and password use those instead of an external authentication site to get access.")

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
        except Exception as e:
            print e
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


    def setMessageError(self, severity, msg):
        facesMessages = CdiUtil.bean(FacesMessages)
        facesMessages.setKeepMessages()
        facesMessages.clear()
        facesMessages.add(severity, msg)


    def checkRequiredAttributes(self, profile, attrs):

        for attr in attrs:
            if (not attr in profile) or len(profile[attr]) == 0:
                print "Passport. checkRequiredAttributes. Attribute '%s' is missing in profile" % attr
                return False
        return True


    def addUser(self, externalUid, profile, userService):

        newUser = User()
        #Fill user attrs
        newUser.setAttribute("jansExtUid", externalUid, True)
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
                    foundUser.setAttribute("jansEmail", oxtrustMails)