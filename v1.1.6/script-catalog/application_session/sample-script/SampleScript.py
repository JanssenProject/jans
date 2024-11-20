# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2016, Janssen
#
# Author: Yuriy Movchan
#

from io.jans.model.custom.script.type.session import ApplicationSessionType
from io.jans.service.cdi.util import CdiUtil
from io.jans.persist import PersistenceEntryManager
from io.jans.as.model.config import StaticConfiguration
from io.jans.as.model.ldap import TokenEntity
from jakarta.faces.application import FacesMessage
from io.jans.jsf2.message import FacesMessages
from io.jans.util import StringHelper, ArrayHelper
from io.jans.as.model.config import Constants
from java.util import Arrays, ArrayList
from io.jans.as.service.external.session import SessionEventType

import java

class ApplicationSession(ApplicationSessionType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Application session. Initialization"

        self.entryManager = CdiUtil.bean(PersistenceEntryManager)
        self.staticConfiguration = CdiUtil.bean(StaticConfiguration)

        print "Application session. Initialized successfully"

        return True   

    def destroy(self, configurationAttributes):
        print "Application session. Destroy"
        print "Application session. Destroyed successfully"
        return True   

    def getApiVersion(self):
        return 11

    # Called each time specific session event occurs
    # event is io.jans.as.service.external.session.SessionEvent
    def onEvent(self, event):
        if event.getType() == SessionEventType.AUTHENTICATED:
            print "Session is authenticated, session: " + event.getSessionId().getId()
        return

    # Application calls it at start session request to allow notify 3rd part systems
    #   httpRequest is jakarta.servlet.http.HttpServletRequest
    #   sessionId is io.jans.as.model.common.SessionId
    #   configurationAttributes is java.util.Map<String, SimpleCustomProperty>
    def startSession(self, httpRequest, sessionId, configurationAttributes):
        print "Application session. Starting external session"

        user_name = sessionId.getSessionAttributes().get(Constants.AUTHENTICATED_USER)

        first_session = self.isFirstSession(user_name)
        if not first_session:
            facesMessages = CdiUtil.bean(FacesMessages)
            facesMessages.add(FacesMessage.SEVERITY_ERROR, "Please, end active session first!")
            return False

        print "Application session. External session started successfully"
        return True

    # Application calls it at end session request to allow notify 3rd part systems
    #   httpRequest is jakarta.servlet.http.HttpServletRequest
    #   sessionId is io.jans.as.model.common.SessionId
    #   configurationAttributes is java.util.Map<String, SimpleCustomProperty>
    def endSession(self, httpRequest, sessionId, configurationAttributes):
        print "Application session. Starting external session end"

        print "Application session. External session ended successfully"
        return True

    # Application calls it during /session/active endpoint call to modify response if needed
    #   jsonArray is org.json.JSONArray
    #   context is io.jans.as.server.model.common.ExecutionContext
    def modifyActiveSessionsResponse(self, jsonArray, context):
        return False

    def isFirstSession(self, user_name):
        tokenLdap = TokenEntity()
        tokenLdap.setDn(self.staticConfiguration.getBaseDn().getClients())
        tokenLdap.setUserId(user_name)

        tokenLdapList = self.entryManager.findEntries(tokenLdap, 1)
        print "Application session. isFirstSession. Get result: '%s'" % tokenLdapList

        if (tokenLdapList != None) and (tokenLdapList.size() > 0):
            print "Application session. isFirstSession: False"
            return False

        print "Application session. isFirstSession: True"
        return True
