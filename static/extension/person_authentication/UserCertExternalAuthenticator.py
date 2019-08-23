#
# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2016, Gluu
#
# Author: Yuriy Movchan
#

from org.gluu.service.cdi.util import CdiUtil
from org.gluu.model.custom.script.type.auth import PersonAuthenticationType
from javax.faces.context import FacesContext
from org.gluu.oxauth.security import Identity
from org.gluu.oxauth.service import UserService, AuthenticationService
from org.gluu.util import StringHelper
from org.gluu.oxauth.util import ServerUtil
from org.gluu.oxauth.service import EncryptionService
from java.util import Arrays
from org.gluu.oxauth.cert.fingerprint import FingerprintHelper
from org.gluu.oxauth.cert.validation import GenericCertificateVerifier, PathCertificateVerifier, OCSPCertificateVerifier, CRLCertificateVerifier
from org.gluu.oxauth.cert.validation.model import ValidationStatus
from org.gluu.oxauth.util import CertUtil
from org.gluu.oxauth.service.net import HttpService
from org.apache.http.params import CoreConnectionPNames

import sys
import base64
import urllib

import java
import json

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        print "Cert. Initialization"

        if not (configurationAttributes.containsKey("chain_cert_file_path")):
            print "Cert. Initialization. Property chain_cert_file_path is mandatory"
            return False

        if not (configurationAttributes.containsKey("map_user_cert")):
            print "Cert. Initialization. Property map_user_cert is mandatory"
            return False

        chain_cert_file_path = configurationAttributes.get("chain_cert_file_path").getValue2()

        self.chain_certs = CertUtil.loadX509CertificateFromFile(chain_cert_file_path)
        if self.chain_certs == None:
            print "Cert. Initialization. Failed to load chain certificates from '%s'" % chain_cert_file_path
            return False

        print "Cert. Initialization. Loaded '%d' chain certificates" % self.chain_certs.size()
        
        crl_max_response_size = 5 * 1024 * 1024  # 10Mb
        if configurationAttributes.containsKey("crl_max_response_size"):
            crl_max_response_size = StringHelper.toInteger(configurationAttributes.get("crl_max_response_size").getValue2(), crl_max_response_size)
            print "Cert. Initialization. CRL max response size is '%d'" % crl_max_response_size

        # Define array to order methods correctly
        self.validator_types = [ 'generic', 'path', 'ocsp', 'crl']
        self.validators = { 'generic' : [GenericCertificateVerifier(), False],
                            'path' : [PathCertificateVerifier(False), False],
                            'ocsp' : [OCSPCertificateVerifier(), False],
                            'crl' : [CRLCertificateVerifier(crl_max_response_size), False] }

        for type in self.validator_types:
            validator_param_name = "use_%s_validator" % type
            if configurationAttributes.containsKey(validator_param_name):
                validator_status = StringHelper.toBoolean(configurationAttributes.get(validator_param_name).getValue2(), False)
                self.validators[type][1] = validator_status

            print "Cert. Initialization. Validation method '%s' status: '%s'" % (type, self.validators[type][1])

        self.map_user_cert = StringHelper.toBoolean(configurationAttributes.get("map_user_cert").getValue2(), False)
        print "Cert. Initialization. map_user_cert: '%s'" % self.map_user_cert

        self.enabled_recaptcha = self.initRecaptcha(configurationAttributes)
        print "Cert. Initialization. enabled_recaptcha: '%s'" % self.enabled_recaptcha

        print "Cert. Initialized successfully"

        return True   

    def destroy(self, configurationAttributes):
        print "Cert. Destroy"

        for type in self.validator_types:
            self.validators[type][0].destroy()

        print "Cert. Destroyed successfully"

        return True

    def getApiVersion(self):
        return 1

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def authenticate(self, configurationAttributes, requestParameters, step):
        identity = CdiUtil.bean(Identity)
        credentials = identity.getCredentials()

        user_name = credentials.getUsername()

        userService = CdiUtil.bean(UserService)
        authenticationService = CdiUtil.bean(AuthenticationService)

        if step == 1:
            print "Cert. Authenticate for step 1"
            login_button = ServerUtil.getFirstValue(requestParameters, "loginForm:loginButton")
            if StringHelper.isEmpty(login_button):
                print "Cert. Authenticate for step 1. Form were submitted incorrectly"
                return False
            if self.enabled_recaptcha:
                print "Cert. Authenticate for step 1. Validating recaptcha response"
                recaptcha_response = ServerUtil.getFirstValue(requestParameters, "g-recaptcha-response")

                recaptcha_result = self.validateRecaptcha(recaptcha_response)
                print "Cert. Authenticate for step 1. recaptcha_result: '%s'" % recaptcha_result
                
                return recaptcha_result

            return True
        elif step == 2:
            print "Cert. Authenticate for step 2"

            # Validate if user selected certificate
            cert_x509 = self.getSessionAttribute("cert_x509")
            if cert_x509 == None:
                print "Cert. Authenticate for step 2. User not selected any certs"
                identity.setWorkingParameter("cert_selected", False)
                    
                # Return True to inform user how to reset workflow
                return True
            else:
                identity.setWorkingParameter("cert_selected", True)
                x509Certificate = self.certFromString(cert_x509)

            subjectX500Principal = x509Certificate.getSubjectX500Principal()
            print "Cert. Authenticate for step 2. User selected certificate with DN '%s'" % subjectX500Principal
            
            # Validate certificates which user selected
            valid = self.validateCertificate(x509Certificate)
            if not valid:
                print "Cert. Authenticate for step 2. Certificate DN '%s' is not valid" % subjectX500Principal
                identity.setWorkingParameter("cert_valid", False)
                
                # Return True to inform user how to reset workflow
                return True

            identity.setWorkingParameter("cert_valid", True)
            
            # Calculate certificate fingerprint
            x509CertificateFingerprint = self.calculateCertificateFingerprint(x509Certificate)
            identity.setWorkingParameter("cert_x509_fingerprint", x509CertificateFingerprint)
            print "Cert. Authenticate for step 2. Fingerprint is '%s' of certificate with DN '%s'" % (x509CertificateFingerprint, subjectX500Principal)
            
            # Attempt to find user by certificate fingerprint
            cert_user_external_uid = "cert:%s" % x509CertificateFingerprint
            print "Cert. Authenticate for step 2. Attempting to find user by oxExternalUid attribute value %s" % cert_user_external_uid

            find_user_by_external_uid = userService.getUserByAttribute("oxExternalUid", cert_user_external_uid)
            if find_user_by_external_uid == None:
                print "Cert. Authenticate for step 2. Failed to find user"
                
                if self.map_user_cert:
                    print "Cert. Authenticate for step 2. Storing cert_user_external_uid for step 3"
                    identity.setWorkingParameter("cert_user_external_uid", cert_user_external_uid)
                    return True
                else:
                    print "Cert. Authenticate for step 2. Mapping cert to user account is not allowed"
                    identity.setWorkingParameter("cert_count_login_steps", 2)
                    return False

            foundUserName = find_user_by_external_uid.getUserId()
            print "Cert. Authenticate for step 2. foundUserName: " + foundUserName

            logged_in = False
            userService = CdiUtil.bean(UserService)
            logged_in = authenticationService.authenticate(foundUserName)
        
            print "Cert. Authenticate for step 2. Setting count steps to 2"
            identity.setWorkingParameter("cert_count_login_steps", 2)

            return logged_in
        elif step == 3:
            print "Cert. Authenticate for step 3"

            cert_user_external_uid = self.getSessionAttribute("cert_user_external_uid")
            if cert_user_external_uid == None:
                print "Cert. Authenticate for step 3. cert_user_external_uid is empty"
                return False

            user_password = credentials.getPassword()

            logged_in = False
            if (StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password)):
                logged_in = authenticationService.authenticate(user_name, user_password)

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
        identity = CdiUtil.bean(Identity)
        
        if step == 1:
            if self.enabled_recaptcha:
                identity.setWorkingParameter("recaptcha_site_key", self.recaptcha_creds['site_key'])
        elif step == 2:
            # Store certificate in session
            facesContext = CdiUtil.bean(FacesContext)
            externalContext = facesContext.getExternalContext()
            request = externalContext.getRequest()

            # Try to get certificate from header X-ClientCert
            clientCertificate = externalContext.getRequestHeaderMap().get("X-ClientCert")
            if clientCertificate != None:
                x509Certificate = self.certFromPemString(clientCertificate)
                identity.setWorkingParameter("cert_x509",  self.certToString(x509Certificate))
                print "Cert. Prepare for step 2. Storing user certificate obtained from 'X-ClientCert' header"
                return True

            # Try to get certificate from attribute javax.servlet.request.X509Certificate
            x509Certificates = request.getAttribute('javax.servlet.request.X509Certificate')
            if (x509Certificates != None) and (len(x509Certificates) > 0):
                identity.setWorkingParameter("cert_x509", self.certToString(x509Certificates[0]))
                print "Cert. Prepare for step 2. Storing user certificate obtained from 'javax.servlet.request.X509Certificate' attribute"
                return True

        if step < 4:
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
        if step == 1:
            return "/auth/cert/login.xhtml"
        if step == 2:
            return "/auth/cert/cert-login.xhtml"
        elif step == 3:
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
        userService = CdiUtil.bean(UserService)
        authenticationService = CdiUtil.bean(AuthenticationService)

        user_name = credentials.getUsername()
        user_password = credentials.getPassword()

        logged_in = False
        if (StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password)):
            logged_in = authenticationService.authenticate(user_name, user_password)

        if (not logged_in):
            return None

        find_user_by_uid = authenticationService.getAuthenticatedUser()
        if (find_user_by_uid == None):
            print "Cert. Process basic authentication. Failed to find user '%s'" % user_name
            return None
        
        return find_user_by_uid

    def getSessionAttribute(self, attribute_name):
        identity = CdiUtil.bean(Identity)

        # Try to get attribute value from Seam event context
        if identity.isSetWorkingParameter(attribute_name):
            return identity.getWorkingParameter(attribute_name)
        
        # Try to get attribute from persistent session
        session_id = identity.getSessionId()
        if session_id == None:
            return None

        session_attributes = session_id.getSessionAttributes()
        if session_attributes == None:
            return None

        if session_attributes.containsKey(attribute_name):
            return session_attributes.get(attribute_name)

        return None

    def calculateCertificateFingerprint(self, x509Certificate):
        print "Cert. Calculate fingerprint for certificate DN '%s'" % x509Certificate.getSubjectX500Principal()
        
        publicKey = x509Certificate.getPublicKey()
        
        # Use oxAuth implementation
        fingerprint = FingerprintHelper.getPublicKeySshFingerprint(publicKey)
        
        return fingerprint      

    def validateCertificate(self, x509Certificate):
        subjectX500Principal = x509Certificate.getSubjectX500Principal()

        print "Cert. Validating certificate with DN '%s'" % subjectX500Principal
        
        validation_date = java.util.Date()

        for type in self.validator_types:
            if self.validators[type][1]:
                result = self.validators[type][0].validate(x509Certificate, self.chain_certs, validation_date)
                print "Cert. Validate certificate: '%s'. Validation method '%s' result: '%s'" % (subjectX500Principal, type, result)
                
                if (result.getValidity() != ValidationStatus.CertificateValidity.VALID):
                    print "Cert. Certificate: '%s' is invalid" % subjectX500Principal
                    return False
        
        return True

    def certToString(self, x509Certificate):
        if x509Certificate == None:
            return None
        return base64.b64encode(x509Certificate.getEncoded())

    def certFromString(self, x509CertificateEncoded):
        x509CertificateDecoded = base64.b64decode(x509CertificateEncoded)
        return CertUtil.x509CertificateFromBytes(x509CertificateDecoded)

    def certFromPemString(self, pemCertificate):
        x509CertificateEncoded = pemCertificate.replace("-----BEGIN CERTIFICATE-----", "").replace("-----END CERTIFICATE-----", "").strip()
        return self.certFromString(x509CertificateEncoded)

    def initRecaptcha(self, configurationAttributes):
        print "Cert. Initialize recaptcha"
        if not configurationAttributes.containsKey("credentials_file"):
            return False

        cert_creds_file = configurationAttributes.get("credentials_file").getValue2()

        # Load credentials from file
        f = open(cert_creds_file, 'r')
        try:
            creds = json.loads(f.read())
        except:
            print "Cert. Initialize recaptcha. Failed to load credentials from file: %s" % cert_creds_file
            return False
        finally:
            f.close()
        
        try:
            recaptcha_creds = creds["recaptcha"]
        except:
            print "Cert. Initialize recaptcha. Invalid credentials file '%s' format:" % cert_creds_file
            return False
        
        self.recaptcha_creds = None
        if recaptcha_creds["enabled"]:
            print "Cert. Initialize recaptcha. Recaptcha is enabled"

            encryptionService = CdiUtil.bean(EncryptionService)

            site_key = recaptcha_creds["site_key"]
            secret_key = recaptcha_creds["secret_key"]

            try:
                site_key = encryptionService.decrypt(site_key)
            except:
                # Ignore exception. Value is not encrypted
                print "Cert. Initialize recaptcha. Assuming that 'site_key' in not encrypted"

            try:
                secret_key = encryptionService.decrypt(secret_key)
            except:
                # Ignore exception. Value is not encrypted
                print "Cert. Initialize recaptcha. Assuming that 'secret_key' in not encrypted"

            
            self.recaptcha_creds = { 'site_key' : site_key, "secret_key" : secret_key }
            print "Cert. Initialize recaptcha. Recaptcha is configured correctly"

            return True
        else:
            print "Cert. Initialize recaptcha. Recaptcha is disabled"

        return False

    def validateRecaptcha(self, recaptcha_response):
        print "Cert. Validate recaptcha response"

        facesContext = CdiUtil.bean(FacesContext)
        request = facesContext.getExternalContext().getRequest()

        remoteip = ServerUtil.getIpAddress(request)
        print "Cert. Validate recaptcha response. remoteip: '%s'" % remoteip

        httpService = CdiUtil.bean(HttpService)

        http_client = httpService.getHttpsClient()
        http_client_params = http_client.getParams()
        http_client_params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 15 * 1000)
        
        recaptcha_validation_url = "https://www.google.com/recaptcha/api/siteverify"
        recaptcha_validation_request = urllib.urlencode({ "secret" : self.recaptcha_creds['secret_key'], "response" : recaptcha_response, "remoteip" : remoteip })
        recaptcha_validation_headers = { "Content-type" : "application/x-www-form-urlencoded", "Accept" : "application/json" }

        try:
            http_service_response = httpService.executePost(http_client, recaptcha_validation_url, None, recaptcha_validation_headers, recaptcha_validation_request)
            http_response = http_service_response.getHttpResponse()
        except:
            print "Cert. Validate recaptcha response. Exception: ", sys.exc_info()[1]
            return False

        try:
            if not httpService.isResponseStastusCodeOk(http_response):
                print "Cert. Validate recaptcha response. Get invalid response from validation server: ", str(http_response.getStatusLine().getStatusCode())
                httpService.consume(http_response)
                return False
    
            response_bytes = httpService.getResponseContent(http_response)
            response_string = httpService.convertEntityToString(response_bytes)
            httpService.consume(http_response)
        finally:
            http_service_response.closeConnection()

        if response_string == None:
            print "Cert. Validate recaptcha response. Get empty response from validation server"
            return False
        
        response = json.loads(response_string)
        
        return response["success"]
