from io.jans.model.custom.script.type.ssa import ModifySsaResponseType

class ModifySsaResponse(ModifySsaResponseType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Modify ssa response script. Initializing ..."
        print "Modify ssa response script. Initialized successfully"

        return True

    def destroy(self, configurationAttributes):
        print "Modify ssa response script. Destroying ..."
        print "Modify ssa response script. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 11

    def create(self, jsonWebResponse, context):
        print "Modify ssa response script. Modify idToken: %s" % jsonWebResponse

        jsonWebResponse.getHeader().setClaim("custom_header_name", "custom_header_value")
        jsonWebResponse.getClaims().setClaim("custom_claim_name", "custom_claim_value")

        print "Modify ssa response script. After modify idToken: %s" % jsonWebResponse
        return True

    def get(self, jsonArray, context):
        print "Modify ssa response script. Modify get ssa list: %s" % jsonArray
        return True

    def revoke(self, ssaList, context):
        print "Modify ssa response script. Modify revoke ssaList: %s" % ssaList
        return True