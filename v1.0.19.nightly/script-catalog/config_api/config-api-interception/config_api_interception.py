# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2018, Janssen
#
# Author: Puja Sharma
#
#

from io.jans.as.model.jwt import Jwt
from io.jans.as.model.crypto import AuthCryptoProvider
from io.jans.model.custom.script.conf import CustomScriptConfiguration
from io.jans.model.custom.script.type.configapi import ConfigApiType
from io.jans.orm import PersistenceEntryManager
from io.jans.service.cdi.util import CdiUtil
from io.jans.util import StringHelper, ArrayHelper
from io.jans.configapi.model.configuration import ApiAppConfiguration

from org.json import JSONObject
from java.lang import String
from jakarta.servlet.http import HttpServletRequest
from jakarta.servlet.http import HttpServletResponse


class ConfigApiAuthorization(ConfigApiType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        print "ConfigApiType script. Initializing ..."
        print "ConfigApiType script. Initialized successfully"
        return True

    def destroy(self, configurationAttributes):
        print "ConfigApiType script. Destroying ..."
        print "ConfigApiType script. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 1


    # Returns boolean true or false depending on the process, if the client is authorized
    # or not.
    # This method is called after introspection response is ready. This method can modify introspection response.
    # Note :
    # responseAsJsonObject - is org.codehaus.jettison.json.JSONObject, you can use any method to manipulate json
    # context is reference of io.jans.as.service.external.context.ExternalIntrospectionContext (in https://github.com/JanssenFederation/oxauth project, )
    def authorize(self, responseAsJsonObject, context):
        print " responseAsJsonObject: %s" % responseAsJsonObject
        print " context: %s" % context

        print "Config Authentication process"
        request = context.httpRequest
        response = context.httpResponse
        print " request = : %s" % request
        print " response = : %s" % response

        appConfiguration = context.getApiAppConfiguration()
        customScriptConfiguration = context.getScript()
        issuer = context.getRequestParameters().get("ISSUER")
        token =  context.getRequestParameters().get("TOKEN")
        method = context.getRequestParameters().get("METHOD")
        path = context.getRequestParameters().get("PATH")
      
        print " requese2: %s" % request
        print " response2 new: %s" % response
        print "ConfigApiType.appConfiguration: %s" % appConfiguration
        print "ConfigApiType.customScriptConfiguration: %s" % customScriptConfiguration
        print "ConfigApiType.issuer: %s" % issuer
        print "ConfigApiType.token: %s" % token
        print "ConfigApiType.method: %s" % method
        print "ConfigApiType.path: %s" % path

        #Example to validate method
        if ("GET" == StringHelper.toUpperCase(method) ):
          print "Validate method: %s" % method
        
        if ("attributes" == StringHelper.toLowerCase(path) ):
          print "ConfigApiType.path: %s" % path
  
        responseAsJsonObject.accumulate("key_from_script", "value_from_script")
        print " final responseAsJsonObject: %s" % responseAsJsonObject

        return True




