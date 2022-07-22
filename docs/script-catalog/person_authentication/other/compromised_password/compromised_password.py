# Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
# Copyright (c) 2020, Janssen Project
#
# Author: Yuriy Movchan
#

from io.jans.service.cdi.util import CdiUtil
from io.jans.as.server.security import Identity
from io.jans.model.custom.script.type.auth import PersonAuthenticationType
from io.jans.as.server.service import UserService, AuthenticationService
from io.jans.util import StringHelper
import javax.crypto.spec.SecretKeySpec as SecretKeySpec
import javax.crypto.spec.IvParameterSpec as IvParameterSpec
import javax.crypto.Cipher
from javax.crypto import *
from io.jans.util import ArrayHelper
from java.util import Arrays
import urllib, urllib2, json

import java
class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "compromised_password. Initialization"
        if not configurationAttributes.containsKey("secret_question"):
            print "compromised_password. Initialization. Property secret_question is mandatory"
            return False
        self.secretquestion = configurationAttributes.get("secret_question").getValue2()

        if not configurationAttributes.containsKey("credentials_file"):
            print "credentials_file property not defined"
            return False
        self.credentialfile = configurationAttributes.get("credentials_file").getValue2()

        if not configurationAttributes.containsKey("secret_answer"):
            print "compromised_password. Initialization. Property secret_answer is mandatory"
            return False
        self.secretanswer = configurationAttributes.get("secret_answer").getValue2()
        print "compromised_password. Initialized successfully"
        return True

    def destroy(self, configurationAttributes):
        print "compromised_password. Destroy"
        print "compromised_password. Destroyed successfully"
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
        identity = CdiUtil.bean(Identity)
        userService = CdiUtil.bean(UserService)
        authenticationService = CdiUtil.bean(AuthenticationService)

        if step == 1:
            credentials = identity.getCredentials()
            user_name = credentials.getUsername()
            user_password = credentials.getPassword()
            logged_in = False
            if (StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password)):
                userService = CdiUtil.bean(UserService)
                logged_in = authenticationService.authenticate(user_name, user_password)
            if (not logged_in):
                return False
            else:
                find_user_by_uid = authenticationService.getAuthenticatedUser()
                status_attribute_value = userService.getCustomAttribute(find_user_by_uid, "mail")
                user_mail = status_attribute_value.getValue()
                self.setRequestScopedParameters(identity)
                isCompromised = False
                isCompromised = self.is_compromised(user_mail,user_password,configurationAttributes)
                if(isCompromised):
                    identity.setWorkingParameter("pwd_compromised", isCompromised)
                    identity.setWorkingParameter("user_name", user_name)
                    return True
                else: 
                    return True
        elif step == 2:
            print "compromised_password. Authenticate for step 2"
            form_answer_array = requestParameters.get("loginForm:question")
            if ArrayHelper.isEmpty(form_answer_array):
                return False
            form_answer = form_answer_array[0]
            if (form_answer == self.secretanswer):
                return True
            return False
        elif step == 3:
            authenticationService = CdiUtil.bean(AuthenticationService)
            print "compromised_password (with password update). Authenticate for step 3"
            userService = CdiUtil.bean(UserService)
            update_button = requestParameters.get("loginForm:updateButton")
            new_password_array = requestParameters.get("new_password")
            if ArrayHelper.isEmpty(new_password_array) or StringHelper.isEmpty(new_password_array[0]):
                print "compromised_password (with password update). Authenticate for step 3. New password is empty"
                return False
            new_password = new_password_array[0]

            user = authenticationService.getAuthenticatedUser()
            if user == None:
                print "compromised_password (with password update). Authenticate for step 3. Failed to determine user name"
                return False

            user_name = user.getUserId()
            print "compromised_password (with password update). Authenticate for step 3. Attempting to set new user '" + user_name + "' password"
            find_user_by_uid = userService.getUser(user_name)
            if (find_user_by_uid == None):
                print "compromised_password (with password update). Authenticate for step 3. Failed to find user"
                return False

            find_user_by_uid.setAttribute("userPassword", new_password)
            userService.updateUser(find_user_by_uid)
            print "compromised_password (with password update). Authenticate for step 3. Password updated successfully"
            logged_in = authenticationService.authenticate(user_name)
            return True

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        identity = CdiUtil.bean(Identity)
        self.setRequestScopedParameters(identity)
        session_attributes = identity.getSessionId().getSessionAttributes()
        pwdcompromised = session_attributes.get("pwd_compromised")
        if(pwdcompromised != None):
            if step == 1:
                print "compromised_password. Prepare for step 1"
                return True
            elif step == 2:
                print "compromised_password. Prepare for step 2"
                return True
            return False
        else:
            print "compromised_password. Prepare for step 1"
            return True
      
    def getExtraParametersForStep(self, configurationAttributes, step):
        return Arrays.asList("pwd_compromised","user_name")

    def getCountAuthenticationSteps(self, configurationAttributes):
        identity = CdiUtil.bean(Identity)
        self.setRequestScopedParameters(identity)
        self.setRequestScopedParameters(identity)
        session_attributes = identity.getSessionId().getSessionAttributes()
        pwdcompromised = session_attributes.get("pwd_compromised")
        if(pwdcompromised != None):
            return 3
        return 1

    def getPageForStep(self, configurationAttributes, step):
        identity = CdiUtil.bean(Identity)
        session_attributes = identity.getSessionId().getSessionAttributes()
        pwdcompromised = session_attributes.get("pwd_compromised")
        if(pwdcompromised != None):
            if step == 2:
                return "/auth/compromised/complogin.xhtml"
            elif step == 3:
                return "/auth/compromised/newpassword.xhtml"
            return ""
        else:
            return ""
            
    def getNextStep(self, configurationAttributes, requestParameters, step):
        return -1

    def getLogoutExternalUrl(self, configurationAttributes, requestParameters):
        print "Get external logout URL call"
        return None
                    
    def logout(self, configurationAttributes, requestParameters):
        return True

    def setRequestScopedParameters(self, identity):
        identity.setWorkingParameter("question_label", self.secretquestion)

    def is_compromised(self, userid, password,configurationAttributes):
        print "Vericloud APIs Initialization"

        vericloud_gluu_creds_file = self.credentialfile
        # Load credentials from file
        f = open(vericloud_gluu_creds_file, 'r')
        try:
            creds = json.loads(f.read())
        except:
            print "Vericloud API. Initialize notification services. Failed to load credentials from file:", vericloud_gluu_creds_file
            return False
        finally:
            f.close()
        
        try:
            url = str(creds["api_url"])
            api_key=str(creds["api_key"])
            api_secret= str(creds["api_secret"])
        except:
            print "Vericloud API. Initialize notification services. Invalid credentials file '%s' format:" % super_gluu_creds_file
            return False
      

	reqdata = {"mode":"search_leaked_password_with_userid", "api_key": api_key, "api_secret": api_secret, "userid": userid}
	reqdata = urllib.urlencode(reqdata)
        resp = urllib2.urlopen(urllib2.Request(url, reqdata)).read()
	resp = json.loads(resp)
	if resp['result'] != 'succeeded':
	    return None
	for pass_enc in resp['passwords_encrypted']:
	    plaintext = self.AESCipherdecrypt(api_secret, pass_enc) 
	    if (len(password), password[0], password[-1]) == (len(plaintext), plaintext[0], plaintext[-1]) :
	        return True
	return False

    def AESCipherdecrypt(self, key, enc ):
        enc, iv = enc.split(':')
	cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
	cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key.decode("hex"), "AES"),IvParameterSpec(iv.decode("hex")))
	decrypted_password = cipher.doFinal(enc.decode("hex"))
	return decrypted_password.tostring()
