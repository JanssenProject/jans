from java.util import Collections, HashMap, HashSet, ArrayList, Arrays, Date
from io.jans.service.cdi.util import CdiUtil
from io.jans.model.custom.script.type.auth import PersonAuthenticationType
from io.jans.as.server.security import Identity
from io.jans.util import StringHelper
from io.jans.jsf2.service import FacesService
from jakarta.faces.context import FacesContext
from io.jans.jsf2.message import FacesMessages
from io.jans.as.server.util import ServerUtil
from io.jans.as.server.service import UserService, SessionIdService,AuthenticationService
from io.jans.as.common.model.common import User 
from java.lang import String
from com.nimbusds.jwt import SignedJWT
from com.nimbusds.jose.jwk import JWKSet
from org.json import JSONObject

from com.nimbusds.jose import JWSVerifier
from com.nimbusds.jose.crypto import RSASSAVerifier
from com.nimbusds.jose.jwk import RSAKey

from org.apache.commons.codec.binary import Base64
from com.nimbusds.jose import EncryptionMethod
from com.nimbusds.jose import JWEAlgorithm
from com.nimbusds.jose import JWEHeader
from com.nimbusds.jose import JWEObject
from com.nimbusds.jose import Payload
from com.nimbusds.jose.crypto import DirectDecrypter
from com.nimbusds.jose.crypto import DirectEncrypter

from java.util import UUID

from java.net import URL
import time
import java
import sys
import os

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis   

    def init(self, customScript, configurationAttributes):
        # TODO: ideally this will come from a configuration
        
        if (not configurationAttributes.containsKey("tpp_jwks_url")):
	        print "Person Authentication. Initialization. Property tpp_jwks_url is not specified"
	        return False
        else: 
        	self.tpp_jwks_url = configurationAttributes.get("tpp_jwks_url").getValue2() 
        
        if (not configurationAttributes.containsKey("redirect_url")):
	        print "Person Authentication. Initialization. Property redirect_url is not specified"
	        return False
        else: 
        	self.redirect_url = configurationAttributes.get("redirect_url").getValue2() 
        
        print "OpenBanking Person authentication script initialized."
        return True   

    def destroy(self, configurationAttributes):
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
        print "Person Authentication. Authenticate. Step %s " % step
        identity = CdiUtil.bean(Identity)
        # handle error from the consent app       
        error = ServerUtil.getFirstValue(requestParameters, "error") 
        if error is not None:
            print "Restarting consent flow. Error from consent app.  - %s" % error
            identity.setWorkingParameter("pass_authentication", False)
            return False

        authenticationService = CdiUtil.bean(AuthenticationService)
               
        # TODO: create a dummy user and authenticate
        newUser = User()
        uid = "ob_"+str(int(time.time()*1000.0))
        newUser.setAttribute("displayName",uid)
        newUser.setAttribute("sn", uid)
        newUser.setAttribute("cn", uid)
        newUser.setAttribute("uid", uid)
        print "new user %s "% uid       
               #TODO: add a new parameter called expiry and set expiry time 
               # TODO:  A clean up task should be written which will delete this record
        userService = CdiUtil.bean(UserService)
        foundUser = userService.addUser(newUser, True)
        print "%s found : "% foundUser.getUserId()
               # TODO: create a dummy user and authenticate
        logged_in = authenticationService.authenticate(foundUser.getUserId())
        #identity.setWorkingParameter("pass_authentication", True)
        print "logged In %s " % logged_in
        openbanking_intent_id = identity.getWorkingParameter("openbanking_intent_id")  #resultObject.get("login").get("account")
        acr_ob = "something"#resultObject.get("login").get("acr")
               
        # add a few things in session, this will be used by the introspection and update token custom script
        sessionIdService = CdiUtil.bean(SessionIdService)
        sessionId = sessionIdService.getSessionId() # fetch from persistence
        sessionId.getSessionAttributes().put("openbanking_intent_id",openbanking_intent_id )
        sessionId.getSessionAttributes().put("acr_ob", acr_ob )
        print "Person Authentication. Successful authentication"
        return True

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        print "Person Authentication. prepare for step... %s" % step 
        
        jwkSet = JWKSet.load( URL(self.tpp_jwks_url));
        signedRequest = ServerUtil.getFirstValue(requestParameters, "request")
        for key in jwkSet.getKeys() : 
            result = self.isSignatureValid(signedRequest, key)
            if (result == True):
                signedJWT = SignedJWT.parse(signedRequest)
                claims = JSONObject(signedJWT.getJWTClaimsSet().getClaims().get("claims"))
                print "Person Authentication. claims : %s " % claims.toString()
                id_token = claims.get("id_token");
                openbanking_intent_id = id_token.getJSONObject("openbanking_intent_id").getString("value")
                print "Person Authentication. openbanking_intent_id %s " % openbanking_intent_id
                redirectURL = self.redirect_url+"&state="+UUID.randomUUID().toString()+"&intent_id="+openbanking_intent_id
                identity = CdiUtil.bean(Identity)
                identity.setWorkingParameter("openbanking_intent_id",openbanking_intent_id)
                print "OpenBanking. Redirecting to ... %s " % redirectURL 
                facesService = CdiUtil.bean(FacesService)
                facesService.redirectToExternalURL(redirectURL)
                return True
      
        
        
        print "Person Authentication. Call to Jans-auth server's /authorize endpoint should contain openbanking_intent_id as an encoded JWT"
        return False


    def getCountAuthenticationSteps(self, configurationAttributes):
        return 1
    def getNextStep(self, configurationAttributes, requestParameters, step):
        
        #identity = CdiUtil.bean(Identity)

        # If user not pass current step authenticaton redirect to current step
        #pass_authentication = identity.getWorkingParameter("pass_authentication")
        #if not pass_authentication:
        #    resultStep = step
        #    print "Restarting consent flow. %s'" % resultStep
        #    return resultStep

        return -1
    


    def getPageForStep(self, configurationAttributes, step):
        print "Person Authentication. getPageForStep... %s" % step
        if step == 1:
            return "/redirect.xhtml"
        return ""
        
    def getExtraParametersForStep(self, configurationAttributes, step):
          return Arrays.asList("openbanking_intent_id", "acr_ob")

    def logout(self, configurationAttributes, requestParameters):
        return True
        
    def isSignatureValid(self, token,  publickey) :
        # Parse the JWS and verify its RSA signature
        
        try:
            signedJWT = SignedJWT.parse(token)
            #verifier =  RSASSAVerifier((RSAKey) publickey)
            verifier =  RSASSAVerifier( publickey)
            return signedJWT.verify(verifier)
        except:
            print "isSignatureValid. Exception: ", sys.exc_info()[1]
            return False
        
        
