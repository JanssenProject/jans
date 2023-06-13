# Janssen Project software is available under the Apache 2.0 License (2004). See http://www.apache.org/licenses/ for full text.
# Copyright (c) 2020, Janssen Project
#
# Author: Madhumita Subramaniam
#

from io.jans.as.server.util import ServerUtil
from io.jans.service.cdi.util import CdiUtil
from io.jans.model.security import Identity
from io.jans.as.common.model.common import User
from jakarta.faces.context import FacesContext
from io.jans.as.server.service.net import HttpService
from io.jans.model.custom.script.type.auth import PersonAuthenticationType
from io.jans.as.server.service import AuthenticationService, UserService
from io.jans.orm import PersistenceEntryManager
from io.jans.as.persistence.model.configuration import GluuConfiguration
from io.jans.util import StringHelper
from java.math import BigInteger
from java.security import SecureRandom
import java
import sys
import json
from java.util import UUID
from io.jans.as.model.jws import ECDSASigner;
from io.jans.as.model.jws import RSASigner;
from io.jans.as.model.jwt import Jwt;
from io.jans.as.model.crypto.signature import ECDSAPublicKey;
from io.jans.as.model.crypto.signature import RSAPublicKey;
from io.jans.as.client import JwkClient;
from io.jans.as.model.crypto.signature import AlgorithmFamily;
from java.util import Collections, HashMap, HashSet, ArrayList, Arrays, Date

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript,  configurationAttributes):
        print "Apple Initialization"
        if (not configurationAttributes.containsKey("apple_client_id")):
            print "Apple. Initialization. Property apple_client_id is not specified"
            return False
        else:
            self.apple_client_id = configurationAttributes.get("apple_client_id").getValue2()

        if (not configurationAttributes.containsKey("apple_jwks")):
            print "Apple. Initialization. Property apple_jwks is not specified"
            return False
        else:
            self.apple_jwks = configurationAttributes.get("apple_jwks").getValue2()
        print "Apple Initialized successfully"
        return True

    def destroy(self, configurationAttributes):
        print "Apple Destroy"
        print "Apple Destroyed successfully"
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
            print "Apple Authenticate for step 1"
            identity = CdiUtil.bean(Identity)
            print requestParameters

            id_token = ServerUtil.getFirstValue(requestParameters, "id_token")
            if id_token is not None:

                apple_id = self.verifyIDToken(id_token)
                print apple_id
                # if user doesnt exist in persistence, add
                foundUser = self.findUserByAppleId(apple_id)
                print foundUser
                if foundUser is None:
                    foundUser = User()
                    foundUser.setAttribute("jansExtUid", "passport-apple:"+apple_id)
                    foundUser.setAttribute(self.getLocalPrimaryKey(),apple_id)

                    userService = CdiUtil.bean(UserService)
                    result = userService.addUser(foundUser, True)
                    foundUser = self.findUserByAppleId(apple_id)


                logged_in = authenticationService.authenticate(apple_id)
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
            print "Apple Authenticate Error"
            return False

    def verifyIDToken(self, appleIdToken):

        if appleIdToken is not  None:
            jwt = Jwt.parse(appleIdToken)
            algorithm = jwt.getHeader().getSignatureAlgorithm()
            keyId = jwt.getHeader().getKeyId();
            userId = jwt.getClaims().getClaimAsString("sub")
            print userId
            jwksURI = self.apple_jwks;

            validSignature = False;
            if (algorithm.getFamily() == AlgorithmFamily.RSA) :
                publicKey = JwkClient.getRSAPublicKey(jwksURI, keyId);
                rsaSigner = RSASigner(algorithm, publicKey);
                validSignature = rsaSigner.validate(jwt);
            elif (algorithm.getFamily() == AlgorithmFamily.EC) :
                publicKey = JwkClient.getECDSAPublicKey(jwksURI, keyId);
                ecdsaSigner = ECDSASigner(algorithm, publicKey);
                validSignature = ecdsaSigner.validate(jwt);

            if(validSignature is True):
                return userId
            else:
                print "Apple. Failed to validate signature of ID_token"
                return None

        else :
            print "Invalid ID token."
            return None

    def findUserByAppleId(self, apple_id):
        userService = CdiUtil.bean(UserService)
        return userService.getUserByAttribute("jansExtUid", "passport-apple:"+apple_id)

    def getLocalPrimaryKey(self):
        entryManager = CdiUtil.bean(PersistenceEntryManager)
        config = GluuConfiguration()
        config = entryManager.find(config.getClass(), "ou=configuration,o=jans")
        #Pick (one) attribute where user id is stored (e.g. uid/mail)
        # primaryKey is the primary key on the backend AD / LDAP Server
        # localPrimaryKey is the primary key on Janssen. This attr value has been mapped with the primary key attr of the backend AD / LDAP when configuring cache refresh
        uid_attr = config.getIdpAuthn().get(0).getConfig().findValue("localPrimaryKey").asText()
        print "Casa. init. uid attribute is '%s'" % uid_attr
        return

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        if (step == 1):
            print "Apple Prepare for Step 1"
            identity = CdiUtil.bean(Identity)
            identity.setWorkingParameter("aclient_id",self.apple_client_id)
            identity.setWorkingParameter("astate", UUID.randomUUID().toString())
            identity.setWorkingParameter("anonce", UUID.randomUUID().toString())

            facesContext = CdiUtil.bean(FacesContext)
            request = facesContext.getExternalContext().getRequest()
            httpService = CdiUtil.bean(HttpService)
            url = httpService.constructServerUrl(request) + "/postlogin.htm"

            identity.setWorkingParameter("aredirectURI",url)
            return True
        else:
            return False

    def getExtraParametersForStep(self, configurationAttributes, step):
        list = ArrayList()
        list.addAll(Arrays.asList("apple_client_id","astate","anonce"))
        return list

    def getCountAuthenticationSteps(self, configurationAttributes):
        return 1

    def getPageForStep(self, configurationAttributes, step):

        if(step == 1):
            return "/auth/apple/login.xhtml"
        return ""

    def getNextStep(self, configurationAttributes, requestParameters, step):
        return -1

    def getLogoutExternalUrl(self, configurationAttributes, requestParameters):
        print "Get external logout URL call"
        return None

    def logout(self, configurationAttributes, requestParameters):
        return True
