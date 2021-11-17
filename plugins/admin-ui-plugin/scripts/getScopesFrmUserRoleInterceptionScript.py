# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2018, Janssen
#
# Author: Yuriy Zabrovarnyy
#
#
from io.jans.as.model.jwt import Jwt
from io.jans.service.cdi.util import CdiUtil
from io.jans.as.model.crypto import AuthCryptoProvider
from io.jans.orm import PersistenceEntryManager
from io.jans.model.custom.script.type.introspection import IntrospectionType
from io.jans.as.model.config.adminui import Configuration
from java.net import HttpURLConnection, URL
from org.json import JSONArray, JSONObject
from java.lang import String
from java.io import BufferedReader, InputStreamReader
from java.lang import System
try:
    import json
except ImportError:
    import simplejson as json


class Introspection(IntrospectionType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Introspection script. Initializing ..."
        print "Introspection script. Initialized successfully"

        return True

    def destroy(self, configurationAttributes):
        print "Introspection script. Destroying ..."
        print "Introspection script. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 11

    # Returns boolean, true - apply introspection method, false - ignore it.
    # This method is called after introspection response is ready. This method can modify introspection response.
    # Note :
    # responseAsJsonObject - is org.codehaus.jettison.json.JSONObject, you can use any method to manipulate json
    # context is reference of io.jans.as.service.external.context.ExternalIntrospectionContext (in https://github.com/JanssenFederation/oxauth project, )
    def modifyResponse(self, responseAsJsonObject, context):
        print "Inside modifyResponse method of introspection script ...."
        try:
            # Getting user-info-jwt
            ujwt = context.getHttpRequest().getParameter("ujwt")
            print ujwt
            print "---jwks_url---"
            jwks_url = System.getenv("ADMIN_UI_JWKS")
            print jwks_url
            if not ujwt:
                print "UJWT is empty or null"
            # Parse jwt
            userInfoJwt = Jwt.parse(ujwt)
            # Get auth-server keys
            url = URL("https://admin-ui-test.gluu.org/jans-auth/restv1/jwks")
            conn = url.openConnection()
            conn.setDoOutput(True)
            conn.setRequestMethod("GET")
            conn.setRequestProperty("Content-type", "application/json")
            if conn.getResponseCode() != 200:
                print "Failed!!"
                print conn.getResponseCode()
                print conn.getResponseMessage()
            else:
                print "Success!! Able to connect for auth-server jwks"
                print conn.getResponseCode()
                print conn.getResponseMessage()

            instr = conn.getInputStream()
            instrreader = InputStreamReader(instr)
            breader = BufferedReader(instrreader)
            output = breader.readLine()
            jsonResult = ""
            while output != None:
                if output != None:
                    jsonResult += output
                output = breader.readLine()
            # JWKS
            jwks = JSONObject(jsonResult)
            conn.disconnect()

            # Validate JWT
            authCryptoProvider = AuthCryptoProvider()
            validJwt = authCryptoProvider.verifySignature(userInfoJwt.getSigningInput(), userInfoJwt.getEncodedSignature(), userInfoJwt.getHeader().getKeyId(), jwks, None, userInfoJwt.getHeader().getSignatureAlgorithm())
            print validJwt

            if validJwt == True:
                print "user-info jwt is valid"
                # Get claims from parsed JWT
                jwtClaims = userInfoJwt.getClaims()
                jansAdminUIRole = jwtClaims.getClaim("jansAdminUIRole")
                print jansAdminUIRole.getString(0)
                # fetch role-scope mapping from database
                scopes = None
                try:
                    entryManager = CdiUtil.bean(PersistenceEntryManager)
                    adminUIConfig = Configuration()
                    adminUIConfig = entryManager.find(adminUIConfig.getClass(), "ou=admin-ui,ou=configuration,o=jans")

                    roleScopeMapping = adminUIConfig.getDynamic().getRoleScopeMapping()
                    for ele in roleScopeMapping:
                        if ele.getRole() == jansAdminUIRole.getString(0):
                            scopes = ele.getScopes()
                except:
                    print "Admin UI: roleScopeMapping. Failed to fetch/parse Admin UI roleScopeMapping from DB"

                print scopes

            responseAsJsonObject.accumulate("scope", scopes)
        except Exception as e:
                print "Exception occured. Unable to resolve role/scope mapping."
                print e
        return True

