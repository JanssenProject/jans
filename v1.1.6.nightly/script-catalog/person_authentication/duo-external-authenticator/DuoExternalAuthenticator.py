from io.jans.service.cdi.util import CdiUtil
from io.jans.as.server.security import Identity
from io.jans.model.custom.script.type.auth import PersonAuthenticationType
from io.jans.as.server.service import AuthenticationService, SessionIdService
from io.jans.as.server.service.common import UserService
from io.jans.util import StringHelper
from io.jans.util import ArrayHelper
from java.util import Arrays
from io.jans.as.server.service.net import HttpService
import os
import java
import sys
from com.duosecurity import Client
from com.duosecurity.exception import DuoException
from com.duosecurity.model import Token
from io.jans.jsf2.service import FacesService
from jakarta.faces.context import FacesContext
from io.jans.jsf2.message import FacesMessages
from io.jans.as.server.util import ServerUtil


class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Duo-Universal. Initialization"
        
        if (not configurationAttributes.containsKey("client_id")):
	        print "Duo Universal. Initialization. Property client_id is not specified"
	        return False
        else: 
        	self.client_id = configurationAttributes.get("client_id").getValue2() 
        	
        if (not configurationAttributes.containsKey("client_secret")):
	        print "Duo Universal. Initialization. Property client_secret is not specified"
	        return False
        else: 
        	self.client_secret = configurationAttributes.get("client_secret").getValue2() 
        	
        if (not configurationAttributes.containsKey("api_hostname")):
	        print "Duo Universal. Initialization. Property api_hostname is not specified"
	        return False
        else: 
        	self.api_hostname = configurationAttributes.get("api_hostname").getValue2() 
            
        print "Duo-Universal. Initialized successfully"
        return True   

    def destroy(self, configurationAttributes):
        print "Duo-Universal. Destroy"
        print "Duo-Universal. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 11
        
    def getAuthenticationMethodClaims(self, requestParameters):
        return None
        
    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def authenticate(self, configurationAttributes, requestParameters, step):
        print "Duo-Universal. Authenticate for step %s" % step
        
        identity = CdiUtil.bean(Identity)
        if (step == 1):
            authenticationService = CdiUtil.bean(AuthenticationService)

            # Check if user authenticated already in another custom script
            user = authenticationService.getAuthenticatedUser()
            
            if user == None:
                print "user is none"
                credentials = identity.getCredentials()
                
                user_name = credentials.getUsername()
                user_password = credentials.getPassword()
    
                logged_in = False
                if (StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password)):
                    userService = CdiUtil.bean(UserService)
                    logged_in = authenticationService.authenticate(user_name, user_password)
    				
                if (not logged_in):
                    print "return false"
                    return False
                identity.setWorkingParameter('username',user_name)
            return True
            
        elif (step == 2):
               
            identity = CdiUtil.bean(Identity)
            
            state = ServerUtil.getFirstValue(requestParameters, "state")
 			# Get state to verify consistency and originality
            if  identity.getWorkingParameter('state_duo') == state :
        	
            	# Get authorization token to trade for 2FA
            	duoCode = ServerUtil.getFirstValue(requestParameters, "duo_code")
	        	try:
	               token = self.duo_client.exchangeAuthorizationCodeFor2FAResult(duoCode, identity.getWorkingParameter('username'))
                   print "token status %s " % token.getAuth_result().getStatus()
	        	except:
	                # Handle authentication failure.
	               print "authentication failure", sys.exc_info()[1]
	               return False
	        
	        # User successfully passed Duo authentication.
	        
	        if "allow" == token.getAuth_result().getStatus():
	           return True
	           
	        return False
                
        else:
            print "Neither step 1 or 2" 
            return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
    	print "Duo-Universal. Prepare for step %s" % step
        
        if (step == 1):
            return True
        elif (step == 2):
        	identity = CdiUtil.bean(Identity)
            user_name = identity.getWorkingParameter('username')
        	facesContext = CdiUtil.bean(FacesContext)
        	request = facesContext.getExternalContext().getRequest()
        	httpService = CdiUtil.bean(HttpService)
        	url = httpService.constructServerUrl(request) + "/postlogin.htm"
        	
        	try:
	        	self.duo_client = Client(self.client_id,self.client_secret,self.api_hostname,url)
	        	self.duo_client.healthCheck()
	    	except:
                print "Duo-Universal. Duo config error. Verify the values in Duo-Universal.conf are correct ", sys.exc_info()[1]
                            
                state = self.duo_client.generateState()
                identity.setWorkingParameter("state_duo",state)
                prompt_uri = self.duo_client.createAuthUrl(user_name, state)
                
                facesService = CdiUtil.bean(FacesService)
                facesService.redirectToExternalURL(prompt_uri )

                return True
                    
        else:
            return False

    def getExtraParametersForStep(self, configurationAttributes, step):
        return Arrays.asList("state_duo", "username")

    def getCountAuthenticationSteps(self, configurationAttributes):
        return 2

    def getPageForStep(self, configurationAttributes, step):
        print "Duo-Universal. getPageForStep - %s " % step
        return ""

    def getNextStep(self, configurationAttributes, requestParameters, step):
        return -1

    def getLogoutExternalUrl(self, configurationAttributes, requestParameters):
        print "Get external logout URL call"
        return None

    def logout(self, configurationAttributes, requestParameters):
        return True
        