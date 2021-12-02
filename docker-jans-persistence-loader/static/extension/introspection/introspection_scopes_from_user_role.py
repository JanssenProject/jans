# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2018, Janssen
#
# Author: Yuriy Zabrovarnyy
#
#
from io.jans.as.model.jwt import Jwt
from io.jans.service.cdi.util import CdiUtil
from io.jans.as.model.crypto import AuthCryptoProvider
from io.jans.model.custom.script.type.introspection import IntrospectionType
from java.net import HttpURLConnection, URL
from org.json import JSONArray, JSONObject
from java.lang import String
from java.io import BufferedReader, InputStreamReader
from java.lang import System

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
            jwks_url = System.getenv("ADMIN_UI_JWKS")
            print jwks_url
            if not ujwt:
                print "UJWT is empty or null"
            # Parse jwt
            userInfoJwt = Jwt.parse(ujwt)
            # Get auth-server keys
            url = URL(jwks_url)
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
                # role-scope mapping
                scope = []
                if jansAdminUIRole.getString(0) == 'api-viewer':
                    scope.append("https://jans.io/oauth/config/attributes.readonly")
                    scope.append("https://jans.io/oauth/config/acrs.readonly")
                    scope.append("https://jans.io/oauth/config/scopes.readonly")
                    scope.append("https://jans.io/oauth/config/scripts.readonly")
                    scope.append("https://jans.io/oauth/config/openid/clients.readonly")
                    scope.append("https://jans.io/oauth/config/smtp.readonly")
                    scope.append("https://jans.io/oauth/config/logging.readonly")
                    scope.append("https://jans.io/oauth/config/uma/resources.readonly")
                    scope.append("https://jans.io/oauth/config/database/ldap.readonly")
                    scope.append("https://jans.io/oauth/config/jwks.readonly")
                    scope.append("https://jans.io/oauth/config/fido2.readonly")
                    scope.append("https://jans.io/oauth/config/cache.readonly")
                    scope.append("https://jans.io/oauth/jans-auth-server/config/properties.readonly")
                    scope.append("https://jans.io/oauth/config/database/couchbase.readonly")
                elif jansAdminUIRole.getString(0) == 'api-editor':
                    scope.append("https://jans.io/oauth/config/attributes.readonly")
                    scope.append("https://jans.io/oauth/config/attributes.write")
                    scope.append("https://jans.io/oauth/config/acrs.readonly")
                    scope.append("https://jans.io/oauth/config/acrs.write")
                    scope.append("https://jans.io/oauth/config/scopes.readonly")
                    scope.append("https://jans.io/oauth/config/scopes.write")
                    scope.append("https://jans.io/oauth/config/scripts.readonly")
                    scope.append("https://jans.io/oauth/config/scripts.write")
                    scope.append("https://jans.io/oauth/config/openid/clients.readonly")
                    scope.append("https://jans.io/oauth/config/openid/clients.write")
                    scope.append("https://jans.io/oauth/config/smtp.readonly")
                    scope.append("https://jans.io/oauth/config/smtp.write")
                    scope.append("https://jans.io/oauth/config/logging.readonly")
                    scope.append("https://jans.io/oauth/config/logging.write")
                    scope.append("https://jans.io/oauth/config/uma/resources.readonly")
                    scope.append("https://jans.io/oauth/config/uma/resources.write")
                    scope.append("https://jans.io/oauth/config/database/ldap.readonly")
                    scope.append("https://jans.io/oauth/config/database/ldap.write")
                    scope.append("https://jans.io/oauth/config/jwks.readonly")
                    scope.append("https://jans.io/oauth/config/jwks.write")
                    scope.append("https://jans.io/oauth/config/fido2.readonly")
                    scope.append("https://jans.io/oauth/config/fido2.write")
                    scope.append("https://jans.io/oauth/config/cache.readonly")
                    scope.append("https://jans.io/oauth/config/cache.write")
                    scope.append("https://jans.io/oauth/config/database/couchbase.readonly")
                    scope.append("https://jans.io/oauth/config/database/couchbase.write")
                    scope.append("https://jans.io/oauth/jans-auth-server/config/properties.readonly")
                elif jansAdminUIRole.getString(0) == 'api-manager':
                    scope.append("https://jans.io/oauth/config/attributes.readonly")
                    scope.append("https://jans.io/oauth/config/attributes.write")
                    scope.append("https://jans.io/oauth/config/attributes.delete")
                    scope.append("https://jans.io/oauth/config/acrs.readonly")
                    scope.append("https://jans.io/oauth/config/acrs.write")
                    scope.append("https://jans.io/oauth/config/acrs.delete")
                    scope.append("https://jans.io/oauth/config/scopes.readonly")
                    scope.append("https://jans.io/oauth/config/scopes.write")
                    scope.append("https://jans.io/oauth/config/scopes.delete")
                    scope.append("https://jans.io/oauth/config/scripts.readonly")
                    scope.append("https://jans.io/oauth/config/scripts.write")
                    scope.append("https://jans.io/oauth/config/scripts.delete")
                    scope.append("https://jans.io/oauth/config/openid/clients.readonly")
                    scope.append("https://jans.io/oauth/config/openid/clients.write")
                    scope.append("https://jans.io/oauth/config/openid/clients.delete")
                    scope.append("https://jans.io/oauth/config/smtp.readonly")
                    scope.append("https://jans.io/oauth/config/smtp.write")
                    scope.append("https://jans.io/oauth/config/smtp.delete")
                    scope.append("https://jans.io/oauth/config/logging.readonly")
                    scope.append("https://jans.io/oauth/config/logging.write")
                    scope.append("https://jans.io/oauth/config/uma/resources.readonly")
                    scope.append("https://jans.io/oauth/config/uma/resources.write")
                    scope.append("https://jans.io/oauth/config/uma/resources.delete")
                    scope.append("https://jans.io/oauth/config/database/ldap.readonly")
                    scope.append("https://jans.io/oauth/config/database/ldap.write")
                    scope.append("https://jans.io/oauth/config/database/ldap.delete")
                    scope.append("https://jans.io/oauth/config/jwks.readonly")
                    scope.append("https://jans.io/oauth/config/jwks.write")
                    scope.append("https://jans.io/oauth/config/fido2.readonly")
                    scope.append("https://jans.io/oauth/config/fido2.write")
                    scope.append("https://jans.io/oauth/config/cache.readonly")
                    scope.append("https://jans.io/oauth/config/cache.write")
                    scope.append("https://jans.io/oauth/config/database/couchbase.readonly")
                    scope.append("https://jans.io/oauth/config/database/couchbase.write")
                    scope.append("https://jans.io/oauth/jans-auth-server/config/properties.readonly")
                elif jansAdminUIRole.getString(0) == 'api-admin':
                    scope.append("https://jans.io/oauth/config/attributes.readonly")
                    scope.append("https://jans.io/oauth/config/attributes.write")
                    scope.append("https://jans.io/oauth/config/attributes.delete")
                    scope.append("https://jans.io/oauth/config/acrs.readonly")
                    scope.append("https://jans.io/oauth/config/acrs.write")
                    scope.append("https://jans.io/oauth/config/acrs.delete")
                    scope.append("https://jans.io/oauth/config/scopes.readonly")
                    scope.append("https://jans.io/oauth/config/scopes.write")
                    scope.append("https://jans.io/oauth/config/scopes.delete")
                    scope.append("https://jans.io/oauth/config/scripts.readonly")
                    scope.append("https://jans.io/oauth/config/scripts.write")
                    scope.append("https://jans.io/oauth/config/scripts.delete")
                    scope.append("https://jans.io/oauth/config/openid/clients.readonly")
                    scope.append("https://jans.io/oauth/config/openid/clients.write")
                    scope.append("https://jans.io/oauth/config/openid/clients.delete")
                    scope.append("https://jans.io/oauth/config/smtp.readonly")
                    scope.append("https://jans.io/oauth/config/smtp.write")
                    scope.append("https://jans.io/oauth/config/smtp.delete")
                    scope.append("https://jans.io/oauth/config/logging.readonly")
                    scope.append("https://jans.io/oauth/config/logging.write")
                    scope.append("https://jans.io/oauth/config/uma/resources.readonly")
                    scope.append("https://jans.io/oauth/config/uma/resources.write")
                    scope.append("https://jans.io/oauth/config/uma/resources.delete")
                    scope.append("https://jans.io/oauth/config/database/ldap.readonly")
                    scope.append("https://jans.io/oauth/config/database/ldap.write")
                    scope.append("https://jans.io/oauth/config/database/ldap.delete")
                    scope.append("https://jans.io/oauth/config/jwks.readonly")
                    scope.append("https://jans.io/oauth/config/jwks.write")
                    scope.append("https://jans.io/oauth/config/fido2.readonly")
                    scope.append("https://jans.io/oauth/config/fido2.write")
                    scope.append("https://jans.io/oauth/config/cache.readonly")
                    scope.append("https://jans.io/oauth/config/cache.write")
                    scope.append("https://jans.io/oauth/config/database/couchbase.readonly")
                    scope.append("https://jans.io/oauth/config/database/couchbase.write")
                    scope.append("https://jans.io/oauth/jans-auth-server/config/properties.write")
                    scope.append("https://jans.io/oauth/jans-auth-server/config/properties.readonly")
            responseAsJsonObject.accumulate("scope", scope)
        except Exception as e:
                print "Exception occured. Unable to resolve role/scope mapping."
                print e
        return True
