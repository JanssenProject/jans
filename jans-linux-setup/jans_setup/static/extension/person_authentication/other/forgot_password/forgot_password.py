# coding: utf-8
# Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
# Copyright (c) 2020, Janssen Project
#
# Author: Christian Eland


from io.jans.as.server.service import AuthenticationService
from io.jans.as.server.service import UserService
# from org.gluu.oxauth.auth import Authenticator
from io.jans.as.server.security import Identity
from io.jans.model.custom.script.type.auth import PersonAuthenticationType
from io.jans.service.cdi.util import CdiUtil
from io.jans.util import StringHelper
from io.jans.as.server.util import ServerUtil
from io.jans.as.common.service.common import ConfigurationService
from io.jans.as.common.service.common import EncryptionService
from io.jans.jsf2.message import FacesMessages
from jakarta.faces.application import FacesMessage
from io.jans.orm.exception import AuthenticationException

#dealing with smtp server
import smtplib

#dealing with emails
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText

# This one is from core Java
from java.util import Arrays

# to generate string token
import random
import string

# regex
import re

import urllib

import java

class EmailValidator():
    '''
    Class to check e-mail format
    '''
    regex = '^\w+([\.-]?\w+)*@\w+([\.-]?\w+)*(\.\w{2,3})+$'

    def check(self, email):
        '''
        Check if email format is valid
        returns: boolean
        '''

        if(re.search(self.regex,email)):
            print "Forgot Password - %s is a valid email format" % email
            return True
        else:
            print "Forgot Password - %s is an invalid email format" % email
            return False

class Token:
    #class that deals with string token

    def generateToken(self):
        ''' method to generate token string
        returns: String
        '''
        letters = string.ascii_lowercase

        #token lenght
        lenght = 20

        #generate token
        token = ''.join(random.choice(letters) for i in range(lenght))
        print "Forgot Password - Generating token"

        return token


class EmailSender():
    #class that sends e-mail through smtp

    def getSmtpConfig(self):
        '''
        get SMTP config from Gluu Server
        return dict
        '''
       
        smtpconfig = CdiUtil.bean(ConfigurationService).getConfiguration().getSmtpConfiguration()
        
        if smtpconfig is None:
            print "Forgot Password - SMTP CONFIG DOESN'T EXIST - Please configure"

        else:
            print "Forgot Password - SMTP CONFIG FOUND"
            encryptionService = CdiUtil.bean(EncryptionService)
            smtp_config = {
                'host' : smtpconfig.getHost(),
                'port' : smtpconfig.getPort(),
                'user' : smtpconfig.getUserName(),
                'from' : smtpconfig.getFromEmailAddress(),
                'pwd_decrypted' : encryptionService.decrypt(smtpconfig.getPassword()),
                'req_ssl' : smtpconfig.isRequiresSsl(),
                'requires_authentication' : smtpconfig.isRequiresAuthentication(),
                'server_trust' : smtpconfig.isServerTrust()
            }

        return smtp_config

            

    def sendEmail(self,useremail,token):
        '''
        send token by e-mail to useremail
        '''

        # server connection 
        smtpconfig = self.getSmtpConfig()
        
        try:
            s = smtplib.SMTP(smtpconfig['host'], port=smtpconfig['port'])
            

            if smtpconfig['requires_authentication']:
                
                if smtpconfig['req_ssl']:
                    s.starttls()
            
                s.login(smtpconfig['user'], smtpconfig['pwd_decrypted'])

        
            #message setup
            msg = MIMEMultipart() #create message
            
            message = "Here is your token: %s" % token

            msg['From'] = smtpconfig['from'] #sender
            msg['To'] = useremail #recipient
            msg['Subject'] = "Password Reset Request" #subject

            #attach message body
            msg.attach(MIMEText(message, 'plain'))

            #send message via smtp server
            # send_message method is for python3 only s.send_message(msg)

            #send email (python2)
            s.sendmail(msg['From'],msg['To'],msg.as_string())
            
            #after sent, delete
            del msg

        except smtplib.SMTPAuthenticationError as err:
            print "Forgot Password - SMTPAuthenticationError - %s - %s" % (MY_ADDRESS,PASSWORD)
            print err

        except smtplib.smtplib.SMTPSenderRefused as err:
            print "Forgot Password - SMTPSenderRefused - " + err


