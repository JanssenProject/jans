# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2020, Janssen
#
# Author: Yuriy Z
#
#

from io.jans.model.custom.script.type.selectaccount import SelectAccountType
from java.lang import String

class SelectAccount(SelectAccountType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "SelectAccount script. Initializing ..."
        print "SelectAccount script. Initialized successfully"

        return True

    def destroy(self, configurationAttributes):
        print "SelectAccount script. Destroying ..."
        print "SelectAccount script. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 11

    # Returns path to select account page (e.g. /customs/page/path/myselectaccount.xhtml)
    # If none or empty string is returned, AS uses built-in page.
    # (Note: Custom page can be also put into `custom/pages/selectAccount.xhtml` and used without custom script.)
    # context is reference of io.jans.as.server.model.common.ExecutionContext( https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/model/common/ExecutionContext.java )
    def getSelectAccountPage(self, context):
        return ""

    # This method is called before select account page is loaded.
    # It is good place to make preparation processing.
    # E.g. check whether it is ok to land on select account page or maybe redirect to external page.
    # Return True - continue loading of the page
    # Return False - stop loading and show error page
    # context is reference of io.jans.as.server.model.common.ExecutionContext( https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/model/common/ExecutionContext.java )
    def prepare(self, context):
        return True

    # Returns display name for given session id (https://github.com/JanssenProject/jans/blob/main/jans-auth-server/common/src/main/java/io/jans/as/common/model/session/SessionId.java).
    # Session can be accessed via context.getSessionId()
    # Typical use is: context.getSessionId().getUser().getAttribute("myDisplayName")
    # Returns string. If blank string is returned, AS will return built-in implementation to return display name
    # which is context.getSessionId().getUser().getAttribute("displayName")
    # context is reference of io.jans.as.server.model.common.ExecutionContext( https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/model/common/ExecutionContext.java )
    def getAccountDisplayName(self, context):
        return ""

    # This method is called on session selection.
    # Selected session can be accessed as context.getSessionId()
    # Return True - continue session selection
    # Return False - stop session selection (forbid it)
    # context is reference of io.jans.as.server.model.common.ExecutionContext( https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/model/common/ExecutionContext.java )
    def onSelect(self, context):
        return True
