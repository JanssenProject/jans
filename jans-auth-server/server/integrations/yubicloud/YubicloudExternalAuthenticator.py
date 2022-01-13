# Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
# Copyright (c) 2020, Janssen Project
#
# Author: Yuriy Movchan, Arunmozhi
#

from io.jans.service.cdi.util import CdiUtil
from io.jans.as.server.security import Identity
from io.jans.model.custom.script.type.auth import PersonAuthenticationType
from io.jans.as.server.service import UserService
from io.jans.util import StringHelper

import java

import urllib2
import urllib
import uuid


class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Yubicloud. Initialization"

        self.api_server = configurationAttributes.get("yubicloud_uri").getValue2()
        self.api_key = configurationAttributes.get("yubicloud_api_key").getValue2()
        self.client_id = configurationAttributes.get("yubicloud_id").getValue2()

        return True

    def destroy(self, configurationAttributes):
        print "Yubicloud. Destroyed successfully"
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
        if (step == 1):
            print "Yubicloud. Authenticate for step 1"

            identity = CdiUtil.bean(Identity)
            credentials = identity.getCredentials()

            username = credentials.getUsername()
            otp = credentials.getPassword()

            # Validate otp length
            if len(otp) < 32 or len(otp) > 48:
                print "Yubicloud. Invalid OTP length"
                return False

            user_service = CdiUtil.bean(UserService)
            user = user_service.getUser(username)

            public_key = user.getAttribute('yubikeyId')

            # Match the user with the yubikey
            if public_key not in otp:
                print "Yubicloud. Public Key not matching OTP"
                return False

            data = ""
            try:
                nonce = str(uuid.uuid4()).replace("-", "")
                params = urllib.urlencode({"id": self.client_id, "otp": otp, "nonce": nonce})
                url = "https://" + self.api_server + "/wsapi/2.0/verify/?" + params
                f = urllib2.urlopen(url)
                data = f.read()
            except Exception as e:
                print "Yubicloud. Exception ", e

            if 'status=OK' in data:
                user_service.authenticate(username)
                print "Yubicloud. Authentication Successful"
                return True

            print "Yubicloud. End of Step 1. Returning False."
            return False
        else:
            return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        if (step == 1):
            print "Yubicloud. Prepare for Step 1"
            return True
        else:
            return False

    def getExtraParametersForStep(self, configurationAttributes, step):
        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        return 1

    def getPageForStep(self, configurationAttributes, step):
        return ""

    def getNextStep(self, configurationAttributes, requestParameters, step):
        return -1

    def getLogoutExternalUrl(self, configurationAttributes, requestParameters):
        print "Get external logout URL call"
        return None

    def logout(self, configurationAttributes, requestParameters):
        return True
