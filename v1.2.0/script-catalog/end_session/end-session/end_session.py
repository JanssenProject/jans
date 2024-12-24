# Copyright (c) 2020, Janssen
#
# Author: Yuriy Zabrovarnyy
#

from io.jans.model.custom.script.type.logout import EndSessionType
from java.lang import String

class EndSession(EndSessionType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "EndSession script. Initializing ..."
        print "EndSession script. Initialized successfully"

        return True

    def destroy(self, configurationAttributes):
        print "EndSession script. Destroying ..."
        print "EndSession script. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 11

    # Returns string, it must be valid HTML (with iframes according to spec http://openid.net/specs/openid-connect-frontchannel-1_0.html)
    # This method is called on `/end_session` after actual session is killed and oxauth construct HTML to return to RP.
    # Note :
    # context is reference of io.jans.as.service.external.context.EndSessionContext (in https://github.com/JanssenFederation/oxauth project, )
    def getFrontchannelHtml(self, context):
        return ""