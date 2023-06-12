# Janssen Project software is available under the Apache 2.0 License (2004). See http://www.apache.org/licenses/ for full text.
# Copyright (c) 2020, Janssen Project
#
# Author: Madhumita Subramaniam
#

from io.jans.service.cdi.util import CdiUtil
from io.jans.as.server.security import Identity
from io.jans.model.custom.script.type.auth import PersonAuthenticationType
from io.jans.as.server.service import AuthenticationService, UserService
from io.jans.util import StringHelper
from io.jans.as.server.util import ServerUtil

from io.jans.as.common.model.common import User
from io.jans.orm import PersistenceEntryManager
from io.jans.as.persistence.model.configuration import GluuConfiguration
from java.math import BigInteger
from java.security import SecureRandom
import java
import sys
import json


from java.util import Collections, HashMap, HashSet, ArrayList, Arrays, Date

from com.google.api.client.googleapis.auth.oauth2 import GoogleIdToken
from com.google.api.client.googleapis.auth.oauth2.GoogleIdToken import Payload
from com.google.api.client.googleapis.auth.oauth2 import GoogleIdTokenVerifier

from com.google.api.client.http.javanet import NetHttpTransport;
from com.google.api.client.json.jackson2 import JacksonFactory;


class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):


        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript,  configurationAttributes):
        print "Google. Initialization"
        google_creds_file = configurationAttributes.get("google_creds_file").getValue2()
        # Load credentials from file
        f = open(google_creds_file, 'r')
        try:
            data = json.loads(f.read())
            print data
            creds = data["web"]
            print creds
        except:
            print "Google. Initialization. Failed to load creds from file:", google_creds_file
            print "Exception: ", sys.exc_info()[1]

            return False
        finally:
            f.close()

        self.client_id = str(creds["client_id"])
        self.project_id = str(creds["project_id"])
        self.auth_uri = str(creds["auth_uri"])
        self.token_uri = str(creds["token_uri"])
        self.auth_provider_x509_cert_url = str(creds["auth_provider_x509_cert_url"])
        self.redirect_uris = str(creds["redirect_uris"])
        self.javascript_origins = str(creds["javascript_origins"])
        print "Google. Initialized successfully"
        return True

    def destroy(self, configurationAttributes):
        print "Google. Destroy"
        print "Google. Destroyed successfully"
        return True

    def getAuthenticationMethodClaims(self, requestParameters):
        return None

    def getApiVersion(self):
        return 11

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None


    def authenticate(self, configurationAttributes, requestParameters, step):
        authenticationService = CdiUtil.bean(AuthenticationService)

        if (step == 1):
            print "Google. Authenticate for step 1"
            identity = CdiUtil.bean(Identity)

            googleCred = ServerUtil.getFirstValue(requestParameters, "credential")
            if googleCred is not None:
                googleIdToken = ServerUtil.getFirstValue(requestParameters, "credential")
                google_Id = self.verifyIDToken(googleIdToken)
                # if user doesnt exist in persistence, add
                foundUser = self.findUserByGoogleId(google_Id)
                if foundUser is None:
                    foundUser = User()
                    foundUser.setAttribute("jansExtUid", "passport-google:"+google_Id)
                    foundUser.setAttribute(self.getLocalPrimaryKey(),google_Id)

                    userService = CdiUtil.bean(UserService)
                    result = userService.addUser(foundUser, True)
                    foundUser = self.findUserByGoogleId(google_Id)

                logged_in = authenticationService.authenticate(foundUser.getUserId())
                return logged_in

            else:
                credentials = identity.getCredentials()
                user_name = credentials.getUsername()
                user_password = credentials.getPassword()

                logged_in = False
                if (StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password)):
                    logged_in = authenticationService.authenticate(user_name, user_password)

                return logged_in
        else:
            print "Google. Authenticate Error"
            return False

    def verifyIDToken(self, googleIdToken):
        verifier = GoogleIdTokenVerifier.Builder(NetHttpTransport(), JacksonFactory()).setAudience(Collections.singletonList(self.client_id)).build()
        # the GoogleIdTokenVerifier.verify() method verifies the JWT signature, the aud claim, the iss claim, and the exp claim.
        idToken = verifier.verify(googleIdToken)
        if idToken is not  None:
            payload = idToken.getPayload()
            userId = payload.getSubject()
            print "User ID: %s"  % userId

            #email = payload.getEmail()
            #emailVerified = Boolean.valueOf(payload.getEmailVerified())
            #name = str( payload.get("name"))
            #pictureUrl = str(payload.get("picture"))
            #locale = str( payload.get("locale"))
            #familyName = str( payload.get("family_name"))
            #givenName = str( payload.get("given_name"))
            return userId

        else :
            print "Invalid ID token."
            return None

    def findUserByGoogleId(self, googleId):
        userService = CdiUtil.bean(UserService)
        return userService.getUserByAttribute("jansExtUid", "passport-google:"+googleId)

    def getLocalPrimaryKey(self):
        entryManager = CdiUtil.bean(PersistenceEntryManager)
        config = GluuConfiguration()
        config = entryManager.find(config.getClass(), "ou=configuration,o=jans")
        #Pick (one) attribute where user id is stored (e.g. uid/mail)
        # primaryKey is the primary key on the backend AD / LDAP Server
        # localPrimaryKey is the primary key on Gluu. This attr value has been mapped with the primary key attr of the backend AD / LDAP when configuring cache refresh
        uid_attr = config.getIdpAuthn().get(0).getConfig().findValue("localPrimaryKey").asText()
        print "Casa. init. uid attribute is '%s'" % uid_attr
        return uid_attr

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        if (step == 1):
            print "Google. Prepare for Step 1"
            identity = CdiUtil.bean(Identity)
            identity.setWorkingParameter("gclient_id",self.client_id)
            return True
        else:
            return False

    def getExtraParametersForStep(self, configurationAttributes, step):
        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        return 1

    def getPageForStep(self, configurationAttributes, step):

        if(step == 1):
            return "/auth/google/login.xhtml"
        return ""

    def getNextStep(self, configurationAttributes, requestParameters, step):
        return -1

    def getLogoutExternalUrl(self, configurationAttributes, requestParameters):
        print "Get external logout URL call"
        return None

    def logout(self, configurationAttributes, requestParameters):
        return True
