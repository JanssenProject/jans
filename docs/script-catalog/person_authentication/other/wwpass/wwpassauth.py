from io.jans.service.cdi.util import CdiUtil
from io.jans.as.server.security import Identity
from org.gluu.oxauth.model.configuration import AppConfiguration
from io.jans.model.custom.script.type.auth import PersonAuthenticationType
from io.jans.as.server.service import AuthenticationService
from org.xdi.oxauth.service import UserService
from io.jans.util import StringHelper
from io.jans.service import MailService
from org.gluu.oxauth.model.configuration import AppConfiguration

from jakarta.faces.context import FacesContext

from com.google.common.io import BaseEncoding

from urlparse import urlparse

import jarray
from java.util import Arrays
from java.security import SecureRandom
import java

from time import time

from wwpass import WWPassConnection


class PersonAuthentication(PersonAuthenticationType):
    EMAIL_NONCE_EXPIRATION = 600 # seconds

    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis
        self.user = None

    @staticmethod
    def generateNonce(keyLength):
        bytes = jarray.zeros(keyLength, "b")
        secureRandom = SecureRandom()
        secureRandom.nextBytes(bytes)
        return BaseEncoding.base64().omitPadding().encode(bytes)

    def init(self, configurationAttributes):
        print "WWPASS. Initialization"
        self.allow_email_bind = configurationAttributes.get("allow_email_bind").getValue2() if configurationAttributes.containsKey("allow_email_bind") else ''
        self.allow_password_bind = configurationAttributes.get("allow_password_bind").getValue2() if configurationAttributes.containsKey("allow_password_bind") else ''
        self.allow_passkey_bind = configurationAttributes.get("allow_passkey_bind").getValue2() if configurationAttributes.containsKey("allow_passkey_bind") else ''
        self.registration_url = configurationAttributes.get("registration_url").getValue2() if configurationAttributes.containsKey("registration_url") else ''
        self.recovery_url = configurationAttributes.get("recovery_url").getValue2() if configurationAttributes.containsKey("recovery_url") else ''
        self.wwc = WWPassConnection(
            configurationAttributes.get("wwpass_key_file").getValue2(),
            configurationAttributes.get("wwpass_crt_file").getValue2())
        self.use_pin = configurationAttributes.get("use_pin").getValue2() if configurationAttributes.containsKey("use_pin") else None
        self.auth_type = ('p',) if self.use_pin else ()
        self.sso_cookie_domain = '.'.join(urlparse(CdiUtil.bean(AppConfiguration).getBaseEndpoint()).netloc.split('.')[-2:])
        sso_cookie_tags = configurationAttributes.get("sso_cookie_tags").getValue2() if configurationAttributes.containsKey("sso_cookie_tags") else None
        if sso_cookie_tags:
            self.sso_cookie_tags = sso_cookie_tags.split(' ')
        else:
            self.sso_cookie_tags = []
        print "WWPASS. Initialized successfully"
        return True

    def destroy(self, configurationAttributes):
        print "WWPASS. Destroy"
        return True

    def getApiVersion(self):
        return 2

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def tryFirstLogin(self, puid, userService, authenticationService): # Login user that was just registered via external link
        user = userService.getUserByAttribute("oxTrustExternalId", "wwpass:%s"%puid)
        if user and authenticationService.authenticate(user.getUserId()):
                userService.addUserAttribute(user.getUserId(), "oxExternalUid", "wwpass:%s"%puid)
                userService.removeUserAttribute(user.getUserId(),"oxTrustExternalId", "wwpass:%s"%puid)
                return True
        return False

    def getPuid(self, ticket):
        puid = self.wwc.getPUID(ticket, self.auth_type)['puid']
        assert puid #Just in case it's empty or None
        return puid

    def authenticateWithWWPass(self, userService, authenticationService, identity, ticket):
        puid = self.getPuid(ticket)
        user = userService.getUserByAttribute("oxExternalUid", "wwpass:%s"%puid)
        if user:
            if authenticationService.authenticate(user.getUserId()):
                return True
        else:
            if self.registration_url and self.tryFirstLogin(puid, userService, authenticationService):
                return True
            identity.setWorkingParameter("puid", puid)
            identity.setWorkingParameter("ticket", ticket)
            return True
        return False

    def bindWWPass(self, requestParameters, userService, authenticationService, identity, ticket):
        puid = identity.getWorkingParameter("puid")
        email = requestParameters.get('email')[0] if 'email' in requestParameters else None
        if not puid:
            identity.setWorkingParameter("errors", "WWPass login failed")
            return False
        if ticket:
            puid_new = self.getPuid(ticket)
            # Always use the latest PUID when retrying step 2
            identity.setWorkingParameter("puid", puid_new)
            if puid == puid_new:
                # Registering via external web service
                if not self.registration_url:
                    return False
                if self.tryFirstLogin(puid, userService, authenticationService):
                    identity.setWorkingParameter("puid", None)
                    return True
            else:
                if not self.allow_passkey_bind:
                    return False
                # Binding with existing PassKey
                user = userService.getUserByAttribute("oxExternalUid", "wwpass:%s"%puid_new)
                if user:
                    if authenticationService.authenticate(user.getUserId()):
                        userService.addUserAttribute(user.getUserId(), "oxExternalUid", "wwpass:%s"%puid)
                        identity.setWorkingParameter("puid", None)
                        return True
                identity.setWorkingParameter("errors", "Invalid user")
                return False
        elif email:
            # Binding via email
            if not self.allow_email_bind:
                return False
            email = requestParameters.get('email')[0] if 'email' in requestParameters else None
            identity.setWorkingParameter("email", email)
            user = userService.getUserByAttribute('mail', email)
            if not user:
                print("User with email '%s' not found." % email)
                return True
            nonce = self.generateNonce(33)
            mailService = CdiUtil.bean(MailService)
            identity.setWorkingParameter("email_nonce", nonce)
            identity.setWorkingParameter("email_nonce_exp", str(time() + self.EMAIL_NONCE_EXPIRATION))
            subject = "Bind your WWPass Key"
            body = """
To bind your WWPass Key to your account, copy and paste the following
code into "Email code" field in the login form:
%s
If you haven't requested this operation, you can safely disregard this email.
            """
            mailService.sendMail(email, subject, body % nonce)
            return True
        else:
            # Binding via username/password
            if not self.allow_password_bind:
                return False
            puid = identity.getWorkingParameter("puid")
            if not puid:
                return False
            credentials = identity.getCredentials()
            user_name = credentials.getUsername()
            user_password = credentials.getPassword()
            logged_in = False
            if (StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password)):
                try:
                    logged_in = authenticationService.authenticate(user_name, user_password)
                except Exception as e:
                    print(e)
            if not logged_in:
                identity.setWorkingParameter("errors", "Invalid username or password")
                return False
            user = authenticationService.getAuthenticatedUser()
            if not user:
                identity.setWorkingParameter("errors", "Invalid user")
                return False
            userService.addUserAttribute(user_name, "oxExternalUid", "wwpass:%s"%puid)
            identity.setWorkingParameter("puid", None)
            return True
        return False

    def checkEmailNonce(self, requestParameters, userService, authenticationService, identity, ticket):
        # Verify email nonce
        if not self.allow_email_bind:
            identity.setWorkingParameter("email", None)
            return False
        puid = identity.getWorkingParameter("puid")
        if not puid:
            return False
        nonce = requestParameters.get('code')[0] if 'code' in requestParameters else None
        email = identity.getWorkingParameter("email")
        proper_nonce = identity.getWorkingParameter("email_nonce")
        nonce_expiration = float(identity.getWorkingParameter("email_nonce_exp") or 0.0)
        if not nonce or not email or not proper_nonce or not nonce_expiration or nonce_expiration < time() or nonce != proper_nonce:
            print("WWPass. Wrong email verification code", nonce,email,proper_nonce,nonce_expiration, time())
            identity.setWorkingParameter("email", None)
            identity.setWorkingParameter("errors", "Invalid email or verification code")
            return False
        user = userService.getUserByAttribute('mail', email)
        identity.setWorkingParameter("email", None)
        if user:
            if authenticationService.authenticate(user.getUserId()):
                userService.addUserAttribute(user.getUserId(), "oxExternalUid", "wwpass:%s"%identity.getWorkingParameter("puid"))
                identity.setWorkingParameter("puid", None)
                return True
        print("No user")
        return False


    def doAuthenticate(self, step, requestParameters, userService, authenticationService, identity, ticket):
        if step == 1:
            return self.authenticateWithWWPass(userService, authenticationService, identity, ticket)
        elif step == 2:
            return self.bindWWPass(requestParameters, userService, authenticationService, identity, ticket)
        elif step == 3:
            return self.checkEmailNonce(requestParameters, userService, authenticationService, identity, ticket)
        else:
            return False


    def authenticate(self, configurationAttributes, requestParameters, step):
        print("WWPass. Authenticate for step %d" %step)
        authenticationService = CdiUtil.bean(AuthenticationService)
        userService = CdiUtil.bean(UserService)
        ticket = requestParameters.get('wwp_ticket')[0] if 'wwp_ticket' in requestParameters else None
        identity = CdiUtil.bean(Identity)
        identity.setWorkingParameter("errors", "")
        result = self.doAuthenticate(step, requestParameters, userService, authenticationService, identity, ticket)
        if result and self.sso_cookie_tags:
            externalContext = CdiUtil.bean(FacesContext).getExternalContext()
            for tag in self.sso_cookie_tags:
                externalContext.addResponseCookie("sso_magic_%s"%tag, "auth", {"path":"/", "domain":self.sso_cookie_domain, "maxAge": CdiUtil.bean(AppConfiguration).getSessionIdUnusedLifetime()})
        return result

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        identity = CdiUtil.bean(Identity)
        identity.setWorkingParameter("sessionLifetime",CdiUtil.bean(AppConfiguration).getSessionIdUnauthenticatedUnusedLifetime())
        identity.setWorkingParameter("use_pin", bool(self.use_pin))
        print("PrepareForStep %s" % step)
        if (step == 1):
            return True
        elif (step == 2):
            identity.setWorkingParameter("registration_url", self.registration_url)
            identity.setWorkingParameter("recovery_url", self.recovery_url)
            identity.setWorkingParameter("allow_email_bind", self.allow_email_bind)
            identity.setWorkingParameter("allow_password_bind", self.allow_password_bind)
            identity.setWorkingParameter("allow_passkey_bind", self.allow_passkey_bind)
            print("WWPASS. Errors:%s" % identity.getWorkingParameter("errors"))
            return True
        elif (step == 3):
            return True
        return False

    def getExtraParametersForStep(self, configurationAttributes, step):
        if step == 3:
            return Arrays.asList("puid", "email", "email_nonce", "email_nonce_exp", "sessionLifetime")
        return Arrays.asList("puid", "ticket", "use_pin", "errors", "email", "sessionLifetime")

    def getCountAuthenticationSteps(self, configurationAttributes):
        identity = CdiUtil.bean(Identity)
        if not identity.isSetWorkingParameter("puid"):
            return 1
        if not identity.isSetWorkingParameter("email"):
            return 2
        return 3

    def getPageForStep(self, configurationAttributes, step):
        if step == 1:
            return "/auth/wwpass/wwpass.xhtml"
        if step == 2:
            return "/auth/wwpass/wwpassbind.xhtml"
        return "/auth/wwpass/checkemail.xhtml"

    def logout(self, configurationAttributes, requestParameters):
        print("WWPASS. Logout")
        # This is not called. Probably bug in Gluu
        # externalContext = CdiUtil.bean(FacesContext).getExternalContext()
        # externalContext.addResponseCookie("sso_magic", "auth", {"path":"/", "domain":self.sso_cookie_domain, "maxAge": 0})
        return True

    def getNextStep(self, configurationAttributes, requestParameters, step):
        # If user did not pass this step, change the step to previous one
        identity = CdiUtil.bean(Identity)
        puid = identity.getWorkingParameter("puid")
        email = identity.getWorkingParameter("email")
        print ("WWPass getNextStep for step %d, email: %s" % (step, email))
        if puid and not email and step != 1:
            return 2
        return -1
