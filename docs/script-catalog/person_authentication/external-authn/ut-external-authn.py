# UpdateToken External Authn

from io.jans.model.custom.script.type.token import UpdateTokenType
from io.jans.service.cache import CacheProvider
from io.jans.service.cdi.util import CdiUtil
from io.jans.util import StringHelper

class UpdateToken(UpdateTokenType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "UT External Authn. Initializing ..."
        print "UT External Authn. Initialized successfully"

        return True

    def destroy(self, configurationAttributes):
        print "UT External Authn. Destroying ..."
        print "UT External Authn. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 11

    def modifyIdToken(self, jsonWebResponse, context):
        
        # Retrieve jansKey from cache
        jansKey = context.getExecutionContext().getHttpRequest().getParameter("jansKey")
        if (jansKey == None or StringHelper.isEmpty(jansKey)):
            print "UT External Authn. ModifyIdToken Could not find jansKey in request"
            return False
        print "UT External Authn. ModifyIdToken jansKey '%s' found in request" % jansKey

        # Retrieve jsonValues from cache using jansKey
        cacheProvider = CdiUtil.bean(CacheProvider)
        jsonValues = cacheProvider.get(jansKey)
        if (jsonValues == None):
            print "UT External Authn. ModifyIdToken Could not find jansKey in cache"
            return False
        print "UT External Authn. ModifyIdToken jansKey found in cache"

        # Retrieve redirectUri from cache using jansKey
        jsonValCallbackUrl = jsonValues.get("callbackUrl")
        if (jsonValCallbackUrl == None):
            print "UT External Authn. ModifyIdToken Could not find callbackUrl in cache"
            return False
        print "UT External Authn. ModifyIdToken callbackUrl '%s' found in cache" % jsonValCallbackUrl

        # Decide where to set the callback_uri in header or payload
        jsonWebResponse.getHeader().setClaim("callback_url", jsonValCallbackUrl)
        jsonWebResponse.getClaims().setClaim("callback_url", jsonValCallbackUrl)

        # Remove jansKey from cache
        cacheProvider.remove(jansKey)
        print "UT External Authn. ModifyIdToken jansKey removed from cache"

        return True

    def modifyRefreshToken(self, refreshToken, context):
        return True

    def modifyAccessToken(self, accessToken, context):
        return True

    def getRefreshTokenLifetimeInSeconds(self, context):
        return 0

    def getIdTokenLifetimeInSeconds(self, context):
        return 0

    def getAccessTokenLifetimeInSeconds(self, context):
        return 0
