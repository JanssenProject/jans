# PersonAuthentication External Authn

from io.jans.model.custom.script.type.auth import PersonAuthenticationType
from io.jans.service.cdi.util import CdiUtil
from io.jans.as.server.security import Identity
from io.jans.as.server.service import AuthenticationService
from io.jans.util import StringHelper
from io.jans.as.server.util import ServerUtil
from io.jans.as.server.service import SessionIdService
from io.jans.as.server.service import CookieService
from io.jans.service.cache import CacheProvider
from jakarta.faces.context import ExternalContext
from java.util import HashMap
from io.jans.as.server.service import UserService, RequestParameterService
from io.jans.as.server.service.net import HttpService
from jakarta.faces.context import FacesContext
from io.jans.jsf2.service import FacesService

import java
import uuid

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript,  configurationAttributes):
        print "PA External Authn. Initialization"
        print "PA External Authn. Initialized successfully"

        self.url_step1 = None

        # Get Custom Properties
        try:
            self.url_step1 = configurationAttributes.get("urlstep1").getValue2()
            print "PA External Authn. Initialization. url_step1: '%s'" % self.url_step1
        except:
            print 'Missing required configuration attribute "urlstep1"'

        return True

    def destroy(self, configurationAttributes):
        print "PA External Authn. Destroy"
        print "PA External Authn. Destroyed successfully"
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
        print "PA External Authn. Authenticate for step: %s" % step
        return True

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        if (step == 1):
            return True
        else:
            return False

    def getExtraParametersForStep(self, configurationAttributes, step):
        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        return 1

    def getPageForStep(self, configurationAttributes, step):
        print "PA External Authn. GetPageForStep for step: %s" % step

        externalContext = CdiUtil.bean(ExternalContext)
        sessionId = ServerUtil.getFirstValue(externalContext.getRequestParameterValuesMap(), "session_id")
        if (sessionId == None):

            # Remove session id cookie
            cookieService = CdiUtil.bean(CookieService)
            cookieService.removeSessionIdCookie(externalContext.getResponse())
            print "PA External Authn. GetPageForStep removed session id cookie"

            # Retrieve sessionId from request parameters and validate it
            redirectUri = ServerUtil.getFirstValue(externalContext.getRequestParameterValuesMap(), "redirect_uri")
            if (redirectUri == None or StringHelper.isEmpty(redirectUri)):
                print "PA External Authn. GetPageForStep redirect_uri is null or empty"
                return ""
            print "PA External Authn. GetPageForStep redirect_uri '%s' found in request parameters" % redirectUri

            # Generate jansKey
            jansKey = str(uuid.uuid4())
            print "PA External Authn. GetPageForStep jansKey '%s' generated" % jansKey

            # Create JSON Values
            jsonValues = {}
            jsonValues["redirectUri"] = str(redirectUri)

            cacheProvider = CdiUtil.bean(CacheProvider)
            cacheProvider.put(300, jansKey, jsonValues)
            print "PA External Authn. GetPageForStep jansKey '%s' added to cache: %s" % (jansKey, jsonValues)

            requestParameterService = CdiUtil.bean(RequestParameterService)
            parametersMap = HashMap()
            parametersMap.put("jansKey", jansKey)
            callBackUrl = requestParameterService.parametersAsString(parametersMap)
            callBackUrl = "%s?%s" % (self.url_step1, callBackUrl)

            print "PA External Authn. GetPageForStep redirect to %s" % callBackUrl

            facesService = CdiUtil.bean(FacesService)
            facesService.redirectToExternalURL(callBackUrl)

            return ""

        return "postlogin.xhtml"

    def getNextStep(self, configurationAttributes, requestParameters, step):
        return -1

    def getLogoutExternalUrl(self, configurationAttributes, requestParameters):
        return None

    def logout(self, configurationAttributes, requestParameters):
        return True
