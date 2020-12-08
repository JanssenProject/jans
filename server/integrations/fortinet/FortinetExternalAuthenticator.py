# Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
# Copyright (c) 2020, Janssen Project
#
# Author: Madhumita Subramaniam
#

from io.jans.service.cdi.util import CdiUtil
from io.jans.as.server.security import Identity
from io.jans.model.custom.script.type.auth import PersonAuthenticationType
from io.jans.as.server.service import AuthenticationService
from io.jans.util import StringHelper

from net.sourceforge.jradiusclient import RadiusClient
from net.sourceforge.jradiusclient import RadiusAttribute
from net.sourceforge.jradiusclient import RadiusAttributeValues
from net.sourceforge.jradiusclient import RadiusPacket

import java

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript,  configurationAttributes):
        print "Radius. Initialization"
        if not configurationAttributes.containsKey("RADIUS_SERVER_IP"):
            print "Fortinet. Initialization. Property RADIUS_SERVER_IP is mandatory"
            return False
        self.RADIUS_SERVER_IP = configurationAttributes.get("RADIUS_SERVER_IP").getValue2()
        
        if not configurationAttributes.containsKey("RADIUS_SERVER_SECRET"):
            print "Fortinet. Initialization. Property RADIUS_SERVER_SECRET is mandatory"
            return False
        self.RADIUS_SERVER_SECRET = configurationAttributes.get("RADIUS_SERVER_SECRET").getValue2()
       
        if not configurationAttributes.containsKey("RADIUS_SERVER_AUTH_PORT"):
            print "Fortinet. Initialization. Property RADIUS_SERVER_AUTH_PORT is mandatory"
            return False
        self.RADIUS_SERVER_AUTH_PORT = configurationAttributes.get("RADIUS_SERVER_AUTH_PORT").getValue2()
       
        if not configurationAttributes.containsKey("RADIUS_SERVER_ACCT_PORT"):
            print "Fortinet. Initialization. Property RADIUS_SERVER_ACCT_PORT is mandatory"
            return False
        self.RADIUS_SERVER_ACCT_PORT = configurationAttributes.get("RADIUS_SERVER_ACCT_PORT").getValue2()
       
        print "Radius. Initialized successfully"
        return True   

    def destroy(self, configurationAttributes):
        print "Radius. Destroy"
        print "Radius. Destroyed successfully"
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
        authenticationService = CdiUtil.bean(AuthenticationService)

        if (step == 1):
            print "Radius. Authenticate for step 1"

            identity = CdiUtil.bean(Identity)
            credentials = identity.getCredentials()

            user_name = credentials.getUsername()
            user_password = credentials.getPassword()

            if StringHelper.isNotEmptyString(user_name ) and StringHelper.isNotEmptyString(user_password ):
            	user_exists_in_gluu = authenticationService.authenticate(user_name )
            	if user_exists_in_gluu :
            		client =  RadiusClient(self.RADIUS_SERVER_IP,int (self.RADIUS_SERVER_AUTH_PORT), int(self.RADIUS_SERVER_ACCT_PORT), self.RADIUS_SERVER_SECRET)
               		accessRequest = RadiusPacket(RadiusPacket.ACCESS_REQUEST)
            		userNameAttribute = RadiusAttribute(RadiusAttributeValues.USER_NAME,user_name )
	    			userPasswordAttribute =  RadiusAttribute(RadiusAttributeValues.USER_PASSWORD,user_password )
            		accessRequest.setAttribute(userNameAttribute)
            		accessRequest.setAttribute(userPasswordAttribute)
	    			accessResponse = client.authenticate(accessRequest)
	    			print "Packet type - %s " % accessResponse.getPacketType()
	    			if accessResponse.getPacketType() == RadiusPacket.ACCESS_ACCEPT:
		           		return True
		        #elif accessResponse.getPacketType() == RadiusPacket.ACCESS_CHALLENGE:
		        #    	return False
	    		
        
		return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        if (step == 1):
            print "Radius. Prepare for Step 1"
            return True
        else:
            return False

    def getExtraParametersForStep(self, configurationAttributes, step):
        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        return 1

    def getPageForStep(self, configurationAttributes, step):
        return ""

    def getNextStep(self, configurationAttributes, requestParameters, step):
        return -1

    def getLogoutExternalUrl(self, configurationAttributes, requestParameters):
        print "Get external logout URL call"
        return None

    def logout(self, configurationAttributes, requestParameters):
        return True

 
    def printRadiusPacket(self, radiusPacket):
 		attributes = radiusPacket.getAttributes()
		for attribute in attributes:
            print("%s : %s" %attribute.getType() % attribute.getValue)