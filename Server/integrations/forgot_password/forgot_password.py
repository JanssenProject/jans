# coding: utf-8
# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2020, Gluu
#
# Author: Christian Eland


from org.xdi.oxauth.service import AuthenticationService
from org.gluu.oxauth.service import UserService
from org.gluu.oxauth.auth import Authenticator
from org.xdi.oxauth.security import Identity
from org.xdi.model.custom.script.type.auth import PersonAuthenticationType
from org.xdi.service.cdi.util import CdiUtil
from org.xdi.util import StringHelper
from org.xdi.oxauth.util import ServerUtil
from org.gluu.oxauth.service import ConfigurationService
from org.gluu.oxauth.service import EncryptionService



from org.gluu.jsf2.message import FacesMessages
from javax.faces.application import FacesMessage

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
        print "Forgot Password - SMTP CONFIG:"
        smtpconfig = CdiUtil.bean(ConfigurationService).getConfiguration().getSmtpConfiguration()
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
        #server connection 
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

    def init(self, configurationAttributes):

        print "Forgot Password - Initialized successfully"
        return True   

    def destroy(self, configurationAttributes):
        print "Forgot Password - Destroyed successfully"
        return True

    def getApiVersion(self):
        return 1

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True


    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def authenticate(self, configurationAttributes, requestParameters, step):
        '''
        Retorna True se a autenticação foi bem sucedida no passo atual
        '''
                    
        print "Forgot Password - Authenticate for step %s" % step

       
        identity = CdiUtil.bean(Identity)
        

        if step == 1:
            
            credentials = identity.getCredentials()
            user_name = credentials.getUsername()
            user_password = credentials.getPassword()

            print "Forgot Password - user_name = " + str(user_name)


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
                        sender.sendEmail(email,token)

                    
                        identity.setWorkingParameter("token", token)
                        print identity.getWorkingParameter("token")
                    
 
                        
                    else:
                        print "Forgot Password - User with e-mail %s not found" % email

                    return True


            else:
                

                user = authenticationService.getAuthenticatedUser()
                print "Forgot Password - User %s is authenticated" % user.getUserId()

                

                return True

        if step == 2:

            credentials = identity.getCredentials()
            user_name = credentials.getUsername()
            user_password = credentials.getPassword()
            
           
            authenticationService = CdiUtil.bean(AuthenticationService)
            logged_in = authenticationService.authenticate(user_name, user_password)


            input_token = ServerUtil.getFirstValue(requestParameters, "ResetTokenForm:inputToken")
            # retrieves token typed by user
            print "Forgot Password - Token inputed by user is %s" % input_token

            token = identity.getWorkingParameter("token")
            print "Forgot Password - Retrieved token %s" % token
            email = identity.getWorkingParameter("useremail")
            print "Forgot Password - Retrieved email %s" % email

            if input_token == token:
                print "Forgot Password - token entered correctly"
                identity.setWorkingParameter("token_valid",True)
                
                return True

            else:
                print "Forgot Password - wrong token"
                return False

        
        # step 3 enters new password
        if step == 3:
            user_service = CdiUtil.bean(UserService)

            email = identity.getWorkingParameter("useremail")
            user2 = user_service.getUserByAttribute("mail", email)
            user_name = user2.getUserId()
            
            
            new_password = ServerUtil.getFirstValue(requestParameters, "UpdatePasswordForm:newPassword")
            
            print "Forgot Password - New password submited"
            #print "Forgot Password - New password: %s" % new_password
        


            #update user info with new password
            user2.setAttribute("userPassword",new_password)

            user_service.updateUser(user2)

            authenticationService2 = CdiUtil.bean(AuthenticationService)
            
            #authenticates and login user
            login = authenticationService2.authenticate(user_name, new_password)
            
            return True

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        
        print "Forgot Password - Forgot. Prepare for Step %s" %step
        
        return True


    # Return value is a java.util.List<String> 
    def getExtraParametersForStep(self, configurationAttributes, step):
        return Arrays.asList("token","useremail","token_valid")


    # This method determines how many steps the authentication flow may have
    # It doesn't have to be a constant value
    def getCountAuthenticationSteps(self, configurationAttributes):
    
        identity = CdiUtil.bean(Identity)

        if not identity.getWorkingParameter("token_valid"):
            return 2

        if identity.getWorkingParameter("token_valid"):
            return 3

        else:
            return 1

    # The xhtml page to render upon each step of the flow
    # returns a string relative to oxAuth webapp root
    def getPageForStep(self, configurationAttributes, step):
        
        if step == 1:
            return "/auth/forgot_password/forgot.xhtml"
            
        if step == 2:
            return "/auth/forgot_password/resettoken.xhtml"

        if step == 3:
            return "/auth/forgot_password/newpassword.xhtml"

    def logout(self, configurationAttributes, requestParameters):
        return True