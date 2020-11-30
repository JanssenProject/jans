# Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
# Copyright (c) 2020, Janssen Project
#
# Author: Yuriy Movchan, Chris Blanton
#

from io.jans.service.cdi.util import CdiUtil
from io.jans.as.server.security import Identity
from io.jans.model.custom.script.type.auth import PersonAuthenticationType
from io.jans.as.server.service import AuthenticationService
from io.jans.util import StringHelper
from io.jans.as.server.service import UserService
from io.jans.util.security import BCrypt

import java

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "BCrypt Auth. Initialization"
        print "BCrypt Auth. Initialized successfully"
        return True   

    def destroy(self, configurationAttributes):
        print "BCrypt Auth. Destroy"
        print "BCrypt Auth. Destroyed successfully"
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

        authenticationService = CdiUtil.bean(AuthenticationService)

        if (step == 1):
            print "BCrypt Auth. Authenticate for step 1"

            identity = CdiUtil.bean(Identity)
            credentials = identity.getCredentials()

            user_name = credentials.getUsername()
            user_password = credentials.getPassword()

            logged_in = False

            if (StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password)):
                userService = CdiUtil.bean(UserService)
                user = userService.getUser(user_name)
                hashed_stored_pass = user.getAttribute("userPassword")

                password_schema = ''

                # Determine password schema
                # Example for BCrypt: {BCRYPT}$2b$08$71gBXNKJ/iUBXqLjEdEXFesoUYQm5vrpKefi8YhV7ITGfAd9VNFaG
                for char in hashed_stored_pass:
                    if char == '{':
                        continue
                    if char == '}':
                        break    
                    password_schema = password_schema + char
                print("Password Schema is: " + password_schema)

                # OpenDJ's SSHA(512)
                if 'SSHA' in password_schema:
                    # Returns True if authenticated on the backend
                    logged_in = authenticationService.authenticate(user_name, user_password)

                # Pattern match BCRYPT and rewrite to SSHA
                elif 'BCRYPT' in password_schema:
                    # Pull salt from the stored hashed password
                    salt = hashed_stored_pass[8:]
                    salt = salt.split("$")[3].strip()
                    salt = salt[0:22]
                    salt = '$2a$08$' + salt

                    # Create BCrypt hash of challenge cleartext password using the gathered salt
                    challenge = BCrypt.hashpw(user_password,salt)

                    # Strip unnecessary revision($2a$) and rounds(08$) from both hashed passwords for comparison.
                    challenge = challenge.split("$")[3].strip()
                    stored = hashed_stored_pass.split("$")[3].strip()

                    print("Challenge Salt+Hash: " + challenge)
                    print("Stored Salt+Hash:    " + stored)

                    # Compare the hashses and update hash if there is a match.
                    if challenge in stored:

                        # Users hashed challenge password matches the stored hashed password in the backend
                        # Therefore we update the users password to the backend's password schema by passing it to OpenDJ
                        print("Updating hash..")
                        user.setAttribute("userPassword",user_password)
                        user = userService.updateUser(user)
                        print("Logging in..")

                        # Returns True
                        logged_in = authenticationService.authenticate(user_name)

                # Catch unknown schema types and output to oxauth_script.log
                # This script can be expanded to include other password schemas.
                else:
                    print("Unrecognized algorithm: " + password_schema)

            # If there is no match, logged_in will still be False and authentication will fail.
            if (not logged_in):
                return False
            logged_in = authenticationService.authenticate(user_name)
            return logged_in
        else:
            return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        if (step == 1):
            print "BCrypt Auth. Prepare for Step 1"
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
