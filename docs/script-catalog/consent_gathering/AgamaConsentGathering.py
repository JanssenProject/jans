from io.jans.agama import NativeJansFlowBridge
from io.jans.agama.engine.misc import FlowUtils
from io.jans.as.model.util import Base64Util
from io.jans.as.server.authorize.ws.rs import ConsentGatheringSessionService
from io.jans.jsf2.service import FacesService
from io.jans.model.custom.script.type.authz import ConsentGatheringType
from io.jans.service.cdi.util import CdiUtil

import java
import sys

class ConsentGathering(ConsentGatheringType):

    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Agama-Consent. Initializing ..."
        print "Agama-Consent. Initialized successfully"
        self.enterUrl = "agama/consent.xhtml"

        return True

    def destroy(self, configurationAttributes):
        print "Agama-Consent. Destroying ..."
        print "Agama-Consent. Destroyed successfully"

        return True

    def getApiVersion(self):
        return 11

    def authorize(self, step, context):
        print "Agama-Consent. Authorizing..."
        
        if step == 1:
            print "Agama-Consent. Authorize for step 1"
            
            try:
                bridge = CdiUtil.bean(NativeJansFlowBridge)
                result = bridge.close()

                if result == None or not result.isSuccess():
                    print "Agama-Consent. Flow DID NOT finished successfully"
                    return False
            except:
                print "Agama-Consent. Exception: ", sys.exc_info()[1]
                return False

            return True


    def getNextStep(self, step, context):
        return -1

    def prepareForStep(self, step, context):

        if not context.isAuthenticated():
            print "Agama-Consent. User is not authenticated"
            return False

        if not CdiUtil.bean(FlowUtils).serviceEnabled():
            print "Agama-Consent. Please ENABLE Agama engine in auth-server configuration"
            return False

        if step == 1:
            print "Agama-Consent. Prepare for Step 1"

            cgss = CdiUtil.bean(ConsentGatheringSessionService)
            #userDn = context.getUserDn()
            #session = cgss.getConsentSession(context.getHttpRequest(), context.getHttpResponse(), userDn, False)
            session = cgss.getConnectSession(context.getHttpRequest())
            
            if session == None:
                print "Agama-Consent. Failed to retrieve session_id"
                return False
                
            cesar = session.getSessionAttributes()
            param = cesar.get("agama_flow")

            if not param:
                param = self.extractAgamaFlow(cesar.get("acr_values"))

                if not param:
                    print "Agama-Consent. Unable to determine the Agama flow to launch. Check the docs"
                    return False
            
            (qn, ins) = self.extractParams(param)
            if qn == None:
                print "Agama-Consent. Unable to determine the Agama flow to launch. Check the docs"
                return False
                
            try:
                sessionId = session.getId()
                # print "==================================== %s" % sessionId

                bridge = CdiUtil.bean(NativeJansFlowBridge)
                running = bridge.prepareFlow(sessionId, qn, ins, False, self.enterUrl)
                
                if running == None:
                    print "Agama-Consent. Flow '%s' does not exist or cannot be launched from a browser!" % qn
                    return False
                elif running:
                    print "Agama-Consent. A flow is already in course"
                    
                print "Agama-Consent. Redirecting to start/resume agama flow '%s'..." % qn
                
                CdiUtil.bean(FacesService).redirectToExternalURL(bridge.getTriggerUrl())
            except:
                print "Agama-Consent. An error occurred when launching flow '%s'. Check jans-auth logs" % qn
                print "Agama-Consent. Exception: ", sys.exc_info()[1]
                return False

        return True

    def getStepsCount(self, context):
        return 1

    def getPageForStep(self, step, context):
        return "/" + self.enterUrl

# Misc routines
        
    def extractAgamaFlow(self, acr):
        prefix = "agama_"
        if acr and acr.startswith(prefix):
            return acr[len(prefix):]
        return None        
        
    def extractParams(self, param):

        # param must be of the form QN-INPUT where QN is the qualified name of the flow to launch
        # INPUT is a base64URL-encoded JSON object that contains the arguments to use for the flow call.
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
            return (param[:i], Base64Util.base64urldecodeToString(param[i+1:]))