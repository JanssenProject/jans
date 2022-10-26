from io.jans.as.persistence.model.configuration import GluuConfiguration
from io.jans.as.server.security import Identity
from io.jans.as.server.service import AuthenticationService
from io.jans.as.server.service import UserService
from io.jans.as.common.service.common import EncryptionService
from io.jans.as.server.service.custom import CustomScriptService
from io.jans.as.server.service.net import HttpService
from io.jans.as.server.util import ServerUtil
from io.jans.model import SimpleCustomProperty
from io.jans.model.custom.script import CustomScriptType
from io.jans.model.custom.script.type.auth import PersonAuthenticationType
from io.jans.orm import PersistenceEntryManager
from io.jans.service import CacheService
from io.jans.service.cdi.util import CdiUtil
from io.jans.util import StringHelper

from java.lang import Integer
from java.util import Collections, HashMap, HashSet, ArrayList, Arrays, Date
from java.nio.charset import Charset

from org.gluu.casa.model import ApplicationConfiguration
from org.apache.http.params import CoreConnectionPNames

try:
    import json
except ImportError:
    import simplejson as json
import sys

class PersonAuthentication(PersonAuthenticationType):

    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis
        self.ACR_SG = "super_gluu"

        self.modulePrefix = "casa-external_"

    def init(self, customScript, configurationAttributes):

        print "Casa. init called"
        self.authenticators = {}
        self.uid_attr = self.getLocalPrimaryKey()

        custScriptService = CdiUtil.bean(CustomScriptService)
        self.scriptsList = custScriptService.findCustomScripts(Collections.singletonList(CustomScriptType.PERSON_AUTHENTICATION), "jansConfProperty", "displayName", "jansEnabled", "jansLevel")
        dynamicMethods = self.computeMethods(self.scriptsList)

        if len(dynamicMethods) > 0:
            print "Casa. init. Loading scripts for dynamic modules: %s" % dynamicMethods

            for acr in dynamicMethods:
                moduleName = self.modulePrefix + acr
                try:
                    external = __import__(moduleName, globals(), locals(), ["PersonAuthentication"], -1)
                    module = external.PersonAuthentication(self.currentTimeMillis)

                    print "Casa. init. Got dynamic module for acr %s" % acr
                    configAttrs = self.getConfigurationAttributes(acr, self.scriptsList)

                    if acr == self.ACR_SG:
                        application_id = configurationAttributes.get("supergluu_app_id").getValue2()
                        configAttrs.put("application_id", SimpleCustomProperty("application_id", application_id))

                    if module.init(None, configAttrs):
                        module.configAttrs = configAttrs
                        self.authenticators[acr] = module
                    else:
                        print "Casa. init. Call to init in module '%s' returned False" % moduleName
                except:
                    print "Casa. init. Failed to load module %s" % moduleName
                    print "Exception: ", sys.exc_info()[1]

            mobile_methods = configurationAttributes.get("mobile_methods")
            self.mobile_methods = [] if mobile_methods == None else StringHelper.split(mobile_methods.getValue2(), ",")

        print "Casa. init. Initialized successfully"
        return True


    def destroy(self, configurationAttributes):
        print "Casa. Destroyed called"
        return True


    def getApiVersion(self):
        return 11


    def getAuthenticationMethodClaims(self, configurationAttributes):
        return None


    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        print "Casa. isValidAuthenticationMethod called"
        return True


    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None


    def authenticate(self, configurationAttributes, requestParameters, step):
        print "Casa. authenticate for step %s" % str(step)

        userService = CdiUtil.bean(UserService)
        authenticationService = CdiUtil.bean(AuthenticationService)
        identity = CdiUtil.bean(Identity)

        if step == 1:
            credentials = identity.getCredentials()
            user_name = credentials.getUsername()
            user_password = credentials.getPassword()

            if StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password):

                foundUser = userService.getUserByAttribute(self.uid_attr, user_name)
                #foundUser = userService.getUser(user_name)
                if foundUser == None:
                    print "Casa. authenticate for step 1. Unknown username"
                else:
                    platform_data = self.parsePlatformData(requestParameters)
                    preferred = foundUser.getAttribute("jansPreferredMethod")
                    mfaOff = preferred == None
                    logged_in = False

                    if mfaOff:
                        logged_in = authenticationService.authenticate(user_name, user_password)
                    else:
                        acr = self.getSuitableAcr(foundUser, platform_data, preferred)
                        if acr != None:
                            module = self.authenticators[acr]
                            logged_in = module.authenticate(module.configAttrs, requestParameters, step)

                    if logged_in:
                        foundUser = authenticationService.getAuthenticatedUser()

                        if foundUser == None:
                            print "Casa. authenticate for step 1. Cannot retrieve logged user"
                        else:
                            if mfaOff:
                                identity.setWorkingParameter("skip2FA", True)
                            else:
                                #Determine whether to skip 2FA based on policy defined (global or user custom)
                                skip2FA = self.determineSkip2FA(userService, identity, foundUser, platform_data)
                                identity.setWorkingParameter("skip2FA", skip2FA)
                                identity.setWorkingParameter("ACR", acr)

                            return True

                    else:
                        print "Casa. authenticate for step 1 was not successful"
            return False

        else:
            user = authenticationService.getAuthenticatedUser()
            if user == None:
                print "Casa. authenticate for step 2. Cannot retrieve logged user"
                return False

            #see casa.xhtml
            alter = ServerUtil.getFirstValue(requestParameters, "alternativeMethod")
            if alter != None:
                #bypass the rest of this step if an alternative method was provided. Current step will be retried (see getNextStep)
                self.simulateFirstStep(requestParameters, alter)
                return True

            session_attributes = identity.getSessionId().getSessionAttributes()
            acr = session_attributes.get("ACR")
            #this working parameter is used in casa.xhtml
            identity.setWorkingParameter("methods", ArrayList(self.getAvailMethodsUser(user, acr)))

            success = False
            if acr in self.authenticators:
                module = self.authenticators[acr]
                success = module.authenticate(module.configAttrs, requestParameters, step)

            #Update the list of trusted devices if 2fa passed
            if success:
                print "Casa. authenticate. 2FA authentication was successful"
                tdi = session_attributes.get("trustedDevicesInfo")
                if tdi == None:
                    print "Casa. authenticate. List of user's trusted devices was not updated"
                else:
                    user.setAttribute("jansTrustedDevices", tdi)
                    userService.updateUser(user)
            else:
                print "Casa. authenticate. 2FA authentication failed"

            return success

        return False


    def prepareForStep(self, configurationAttributes, requestParameters, step):
        print "Casa. prepareForStep %s" % str(step)
        identity = CdiUtil.bean(Identity)

        if step == 1:
            self.prepareUIParams(identity)
            return True
        else:
            session_attributes = identity.getSessionId().getSessionAttributes()

            authenticationService = CdiUtil.bean(AuthenticationService)
            user = authenticationService.getAuthenticatedUser()

            if user == None:
                print "Casa. prepareForStep. Cannot retrieve logged user"
                return False

            acr = session_attributes.get("ACR")
            print "Casa. prepareForStep. ACR = %s" % acr
            identity.setWorkingParameter("methods", ArrayList(self.getAvailMethodsUser(user, acr)))

            if acr in self.authenticators:
                module = self.authenticators[acr]
                return module.prepareForStep(module.configAttrs, requestParameters, step)
            else:
                return False


    def getExtraParametersForStep(self, configurationAttributes, step):
        print "Casa. getExtraParametersForStep %s" % str(step)
        list = ArrayList()

        if step > 1:
            acr = CdiUtil.bean(Identity).getWorkingParameter("ACR")

            if acr in self.authenticators:
                module = self.authenticators[acr]
                params = module.getExtraParametersForStep(module.configAttrs, step)
                if params != None:
                    list.addAll(params)

            list.addAll(Arrays.asList("ACR", "methods", "trustedDevicesInfo"))

        list.addAll(Arrays.asList("casa_contextPath", "casa_prefix", "casa_faviconUrl", "casa_extraCss", "casa_logoUrl"))
        print "extras are %s" % list
        return list


    def getCountAuthenticationSteps(self, configurationAttributes):
        print "Casa. getCountAuthenticationSteps called"

        if CdiUtil.bean(Identity).getWorkingParameter("skip2FA"):
           return 1

        acr = CdiUtil.bean(Identity).getWorkingParameter("ACR")
        if acr in self.authenticators:
            module = self.authenticators[acr]
            return module.getCountAuthenticationSteps(module.configAttrs)
        else:
            return 2

        print "Casa. getCountAuthenticationSteps. Could not determine the step count for acr %s" % acr


    def getPageForStep(self, configurationAttributes, step):
        print "Casa. getPageForStep called %s" % str(step)

        if step > 1:
            acr = CdiUtil.bean(Identity).getWorkingParameter("ACR")
            if acr in self.authenticators:
                module = self.authenticators[acr]
                page = module.getPageForStep(module.configAttrs, step)
            else:
                page=None

            return page

        return "/casa/login.xhtml"


    def getNextStep(self, configurationAttributes, requestParameters, step):

        print "Casa. getNextStep called %s" % str(step)
        if step > 1:
            acr = ServerUtil.getFirstValue(requestParameters, "alternativeMethod")
            if acr != None:
                print "Casa. getNextStep. Use alternative method %s" % acr
                CdiUtil.bean(Identity).setWorkingParameter("ACR", acr)
                #retry step with different acr
                return 2

        return -1


    def logout(self, configurationAttributes, requestParameters):
        print "Casa. logout called"
        return True

