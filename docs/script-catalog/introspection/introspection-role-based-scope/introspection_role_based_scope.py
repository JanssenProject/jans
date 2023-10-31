# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2018, Janssen
#
# Author: Yuriy Zabrovarnyy, Arnab Dutta, Mustafa Baser
#
#
from io.jans.as.model.jwt import Jwt
from io.jans.service.cdi.util import CdiUtil
from io.jans.as.model.crypto import AuthCryptoProvider
from io.jans.orm import PersistenceEntryManager
from io.jans.model.custom.script.type.introspection import IntrospectionType
from io.jans.as.server.model.config import ConfigurationFactory
from io.jans.as.model.config.adminui import AdminConf
from io.jans.as.common.model.session import SessionId
from org.json import JSONObject
from java.lang import String

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
        scopes = []
        try:
            # Getting user-info-jwt
            ujwt = context.getHttpRequest().getParameter("ujwt")
            if not ujwt:
                print "UJWT is empty or null. Only the default scopes will be added to the token."
                entryManager = CdiUtil.bean(PersistenceEntryManager)

                adminConf = AdminConf()
                adminUIConfig = entryManager.find(adminConf.getClass(), "ou=admin-ui,ou=configuration,o=jans")
                permissions = adminUIConfig.getDynamic().getPermissions()

                for ele in permissions:
                    if ele.getDefaultPermissionInToken() is not None and ele.getDefaultPermissionInToken():
                        scopes.append(ele.getPermission())

                responseAsJsonObject.accumulate("scope", scopes)
                return True

            # Parse jwt
            userInfoJwt = Jwt.parse(ujwt)

            configObj = CdiUtil.bean(ConfigurationFactory)
            jwksObj = configObj.getWebKeysConfiguration()
            jwks = JSONObject(jwksObj)

            # Validate JWT
            authCryptoProvider = AuthCryptoProvider()
            validJwt = authCryptoProvider.verifySignature(userInfoJwt.getSigningInput(), userInfoJwt.getEncodedSignature(), userInfoJwt.getHeader().getKeyId(), jwks, None, userInfoJwt.getHeader().getSignatureAlgorithm())

            if validJwt == True:
                # Get claims from parsed JWT
                jwtClaims = userInfoJwt.getClaims()
                jansAdminUIRole = list(jwtClaims.getClaim("jansAdminUIRole"))
                # fetch role-scope mapping from database
                try:
                    entryManager = CdiUtil.bean(PersistenceEntryManager)
                    adminConf = AdminConf()
                    adminUIConfig = entryManager.find(adminConf.getClass(), "ou=admin-ui,ou=configuration,o=jans")
                    roleScopeMapping = adminUIConfig.getDynamic().getRolePermissionMapping()

                    for ele in roleScopeMapping:
                        if ele.getRole() in jansAdminUIRole:
                            for scope in ele.getPermissions():
                                if not scope in scopes:
                                    scopes.append(scope)
             
                    permissionTag = context.getHttpRequest().getParameter("permission_tag")
                    permissions = adminUIConfig.getDynamic().getPermissions()

                    if permissionTag is not None:
                        print "The request has tags : {}".format(permissionTag)
                        permissionTagArr = permissionTag.split()
                        scopesWithMatchingTags = self.filterScopesMatchingWithTags(permissionTagArr, permissions)
                        scopes = self.createScopeListMatchingWithTags(scopesWithMatchingTags, scopes)


                except Exception as e:
                    print "Error:  Failed to fetch/parse Admin UI roleScopeMapping from DB"
                    print e

                print "Following scopes will be added in api token: {}".format(scopes)

            responseAsJsonObject.accumulate("scope", scopes)
        except Exception as e:
                print "Exception occured. Unable to resolve role/scope mapping."
                print e
        return True

    def filterScopesMatchingWithTags(self, permissionTag, permissions):
        scopesWithMatchingTags = []
        for permissionObj in permissions:
            for tag in permissionTag:
                if permissionObj.getTag() == tag:
                    scopesWithMatchingTags.append(permissionObj.getPermission())
        return scopesWithMatchingTags


    def createScopeListMatchingWithTags(self, scopesWithMatchingTags, scopes):
        returnScopes = []
        for scopeWithMatchingTags in scopesWithMatchingTags:
            if scopeWithMatchingTags in scopes:
                returnScopes.append(scopeWithMatchingTags)
        return returnScopes
