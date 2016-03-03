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

        #if not (configurationAttributes.containsKey("authentication_mode")):
        #    print "Cert. Initialization. Property authentication_mode is mandatory"
        #    return False

        #authentication_mode = configurationAttributes.get("authentication_mode").getValue2()
        #if StringHelper.isEmpty(authentication_mode):
        #    print "Cert. Initialization. Failed to determine authentication_mode. authentication_mode configuration parameter is empty"
        #    return False
        
        #self.oneStep = StringHelper.equalsIgnoreCase(authentication_mode, "one_step")
        #self.twoStep = StringHelper.equalsIgnoreCase(authentication_mode, "two_step")

        #if not (self.oneStep or self.twoStep):
        #    print "Cert. Initialization. Valid authentication_mode values are one_step and two_step"
        #    return False
        
        #print "Cert. Initialized successfully. oneStep: '%s', twoStep: '%s'" % (self.oneStep, self.twoStep)
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
                print "Cert. Authenticate for step 1. Form was submitted incorrectly"
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
            
            # Attempt to find user by certificate's fingerprint
            if True:
                print "Cert. Authenticate for step 2. Find user '%s' by fingerprint '%s'" % (None, None)
                # Authenticate user
                # Set 2 authentication steps
            
            
            return True
        elif (step == 3):
            print "Cert. Authenticate for step 3"
            # Map user to selected certificate

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
        return Arrays.asList("cert_selected", "cert_valid", "cert_x509")

    def getCountAuthenticationSteps(self, configurationAttributes):
        context = Contexts.getEventContext()
        if (context.isSet("cert_count_login_steps")):
            return context.get("cert_count_login_steps")
        else:
            return 3

    def getPageForStep(self, configurationAttributes, step):
        if (step == 1):
            return "/auth/cert/login.xhtml"
        if (step == 2):
            return "/cert-login.xhtml"
        elif (step == 3):
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

    def validateCertificate(self, x509Certificate):
        print "Cert. Certificate with DN '%s' is valid" % x509Certificate.getSubjectX500Principal()
        
        # TODO: Implement validation
        
        return True

    def calculateCertificateFingerprint(self, x509Certificate):
        print "Cert. Calculating certificate DN '%s' fingerprint" % x509Certificate.getSubjectX500Principal()
        
        publicKey = x509Certificate.getPublicKey()
        
        # Use oxAuth implementation
        fingerprint = FingerprintUtil.getPublicKeyFingerprint(publicKey)
        
        return fingerprint
        
