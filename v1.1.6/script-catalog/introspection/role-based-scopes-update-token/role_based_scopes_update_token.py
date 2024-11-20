# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2021, Gluu
#
# Author: Yuriy Movchan, Arnab Dutta
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
from com.google.common.collect import Sets
from io.jans.model.custom.script.type.token import UpdateTokenType

class UpdateToken(UpdateTokenType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Update token script. Initializing ..."
        print "Update token script. Initialized successfully"

        return True

    def destroy(self, configurationAttributes):
        print "Update token script. Destroying ..."
        print "Update token script. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 11

    # Returns boolean, true - indicates that script applied changes
    # This method is called after adding headers and claims. Hence script can override them
    # Note :
    # jsonWebResponse - is io.jans.as.model.token.JsonWebResponse, you can use any method to manipulate JWT
    # context is reference of io.jans.as.server.service.external.context.ExternalUpdateTokenContext (in https://github.com/JanssenProject/jans-auth-server project, )
    def modifyIdToken(self, jsonWebResponse, context):
        return True

    # Returns boolean, true - indicates that script applied changes. If false is returned token will not be created.
    # refreshToken is reference of io.jans.as.server.model.common.RefreshToken (note authorization grant can be taken as context.getGrant())
    # context is reference of io.jans.as.server.service.external.context.ExternalUpdateTokenContext (in https://github.com/JanssenProject/jans-auth-server project, )
    def modifyRefreshToken(self, refreshToken, context):
        return True

    # Returns boolean, true - indicates that script applied changes. If false is returned token will not be created.
    # accessToken is reference of io.jans.as.server.model.common.AccessToken (note authorization grant can be taken as context.getGrant())
    # context is reference of io.jans.as.server.service.external.context.ExternalUpdateTokenContext (in https://github.com/JanssenProject/jans-auth-server project, )
    def modifyAccessToken(self, accessToken, context):
        print "Inside modifyAccessToken method of update token script ...."
        try:
            if context.getGrant().getAuthorizationGrantType().toString() != 'client_credentials':
                return True
            scopes = Sets.newHashSet()
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
                        scopes.add(ele.getPermission())

                context.overwriteAccessTokenScopes(accessToken, scopes)
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
                                    scopes.add(scope)
            
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
            context.overwriteAccessTokenScopes(accessToken, scopes)
        except Exception as e:
                print "Exception occured. Unable to resolve role/scope mapping."
                print e
        return True

    # context is reference of io.jans.as.server.service.external.context.ExternalUpdateTokenContext (in https://github.com/JanssenProject/jans-auth-server project, )
    def getRefreshTokenLifetimeInSeconds(self, context):
        return 0

    # context is reference of io.jans.as.server.service.external.context.ExternalUpdateTokenContext (in https://github.com/JanssenProject/jans-auth-server project, )
    def getIdTokenLifetimeInSeconds(self, context):
        return 0

    # context is reference of io.jans.as.server.service.external.context.ExternalUpdateTokenContext (in https://github.com/JanssenProject/jans-auth-server project, )
    def getAccessTokenLifetimeInSeconds(self, context):
        return 0

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