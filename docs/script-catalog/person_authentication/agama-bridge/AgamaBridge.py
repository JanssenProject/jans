# Janssen Project software is available under the Apache 2.0 License (2004). See http://www.apache.org/licenses/ for full text.
# Copyright (c) 2020, Janssen Project
#

from com.fasterxml.jackson.databind import ObjectMapper

from io.jans.agama import NativeJansFlowBridge
from io.jans.agama.engine.misc import FlowUtils
from io.jans.as.server.security import Identity
from io.jans.as.server.service import AuthenticationService, UserService
from io.jans.jsf2.service import FacesService
from io.jans.jsf2.message import FacesMessages
from io.jans.model.custom.script.type.auth import PersonAuthenticationType
from io.jans.orm import PersistenceEntryManager
from io.jans.service.cdi.util import CdiUtil
from io.jans.util import StringHelper

from jakarta.faces.application import FacesMessage
from java.util import Arrays

import java
import sys

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Agama. Initialization"
        self.resultParam = "agamaData"

        prop = "cust_param_name"
        self.cust_param_name = self.configProperty(configurationAttributes, prop)
        
        if self.cust_param_name == None:
            print "Agama. Custom parameter name not referenced via property '%s'" % prop
            return False

        prop = "default_flow_name"
        self.default_flow_name = self.configProperty(configurationAttributes, prop)
        
        prop = "finish_userid_db_attribute"
        self.finish_userid_db_attr = self.configProperty(configurationAttributes, prop)
        
        if self.finish_userid_db_attr == None:
            print "Agama. Property '%s' is missing value" % prop
            return False
            
        print "Agama. Request param '%s' will be used to pass flow name and inputs" % self.cust_param_name
        print "Agama. When '%s' is missing, the flow to launch will be '%s'" % (self.cust_param_name, self.default_flow_name)
        print "Agama. DB attribute '%s' will be used to map the identity of userId passed in Finish directives (if any)" % self.finish_userid_db_attr
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
                    
                    userService = CdiUtil.bean(UserService)
                    fuda = self.finish_userid_db_attr
                    matchingUsers = userService.getUsersByAttribute(fuda, userId, True, 2)
                    matches = len(matchingUsers)
                    
                    if matches != 1:
                        if matches == 0:
                            print "Agama. No user matches the required condition: %s=%s" % (fuda, userId)
                        else:
                            print "Agama. Several users match the required condition: %s=%s" % (fuda, userId)
                        
                        self.setMessageError(FacesMessage.SEVERITY_ERROR, "Unable to determine identity of user")
                        return False
                    
                    inum = matchingUsers[0].getAttribute("inum")
                    print "Agama. Authenticating user %s..." % inum
                    authenticated = CdiUtil.bean(AuthenticationService).authenticateByUserInum(inum)
                    
                    if not authenticated:
                        print "Agama. Unable to authenticate %s" % inum
                        return False
                    
                    jsonData = CdiUtil.bean(ObjectMapper).writeValueAsString(data) 
                    CdiUtil.bean(Identity).setWorkingParameter(self.resultParam, jsonData)
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
            if StringHelper.isEmpty(param):
                print "Agama. Request param '%s' is missing or has no value" % self.cust_param_name
                
                param = self.default_flow_name
                if param == None:
                    print "Agama. Default flow name is not set either..." 

                    print "Agama. Unable to determine the Agama flow to launch. Check the docs"
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
        return Arrays.asList(self.resultParam)

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
        val = None
        
        if prop != None:
            val = prop.getValue2()
            if StringHelper.isEmpty(val):
                val = None

        return val 

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
