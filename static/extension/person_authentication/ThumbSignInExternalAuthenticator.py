# Author: ThumbSignIn

from org.gluu.service.cdi.util import CdiUtil
from org.gluu.oxauth.security import Identity
from org.gluu.model.custom.script.type.auth import PersonAuthenticationType
from org.gluu.oxauth.service import AuthenticationService
from org.gluu.util import StringHelper
from org.gluu.oxauth.util import ServerUtil
from com.pramati.ts.thumbsignin_java_sdk import ThumbsigninApiController
from org.json import JSONObject
from org.gluu.oxauth.model.util import Base64Util
from java.lang import String

import java


class PersonAuthentication(PersonAuthenticationType):

    def __init__(self, current_time_millis):
        self.currentTimeMillis = current_time_millis
        self.thumbsigninApiController = ThumbsigninApiController()

    def init(self, configuration_attributes):
        print "ThumbSignIn. Initialization"

        global ts_host
        ts_host = configuration_attributes.get("ts_host").getValue2()
        print "ThumbSignIn. Initialization. Value of ts_host is %s" % ts_host

        global ts_api_key
        ts_api_key = configuration_attributes.get("ts_apiKey").getValue2()
        print "ThumbSignIn. Initialization. Value of ts_api_key is %s" % ts_api_key

        global ts_api_secret
        ts_api_secret = configuration_attributes.get("ts_apiSecret").getValue2()

        global ts_statusPath
        ts_statusPath = "/ts/secure/txn-status/"

        global AUTHENTICATE
        AUTHENTICATE = "authenticate"

        global REGISTER
        REGISTER = "register"

        global TRANSACTION_ID
        TRANSACTION_ID = "transactionId"

        global USER_ID
        USER_ID = "userId"

        global USER_LOGIN_FLOW
        USER_LOGIN_FLOW = "userLoginFlow"

        global THUMBSIGNIN_AUTHENTICATION
        THUMBSIGNIN_AUTHENTICATION = "ThumbSignIn_Authentication"

        global THUMBSIGNIN_REGISTRATION
        THUMBSIGNIN_REGISTRATION = "ThumbSignIn_Registration"

        global THUMBSIGNIN_LOGIN_POST_REGISTRATION
        THUMBSIGNIN_LOGIN_POST_REGISTRATION = "ThumbSignIn_RegistrationSucess"

        global RELYING_PARTY_ID
        RELYING_PARTY_ID = "relyingPartyId"

        global RELYING_PARTY_LOGIN_URL
        RELYING_PARTY_LOGIN_URL = "relyingPartyLoginUrl"

        global TSI_LOGIN_PAGE
        TSI_LOGIN_PAGE = "/auth/thumbsignin/tsLogin.xhtml"

        global TSI_REGISTER_PAGE
        TSI_REGISTER_PAGE = "/auth/thumbsignin/tsRegister.xhtml"

        global TSI_LOGIN_POST_REGISTRATION_PAGE
        TSI_LOGIN_POST_REGISTRATION_PAGE = "/auth/thumbsignin/tsRegistrationSuccess.xhtml"

        print "ThumbSignIn. Initialized successfully"
        return True

    @staticmethod
    def set_relying_party_login_url(identity):
        print "ThumbSignIn. Inside set_relying_party_login_url..."
        session_id = identity.getSessionId()
        session_attribute = session_id.getSessionAttributes()
        state_jwt_token = session_attribute.get("state")
        print "ThumbSignIn. Value of state_jwt_token is %s" % state_jwt_token
        relying_party_login_url = ""
        if (state_jwt_token is None) or ("." not in state_jwt_token):
            print "ThumbSignIn. Value of state parameter is not in the format of JWT Token"
            identity.setWorkingParameter(RELYING_PARTY_LOGIN_URL, relying_party_login_url)
            return None

        state_jwt_token_array = String(state_jwt_token).split("\\.")
        state_jwt_token_payload = state_jwt_token_array[1]
        state_payload_str = String(Base64Util.base64urldecode(state_jwt_token_payload), "UTF-8")
        state_payload_json = JSONObject(state_payload_str)
        print "ThumbSignIn. Value of state JWT token Payload is %s" % state_payload_json
        if state_payload_json.has("additional_claims"):
            additional_claims = state_payload_json.get("additional_claims")
            relying_party_id = additional_claims.get(RELYING_PARTY_ID)
            print "ThumbSignIn. Value of relying_party_id is %s" % relying_party_id
            identity.setWorkingParameter(RELYING_PARTY_ID, relying_party_id)

            if String(relying_party_id).startsWith("google.com"):
                # google.com/a/unphishableenterprise.com
                relying_party_id_array = String(relying_party_id).split("/")
                google_domain = relying_party_id_array[2]
                print "ThumbSignIn. Value of google_domain is %s" % google_domain
                relying_party_login_url = "https://www.google.com/accounts/AccountChooser?hd="+ google_domain + "%26continue=https://apps.google.com/user/hub"
                # elif (String(relying_party_id).startsWith("xyz")):
                # relying_party_login_url = "xyz.com"
            else:
                # If relying_party_login_url is empty, Gluu's default login URL will be used
                relying_party_login_url = ""

        print "ThumbSignIn. Value of relying_party_login_url is %s" % relying_party_login_url
        identity.setWorkingParameter(RELYING_PARTY_LOGIN_URL, relying_party_login_url)
        return None

    def initialize_thumbsignin(self, identity, request_path):
        # Invoking the authenticate/register ThumbSignIn API via the Java SDK
        thumbsignin_response = self.thumbsigninApiController.handleThumbSigninRequest(request_path, ts_api_key, ts_api_secret)
        print "ThumbSignIn. Value of thumbsignin_response is %s" % thumbsignin_response

        thumbsignin_response_json = JSONObject(thumbsignin_response)
        transaction_id = thumbsignin_response_json.get(TRANSACTION_ID)
        status_request_type = "authStatus" if request_path == AUTHENTICATE else "regStatus"
        status_request = status_request_type + "/" + transaction_id
        print "ThumbSignIn. Value of status_request is %s" % status_request

        authorization_header = self.thumbsigninApiController.getAuthorizationHeaderJsonStr(status_request, ts_api_key, ts_api_secret)
        print "ThumbSignIn. Value of authorization_header is %s" % authorization_header
        # {"authHeader":"HmacSHA256 Credential=X,SignedHeaders=accept;content-type;x-ts-date,Signature=X","XTsDate":"X"}
        authorization_header_json = JSONObject(authorization_header)
        auth_header = authorization_header_json.get("authHeader")
        x_ts_date = authorization_header_json.get("XTsDate")

        tsi_response_key = "authenticateResponseJsonStr" if request_path == AUTHENTICATE else "registerResponseJsonStr"
        identity.setWorkingParameter(tsi_response_key, thumbsignin_response)
        identity.setWorkingParameter("authorizationHeader", auth_header)
        identity.setWorkingParameter("xTsDate", x_ts_date)
        return None

    def prepareForStep(self, configuration_attributes, request_parameters, step):
        print "ThumbSignIn. Inside prepareForStep. Step %d" % step
        identity = CdiUtil.bean(Identity)
        authentication_service = CdiUtil.bean(AuthenticationService)

        identity.setWorkingParameter("ts_host", ts_host)
        identity.setWorkingParameter("ts_statusPath", ts_statusPath)

        self.set_relying_party_login_url(identity)

        if step == 1 or step == 3:
            print "ThumbSignIn. Prepare for step 1"
            self.initialize_thumbsignin(identity, AUTHENTICATE)
            return True

        elif step == 2:
            print "ThumbSignIn. Prepare for step 2"
            if identity.isSetWorkingParameter(USER_LOGIN_FLOW):
                user_login_flow = identity.getWorkingParameter(USER_LOGIN_FLOW)
                print "ThumbSignIn. Value of user_login_flow is %s" % user_login_flow
            user = authentication_service.getAuthenticatedUser()
            if user is None:
                print "ThumbSignIn. Prepare for step 2. Failed to determine user name"
                return False
            user_name = user.getUserId()
            print "ThumbSignIn. Prepare for step 2. user_name: " + user_name
            if user_name is None:
                return False
            identity.setWorkingParameter(USER_ID, user_name)
            self.initialize_thumbsignin(identity, REGISTER + "/" + user_name)
            return True
        else:
            return False

    def get_user_id_from_thumbsignin(self, request_parameters):
        transaction_id = ServerUtil.getFirstValue(request_parameters, TRANSACTION_ID)
        print "ThumbSignIn. Value of transaction_id is %s" % transaction_id
        get_user_request = "getUser/" + transaction_id
        print "ThumbSignIn. Value of get_user_request is %s" % get_user_request

        get_user_response = self.thumbsigninApiController.handleThumbSigninRequest(get_user_request, ts_api_key, ts_api_secret)
        print "ThumbSignIn. Value of get_user_response is %s" % get_user_response
        get_user_response_json = JSONObject(get_user_response)
        thumbsignin_user_id = get_user_response_json.get(USER_ID)
        print "ThumbSignIn. Value of thumbsignin_user_id is %s" % thumbsignin_user_id
        return thumbsignin_user_id

    def authenticate(self, configuration_attributes, request_parameters, step):
        print "ThumbSignIn. Inside authenticate. Step %d" % step
        authentication_service = CdiUtil.bean(AuthenticationService)
        identity = CdiUtil.bean(Identity)

        identity.setWorkingParameter("ts_host", ts_host)
        identity.setWorkingParameter("ts_statusPath", ts_statusPath)

        if step == 1 or step == 3:
            print "ThumbSignIn. Authenticate for Step %d" % step

            login_flow = ServerUtil.getFirstValue(request_parameters, "login_flow")
            print "ThumbSignIn. Value of login_flow parameter is %s" % login_flow

            # Logic for ThumbSignIn Authentication Flow (Either step 1 or step 3)
            if login_flow == THUMBSIGNIN_AUTHENTICATION or login_flow == THUMBSIGNIN_LOGIN_POST_REGISTRATION:
                identity.setWorkingParameter(USER_LOGIN_FLOW, login_flow)
                print "ThumbSignIn. Value of userLoginFlow is %s" % identity.getWorkingParameter(USER_LOGIN_FLOW)
                logged_in_status = authentication_service.authenticate(self.get_user_id_from_thumbsignin(request_parameters))
                print "ThumbSignIn. logged_in status : %r" % logged_in_status
                return logged_in_status

            # Logic for traditional login flow (step 1)
            print "ThumbSignIn. User credentials login flow"
            identity.setWorkingParameter(USER_LOGIN_FLOW, THUMBSIGNIN_REGISTRATION)
            print "ThumbSignIn. Value of userLoginFlow is %s" % identity.getWorkingParameter(USER_LOGIN_FLOW)
            logged_in = self.authenticate_user_credentials(identity, authentication_service)
            print "ThumbSignIn. Status of User Credentials based Authentication : %r" % logged_in

            # When the traditional login fails, reinitialize the ThumbSignIn data before sending error response to UI
            if not logged_in:
                self.initialize_thumbsignin(identity, AUTHENTICATE)
                return False

            print "ThumbSignIn. Authenticate successful for step %d" % step
            return True

        elif step == 2:
            print "ThumbSignIn. Registration flow (step 2)"
            self.verify_user_login_flow(identity)

            user = self.get_authenticated_user_from_gluu(authentication_service)
            if user is None:
                print "ThumbSignIn. Registration flow (step 2). Failed to determine user name"
                return False

            user_name = user.getUserId()
            print "ThumbSignIn. Registration flow (step 2) successful. user_name: %s" % user_name
            return True

        else:
            return False

    def authenticate_user_credentials(self, identity, authentication_service):
        credentials = identity.getCredentials()
        user_name = credentials.getUsername()
        user_password = credentials.getPassword()
        print "ThumbSignIn. user_name: " + user_name
        logged_in = False
        if StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password):
            logged_in = self.authenticate_user_in_gluu_ldap(authentication_service, user_name, user_password)
        return logged_in

    @staticmethod
    def authenticate_user_in_gluu_ldap(authentication_service, user_name, user_password):
        return authentication_service.authenticate(user_name, user_password)

    @staticmethod
    def get_authenticated_user_from_gluu(authentication_service):
        return authentication_service.getAuthenticatedUser()

    @staticmethod
    def verify_user_login_flow(identity):
        if identity.isSetWorkingParameter(USER_LOGIN_FLOW):
            user_login_flow = identity.getWorkingParameter(USER_LOGIN_FLOW)
            print "ThumbSignIn. Value of user_login_flow is %s" % user_login_flow
        else:
            identity.setWorkingParameter(USER_LOGIN_FLOW, THUMBSIGNIN_REGISTRATION)
            print "ThumbSignIn. Setting the value of user_login_flow to %s" % identity.getWorkingParameter(USER_LOGIN_FLOW)

    def getExtraParametersForStep(self, configuration_attributes, step):
        return None

    def getCountAuthenticationSteps(self, configuration_attributes):
        print "ThumbSignIn. Inside getCountAuthenticationSteps.."
        identity = CdiUtil.bean(Identity)

        user_login_flow = identity.getWorkingParameter(USER_LOGIN_FLOW)
        print "ThumbSignIn. Value of userLoginFlow is %s" % user_login_flow
        if user_login_flow == THUMBSIGNIN_AUTHENTICATION:
            print "ThumbSignIn. Total Authentication Steps is: 1"
            return 1
        print "ThumbSignIn. Total Authentication Steps is: 3"
        return 3

    def getPageForStep(self, configuration_attributes, step):
        print "ThumbSignIn. Inside getPageForStep. Step %d" % step
        if step == 3:
            return TSI_LOGIN_POST_REGISTRATION_PAGE
        thumbsignin_page = TSI_REGISTER_PAGE if step == 2 else TSI_LOGIN_PAGE
        return thumbsignin_page

    def destroy(self, configurationAttributes):
        print "ThumbSignIn. Destroy"
        return True

    def getApiVersion(self):
        return 1

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def logout(self, configurationAttributes, requestParameters):
        return True
