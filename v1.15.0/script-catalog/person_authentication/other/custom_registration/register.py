# Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
# Copyright (c) 2020, Janssen Project
#
# Author: Jose Gonzalez
# Author: Gasmyr Mougang

from org.gluu.oxauth.model.common import User, WebKeyStorage
from io.jans.service.cdi.util import CdiUtil
from io.jans.as.server.security import Identity
from io.jans.model.custom.script.type.auth import PersonAuthenticationType
from io.jans.as.server.service import UserService, AuthenticationService
from io.jans.as.server.util import ServerUtil
from io.jans.util import StringHelper, ArrayHelper
from java.util import Arrays
from jakarta.faces.application import FacesMessage
from io.jans.jsf2.message import FacesMessages
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from io.jans.service import MailService


import org.codehaus.jettison.json.JSONArray as JSONArray

import json, ast
import java
import random
import jarray
import smtplib

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis
        self.emailid = None
        self.identity = CdiUtil.bean(Identity)

    def init(self, customScript, configurationAttributes):

        print "Register. Initialized successfully"
        if not (configurationAttributes.containsKey("attributes_json_file_path")):
            #print "Cert. Initialization. Property chain_cert_file_path is mandatory"
            return False
        self.attributes_json_file_path = configurationAttributes.get("attributes_json_file_path").getValue2()

        return True

    def destroy(self, configurationAttributes):
        print "Register. Destroy"
        print "Register. Destroyed successfully"
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
       
        userService = CdiUtil.bean(UserService)
        identity = CdiUtil.bean(Identity)
        authenticationService = CdiUtil.bean(AuthenticationService)

        facesMessages = CdiUtil.bean(FacesMessages)
        facesMessages.setKeepMessages()

        session_attributes = self.identity.getSessionId().getSessionAttributes()
        form_passcode = ServerUtil.getFirstValue(requestParameters, "passcode")
       

        print "Register. form_response_passcode: %s" % str(form_passcode)

        if step == 1:
            print "inside step 1"
            ufnm = ServerUtil.getFirstValue(requestParameters, "fnm")
            ulnm = ServerUtil.getFirstValue(requestParameters, "lnm")
            umnm = ServerUtil.getFirstValue(requestParameters, "mnm")
            umail = ServerUtil.getFirstValue(requestParameters, "email")
            upass = ServerUtil.getFirstValue(requestParameters, "pass")
                
        
           
	    #rufnm1 = identity.getWorkingParameter("vufnm")
            #print "rufnm"
            #print rufnm1
           		
            #print "Register. Step 1 Password Authentication"
            
		
			   
            # Generate Random six digit code and store it in array
            code = random.randint(100000, 999999)
	        		

            # Get code and save it in LDAP temporarily with special session entry
            self.identity.setWorkingParameter("vufnm", ufnm)
            self.identity.setWorkingParameter("vulnm", ulnm)
            self.identity.setWorkingParameter("vumnm", umnm)
            self.identity.setWorkingParameter("vumail", umail)
            self.identity.setWorkingParameter("vupass", upass)
            self.identity.setWorkingParameter("code", code)

            try:
                mailService = CdiUtil.bean(MailService)	
                subject = "Registration Details"
                

                body = "<h2 style='margin-left:10%%;color: #337ab7;'>Welcome</h2><hr style='width:80%%;border: 1px solid #337ab7;'></hr><div style='text-align:center;'>"  
                                
                if ufnm is not None:
                    body = body + "<p>First Name : <span style='color: #337ab7;'>"+str(ufnm)+"</span>,</p>"
                    
                else:
                    body = body
                    
                    
                if ulnm is not None:
                    body = body + "<p>Last Name <span style='color: #337ab7;'>"+str(ulnm)+"</span>,</p>"
                    
                else:
                    body = body
                    

                if umnm is not None:
                    body = body + "<p>Middle Name <span style='color: #337ab7;'>"+str(umnm)+"</span>,</p>"
                    
                else:
                    body = body
                    
                body = body + "<p>Email : <span style='color: #337ab7;'>"+str(umail)+"</span>,</p><p>Password : <span style='color: #337ab7;'>"+str(upass)+"</span>,</p><p>Use <span style='color: #337ab7;'>%s</span> OTP to finish Registration.</p></div>"

                
                mailService.sendMail(umail, None, subject, body, body)
            
                
                return True
            except Exception, ex: 
                facesMessages.add(FacesMessage.SEVERITY_ERROR,"Failed to send message to mobile phone")
                print "Register. Error sending message to Twilio"
                print "Register. Unexpected error:", ex

            return False
        elif step == 2:
            # Retrieve the session attribute
            print "Register. Step 2 SMS/OTP Authentication"
            code = session_attributes.get("code")
            rufnm = identity.getWorkingParameter("vufnm")
            rulnm = identity.getWorkingParameter("vulnm")
            rumnm = identity.getWorkingParameter("vumnm")
            rumail = identity.getWorkingParameter("vumail")
            rupass = identity.getWorkingParameter("vupass")

            
        
			
             
            print "----------------------------------"
            print "Register. Code: %s" % str(code)
            print "----------------------------------"

            if code is None:
                print "Register. Failed to find previously sent code"
                return False

            if form_passcode is None:
                print "Register. Passcode is empty"
                return False

            if len(form_passcode) != 6:
                print "Register. Passcode from response is not 6 digits: %s" % form_passcode
                return False

            if form_passcode == code:
                print "Register, SUCCESS! User entered the same code!"
				
                newUser = User()
                newUser.setAttribute("givenName", rufnm)
                newUser.setAttribute("sn", rulnm)
                newUser.setAttribute("middleName", rumnm)
                newUser.setAttribute("mail", rumail)
                newUser.setAttribute("uid", rufnm)
                newUser.setAttribute("userPassword", rupass)
                userService.addUser(newUser, True)		
		
                logged_in = False
                logged_in = authenticationService.authenticate(rufnm, rupass)

                if (not logged_in):
                    return False

                return True		
				
                #return True

            print "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++" 
            print "Register. FAIL! User entered the wrong code! %s != %s" % (form_passcode, code)
            print "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++" 
            #facesMessages.add(facesMessage.SEVERITY_ERROR, "Incorrect Twilio code, please try again.")
            
         

       

        

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        if step == 1:
            print "Register. Prepare for Step 1"
            identity = CdiUtil.bean(Identity)
            print self.getAttributesFromJson()
            print "pass strength"
            print self.getPasswordStrength()
            identity.setWorkingParameter("CustomAtrributes", self.getAttributesFromJson())
            identity.setWorkingParameter("passStrength", str(self.getPasswordStrength()))
            return True
        elif step == 2:
            print "Register. Prepare for Step 2"
            return True
        return False

    def getExtraParametersForStep(self, configurationAttributes, step):
        if step == 1:
            return Arrays.asList("CustomAtrributes","PasswordStrength")
        elif step == 2:
            return Arrays.asList("code","vufnm","vulnm","vumnm","vumail","vupass")

        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        return 2

    def getPageForStep(self, configurationAttributes, step):
        if step == 1:
            return "/auth/reg.xhtml"
	elif step == 2:
            return "/auth/otp_sms/otp_sms.xhtml"

        return ""

    def getNextStep(self, configurationAttributes, requestParameters, step):
        return -1

    def getLogoutExternalUrl(self, configurationAttributes, requestParameters):
        print "Get external logout URL call"
        return None

    def logout(self, configurationAttributes, requestParameters):
        return True
        
    def getAttributesFromJson(self):
        f = open(self.attributes_json_file_path)
        data = json.load(f)
        data = ast.literal_eval(json.dumps(data))
        attributes = data["en"].keys()

        jsonString = ",".join(attributes)
        return jsonString
    def getPasswordStrength(self):
        f = open(self.attributes_json_file_path)
        data = json.load(f)
        data = ast.literal_eval(json.dumps(data))
        strength = data["passStrength"]
        return strength   