class PersonAuthentication(PersonAuthenticationType):


    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):

        print "Forgot Password - Initialized successfully"
        return True   

    def destroy(self, configurationAttributes):
        print "Forgot Password - Destroyed successfully"
        return True

    def getApiVersion(self):
        # I'm not sure why is 11 and not 2
        return 11

    def getAuthenticationMethodClaims(self, requestParameters):
        return None

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True


    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def authenticate(self, configurationAttributes, requestParameters, step):
        '''
        Authenticates user
        Step 1 will be defined according to SCRIPT_FUNCTION custom attribute
        returns: boolean
        '''

        #gets custom attribute
        sf = configurationAttributes.get("SCRIPT_FUNCTION").getValue2()

        print "Forgot Password - %s - Authenticate for step %s" % (sf, step)

        identity = CdiUtil.bean(Identity)
        credentials = identity.getCredentials()
        user_name = credentials.getUsername()
        user_password = credentials.getPassword()


        if step == 1:

            if sf == "forgot_password":

                
                authenticationService = CdiUtil.bean(AuthenticationService)

                logged_in = authenticationService.authenticate(user_name, user_password)

                
                if not logged_in:

                    
                    email = ServerUtil.getFirstValue(requestParameters, "ForgotPasswordForm:useremail")
                    validator = EmailValidator()
                    if not validator.check(email):
                        print "Forgot Password - Email format invalid"
                        return False

                    else:
                        print "Forgot Password -Email format valid"
     
                        print "Forgot Password - Entered email is %s" % email
                        identity.setWorkingParameter("useremail",email)
                        
                        # Just trying to get the user by the email
                        user_service = CdiUtil.bean(UserService)
                        user2 = user_service.getUserByAttribute("mail", email)

                        if user2 is not None:
                        
                            print user2
                            print "Forgot Password - User with e-mail %s found." % user2.getAttribute("mail")
                        
                            # send email
                            new_token = Token()
                            token = new_token.generateToken()                
                            sender = EmailSender()
                            print "Email: " + email
                            print "Token: " + token
                            sender.sendEmail(email,token)

                        
                            identity.setWorkingParameter("token", token)
                            print identity.getWorkingParameter("token")
                        
     
                            
                        else:
                            print "Forgot Password - User with e-mail %s not found" % email

                        return True


                else:
                    # if user is already authenticated, returns true.

                    user = authenticationService.getAuthenticatedUser()
                    print "Forgot Password - User %s is authenticated" % user.getUserId()

                    return True

            if sf == "email_2FA":

                try:
                    # Just trying to get the user by the uid
                    authenticationService = CdiUtil.bean(AuthenticationService)
                    logged_in = authenticationService.authenticate(user_name, user_password)
                    
                    print 'email_2FA user_name: ' + str(user_name)
                    
                    user_service = CdiUtil.bean(UserService)
                    user2 = user_service.getUserByAttribute("uid", user_name)

                    if user2 is not None:
                        print "user:"
                        print user2
                        print "Forgot Password - User with e-mail %s found." % user2.getAttribute("mail")
                        email = user2.getAttribute("mail")
                        uid = user2.getAttribute("uid")

                        # send token
                        # send email
                        new_token = Token()
                        token = new_token.generateToken()                
                        sender = EmailSender()
                        print "Email: " + email
                        print "Token: " + token
                        sender.sendEmail(email,token)

                        identity.setWorkingParameter("token", token)

                        return True

                except AuthenticationException as err:
                    print err
                    return False

                
   

        if step == 2:
            # step 2 user enters token
            credentials = identity.getCredentials()
            user_name = credentials.getUsername()
            user_password = credentials.getPassword()
            
            authenticationService = CdiUtil.bean(AuthenticationService)
            logged_in = authenticationService.authenticate(user_name, user_password)

            # retrieves token typed by user
            input_token = ServerUtil.getFirstValue(requestParameters, "ResetTokenForm:inputToken")

            print "Forgot Password - Token inputed by user is %s" % input_token

            token = identity.getWorkingParameter("token")
            print "Forgot Password - Retrieved token"
            email = identity.getWorkingParameter("useremail")
            print "Forgot Password - Retrieved email" 

            # compares token sent and token entered by user
            if input_token == token:
                print "Forgot Password - token entered correctly"
                identity.setWorkingParameter("token_valid", True)
                
                return True

            else:
                print "Forgot Password - wrong token"
                return False

        
        if step == 3:
            # step 3 enters new password (only runs if custom attibute is forgot_password

            user_service = CdiUtil.bean(UserService)

            email = identity.getWorkingParameter("useremail")
            user2 = user_service.getUserByAttribute("mail", email)


            user_name = user2.getUserId()
            
            new_password = ServerUtil.getFirstValue(requestParameters, "UpdatePasswordForm:newPassword")
            
            print "Forgot Password - New password submited"
        
            # update user info with new password
            user2.setAttribute("userPassword",new_password)
            print "Forgot Password - user uid is %s" % user_name
            print "Forgot Password - Updating user with new password..."
            user_service.updateUser(user2)
            print "Forgot Password - User updated with new password"
            # authenticates and login user
            print "Forgot Password - Loading authentication service..."
            authenticationService2 = CdiUtil.bean(AuthenticationService)

            print "Forgot Password - Trying to authenticate user..."
            login = authenticationService2.authenticate(user_name, new_password)
            
            return True

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        
        print "Forgot Password - Preparing for step %s" % step
        
        return True


    # Return value is a java.util.List<String> 
    def getExtraParametersForStep(self, configurationAttributes, step):
        return Arrays.asList("token","useremail","token_valid")


    # This method determines how many steps the authentication flow may have
    # It doesn't have to be a constant value
    def getCountAuthenticationSteps(self, configurationAttributes):
        
        sf = configurationAttributes.get("SCRIPT_FUNCTION").getValue2()
        

        # if option is forgot_token
        if sf == "forgot_password":
            print "Entered sf == forgot_password"
            return 3
            
        # if ption is email_2FA
        if sf == "email_2FA":
            print "Entered if sf=email_2FA"
            return 2

        else:
            print "Forgot Password - Custom Script Custom Property Incorrect, please check"


    # The xhtml page to render upon each step of the flow
    # returns a string relative to oxAuth webapp root
    def getPageForStep(self, configurationAttributes, step):
        
        sf = configurationAttributes.get("SCRIPT_FUNCTION").getValue2()

        if step == 1:

            if sf == "forgot_password":
                return "/auth/forgot_password/forgot.xhtml"

            if sf == 'email_2FA':
                return ""

        if step == 2:
            return "/auth/forgot_password/entertoken.xhtml"

        if step == 3:
            if sf == "forgot_password":
                return "/auth/forgot_password/newpassword.xhtml"

    
    def getNextStep(self, configurationAttributes, requestParameters, step):
        # Method used on version 2 (11?)
        return -1
    
    def getLogoutExternalUrl(self, configurationAttributes, requestParameters):
        print "Get external logout URL call"
        return None
        
    def logout(self, configurationAttributes, requestParameters):
        return True

