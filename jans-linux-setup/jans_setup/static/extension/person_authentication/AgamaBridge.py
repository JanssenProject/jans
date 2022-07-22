# Janssen Project software is available under the Apache 2.0 License (2004). See http://www.apache.org/licenses/ for full text.
# Copyright (c) 2020, Janssen Project
#
from io.jans.agama import NativeJansFlowBridge
from io.jans.agama.engine.misc import FlowUtils
from io.jans.as.server.security import Identity
from io.jans.as.server.service import AuthenticationService
from io.jans.jsf2.service import FacesService
from io.jans.jsf2.message import FacesMessages
from io.jans.model.custom.script.type.auth import PersonAuthenticationType
from io.jans.orm import PersistenceEntryManager
from io.jans.service.cdi.util import CdiUtil
from io.jans.util import StringHelper

from jakarta.faces.application import FacesMessage

import java
import sys

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Agama. Initialization"        
        prop = "cust_param_name"
        self.cust_param_name = self.configProperty(configurationAttributes, prop)
        
        if self.cust_param_name == None:
            print "Agama. Custom parameter name not referenced via property '%s'" % prop
            return False
            
        print "Agama. Request param '%s' will be used to pass flow inputs" % self.cust_param_name
        print "Agama. Initialized successfully"
        return True

    def destroy(self, configurationAttributes):
        print "Agama. Destroy"
        print "Agama. Destroyed successfully"
        return True
        
    def getAuthenticationMethodClaims(self, requestParameters):
        return None
        
    def getApiVersion(self):
        return 11

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def authenticate(self, configurationAttributes, requestParameters, step):

        if step == 1:
            print "Agama. Authenticate for step 1"
            
            try:
                bridge = CdiUtil.bean(NativeJansFlowBridge)
                result = bridge.close()
                
                if result == None or not result.isSuccess():
                    print "Agama. Flow DID NOT finished successfully"
                    return False
                else:
                    print "Agama. Flow finished successfully"
                    data = result.getData()
                    userId = data.get("userId") if data != None else None
                    
                    if userId == None:
                        print "Agama. No userId provided in flow result."                        
                        self.setMessageError(FacesMessage.SEVERITY_ERROR, "Unable to determine identity of user")
                        return False
                        
                    authenticated = CdiUtil.bean(AuthenticationService).authenticate(userId)
                    
                    if not authenticated:
                        print "Agama. Unable to authenticate %s" % userId
                        return False
            except:
                print "Agama. Exception: ", sys.exc_info()[1]
                return False

            return True


    def prepareForStep(self, configurationAttributes, requestParameters, step):
        
        if not CdiUtil.bean(FlowUtils).serviceEnabled():
            print "Agama. Please ENABLE Agama engine in auth-server configuration"
            return False

        if step == 1:
            print "Agama. Prepare for Step 1"

            session = CdiUtil.bean(Identity).getSessionId()
            if session == None:
                print "Agama. Failed to retrieve session_id"
                return False
                
            param = session.getSessionAttributes().get(self.cust_param_name) 
            if param == None:
                print "Agama. Request param '%s' is missing or has no value" % self.cust_param_name
                return False
            
            (qn, ins) = self.extractParams(param)
            if qn == None:
                print "Agama. Param '%s' is missing the name of the flow to be launched" % self.cust_param_name
                return False
                
            try:
                bridge = CdiUtil.bean(NativeJansFlowBridge)
                running = bridge.prepareFlow(session.getId(), qn, ins)
                
                if running == None:
                    print "Agama. Flow '%s' does not exist!" % qn
                    return False
                elif running:
                    print "Agama. A flow is already in course"
                    
                print "Agama. Redirecting to start/resume agama flow '%s'..." % qn
                
                CdiUtil.bean(FacesService).redirectToExternalURL(bridge.getTriggerUrl())
            except:
                print "Agama. An error occurred when launching flow '%s'. Check jans-auth logs" % qn
                print "Agama. Exception: ", sys.exc_info()[1]
                return False
            #except java.lang.Throwable, ex:
            #    ex.printStackTrace() 
            #    return False                
            return True
        
    def getExtraParametersForStep(self, configurationAttributes, step):
        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        return 1

    def getPageForStep(self, configurationAttributes, step):
        # page referenced here is only used when a flow is restarted
        return "/" + CdiUtil.bean(NativeJansFlowBridge).scriptPageUrl()

    def getNextStep(self, configurationAttributes, requestParameters, step):
        return -1

    def getLogoutExternalUrl(self, configurationAttributes, requestParameters):
        return None

    def logout(self, configurationAttributes, requestParameters):
        return True

# Misc routines

    def configProperty(self, configProperties, name):
        prop = configProperties.get(name)
        return None if prop == None else prop.getValue2()

    def setMessageError(self, severity, msg):
        facesMessages = CdiUtil.bean(FacesMessages)
        facesMessages.setKeepMessages()
        facesMessages.clear()
        facesMessages.add(severity, msg)
        
    def extractParams(self, param):

        # param must be of the form QN-INPUT where QN is the qualified name of the flow to launch
        # INPUT is a JSON object that contains the arguments to use for the flow call.
        # The keys of this object should match the already defined flow inputs. Ideally, and   
        # depending on the actual flow implementation, some keys may not even be required 
        # QN and INPUTS are separated by a hyphen
        # INPUT must be properly URL-encoded when HTTP GET is used
        
        i = param.find("-")
        if i == 0:
            return (None, None)
        elif i == -1:
            return (param, None)
        else:
            return (param[:i], param[i+1:])
