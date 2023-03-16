# Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
# Copyright (c) 2020, Janssen Project
#
# Author: Yuriy Movchan
#

from io.jans.service.cdi.util import CdiUtil
from io.jans.as.server.security import Identity
from io.jans.model.custom.script.type.auth import PersonAuthenticationType
from io.jans.as.server.service import UserService, AuthenticationService, AppInitializer
from io.jans.util import StringHelper
from java.util import Arrays, HashMap

import java
import json

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript,  configurationAttributes):
        print "Basic (client group). Initialization"
        
        self.allow_default_login = False
        if configurationAttributes.containsKey("allow_default_login"):
            self.allow_default_login = StringHelper.toBoolean(configurationAttributes.get("allow_default_login").getValue2(), False)

        if not configurationAttributes.containsKey("configuration_file"):
            print "Basic (client group). The property configuration_file is empty"
            return False
            
        configurationFilePath = configurationAttributes.get("configuration_file").getValue2()
        self.client_configurations = self.loadClientConfigurations(configurationFilePath)
        if self.client_configurations == None:
            print "Basic (client group). File with client configuration should be not empty"
            return False

        print "Basic (client group). Initialized successfully"
        return True     

    def destroy(self, clientConfiguration):
        print "Basic (client group). Destroy"

        print "Basic (client group). Destroyed successfully"
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

        identity = CdiUtil.bean(Identity)
        credentials = identity.getCredentials()
        session_attributes = identity.getSessionId().getSessionAttributes()

        client_id = session_attributes.get("client_id")
        print "Basic (client group). Get client_id: '%s' authorization request" % client_id

        user_groups = self.client_configurations.get(client_id)
        if user_groups == None:
            print "Basic (client group). There is no user groups configuration for client_id '%s'. allow_default_login: %s" % (client_id, self.allow_default_login)
            if not self.allow_default_login:
                return False

            result = self.authenticateImpl(credentials, authenticationService)
            return result

        is_member_client_groups = self.isUserMemberOfGroups(credentials, user_groups)
        if not is_member_client_groups:
            print "Basic (client group). User '%s' hasn't permissions to log into client_id '%s' application. " % (credentials.getUsername(), client_id)
            return False

        result = self.authenticateImpl(credentials, authenticationService)
        return result

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        if step == 1:
            print "Basic (client group). Prepare for Step 1"
            return True
        else:
            return False

    def getExtraParametersForStep(self, configurationAttributes, step):
        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        return 1

    def getPageForStep(self, configurationAttributes, step):
        return ""

    def logout(self, configurationAttributes, requestParameters):
        return True

    def authenticateImpl(self, credentials, authenticationService):
        print "Basic (client group). Processing user name/password authentication"

        user_name = credentials.getUsername()
        user_password = credentials.getPassword()

        logged_in = False
        if StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password):
            logged_in = authenticationService.authenticate(user_name, user_password)

        if not logged_in:
            return False

        return True

    def loadClientConfigurations(self, configurationFile):
        clientConfiguration = None

        # Load configuration from file
        f = open(configurationFile, 'r')
        try:
            configurationFileJson = json.loads(f.read())
        except:
            print "Basic (client group). Load configuration from file. Failed to load authentication configuration from file:", configurationFile
            return None
        finally:
            f.close()

        clientConfigurations = HashMap()
        for client_key in configurationFileJson.keys():
            client_config = configurationFileJson[client_key]

            client_inum = client_config["client_inum"]
            user_groups_array = client_config["user_group"]
            user_groups = Arrays.asList(user_groups_array)
            clientConfigurations.put(client_inum, user_groups)

        print "Basic (client group). Load configuration from file. Loaded '%s' configurations" % clientConfigurations.size()
        print clientConfigurations
        
        return clientConfigurations

    def isUserMemberOfGroups(self, credentials, groups):
        userService = CdiUtil.bean(UserService)

        user_name = credentials.getUsername()
        if StringHelper.isEmptyString(user_name):
            return False

        find_user_by_uid = userService.getUser(user_name)

        is_member = False
        member_of_list = find_user_by_uid.getAttributeValues("memberOf")
        if member_of_list == None:
            return is_member
        
        print member_of_list
        print groups

        for member_of in member_of_list:
            for group in groups:
                if StringHelper.equalsIgnoreCase(group, member_of) or member_of.endswith(group):
                    is_member = True
                    break

        return is_member

    def getNextStep(self, configurationAttributes, requestParameters, step):
        return -1

    def getLogoutExternalUrl(self, configurationAttributes, requestParameters):
        print "Get external logout URL call"
        return None