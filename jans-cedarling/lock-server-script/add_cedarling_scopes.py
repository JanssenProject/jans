from io.jans.model.custom.script.type.client import ClientRegistrationType
from io.jans.service.cdi.util import CdiUtil
from io.jans.orm.util import ArrayHelper
from io.jans.as.server.service import ScopeService
from io.jans.as.model.util import JwtUtil

class ClientRegistration(ClientRegistrationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Cedarling Client registration. Initialization"
        
        if (not configurationAttributes.containsKey("jwks_uri")):
            print "Cedarling Client registration. Initialization failed. Property jwks_uri is not specified"
            return False
        else:
            self.jwks_uri = configurationAttributes.get("jwks_uri").getValue2()

        if (not configurationAttributes.containsKey("scope_list")):
            print "Cedarling Client registration. Initialization failed. Property scope_list is not specified"
            return False
        else:
            self.scope_list = configurationAttributes.get("scope_list").getValue2().split(" ")

        if (not configurationAttributes.containsKey("trigger_scope")):
            print "Cedarling Client registration. Initialization failed. Property trigger_scope is not specified"
            return False
        else:
            self.trigger_scope = configurationAttributes.get("trigger_scope").getValue2()
        
        # used to check if the scopes we're adding exists in the AS
        self.scopeService = CdiUtil.bean(ScopeService)

        print "Cedarling Client registration. Initialized successfully"
        return True   

    def destroy(self, configurationAttributes):
        print "Cedarling Client registration. Destroy"
        print "Cedarling Client registration. Destroyed successfully"
        return True   

    def createClient(self, context):
        print "Cedarling Client registration. createClient"

        # Check if the request has an SSA
        registerRequest = context.getRegisterRequest()
        ssa = registerRequest.getSoftwareStatement()
        if ssa == "":
            print "Cedarling Client registration. No SSA provided, defaulting"
            return True

        # Check if the request has a trigger_scope (this is set as 'cedarling' by default) in the request.
        # The trigger_scope is used to determine if we should add the scopes needed by Cedarling to the
        # registering client.
        # request_scopes = ArrayHelper.toString(registerRequest.getScope())
        request_scopes = registerRequest.getScope()
        if self.trigger_scope not in request_scopes:
            print "Cedarling Client registration. the scope '%s' was not included in the request, defaulting" % self.trigger_scope
            return True

        # add cedarling scopes
        client = context.getClient()
        scopes = client.getScopes()
        for scope in self.scope_list:
            foundScope = self.scopeService.getScopeById(scope)
            if foundScope is None:
                print "did not find scope '%s' in the AS" % scope
                return False
            if len(scopes) == 0:
                scopes = [foundScope.getDn()]
            else:
                scopes = ArrayHelper.addItemToStringArray(scopes, foundScope.getDn())

        client.setScopes(scopes)

        print "Cedarling Client registration. added Cedarling scopes"
        return True

    def updateClient(self, context):
        print "Cedarling Client registration. UpdateClient method"
        pass

    def getApiVersion(self):
        return 11

    def getSoftwareStatementHmacSecret(self, context):
        return ""

    def getSoftwareStatementJwks(self, context):
        print "SSA Cedarling Client registration. getting jwks from '%s'" %  self.jwks_uri
        jwks = JwtUtil.getJSONWebKeys(self.jwks_uri)
        if jwks is None:
            print "SSA Cedarling Client registration. jwks not found"
        return jwks.toString()

    def modifyPutResponse(self, responseAsJsonObject, executionContext):
        return False

    def modifyReadResponse(self, responseAsJsonObject, executionContext):
        return False

    def modifyPostResponse(self, responseAsJsonObject, executionContext):
        return False
