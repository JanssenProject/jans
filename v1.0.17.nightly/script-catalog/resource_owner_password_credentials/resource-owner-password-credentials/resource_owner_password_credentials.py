from io.jans.model.custom.script.type.owner import ResourceOwnerPasswordCredentialsType
from io.jans.as.server.service import AuthenticationService
from io.jans.service.cdi.util import CdiUtil
from java.lang import String

class ResourceOwnerPasswordCredentials(ResourceOwnerPasswordCredentialsType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "ROPC script. Initializing ..."

        self.usernameParamName = "username"
        self.passwordParamName = "password"

        print "ROPC script. Initialized successfully"

        return True

    def destroy(self, configurationAttributes):
        print "ROPC script. Destroying ..."
        print "ROPC script. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 11

    # Returns True and set user into context when user authenticated succesfully
    # Returns False when user not authenticated or it's needed to cancel notmal flow
    # Note :
    # context is reference of io.jans.as.service.external.context.ExternalResourceOwnerPasswordCredentialsContext#ExternalResourceOwnerPasswordCredentialsContext (in https://github.com/JanssenFederation/oxauth project, )
    def authenticate(self, context):
        print "ROPC script. Authenticate"
        deviceIdParam = context.getHttpRequest().getParameterValues("device_id")
        if deviceIdParam != None and (deviceIdParam.lenght > 0 ):
            result = deviceIdParam[0] == "device_id_1"
            if not result:
                return False

            # Set auntenticated user in context
            # context.setUser(user)
            return True

        # Do generic authentication in other cases
        authService = CdiUtil.bean(AuthenticationService)

        username = context.getHttpRequest().getParameter(self.usernameParamName)
        password = context.getHttpRequest().getParameter(self.passwordParamName)
        result = authService.authenticate(username, password)
        if not result:
            print "ROPC script. Authenticate. Could not authenticate user '%s' " % username
            return False

        context.setUser(authService.getAuthenticatedUser())

        return True
