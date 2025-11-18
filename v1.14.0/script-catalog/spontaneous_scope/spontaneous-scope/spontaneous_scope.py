# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2018, Janssen
#
# Author: Yuriy Zabrovarnyy
#
#

from io.jans.model.custom.script.type.spontaneous import SpontaneousScopeType
from java.lang import String

class SpontaneousScope(SpontaneousScopeType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Spontaneous scope script. Initializing ..."
        print "Spontaneous scope script. Initialized successfully"

        return True

    def destroy(self, configurationAttributes):
        print "Spontaneous scope script. Destroying ..."
        print "Spontaneous scope script. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 11

    # This method is called before spontaneous scope is persisted. It's possible to disable persistence via context.setAllowSpontaneousScopePersistence(false)
    # Also it's possible to manipulated already granted scopes, e.g. context.getGrantedScopes().remove("transaction:456")
    # Note :
    # context is reference of io.jans.as.service.external.context.SpontaneousScopeExternalContext(in https://github.com/JanssenFederation/oxauth project, )
    def manipulateScopes(self, context):
        return

