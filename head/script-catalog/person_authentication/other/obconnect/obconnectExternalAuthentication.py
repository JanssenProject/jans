from __future__ import print_function

from java.util import Arrays
from io.jans.service.cdi.util import CdiUtil
from io.jans.model.custom.script.type.auth import PersonAuthenticationType
from io.jans.jsf2.service import FacesService
from io.jans.oxauth.util import ServerUtil
from io.jans.oxauth.service import UserService, SessionIdService
# please uncomment the following line when you need AuthenticationService
#from io.jans.oxauth.service import AuthenticationService
from io.jans.oxauth.model.common import User
from java.lang import String

from com.nimbusds.jose import EncryptionMethod;
from com.nimbusds.jose import JWEAlgorithm;
from com.nimbusds.jose import JWEHeader;
from com.nimbusds.jose import JWEObject;
from com.nimbusds.jose import Payload;
from com.nimbusds.jose.crypto import DirectDecrypter;
from com.nimbusds.jose.crypto import DirectEncrypter;
import time
import sys

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        """Construct class.

        Args:
            currentTimeMillis (int): current time in miliseconds
        """
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):

        if (not configurationAttributes.containsKey("sharedSecret")):
            print("Obconnect. Initialization. Property sharedSecret is not specified")
            return False
        else:
            self.sharedSecret = configurationAttributes.get("sharedSecret").getValue2()
        if (not configurationAttributes.containsKey("hostname")):
            print("Obconnect. Initialization. Property hostname is not specified")
            return False
        else:
            self.hostname = configurationAttributes.get("hostname").getValue2()

        if (not configurationAttributes.containsKey("tpp_client_id")):
            print("Obconnect. Initialization. Property tpp_client_id is not specified")
            return False
        else:
            self.tpp_client_id = configurationAttributes.get("tpp_client_id").getValue2()

        if (not configurationAttributes.containsKey("client_name")):
            print("Obconnect. Initialization. Property client_name is not specified")
            return False
        else:
            self.client_name = configurationAttributes.get("client_name").getValue2()

        if (not configurationAttributes.containsKey("organisation_name")):
            print("Obconnect. Initialization. Property organisation_name is not specified")
            return False
        else:
            self.organisation_name = configurationAttributes.get("organisation_name").getValue2()

        if (not configurationAttributes.containsKey("expiry")):
            print("Obconnect. Initialization. Property expiry is not specified")
            return False
        else:
            self.expiry = configurationAttributes.get("expiry").getValue2()

        if (not configurationAttributes.containsKey("consent_app_server_name")):
            print("Obconnect. Initialization. Property consent_app_server_name is not specified")
            return False
        else:
            self.consent_app_server_name = configurationAttributes.get("consent_app_server_name").getValue2()

        return True

    @classmethod
    def destroy(cls, configurationAttributes):
        return True

    @classmethod
    def getApiVersion(cls):
        return 11

    @classmethod
    def getAuthenticationMethodClaims(cls, requestParameters):
        return None

    @classmethod
    def isValidAuthenticationMethod(cls, usageType, configurationAttributes):
        return True

    @classmethod
    def getAlternativeAuthenticationMethod(cls, usageType, configurationAttributes):
        return None

    def authenticate(self, configurationAttributes, requestParameters, step):
        print("Obconnect. Authenticate. Step %s " % step)

        sessionData =  ServerUtil.getFirstValue(requestParameters, "sessionData")

        jweObject = JWEObject.parse(sessionData)
        #Decrypt
        jweObject.decrypt(DirectDecrypter((String(self.sharedSecret)).getBytes()))

        # Get the plain text
        payload = jweObject.getPayload()
        print("session payload - "+payload.toString())
        # A successful authorization will always return the `result` claim with login and consent details
        if payload.toJSONObject().get("result") is not None :
            resultObject =   payload.toJSONObject().get("result")
            if resultObject.get("login") is not None and resultObject.get("consent") is not None:
               print("Obconnect .successful Authentication")
               # question: What is the purpose of the following line?
               #authenticationService = CdiUtil.bean(AuthenticationService)

               # TODO: create a dummy user and authenticate
               newUser = User()
               uid = "obconnect_"+str(int(time.time()*1000.0))
               newUser.setAttribute("uid",uid)

               #TODO: add a new parameter called expiry and set expiry time
               # TODO:  A clean up task should be written which will delete this record
               userService = CdiUtil.bean(UserService)
               userService.addUser(newUser, True)
               # TODO: create a dummy user and authenticate
               #logged_in = authenticationService.authenticate(uid)

               openbanking_intent_id =  resultObject.get("login").get("account")
               acr_ob = resultObject.get("login").get("acr")

               # add a few things in session
               sessionIdService = CdiUtil.bean(SessionIdService)
               sessionId = sessionIdService.getSessionId() # fetch from persistence
               sessionId.getSessionAttributes().put("openbanking_intent_id", openbanking_intent_id)
               sessionId.getSessionAttributes().put("acr_ob", acr_ob )

               return True
        print ("Obconnect.Unsuccessful authentication")
        return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        print("Obconnect. prepare for step... %s" % step)
        scope = ServerUtil.getFirstValue(requestParameters, "scope")
        print("An example of accessing parameter from the request  - Scope: %s " % scope)
        #extract intent id from request object which is an encoded JWT
        request = ServerUtil.getFirstValue(requestParameters, "request")

        jweObject = JWEObject.parse(request)
        #Decrypt
        jweObject.decrypt(DirectDecrypter((String(self.sharedSecret)).getBytes()))

        # Get the plain text
        print("jweobject : %s " % jweObject)
        payload = jweObject.getPayload()
        print("Obconnect.Request payload containing intent id- %s- " % payload)
        print(" open intent id %s - " % payload.toJSONObject().get("openbanking_intent_id"))

        # A successful authorization will always return the `result` claim with login and consent details
        if payload.toJSONObject().get("openbanking_intent_id") is not None:
            print("1.")
            openbanking_intent_id = str(payload.toJSONObject().get("openbanking_intent_id"))
            print("openbanking_intent_id %s" % openbanking_intent_id)
            #sessionId = CdiUtil.bean(SessionIdService).getSessionId()
            sessionId = "1231231231231"
            print("sessionID : %s " % sessionId)
            redirectURL = self.getRedirectURL (openbanking_intent_id, sessionId)

            print("Obconnect. Redirecting to ... %s " % redirectURL)
            facesService = CdiUtil.bean(FacesService)
            facesService.redirectToExternalURL(redirectURL)
            return True

        print("Obconnect. Call to Gluu's /authorize endpoint should contain openbanking_intent_id as an encoded JWT")
        return False

    @classmethod
    def getCountAuthenticationSteps(cls, configurationAttributes):
        return 1

    @classmethod
    def getNextStep(cls, configurationAttributes, requestParameters, step):
        return -1

    @classmethod
    def getPageForStep(cls, configurationAttributes, step):
        print("Obconnect. getPageForStep... %s" % step)
        if step == 1:
            return "/auth/redirect.xhtml"

        return ""

    @classmethod
    def getExtraParametersForStep(cls, configurationAttributes, step):
          return Arrays.asList("openbanking_intent_id", "acr_ob")

    @classmethod
    def logout(cls, configurationAttributes, requestParameters):
        return True

    @classmethod
    def buildSessionIdObject(cls, hostname, intent_id_value,unique_identifier_for_session_object,
                             tpp_client_id, client_name,organisation_name, expiry) :

        sessionIdObject = "{\"returnTo\": \"" + hostname + "/oxauth/postlogin.htm" + \
            "\",\"prompt\": {\"name\": \"login\", \"details\" : { \"openbanking_intent_id\" : {" + \
            "\"value\":  \"" + intent_id_value + "\", \"essential\" : \"true\"}}}, "+ " \"uid\" : \"" + \
            unique_identifier_for_session_object + "\" , \"params\" : { \"client_id\": \"" + \
            tpp_client_id+ "\", \"scope\": \"openid accounts\", \"claims\": " +\
            "\"{\\\"userinfo\\\":{\\\"openbanking_intent_id\\\":{\\\"value\\\":\\\"" + intent_id_value + \
            "\\\",\\\"essential\\\":true}},\\\"id_token\\\":{\\\"openbanking_intent_id\\\":{\\\"value\\\":\\\"" + \
            intent_id_value + "\\\",\\\"essential\\\":true},\\\"acr\\\":{\\\"values\\\":[\\\"urn:openbanking:psd2:sca\\\"\
            ,\\\"urn:openbanking:psd2:ca\\\"],\\\"essential\\\":true}}}\""+ "}, \"exp\" : " + expiry + \
            ", \"client\": { \"client_name\": \"" + client_name + "\",\"org_name\" : \""+ organisation_name + "\"}}"

        print("Obconnect. sessionIdObject: %s " % sessionIdObject)
        return sessionIdObject

    def getRedirectURL (self, openbanking_intent_id, sessionID):

        #TODO: this is not required
        unique_identifier_for_session_object = "12345"

        try :
            # Create the header
            header =  JWEHeader(JWEAlgorithm.DIR, EncryptionMethod.A256GCM)

            # Create the payload
            payloadString = self.buildSessionIdObject(self.hostname, openbanking_intent_id, unique_identifier_for_session_object,
                                                      self.tpp_client_id, self.client_name, self.organisation_name, self.expiry)
            print("Payload String "+ payloadString)
            payload =  Payload(payloadString )

            # Create the JWE object and encrypt it
            jweObject =  JWEObject(header, payload)
            jweObject.encrypt( DirectEncrypter((String(self.sharedSecret)).getBytes()))

            # Serialise to compact JOSE form...
            jweString = jweObject.serialize()
            print("Redirect URL -->"+self.consent_app_server_name+"/?sessionData="+jweString)
            return self.consent_app_server_name+"/?sessionData="+jweString

        except Exception as e:
            print("Obconnect. Failed to build redirect URL -", e, sys.exc_info()[1])
            return None
