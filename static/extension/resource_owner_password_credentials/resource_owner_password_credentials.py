from org.gluu.model.custom.script.type.owner import ResourceOwnerPasswordCredentialsType
from java.lang import String

class ResourceOwnerPasswordCredentials(ResourceOwnerPasswordCredentialsType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        print "ROPC script. Initializing ..."
        print "ROPC script. Initialized successfully"

        return True

    def destroy(self, configurationAttributes):
        print "ROPC script. Destroying ..."
        print "ROPC script. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 1

    # Returns boolean, true - authenticate user, false - ignore script and do not authenticate user.
    # This method is called after normal ROPC authentication. This method can cancel normal authentication if it returns false and sets `context.setUser(null)`.
    # Note :
    # context is reference of org.gluu.oxauth.service.external.context.ExternalResourceOwnerPasswordCredentialsContext#ExternalResourceOwnerPasswordCredentialsContext (in https://github.com/GluuFederation/oxauth project, )
    def authenticate(self, context):
        if (context.getHttpRequest().getParameterValues("device_id")[0] == "device_id_1"):
            return True
        return False