# Miscelaneous

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


    def getSettings(self):
        entryManager = CdiUtil.bean(PersistenceEntryManager)
        config = ApplicationConfiguration()
        config = entryManager.find(config.getClass(), "ou=casa,ou=configuration,o=jans")
        settings = config.getSettings()
        
        if settings == None:
            print "Casa. getSettings. Failed to parse casa settings from DB"
        return settings


    def computeMethods(self, scriptList):

        methods = []
        mapping = {}
        cmConfigs = self.getSettings()

        if cmConfigs != None and cmConfigs.getAcrPluginMap() != None:
            mapping = cmConfigs.getAcrPluginMap().keySet()

        for m in mapping:
            for customScript in scriptList:
                if customScript.getName() == m and customScript.isEnabled():
                    methods.append(m)

        print "Casa. computeMethods. %s" % methods
        return methods


    def getConfigurationAttributes(self, acr, scriptsList):

        configMap = HashMap()
        for customScript in scriptsList:
            if customScript.getName() == acr and customScript.isEnabled():
                for prop in customScript.getConfigurationProperties():
                    configMap.put(prop.getValue1(), SimpleCustomProperty(prop.getValue1(), prop.getValue2()))

        print "Casa. getConfigurationAttributes. %d configuration properties were found for %s" % (configMap.size(), acr)
        return configMap


    def getAvailMethodsUser(self, user, skip=None):
        methods = HashSet()

        for method in self.authenticators:
            try:
                module = self.authenticators[method]
                if module.hasEnrollments(module.configAttrs, user):
                    methods.add(method)
            except:
                print "Casa. getAvailMethodsUser. hasEnrollments call could not be issued for %s module" % method
                print "Exception: ", sys.exc_info()[1]

        try:
            if skip != None:
                # skip is guaranteed to be a member of methods (if hasEnrollments routines are properly implemented).
                # A call to remove strangely crashes when skip is absent
                methods.remove(skip)
        except:
            print "Casa. getAvailMethodsUser. methods list does not contain %s" % skip

        print "Casa. getAvailMethodsUser %s" % methods.toString()
        return methods


    def prepareUIParams(self, identity):
        
        print "Casa. prepareUIParams. Reading UI branding params"
        cacheService = CdiUtil.bean(CacheService)
        casaAssets = cacheService.get("casa_assets")
            
        if casaAssets == None:
            #This may happen when cache type is IN_MEMORY, where actual cache is merely a local variable 
            #(a expiring map) living inside Casa webapp, not oxAuth webapp
            
            sets = self.getSettings()
            
            custPrefix = "/custom"
            logoUrl = "/images/logo.png"
            faviconUrl = "/images/favicon.ico"
            if (sets.getExtraCssSnippet() != None) or sets.isUseExternalBranding():
                logoUrl = custPrefix + logoUrl
                faviconUrl = custPrefix + faviconUrl
            
            prefix = custPrefix if sets.isUseExternalBranding() else ""
            
            casaAssets = {
                "contextPath": "/casa",
                "prefix" : prefix,
                "faviconUrl" : faviconUrl,
                "extraCss": sets.getExtraCssSnippet(),
                "logoUrl": logoUrl
            }
        
        #Setting a single variable with the whole map does not work...
        identity.setWorkingParameter("casa_contextPath", casaAssets['contextPath'])
        identity.setWorkingParameter("casa_prefix", casaAssets['prefix'])
        identity.setWorkingParameter("casa_faviconUrl", casaAssets['contextPath'] + casaAssets['faviconUrl'])
        identity.setWorkingParameter("casa_extraCss", casaAssets['extraCss'])
        identity.setWorkingParameter("casa_logoUrl", casaAssets['contextPath'] + casaAssets['logoUrl'])


    def simulateFirstStep(self, requestParameters, acr):
        #To simulate 1st step, there is no need to call:
        # getPageforstep (no need as user/pwd won't be shown again)
        # isValidAuthenticationMethod (by restriction, it returns True)
        # prepareForStep (by restriction, it returns True)
        # getExtraParametersForStep (by restriction, it returns None)
        print "Casa. simulateFirstStep. Calling authenticate (step 1) for %s module" % acr
        if acr in self.authenticators:
            module = self.authenticators[acr]
            auth = module.authenticate(module.configAttrs, requestParameters, 1)
            print "Casa. simulateFirstStep. returned value was %s" % auth


