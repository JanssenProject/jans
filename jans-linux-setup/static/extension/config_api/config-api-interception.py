# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2018, Janssen
#
# Author: Puja Sharma
#
#

from io.jans.as.model.jwt import Jwt
from io.jans.service.cdi.util import CdiUtil
from io.jans.as.model.crypto import AuthCryptoProvider
from io.jans.orm import PersistenceEntryManager
from io.jans.model.custom.script.conf import CustomScriptConfiguration
from io.jans.model.custom.script.type.configapi import ConfigApiType
from io.jans.configapi.model.configuration import ApiAppConfiguration
from org.json import JSONObject
from java.lang import String
from javax.servlet.http import HttpServletRequest;
from javax.servlet.http import HttpServletResponse;


class ConfigAuthentication(ConfigApiType):
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
    def authorize(self, context):
        print 'Config Authentication process'
        request = context.httpRequest;
        response = context.httpResponse;
        print " request = : %s" % request
        print " response = : %s" % response

        appConfiguration = context.getApiAppConfiguration();
        customScriptConfiguration = context.getScript();
        token = context.getToken();
        issuer = context.getIssuer();
        method = context.getMethod();
        path = context.getPath();
      
        print " requese2: %s" % request;
        print " response2 new: %s" % response;
        print "ConfigApiType.appConfiguration: %s" % appConfiguration;
        print "ConfigApiType.customScriptConfiguration: %s" % customScriptConfiguration;
        print "ConfigApiType.token: %s" % token;
        print "ConfigApiType.issuer: %s" % issuer;
        print "ConfigApiType.method: %s" % method;
        print "ConfigApiType.path: %s" % path;
		
        return True;
		#TODO validation

