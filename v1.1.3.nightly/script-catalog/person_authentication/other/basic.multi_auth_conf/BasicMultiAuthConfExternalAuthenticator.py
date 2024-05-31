# Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
# Copyright (c) 2020, Janssen Project
#
# Author: Yuriy Movchan
#

from io.jans.service.cdi.util import CdiUtil
from io.jans.as.server.security import Identity
from io.jans.model.custom.script.type.auth import PersonAuthenticationType
from io.jans.as.server.service import UserService, AuthenticationService, AppInitializer
from io.jans.as.server.service import MetricService
from io.jans.as.server.service.common import EncryptionService
from io.jans.model.metric import MetricType
from io.jans.util import StringHelper
from io.jans.util import ArrayHelper
from io.jans.orm.service import PersistanceFactoryService
from io.jans.orm.ldap.impl import LdapEntryManagerFactory
from io.jans.model.ldap import GluuLdapConfiguration
from java.util import Arrays, Properties

import java
import json

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Basic (multi auth conf). Initialization"

        if (not configurationAttributes.containsKey("auth_configuration_file")):
            print "Basic (multi auth conf). The property auth_configuration_file is empty"
            return False
            
        authConfigurationFile = configurationAttributes.get("auth_configuration_file").getValue2()
        authConfiguration = self.loadAuthConfiguration(authConfigurationFile)
        if (authConfiguration == None):
            print "Basic (multi auth conf). File with authentication configuration should be not empty"
            return False
        
        validationResult = self.validateAuthConfiguration(authConfiguration)
        if (not validationResult):
            return False

        ldapExtendedEntryManagers = self.createLdapExtendedEntryManagers(authConfiguration)
        if (ldapExtendedEntryManagers == None):
            return False
        
        self.ldapExtendedEntryManagers = ldapExtendedEntryManagers

        print "Basic (multi auth conf). Initialized successfully"
        return True     

    def destroy(self, authConfiguration):
        print "Basic (multi auth conf). Destroy"
        
        result = True
        for ldapExtendedEntryManager in self.ldapExtendedEntryManagers:
            ldapConfiguration = ldapExtendedEntryManager["ldapConfiguration"]
            ldapEntryManager = ldapExtendedEntryManager["ldapEntryManager"]

            destoryResult = ldapEntryManager.destroy()
            result = result and destoryResult
            print "Basic (multi auth conf). Destroyed: " + ldapConfiguration.getConfigId() + ". Result: " + str(destoryResult) 

        print "Basic (multi auth conf). Destroyed successfully"

        return result

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
            print "Basic (multi auth conf). Authenticate for step 1"

            identity = CdiUtil.bean(Identity)
            credentials = identity.getCredentials()

            metricService = CdiUtil.bean(MetricService)
            timerContext = metricService.getTimer(MetricType.USER_AUTHENTICATION_RATE).time()
            try:
                keyValue = credentials.getUsername()
                userPassword = credentials.getPassword()
    
                if (StringHelper.isNotEmptyString(keyValue) and StringHelper.isNotEmptyString(userPassword)):
                    for ldapExtendedEntryManager in self.ldapExtendedEntryManagers:
                        ldapConfiguration = ldapExtendedEntryManager["ldapConfiguration"]
                        ldapEntryManager = ldapExtendedEntryManager["ldapEntryManager"]
                        loginAttributes = ldapExtendedEntryManager["loginAttributes"]
                        localLoginAttributes = ldapExtendedEntryManager["localLoginAttributes"]
    
                        print "Basic (multi auth conf). Authenticate for step 1. Using configuration: " + ldapConfiguration.getConfigId()
    
                        idx = 0
                        count = len(loginAttributes)
                        while (idx < count):
                            primaryKey = loginAttributes[idx]
                            localPrimaryKey = localLoginAttributes[idx]
    
                            loggedIn = authenticationService.authenticate(ldapConfiguration, ldapEntryManager, keyValue, userPassword, primaryKey, localPrimaryKey)
                            if (loggedIn):
                                metricService.incCounter(MetricType.USER_AUTHENTICATION_SUCCESS)
                                return True
                            idx += 1
            finally:
                timerContext.stop()
                
            metricService.incCounter(MetricType.USER_AUTHENTICATION_FAILURES)

            return False
        else:
            return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        if (step == 1):
            print "Basic (multi auth conf). Prepare for Step 1"
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

    def loadAuthConfiguration(self, authConfigurationFile):
        authConfiguration = None

        # Load authentication configuration from file
        f = open(authConfigurationFile, 'r')
        try:
            authConfiguration = json.loads(f.read())
        except:
            print "Basic (multi auth conf). Load auth configuration. Failed to load authentication configuration from file:", authConfigurationFile
            return None
        finally:
            f.close()
        
        return authConfiguration

    def validateAuthConfiguration(self, authConfiguration):
        isValid = True

        if (not ("ldap_configuration" in authConfiguration)):
            print "Basic (multi auth conf). Validate auth configuration. There is no ldap_configuration section in configuration"
            return False
        
        idx = 1
        for ldapConfiguration in authConfiguration["ldap_configuration"]:
            if (not self.containsAttributeString(ldapConfiguration, "configId")):
                print "Basic (multi auth conf). Validate auth configuration. There is no 'configId' attribute in ldap_configuration section #" + str(idx)
                return False

            configId = ldapConfiguration["configId"]

            if (not self.containsAttributeArray(ldapConfiguration, "servers")):
                print "Basic (multi auth conf). Validate auth configuration. Property 'servers' in configuration '" + configId + "' is invalid"
                return False

            if (self.containsAttributeString(ldapConfiguration, "bindDN")):
                if (not self.containsAttributeString(ldapConfiguration, "bindPassword")):
                    print "Basic (multi auth conf). Validate auth configuration. Property 'bindPassword' in configuration '" + configId + "' is invalid"
                    return False

            if (not self.containsAttributeString(ldapConfiguration, "useSSL")):
                print "Basic (multi auth conf). Validate auth configuration. Property 'useSSL' in configuration '" + configId + "' is invalid"
                return False

            if (not self.containsAttributeString(ldapConfiguration, "maxConnections")):
                print "Basic (multi auth conf). Validate auth configuration. Property 'maxConnections' in configuration '" + configId + "' is invalid"
                return False
                
            if (not self.containsAttributeArray(ldapConfiguration, "baseDNs")):
                print "Basic (multi auth conf). Validate auth configuration. Property 'baseDNs' in configuration '" + configId + "' is invalid"
                return False

            if (not self.containsAttributeArray(ldapConfiguration, "loginAttributes")):
                print "Basic (multi auth conf). Validate auth configuration. Property 'loginAttributes' in configuration '" + configId + "' is invalid"
                return False

            if (not self.containsAttributeArray(ldapConfiguration, "localLoginAttributes")):
                print "Basic (multi auth conf). Validate auth configuration. Property 'localLoginAttributes' in configuration '" + configId + "' is invalid"
                return False

            if (len(ldapConfiguration["loginAttributes"]) != len(ldapConfiguration["localLoginAttributes"])):
                print "Basic (multi auth conf). Validate auth configuration. The number of attributes in 'loginAttributes' and 'localLoginAttributes' isn't equal in configuration '" + configId + "'"
                return False

            idx += 1

        return True

    def createLdapExtendedEntryManagers(self, authConfiguration):
        ldapExtendedConfigurations = self.createLdapExtendedConfigurations(authConfiguration)
        
        appInitializer = CdiUtil.bean(AppInitializer)
        persistanceFactoryService = CdiUtil.bean(PersistanceFactoryService)
        ldapEntryManagerFactory = persistanceFactoryService.getPersistenceEntryManagerFactory(LdapEntryManagerFactory)
        persistenceType = ldapEntryManagerFactory.getPersistenceType()

        ldapExtendedEntryManagers = []
        for ldapExtendedConfiguration in ldapExtendedConfigurations:
            connectionConfiguration = ldapExtendedConfiguration["connectionConfiguration"]
            ldapConfiguration = ldapExtendedConfiguration["ldapConfiguration"]

            ldapProperties = Properties()
            for key, value in connectionConfiguration.items():
                value_string = value
                if isinstance(value_string, list):
                    value_string = ", ".join(value)
                else:
                    value_string = str(value)

                ldapProperties.setProperty(persistenceType + "." + key, value_string)

            if StringHelper.isNotEmptyString(ldapConfiguration.getBindPassword()):
                ldapProperties.setProperty(persistenceType + ".bindPassword", ldapConfiguration.getBindPassword())

            ldapEntryManager = ldapEntryManagerFactory.createEntryManager(ldapProperties)

            ldapExtendedEntryManagers.append({ "ldapConfiguration" : ldapConfiguration, "ldapProperties" : ldapProperties, "loginAttributes" : ldapExtendedConfiguration["loginAttributes"], "localLoginAttributes" : ldapExtendedConfiguration["localLoginAttributes"], "ldapEntryManager" : ldapEntryManager })
        
        return ldapExtendedEntryManagers

    def createLdapExtendedConfigurations(self, authConfiguration):
        ldapExtendedConfigurations = []

        for connectionConfiguration in authConfiguration["ldap_configuration"]:
            configId = connectionConfiguration["configId"]
            
            servers = connectionConfiguration["servers"]

            bindDN = None
            bindPassword = None
            useAnonymousBind = True
            if (self.containsAttributeString(connectionConfiguration, "bindDN")):
                useAnonymousBind = False
                bindDN = connectionConfiguration["bindDN"]
                bindPassword = CdiUtil.bean(EncryptionService).decrypt(connectionConfiguration["bindPassword"])

            useSSL = connectionConfiguration["useSSL"]
            maxConnections = connectionConfiguration["maxConnections"]
            baseDNs = connectionConfiguration["baseDNs"]
            loginAttributes = connectionConfiguration["loginAttributes"]
            localLoginAttributes = connectionConfiguration["localLoginAttributes"]
            
            ldapConfiguration = GluuLdapConfiguration()
            ldapConfiguration.setConfigId(configId)
            ldapConfiguration.setBindDN(bindDN)
            ldapConfiguration.setBindPassword(bindPassword)
            ldapConfiguration.setServers(Arrays.asList(servers))
            ldapConfiguration.setMaxConnections(maxConnections)
            ldapConfiguration.setUseSSL(useSSL)
            ldapConfiguration.setBaseDNs(Arrays.asList(baseDNs))
            ldapConfiguration.setPrimaryKey(loginAttributes[0])
            ldapConfiguration.setLocalPrimaryKey(localLoginAttributes[0])
            ldapConfiguration.setUseAnonymousBind(useAnonymousBind)

            ldapExtendedConfigurations.append({ "ldapConfiguration" : ldapConfiguration, "connectionConfiguration" : connectionConfiguration, "loginAttributes" : loginAttributes, "localLoginAttributes" : localLoginAttributes })
        
        return ldapExtendedConfigurations

    def containsAttributeString(self, dictionary, attribute):
        return ((attribute in dictionary) and StringHelper.isNotEmptyString(dictionary[attribute]))

    def containsAttributeArray(self, dictionary, attribute):
        return ((attribute in dictionary) and (len(dictionary[attribute]) > 0))