# 2FA policy enforcement

    def parsePlatformData(self, requestParameters):
        try:
            #Find device info passed in HTTP request params (see index.xhtml)
            platform = ServerUtil.getFirstValue(requestParameters, "loginForm:platform")
            deviceInf = json.loads(platform)
        except:
            print "Casa. parsePlatformData. Error parsing platform data"
            deviceInf = None

        return deviceInf


    def getSuitableAcr(self, user, deviceInf, preferred):

        onMobile = deviceInf != None and 'isMobile' in deviceInf and deviceInf['isMobile']
        id = user.getUserId()
        strongest = -1
        acr = None
        user_methods = self.getAvailMethodsUser(user)

        for s in self.scriptsList:
            name = s.getName()
            level = Integer.MAX_VALUE if name == preferred else s.getLevel()
            if user_methods.contains(name) and level > strongest and (not onMobile or name in self.mobile_methods):
                acr = name
                strongest = level

        print "Casa. getSuitableAcr. On mobile = %s" % onMobile
        if acr == None and onMobile:
            print "Casa. getSuitableAcr. No mobile-friendly authentication method available for user %s" % id
            # user_methods is not empty when this function is called, so just pick any
            acr = user_methods.stream().findFirst().get()

        print "Casa. getSuitableAcr. %s was selected for user %s" % (acr, id)
        return acr


    def determineSkip2FA(self, userService, identity, foundUser, deviceInf):

        cmConfigs = self.getSettings()

        if cmConfigs == None:
            print "Casa. determineSkip2FA. Failed to read policy_2fa"
            return False

        cmConfigs = cmConfigs.getPluginSettings().get('strong-authn-settings')

        policy2FA = 'EVERY_LOGIN'
        if cmConfigs != None and cmConfigs.get('policy_2fa') != None:
            policy2FA = ','.join(cmConfigs.get('policy_2fa'))

        print "Casa. determineSkip2FA with general policy %s" % policy2FA
        policy2FA += ','
        skip2FA = False

        if 'CUSTOM,' in policy2FA:
            #read setting from user profile
            policy = foundUser.getAttribute("jansStrongAuthPolicy")
            if policy == None:
                policy = 'EVERY_LOGIN,'
            else:
                policy = policy.upper() + ','
            print "Casa. determineSkip2FA. Using user's enforcement policy %s" % policy

        else:
            #If it's not custom, then apply the global setting admin defined
            policy = policy2FA

        if not 'EVERY_LOGIN,' in policy:
            locationCriterion = 'LOCATION_UNKNOWN,' in policy
            deviceCriterion = 'DEVICE_UNKNOWN,' in policy

            if locationCriterion or deviceCriterion:
                if deviceInf == None:
                    print "Casa. determineSkip2FA. No user device data. Forcing 2FA to take place..."
                else:
                    skip2FA = self.process2FAPolicy(identity, foundUser, deviceInf, locationCriterion, deviceCriterion)

                    if skip2FA:
                        print "Casa. determineSkip2FA. Second factor is skipped"
                        #Update attribute if authentication will not have second step
                        devInf = identity.getWorkingParameter("trustedDevicesInfo")
                        if devInf != None:
                            foundUser.setAttribute("jansTrustedDevices", devInf)
                            userService.updateUser(foundUser)
            else:
                print "Casa. determineSkip2FA. Unknown %s policy: cannot skip 2FA" % policy

        return skip2FA


    def process2FAPolicy(self, identity, foundUser, deviceInf, locationCriterion, deviceCriterion):

        skip2FA = False
        #Retrieve user's devices info
        devicesInfo = foundUser.getAttribute("jansTrustedDevices")

        #do geolocation
        geodata = self.getGeolocation(identity)
        if geodata == None:
            print "Casa. process2FAPolicy: Geolocation data not obtained. 2FA skipping based on location cannot take place"

        try:
            encService = CdiUtil.bean(EncryptionService)

            if devicesInfo == None:
                print "Casa. process2FAPolicy: There are no trusted devices for user yet"
                #Simulate empty list
                devicesInfo = "[]"
            else:
                devicesInfo = encService.decrypt(devicesInfo)

            devicesInfo = json.loads(devicesInfo)

            partialMatch = False
            idx = 0
            #Try to find a match for device only
            for device in devicesInfo:
                partialMatch = device['browser']['name']==deviceInf['name'] and device['os']['version']==deviceInf['os']['version'] and device['os']['family']==deviceInf['os']['family']
                if partialMatch:
                    break
                idx+=1

            matchFound = False

            #At least one of locationCriterion or deviceCriterion is True
            if locationCriterion and not deviceCriterion:
                #this check makes sense if there is city data only
                if geodata!=None:
                    for device in devicesInfo:
                        #Search all registered cities that are found in trusted devices
                        for origin in device['origins']:
                            matchFound = matchFound or origin['city']==geodata['city']

            elif partialMatch:
                #In this branch deviceCriterion is True
                if not locationCriterion:
                    matchFound = True
                elif geodata!=None:
                    for origin in devicesInfo[idx]['origins']:
                        matchFound = matchFound or origin['city']==geodata['city']

            skip2FA = matchFound
            now = Date().getTime()

            #Update attribute oxTrustedDevicesInfo accordingly
            if partialMatch:
                #Update an existing record (update timestamp in city, or else add it)
                if geodata != None:
                    partialMatch = False
                    idxCity = 0

                    for origin in devicesInfo[idx]['origins']:
                        partialMatch = origin['city']==geodata['city']
                        if partialMatch:
                            break;
                        idxCity+=1

                    if partialMatch:
                        devicesInfo[idx]['origins'][idxCity]['timestamp'] = now
                    else:
                        devicesInfo[idx]['origins'].append({"city": geodata['city'], "country": geodata['country'], "timestamp": now})
            else:
                #Create a new entry
                browser = {"name": deviceInf['name'], "version": deviceInf['version']}
                os = {"family": deviceInf['os']['family'], "version": deviceInf['os']['version']}

                if geodata == None:
                    origins = []
                else:
                    origins = [{"city": geodata['city'], "country": geodata['country'], "timestamp": now}]

                obj = {"browser": browser, "os": os, "addedOn": now, "origins": origins}
                devicesInfo.append(obj)

            enc = json.dumps(devicesInfo, separators=(',',':'))
            enc = encService.encrypt(enc)
            identity.setWorkingParameter("trustedDevicesInfo", enc)

        except:
            print "Casa. process2FAPolicy. Error!", sys.exc_info()[1]

        return skip2FA


    def getGeolocation(self, identity):

        session_attributes = identity.getSessionId().getSessionAttributes()
        if session_attributes.containsKey("remote_ip"):
            remote_ip = session_attributes.get("remote_ip").split(",", 2)[0].strip()
            if StringHelper.isNotEmpty(remote_ip):

                httpService = CdiUtil.bean(HttpService)

                http_client = httpService.getHttpsClient()
                http_client_params = http_client.getParams()
                http_client_params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 4 * 1000)

                geolocation_service_url = "http://ip-api.com/json/%s?fields=country,city,status,message" % remote_ip
                geolocation_service_headers = { "Accept" : "application/json" }

                try:
                    http_service_response = httpService.executeGet(http_client, geolocation_service_url, geolocation_service_headers)
                    http_response = http_service_response.getHttpResponse()
                except:
                    print "Casa. Determine remote location. Exception: ", sys.exc_info()[1]
                    return None

                try:
                    if not httpService.isResponseStastusCodeOk(http_response):
                        print "Casa. Determine remote location. Get non 200 OK response from server:", str(http_response.getStatusLine().getStatusCode())
                        httpService.consume(http_response)
                        return None

                    response_bytes = httpService.getResponseContent(http_response)
                    response_string = httpService.convertEntityToString(response_bytes, Charset.forName("UTF-8"))
                    httpService.consume(http_response)
                finally:
                    http_service_response.closeConnection()

                if response_string == None:
                    print "Casa. Determine remote location. Get empty response from location server"
                    return None

                response = json.loads(response_string)

                if not StringHelper.equalsIgnoreCase(response['status'], "success"):
                    print "Casa. Determine remote location. Get response with status: '%s'" % response['status']
                    return None

                return response

        return None

        
    def getLogoutExternalUrl(self, configurationAttributes, requestParameters):
        print "Get external logout URL call"
        return None
