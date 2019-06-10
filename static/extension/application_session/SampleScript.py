# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2016, Gluu
#
# Author: Yuriy Movchan
#

from org.gluu.model.custom.script.type.session import ApplicationSessionType
from org.gluu.service.cdi.util import CdiUtil
from org.gluu.persist import PersistenceEntryManager
from org.gluu.oxauth.model.config import StaticConfiguration
from org.gluu.oxauth.model.ldap import TokenLdap
from javax.faces.application import FacesMessage
from org.gluu.jsf2.message import FacesMessages
from org.gluu.util import StringHelper, ArrayHelper
from org.gluu.oxauth.model.config import Constants
from java.util import Arrays, ArrayList

import java

class ApplicationSession(ApplicationSessionType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
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
        return 2

    # Application calls it at start session request to allow notify 3rd part systems
    #   httpRequest is javax.servlet.http.HttpServletRequest
    #   authorizationGrant is org.gluu.oxauth.model.common.SessionId
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
    #   httpRequest is javax.servlet.http.HttpServletRequest
    #   authorizationGrant is org.gluu.oxauth.model.common.SessionId
    #   configurationAttributes is java.util.Map<String, SimpleCustomProperty>
    def endSession(self, httpRequest, sessionId, configurationAttributes):
        print "Application session. Starting external session end"

        print "Application session. External session ended successfully"
        return True

    def isFirstSession(self, user_name):
        tokenLdap = TokenLdap()
        tokenLdap.setDn(self.staticConfiguration.getBaseDn().getClients())
        tokenLdap.setUserId(user_name)

        tokenLdapList = self.entryManager.findEntries(tokenLdap, 1)
        print "Application session. isFirstSession. Get result: '%s'" % tokenLdapList

        if (tokenLdapList != None) and (tokenLdapList.size() > 0):
            print "Application session. isFirstSession: False"
            return False

        print "Application session. isFirstSession: True"
        return True
