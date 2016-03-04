#
# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2016, Gluu
#
# Author: Yuriy Movchan
#

from org.xdi.model.custom.script.type.auth import PersonAuthenticationType
from org.jboss.seam.contexts import Context, Contexts
from javax.faces.context import FacesContext
from org.jboss.seam.security import Identity
from org.xdi.oxauth.service import UserService
from org.xdi.util import StringHelper
from org.xdi.util import ArrayHelper
from org.xdi.oxauth.util import ServerUtil
from java.util import HashMap, Arrays
from java.security import MessageDigest
from org.apache.commons.codec.binary import Hex
from org.xdi.oxauth.model.util import FingerprintUtil

import sys
import java
import datetime
import base64

try:
    import json
except ImportError:
    import simplejson as json

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        print "Cert. Initialization"

        if not (configurationAttributes.containsKey("map_user_cert")):
            print "Cert. Initialization. Property map_user_cert is mandatory"
            return False

        self.map_user_cert = StringHelper.toBoolean(configurationAttributes.get("map_user_cert").getValue2(), False)
        print "Cert. Initialization. map_user_cert: '%s'" % self.map_user_cert

        print "Cert. Initialized successfully"

        return True   

    def destroy(self, configurationAttributes):
        print "Cert. Destroy"
        print "Cert. Destroyed successfully"

        return True

    def getApiVersion(self):
        return 1

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def authenticate(self, configurationAttributes, requestParameters, step):
        credentials = Identity.instance().getCredentials()
        user_name = credentials.getUsername()

        context = Contexts.getEventContext()
        userService = UserService.instance()

        if (step == 1):
            print "Cert. Authenticate for step 1"
            login_button = ServerUtil.getFirstValue(requestParameters, "loginForm:loginButton")
            if StringHelper.isEmpty(login_button):
                print "Cert. Authenticate for step 1. Form were submitted incorrectly"
                return False
            
            return True
        elif (step == 2):
            print "Cert. Authenticate for step 2"

            # Validate if user selected certificate
            request = FacesContext.getCurrentInstance().getExternalContext().getRequest()
            x509Certificates = request.getAttribute('javax.servlet.request.X509Certificate')
            if (x509Certificates == None) or (len(x509Certificates) == 0):
                print "Cert. Authenticate for step 2. User not selected any certs"
                context.set("cert_selected", False)
                
                # Return True to inform user how to reset workflow
                return True

            context.set("cert_selected", True)
            
            # Use only first certificate for validation 
            x509Certificate = x509Certificates[0]
            print "Cert. Authenticate for step 2. User selected certificate with DN '%s'" % x509Certificate.getSubjectX500Principal()
            
            # Validate certificates which user selected
            valid = self.validateCertificate(x509Certificate)
            if not valid:
                print "Cert. Authenticate for step 2. Certificate DN '%s' is not valid" % x509Certificate.getSubjectX500Principal()
                context.set("cert_valid", False)
                
                # Return True to inform user how to reset workflow
                return True

            context.set("cert_valid", True)
            context.set("cert_x509", x509Certificate)
            
            # Calculate certificate fingerprint
            x509CertificateFingerprint = self.calculateCertificateFingerprint(x509Certificate)
            context.set("cert_x509_fingerprint", x509CertificateFingerprint)
            print "Cert. Authenticate for step 2. Fingerprint is '%s' of certificate with DN '%s'" % (x509CertificateFingerprint, x509Certificate.getSubjectX500Principal())
            
            # Attempt to find user by certificate fingerprint
            cert_user_external_uid = "cert: %s" % x509CertificateFingerprint
            print "Cert. Authenticate for step 2. Attempting to find user by oxExternalUid attribute value %s" % cert_user_external_uid

            find_user_by_external_uid = userService.getUserByAttribute("oxExternalUid", cert_user_external_uid)
            if find_user_by_external_uid == None:
                print "Cert. Authenticate for step 2. Failed to find user"
                
                if self.map_user_cert:
                    print "Cert. Authenticate for step 2. Storing cert_user_external_uid for step 3"
                    context.set("cert_user_external_uid", cert_user_external_uid)
                    return True
                else:
                    print "Cert. Authenticate for step 2. Mapping cet to user account is not allowed"
                    context.set("cert_count_login_steps", 2)
                    return False

            foundUserName = find_user_by_external_uid.getUserId()
            print "Cert. Authenticate for step 2. foundUserName: " + foundUserName

            logged_in = False
            userService = UserService.instance()
            logged_in = userService.authenticate(foundUserName)
        
            print "Cert. Authenticate for step 2. Setting count steps to 2"
            context.set("cert_count_login_steps", 2)

            return logged_in
        elif (step == 3):
            print "Cert. Authenticate for step 3"

            cert_user_external_uid = self.getSessionAttribute("cert_user_external_uid")
            if cert_user_external_uid == None:
                print "Cert. Authenticate for step 3. cert_user_external_uid is empty"
                return False

            credentials = Identity.instance().getCredentials()
            user_name = credentials.getUsername()
            user_password = credentials.getPassword()

            logged_in = False
            if (StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password)):
                logged_in = userService.authenticate(user_name, user_password)

            if (not logged_in):
                return False

            # Double check just to make sure. We did checking in previous step
            # Check if there is user which has cert_user_external_uid
            # Avoid mapping user cert to more than one IDP account
            find_user_by_external_uid = userService.getUserByAttribute("oxExternalUid", cert_user_external_uid)
            if find_user_by_external_uid == None:
                # Add cert_user_external_uid to user's external GUID list
                find_user_by_external_uid = userService.addUserAttribute(user_name, "oxExternalUid", cert_user_external_uid)
                if find_user_by_external_uid == None:
                    print "Cert. Authenticate for step 3. Failed to update current user"
                    return False

                return True
        
            return True
        else:
            return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        print "Cert. Prepare for step %d" % step

        if (step < 4):
            return True
        else:
            return False

    def getExtraParametersForStep(self, configurationAttributes, step):
        return Arrays.asList("cert_selected", "cert_valid", "cert_x509", "cert_x509_fingerprint", "cert_count_login_steps", "cert_user_external_uid")

    def getCountAuthenticationSteps(self, configurationAttributes):
        cert_count_login_steps = self.getSessionAttribute("cert_count_login_steps")
        if cert_count_login_steps != None:
            return cert_count_login_steps
        else:
            return 3

    def getPageForStep(self, configurationAttributes, step):
        if (step == 1):
            return "/auth/cert/login.xhtml"
        if (step == 2):
            return "/cert-login.xhtml"
        elif (step == 3):
            cert_selected = self.getSessionAttribute("cert_selected")
            if True != cert_selected:
                return "/auth/cert/cert-not-selected.xhtml"

            cert_valid = self.getSessionAttribute("cert_valid")
            if True != cert_valid:
                return "/auth/cert/cert-invalid.xhtml"
            
            return "/login.xhtml"

        return ""

    def logout(self, configurationAttributes, requestParameters):
        return True

    def processBasicAuthentication(self, credentials):
        userService = UserService.instance()

        user_name = credentials.getUsername()
        user_password = credentials.getPassword()

        logged_in = False
        if (StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password)):
            logged_in = userService.authenticate(user_name, user_password)

        if (not logged_in):
            return None

        find_user_by_uid = userService.getUser(user_name)
        if (find_user_by_uid == None):
            print "Cert. Process basic authentication. Failed to find user '%s'" % user_name
            return None
        
        return find_user_by_uid

    def getSessionAttribute(self, attribute_name):
        context = Contexts.getEventContext()

        # Try to get attribute value from Seam event context
        if context.isSet(attribute_name):
            return context.get(attribute_name)
        
        # Try to get attribute from persistent session
        session_attributes = context.get("sessionAttributes")
        if session_attributes.containsKey(attribute_name):
            return session_attributes.get(attribute_name)

        return None

    def calculateCertificateFingerprint(self, x509Certificate):
        print "Cert. Calculating certificate DN '%s' fingerprint" % x509Certificate.getSubjectX500Principal()
        
        publicKey = x509Certificate.getPublicKey()
        
        # Use oxAuth implementation
        fingerprint = FingerprintUtil.getPublicKeyFingerprint(publicKey)
        
        return fingerprint
        

    def validateCertificate(self, x509Certificate):
        print "Cert. Validating certificate with DN '%s'" % x509Certificate.getSubjectX500Principal()
        
        # Validate certificate date
        valid = x509Certificate.checkValidity()
        if not valid:
            print "Cert. Certificate with DN '%s' has expired" % x509Certificate.getSubjectX500Principal()
            return False
        
        # Check if certificate signed by specified CA
        # TODO: Implement validation
        
        
        return True
