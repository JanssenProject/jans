# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2020, Janssen
#
# Author: Yuriy Zabrovarnyy
#
#

from io.jans.model.custom.script.type.uma import UmaRptClaimsType
from java.lang import String

class UmaRptClaims(UmaRptClaimsType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "RPT Claims script. Initializing ..."
        print "RPT Claims script. Initialized successfully"

        return True

    def destroy(self, configurationAttributes):
        print "RPT Claims script. Destroying ..."
        print "RPT Claims script. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 11

    # Returns boolean, true - apply changes from script method, false - ignore it.
    # Note :
    # responseAsJsonObject - is org.codehaus.jettison.json.JSONObject, you can use any method to manipulate json
    # context is reference of io.jans.as.service.external.context.ExternalUmaRptClaimsContext (in https://github.com/JanssenFederation/oxauth project, )
    def modify(self, rptAsJsonObject, context):
        rptAsJsonObject.accumulate("key_from_script", "value_from_script")
        return True

