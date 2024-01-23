
# Example custom application_session script to enforce one login
# and to write an extra audit log to the LDAP server

from io.jans.model.custom.script.type.session import ApplicationSessionType

from io.jans.service.cdi.util import CdiUtil
from io.jans.orm import PersistenceEntryManager
from io.jans.as.model.config import StaticConfiguration

from jakarta.faces.application import FacesMessage
from io.jans.jsf2.message import FacesMessages

from java.util import Date
from java.util import Calendar
from java.util import GregorianCalendar
from java.util import TimeZone
from java.text import SimpleDateFormat

#### Audit Entries Additional Imports ####
from io.jans.as.server.security import Identity
from io.jans.as.server.service import MetricService

from io.jans.orm.model.base import SimpleBranch
from io.jans.model import ApplicationType

from io.jans.as.common.model.session import SessionId
from io.jans.as.common.model.session import SessionIdState

from io.jans.orm.model.base import CustomObjectAttribute
from io.jans.orm.model.base import CustomObjectEntry

import java

import uuid
import time
import json
import ast

class ApplicationSession(ApplicationSessionType):

    log_level = -1

    session_attributes_map = {
            "userDn": "userDn",
            "id": "id",
            "outsideSid": "outsideSid",
            "lastUsedAt": "lastUsedAt",
            "authenticationTime": "authenticationTime",
            "expirationDate": "expirationDate",
            "sessionState": "sessionState",
            "permissionGranted": "permissionGranted",
            "deviceSecrets": "deviceSecrets",
            "state": "authState"
        }

    session_cust_attributes_map = {
            "opbs": "opbs",
            "response_type": "responseType",
            "client_id": "clientId",
            "auth_step": "authStep",
            "acr": "acr",
            "casa_logoUrl": "casaLogoUrl",
            "remote_ip": "remoteIp",
            "scope": "scope",
            "acr_values": "acrValues",
            "casa_faviconUrl": "casaFaviconUrl",
            "redirect_uri": "redirectUri",
            "state": "state",
            "casa_prefix": "casaPrefix",
            "casa_contextPath": "casaContextPath",
            "casa_extraCss": "casaExtraCss"
        }

    # without string processing
    session_cust_attributes_json_map = {
            "auth_external_attributes": "authExternalAttributes"
        }

    def __init__(self, current_time_millis):
        self.currentTimeMillis = current_time_millis

    def init(self, configuration_attributes):
        print ("ApplicationSession.init(): begin")

        self.metric_audit_ou_name = None
        self.metric_audit_conf_json_file_path = None

        self.event_types = None
        self.audit_data = None
        self.audit_cust_data = None

        self.init_ok = False

        self.entry_manager = CdiUtil.bean(PersistenceEntryManager)
        self.static_configuration = CdiUtil.bean(StaticConfiguration)

        try:
            self.metric_audit_ou_name = configuration_attributes.get("metric_audit_ou_name").getValue2()
            self.metric_audit_conf_json_file_path = configuration_attributes.get("metric_audit_conf_json_file_path").getValue2()

            log_level_val = configuration_attributes.get("log_level").getValue2()
            self.log_level = ApplicationSession.logLevelToInt(log_level_val)

            self.event_types, self.audit_data, self.audit_cust_data = self.getMetricAuditParameters(self.metric_audit_conf_json_file_path)
            if self.event_types is not None and self.audit_data is not None and self.audit_cust_data is not None:
                self.init_ok = True
        except Exception as ex:
            self.logOut("ERROR","ApplicationSession.init(): error of initializing: ex = {}".format(ex))

        self.logOut("DEBUG", "ApplicationSession.init(): self.event_types = {}".format(self.event_types))
        self.logOut("DEBUG", "ApplicationSession.init(): self.audit_data = {}".format(self.audit_data))
        self.logOut("DEBUG", "ApplicationSession.init(): self.audit_cust_data = {}".format(self.audit_cust_data))
        self.logOut("DEBUG", "ApplicationSession.init(): self.log_level = {}".format(self.log_level))

        self.logOut("INFO", "ApplicationSession.init(): self.init_ok = {}".format(self.init_ok))

        self.logOut("INFO", "ApplicationSession.init(): end")

        return True

    def destroy(self, configuration_attributes):
        print("ApplicationSession.destroy()")
        return True

    def getApiVersion(self):
        return 2

    # Called each time specific session event occurs
    # event is org.gluu.oxauth.service.external.session.SessionEvent
    def onEvent(self, event):
        self.logOut("INFO","ApplicationSession.onEvent(): start")
        self.logOut("INFO","ApplicationSession.onEvent(): event = {}".format(event))

        if not self.init_ok:
            self.logOut("ERROR","ApplicationSession.onEvent(): isn't initialized")
            return

        if not event or not(str(event.getType()).upper() in (event_type.upper() for event_type in self.event_types)):
            self.logOut("INFO","ApplicationSession.onEvent(): event {} will not be processed".format(event.getType()))
            return

        remote_ip = event.getSessionId().getSessionAttributes()["remote_ip"]
        self.logOut("DEBUG","ApplicationSession.onEvent(): remote_ip = {}".format(remote_ip))

        session = None
        session_attrs = None
        session_id = None
        user_dn = None 
        user = None
        uid = None
        ip = None

        session = event.getSessionId()
        if session:
            session_attrs = session.getSessionAttributes()
            session_id = session.getId()
            user_dn = session.getUserDn()
            user = session.getUser()

        if user:
            uid = user.getUserId()

        if session_attrs:
            client_id = session_attrs.get("client_id")
            redirect_uri = session_attrs.get("redirect_uri")
            acr = session_attrs.get("acr_values")

        http_request = event.getHttpRequest()

        if http_request:
            ip = http_request.getRemoteAddr()

        self.logOut("DEBUG", 'ApplicationSession.onEvent(): "session" = {}'.format(str(session)))

        self.logOut("DEBUG", 'ApplicationSession.onEvent(): "sessionId": {}, "uid": {}, "client_id": {}, "redirect_uri": {}, "acr": {}, "ip": {}, "type": {}'.format(
            session_id, uid, client_id, redirect_uri, acr, ip, str(event.getType())))

        # Don't allow more then one session!
        entity = SessionId()
        entity.setDn(self.static_configuration.getBaseDn().getSessions())
        entity.setUserDn(user_dn)
        entity.setState(SessionIdState.UNAUTHENTICATED)
        results = self.entry_manager.findEntries(entity)
        if results == 1:
            faces_messages = CdiUtil.bean(FacesMessages)
            faces_messages.add(FacesMessage.SEVERITY_ERROR, "Please, end active session first!")
            print("ApplicationSession.onEvent(): User %s denied session--must end active session first" % uid)
            return

        # Audit Log enhancements to store additional data in LDAP.
        #
        # The goal is to create a record here that can be exported and
        # reported on at a later time.

        calendar_curr_date = Calendar.getInstance()
        curr_date = calendar_curr_date.getTime()
        
        pattern = "yyyyMM";
        simple_df = SimpleDateFormat(pattern);
        year_month = simple_df.format(curr_date)

        if self.entry_manager.hasBranchesSupport(""):
            self.logOut("DEBUG","ApplicationSession.onEvent(): self.entry_manager.hasBranchesSupport("") = {}".format(str(self.entry_manager.hasBranchesSupport(""))))
            # Create a base organization unit, for example
            # ou=audit,o=metric
            metric_dn = self.static_configuration.getBaseDn().getMetric().split(",")[1]
            self.logOut("DEBUG","ApplicationSession.onEvent(): metric_dn = {}".format(metric_dn))
            audit_dn = "ou=%s,ou=statistic,%s" % (self.metric_audit_ou_name, metric_dn)
            self.logOut("DEBUG","ApplicationSession.onEvent(): audit_dn = {}".format(audit_dn))

            # If audit organizational unit does not exist, create it
            ou_exists = self.entry_manager.contains(audit_dn, SimpleBranch)
            self.logOut("DEBUG","ApplicationSession.onEvent(): ou_exists = {}".format(ou_exists))
            if not ou_exists:
                self.logOut("DEBUG","ApplicationSession.onEvent(): Creating organizational unit: {}".format(audit_dn))
                branch = SimpleBranch()
                branch.setOrganizationalUnitName(self.metric_audit_ou_name)
                branch.setDn(audit_dn)
                self.logOut("DEBUG","ApplicationSession.onEvent(): branch = {}".format(branch))
                self.entry_manager.persist(branch)

            # If there is no audit organizational unit for this month, create it
            year_month_dn = "ou=%s,%s" % (year_month, audit_dn)
            self.logOut("DEBUG","ApplicationSession.onEvent(): year_month_dn = {}".format(year_month_dn))
            ou_exists = self.entry_manager.contains(year_month_dn, SimpleBranch)
            self.logOut("DEBUG","ApplicationSession.onEvent(): ou_exists = {}".format(ou_exists))
            if not ou_exists:
                self.logOut("DEBUG","ApplicationSession.onEvent(): Creating organizational unit = {}".format(year_month_dn))
                branch = SimpleBranch()
                branch.setOrganizationalUnitName(year_month)
                branch.setDn(year_month_dn)
                self.logOut("DEBUG","ApplicationSession.onEvent(): branch = {}".format(branch))
                self.entry_manager.persist(branch)

        # Write the log
        # TODO Need to figure out edipi
        unique_identifier = str(uuid.uuid4())
        self.logOut("DEBUG","ApplicationSession.onEvent(): unique_identifier = {}".format(unique_identifier))

        dn = "uniqueIdentifier=%s,ou=%s,ou=%s,ou=statistic,o=metric" % (unique_identifier, year_month, self.metric_audit_ou_name)

        metric_entity = CustomObjectEntry()

        metric_entity.setCustomObjectClasses(["jansMetric"])
        metric_entity.setDn(dn)

        metric_entity.getCustomObjectAttributes().add(CustomObjectAttribute("uniqueIdentifier", unique_identifier))
        metric_entity.getCustomObjectAttributes().add(CustomObjectAttribute("creationDate", curr_date))
        metric_entity.getCustomObjectAttributes().add(CustomObjectAttribute("jansAppTyp", str(ApplicationType.OX_AUTH)))
        metric_entity.getCustomObjectAttributes().add(CustomObjectAttribute("jansMetricTyp", "audit"))
        
        jans_data = self.generateJansData(event, self.audit_data, self.audit_cust_data)
        
        self.logOut("DEBUG","ApplicationSession.onEvent(): jans_data = {}".format(jans_data))
        
        metric_entity.getCustomObjectAttributes().add(CustomObjectAttribute("jansData", jans_data))

        self.logOut("DEBUG","ApplicationSession.onEvent(): metric_entity = {}".format(metric_entity))
        self.entry_manager.persist(metric_entity)
        self.logOut("DEBUG","ApplicationSession.onEvent(): Wrote metric entry: dn = {}".format(dn))

        self.logOut("INFO","ApplicationSession.onEvent(): end")

        return

    # This method is called for both authenticated and unauthenticated sessions
    #   http_request is javax.servlet.http.HttpServletRequest
    #   session_id is org.gluu.oxauth.model.common.SessionId
    #   configuration_attributes is java.util.Map<String, SimpleCustomProperty>
    def startSession(self, http_request, session_id, configuration_attributes):
    
        self.logOut("INFO","ApplicationSession.startSession(): start")
        self.logOut("INFO","ApplicationSession.startSession(): session_id = {}".format(session_id))    

        if not self.init_ok:
            self.logOut("ERROR","ApplicationSession.startSession(): isn't initialized")
            return True

        ip = None
        if http_request:
            ip = http_request.getRemoteAddr()

        remote_ip = session_id.getSessionAttributes()["remote_ip"]

        self.logOut("DEBUG","ApplicationSession.startSession(): remote_ip = {}".format(remote_ip))
        self.logOut("DEBUG","ApplicationSession.startSession(): http_request = {}".format(http_request))
        self.logOut("DEBUG","ApplicationSession.startSession(): session_id = {}".format(session_id))
        self.logOut("DEBUG","ApplicationSession.startSession(): ip = {}".format(ip))
        self.logOut("DEBUG","ApplicationSession.startSession(): configuration_attributes = {}".format(configuration_attributes))

        self.logOut("DEBUG","ApplicationSession.startSession(): for session_id: {}".format(session_id.getId()))

        self.logOut("INFO","ApplicationSession.startSession(): end")

        return True

    # Application calls it at end session request to allow notify 3rd part systems
    #   http_request is javax.servlet.http.HttpServletRequest
    #   session_id is org.gluu.oxauth.model.common.SessionId
    #   configuration_attributes is java.util.Map<String, SimpleCustomProperty>
    def endSession(self, http_request, session_id, configuration_attributes):
    
        self.logOut("INFO","ApplicationSession.endSession(): start")
        self.logOut("INFO","ApplicationSession.endSession(): session_id = {}".format(session_id))
    
        if not self.init_ok:
            self.logOut("ERROR","ApplicationSession.endSession(): isn't initialized")
            return True

        ip = None
        if http_request:
            ip = http_request.getRemoteAddr()

        remote_ip = session_id.getSessionAttributes()["remote_ip"]
        self.logOut("DEBUG","ApplicationSession.endSession(): remote_ip = {}".format(remote_ip))

        self.logOut("DEBUG","ApplicationSession.endSession(): http_request = {}".format(http_request))
        self.logOut("DEBUG","ApplicationSession.endSession(): session_id = {}".format(session_id))
        self.logOut("DEBUG","ApplicationSession.endSession(): ip = {}".format(ip))
        self.logOut("DEBUG","ApplicationSession.endSession(): configuration_attributes = {}".format(configuration_attributes))
        
        self.logOut("DEBUG","ApplicationSession.endSession(): for session_id: {}".format(session_id.getId()))        
        
        self.logOut("INFO","ApplicationSession.endSession(): end")

        return True

    def getMetricAuditParameters(self, metric_audit_conf_json_file_path):
        file_data = None
        event_types = None
        audit_data = None
        audit_cust_data = None        
        try:
            file = open(metric_audit_conf_json_file_path)
            file_data = json.load(file)
            file.close()
            file_data = ast.literal_eval(json.dumps(file_data))
            event_types = file_data["event_types"]
            audit_data = file_data["audit_data"]
            audit_cust_data = file_data["audit_cust_data"]
        except Exception as ex:
            self.logOut("ERROR","ApplicationSession.getMetricAuditParameters: Errror Reading of config file: ex = {}".format(ex))
        self.logOut("DEBUG","ApplicationSession.getMetricAuditParameters(): event_types = {}".format(event_types))
        self.logOut("DEBUG","ApplicationSession.getMetricAuditParameters(): audit_data = {}".format(audit_data))
        self.logOut("DEBUG","ApplicationSession.getMetricAuditParameters(): audit_cust_data = {}".format(audit_cust_data))
        return event_types, audit_data, audit_cust_data
        
    def initCustomObjectEntry(self, metric_entity, event, audit_data):
        session = event.getSessionId()
        self.logOut("DEBUG","ApplicationSession.initCustomObjectEntry(): session = {}".format(session))
        #empty first call
        attr_value = getattr(session, "userDn")        
        
        for attr_key, attr_name in self.session_attributes_map.items():
            if attr_key.upper() in (audit_data_el.upper() for audit_data_el in audit_data):
                try:
                    attr_value = getattr(session, attr_key)
                    self.logOut("DEBUG","ApplicationSession.initCustomObjectEntry(): attr_key = {}, attr_name = {}".format(attr_key, attr_name))
                    metric_entity.getCustomObjectAttributes().add(CustomObjectAttribute(attr_name, attr_value))
                    #setattr(audit_metric_data, attr_name, attr_value)
                except Exception as ex:
                    self.logOut("ERROR","ApplicationSession.initCustomObjectEntry(): Error: ex = {0}".format(ex))
        return

    def generateJansData(self, event, audit_data, audit_cust_data):
        session = event.getSessionId()
        
        if not session:
            return '{ }'
        
        jans_data = '{ '
        
        jans_data += '"type": "%s"' % event.getType()

        #empty first call
        attr_value = getattr(session, "userDn")

        for attr_key, attr_name in self.session_attributes_map.items():
            if attr_key.strip().upper() in (audit_data_el.strip().upper() for audit_data_el in audit_data):
                try:
                    attr_value = getattr(session, attr_key)
                    self.logOut("DEBUG","ApplicationSession.generateJansData(): attr_key = {}, attr_value = {}".format(attr_key, attr_value))
                    jans_data += ',"%s": "%s"' % (attr_name, attr_value.replace('"','\\"') if (attr_value and isinstance(attr_value, str)) else str(attr_value).replace('"','\\"'))
                except Exception as ex:
                    self.logOut("ERROR","ApplicationSession.generateJansData(): Errror: ex = {}".format(ex))
                    jans_data += ',"%s": "%s"' % (attr_name, "None")

        attr_key = "permissionGrantedMap"
        attr_name = "permissionGrantedMap"

        if attr_key.strip().upper() in (audit_data_el.strip().upper() for audit_data_el in audit_data):
            try:
                attr_value = getattr(session, attr_key)
                self.logOut("DEBUG","ApplicationSession.generateJansData(): attr_key = {}, attr_value = {}".format(attr_key, attr_value))
                permission_granted_map = attr_value.getPermissionGranted()                

                jans_data += ',"%s": {' % (attr_name)

                first_added = False
                for key, value in permission_granted_map.items():
                    if first_added:
                        jans_data += ','
                    else:
                        first_added = True
                    jans_data += '"%s": %s' % (key, "false" if value == 0 or value == False else "true")

                jans_data += '}'

            except Exception as ex:
                self.logOut("ERROR","ApplicationSession.generateJansData(): Error: ex = {}".format(ex))
                jans_data += ',"%s": "%s"' % (attr_name, "None") 

        session_cust_attributes = session.getSessionAttributes()

        for cust_attr_key, cust_attr_name in self.session_cust_attributes_map.items():
            if ("sessionAttributes".upper() in (audit_data_el.strip().upper() for audit_data_el in audit_cust_data) or
                    not ("sessionAttributes".upper() in (audit_data_el.strip().upper() for audit_data_el in audit_cust_data)) and
                    cust_attr_key.strip().upper() in (audit_data_el.strip().upper() for audit_data_el in audit_cust_data)):
                try:
                    cust_attr_value = session_cust_attributes[cust_attr_key]
                    self.logOut("DEBUG","ApplicationSession.generateJansData(): cust_attr_key = {}, cust_attr_name = {}, cust_attr_value = {}".format(cust_attr_key, cust_attr_name, cust_attr_value))
                    self.logOut("DEBUG","ApplicationSession.generateJansData(): type(cust_attr_value) = {}".format(type(cust_attr_value)))
                    jans_data += ',"%s": "%s"' % (cust_attr_name, cust_attr_value.replace('"','\\"') if (cust_attr_value and isinstance(cust_attr_value, str)) else str(cust_attr_value).replace('"','\\"'))
                except Exception as ex:
                    self.logOut("ERROR","ApplicationSession.generateJansData(): Error: ex = {}".format(ex))
                    jans_data += ',"%s": "%s"' % (cust_attr_name, "None")

        for cust_attr_key, cust_attr_name in self.session_cust_attributes_json_map.items():
            if ("sessionAttributes".upper() in (audit_data_el.strip().upper() for audit_data_el in audit_cust_data) or
                    not ("sessionAttributes".upper() in (audit_data_el.strip().upper() for audit_data_el in audit_cust_data)) and
                    cust_attr_key.strip().upper() in (audit_data_el.strip().upper() for audit_data_el in audit_cust_data)):
                try:
                    cust_attr_value = session_cust_attributes[cust_attr_key]
                    self.logOut("DEBUG","ApplicationSession.generateJansData(): cust_attr_key = {}, cust_attr_name = {}, cust_attr_value = {}".format(cust_attr_key, cust_attr_name, cust_attr_value))
                    self.logOut("DEBUG","ApplicationSession.generateJansData(): type(cust_attr_value) = {}".format(type(cust_attr_value)))
                    jans_data += ',"%s": %s' % (cust_attr_name, cust_attr_value)
                except Exception as ex:
                    self.logOut("ERROR","ApplicationSession.generateJansData(): Error: ex = {}".format(ex))
                    jans_data += ',"%s": "%s"' % (cust_attr_name, "None")

        jans_data += ' }'

        return jans_data

    # log_level = 0     - ERROR
    # log_level = 5     - INFO
    # log_level = 10    - DEBUG
    @staticmethod
    def logLevelToInt(log_level_val):
        log_level = -1
        if log_level_val.strip().upper() == "ERROR":
            log_level = 0
        elif log_level_val.strip().upper() == "INFO":
            log_level = 5
        elif log_level_val.strip().upper() == "DEBUG":
            log_level = 10
        return log_level

    def logOut(self, level, out_data):
        curr_log_level = ApplicationSession.logLevelToInt(level)
        if (curr_log_level <= self.log_level):
            print(out_data)
        return
