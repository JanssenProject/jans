# Introspection Script Guide

## Overview

Introspection scripts allows to modify response of Introspection Endpoint [spec](https://datatracker.ietf.org/doc/html/rfc7662).

## Interface

Introspection script should be associated with client (used for obtaining the token) in order to be run. Otherwise it's possible to set introspectionScriptBackwardCompatibility global AS configuration property to true, in this case AS will run all scripts (ignoring client configuration).

### Methods

The introspection interception script extends the base script type with the `init`, `destroy` and `getApiVersion` methods but also adds the following method(s):

|Method |`def modifyResponse(self, responseAsJsonObject, context)`|
|:-----|:------|
| Method Paramater| `responseAsJsonObject` is `org.codehaus.jettison.json.JSONObject`<br/> `context` is `io.jans.as.service.external.context.ExternalIntrospectionContext`|


### Snippet

    # Returns boolean, true - apply introspection method, false - ignore it.
    # This method is called after introspection response is ready. This method can modify introspection response.
    # Note :
    # responseAsJsonObject - is org.codehaus.jettison.json.JSONObject, you can use any method to manipulate json
    # context is reference of io.jans.as.service.external.context.ExternalIntrospectionContext
    def modifyResponse(self, responseAsJsonObject, context):
        responseAsJsonObject.accumulate("key_from_script", "value_from_script")
        return True
        

It is also possible to run introspection script during `access_token` creation as JWT. It can be controlled by `run_introspection_script_before_access_token_as_jwt_creation_and_include_claims` client property which is set to false by default.

If `run_introspection_script_before_access_token_as_jwt_creation_and_include_claims` set to true and `access_token_as_jwt` set to true then introspection script will be run before JWT (`access_token`) is created and all JSON values will be transfered to JWT. Also `context` inside script has additional method which allows to cancel transfering of claims if needed `context.setTranferIntrospectionPropertiesIntoJwtClaims(false)`
        
## Common Use Cases

### Role based Scopes

Description: Adding User Role based Scopes to the token.
Following sample code snippet can be used to add additional scopes to the token, based on user's role and roleScopeMapping from backend database.

    # Returns boolean, true - apply introspection method, false - ignore it.
    # This method is called after introspection response is ready. This method can modify introspection response.
    # Note :
    # responseAsJsonObject - is org.codehaus.jettison.json.JSONObject, you can use any method to manipulate json
    # context is reference of io.jans.as.service.external.context.ExternalIntrospectionContext
    def modifyResponse(self, responseAsJsonObject, context):
        print "Inside modifyResponse method of introspection script ...."
        try:
            # Getting user-info-jwt
            ujwt = context.getHttpRequest().getParameter("ujwt")
            print ujwt
            if not ujwt:
                print "UJWT is empty or null. Only the default scopes will be added to the token."
                entryManager = CdiUtil.bean(PersistenceEntryManager)
                adminConf = AdminConf()
                adminUIConfig = entryManager.find(adminConf.getClass(), "ou=admin-ui,ou=configuration,o=jans")
                permissions = adminUIConfig.getDynamic().getPermissions()
                scopes = []
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
                jansAdminUIRole = jwtClaims.getClaim("jansAdminUIRole")
                # fetch role-scope mapping from database
                scopes = None
                try:
                    entryManager = CdiUtil.bean(PersistenceEntryManager)
                    adminConf = AdminConf()
                    adminUIConfig = entryManager.find(adminConf.getClass(), "ou=admin-ui,ou=configuration,o=jans")
                    roleScopeMapping = adminUIConfig.getDynamic().getRolePermissionMapping()

                    for ele in roleScopeMapping:
                        if ele.getRole() == jansAdminUIRole.getString(0):
                            scopes = ele.getPermissions()
                except Exception as e:
                    print "Error:  Failed to fetch/parse Admin UI roleScopeMapping from DB"
                    print e

                print "Following scopes will be added in api token: {}".format(scopes)

            responseAsJsonObject.accumulate("scope", scopes)
        except Exception as e:
                print "Exception occured. Unable to resolve role/scope mapping."
                print e
        return True
