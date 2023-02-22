# Janssen Project software is available under the Apache 2.0 License (2004). See http://www.apache.org/licenses/ for full text.
# Copyright (c) 2020, Janssen Project
# Copyright (c) 2019, Tele2

# Author: Jose Gonzalez
# Author: Gasmyr Mougang
# Author: Stefan Andersson

from java.util import Arrays, Date
from java.io import IOException
from java.lang import Enum

from io.jans.service.cdi.util import CdiUtil
from io.jans.as.server.security import Identity
from io.jans.model.custom.script.type.auth import PersonAuthenticationType
from io.jans.as.server.service import AuthenticationService
from io.jans.as.server.service import UserService
from io.jans.as.server.util import ServerUtil
from io.jans.util import ArrayHelper
from io.jans.util import StringHelper
from jakarta.faces.application import FacesMessage
from io.jans.jsf2.message import FacesMessages

from org.jsmpp import InvalidResponseException, PDUException
from org.jsmpp.bean import Alphabet, BindType, ESMClass, GeneralDataCoding, MessageClass, NumberingPlanIndicator, RegisteredDelivery, SMSCDeliveryReceipt, TypeOfNumber
from org.jsmpp.extra import NegativeResponseException, ResponseTimeoutException
from org.jsmpp.session import BindParameter, SMPPSession
from org.jsmpp.util import AbsoluteTimeFormatter, TimeFormatter
import random


class SmppAttributeError(Exception):
    pass


class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis
        self.identity = CdiUtil.bean(Identity)

    def get_and_parse_smpp_config(self, config, attribute, _type = None,  convert = False, optional = False, default_desc = None):
        try:
            value = config.get(attribute).getValue2()
        except:
            if default_desc:
                default_desc = " using default '{}'".format(default_desc)
            else:
                default_desc = ""

            if optional:
                raise SmppAttributeError("SMPP missing optional configuration attribute '{}'{}".format(attribute, default_desc))
            else:
                raise SmppAttributeError("SMPP missing required configuration attribute '{}'".format(attribute))

        if _type and issubclass(_type, Enum):
            try:
                return getattr(_type, value)
            except AttributeError:
                raise SmppAttributeError("SMPP could not find attribute '{}' in {}".format(attribute, _type))

        if convert:
            try:
                value = int(value)
            except AttributeError:
                try:
                    value = int(value, 16)
                except AttributeError:
                    raise SmppAttributeError("SMPP could not parse value '{}' of attribute '{}'".format(value, attribute))

        return value

    def init(self, customScript, configurationAttributes):
        print("SMPP Initialization")

        self.TIME_FORMATTER = AbsoluteTimeFormatter()

        self.SMPP_SERVER = None
        self.SMPP_PORT = None

        self.SYSTEM_ID = None
        self.PASSWORD = None

        # Setup some good defaults for TON, NPI and source (from) address
        # TON (Type of Number), NPI (Number Plan Indicator)
        self.SRC_ADDR_TON = TypeOfNumber.ALPHANUMERIC    # Alphanumeric
        self.SRC_ADDR_NPI = NumberingPlanIndicator.ISDN  # ISDN (E163/E164)
        self.SRC_ADDR = "Janssen OTP"

        # Don't touch these unless you know what your doing, we don't handle number reformatting for
        # any other type than international.
        self.DST_ADDR_TON = TypeOfNumber.INTERNATIONAL   # International
        self.DST_ADDR_NPI = NumberingPlanIndicator.ISDN  # ISDN (E163/E164)

        # Priority flag and data_coding bits
        self.PRIORITY_FLAG = 3  # Very Urgent (ANSI-136), Emergency (IS-95)
        self.DATA_CODING_ALPHABET = Alphabet.ALPHA_DEFAULT  # SMS default alphabet
        self.DATA_CODING_MESSAGE_CLASS = MessageClass.CLASS1  # EM (Mobile Equipment (mobile memory), normal message

        # Required server settings
        try:
            self.SMPP_SERVER = self.get_and_parse_smpp_config(configurationAttributes, "smpp_server")
        except SmppAttributeError as e:
            print(e)

        try:
            self.SMPP_PORT = self.get_and_parse_smpp_config(configurationAttributes, "smpp_port", convert = True)
        except SmppAttributeError as e:
            print(e)

        if None in (self.SMPP_SERVER, self.SMPP_PORT):
            print("SMPP smpp_server and smpp_port is empty, will not enable SMPP service")
            return False

        # Optional system_id and password for bind auth
        try:
            self.SYSTEM_ID = self.get_and_parse_smpp_config(configurationAttributes, "system_id", optional = True)
        except SmppAttributeError as e:
            print(e)

        try:
            self.PASSWORD = self.get_and_parse_smpp_config(configurationAttributes, "password", optional = True)
        except SmppAttributeError as e:
            print(e)

        if None in (self.SYSTEM_ID, self.PASSWORD):
            print("SMPP Authentication disabled")

        # From number and to number settings
        try:
            self.SRC_ADDR_TON = self.get_and_parse_smpp_config(
                configurationAttributes,
                "source_addr_ton",
                _type = TypeOfNumber,
                optional = True,
                default_desc = self.SRC_ADDR_TON
            )
        except SmppAttributeError as e:
            print(e)

        try:
            self.SRC_ADDR_NPI = self.get_and_parse_smpp_config(
                configurationAttributes,
                "source_addr_npi",
                _type = NumberingPlanIndicator,
                optional = True,
                default_desc = self.SRC_ADDR_NPI
            )
        except SmppAttributeError as e:
            print(e)

        try:
            self.SRC_ADDR = self.get_and_parse_smpp_config(
                configurationAttributes,
                "source_addr",
                optional = True,
                default_desc = self.SRC_ADDR
            )
        except SmppAttributeError as e:
            print(e)

        try:
            self.DST_ADDR_TON = self.get_and_parse_smpp_config(
                configurationAttributes,
                "dest_addr_ton",
                _type = TypeOfNumber,
                optional = True,
                default_desc = self.DST_ADDR_TON
            )
        except SmppAttributeError as e:
            print(e)

        try:
            self.DST_ADDR_NPI = self.get_and_parse_smpp_config(
                configurationAttributes,
                "dest_addr_npi",
                _type = NumberingPlanIndicator,
                optional = True,
                default_desc = self.DST_ADDR_NPI
            )
        except SmppAttributeError as e:
            print(e)

        # Priority flag and data coding, don't touch these unless you know what your doing...
        try:
            self.PRIORITY_FLAG = self.get_and_parse_smpp_config(
                configurationAttributes,
                "priority_flag",
                convert = True,
                optional = True,
                default_desc = "3 (Very Urgent, Emergency)"
            )
        except SmppAttributeError as e:
            print(e)

        try:
            self.DATA_CODING_ALPHABET = self.get_and_parse_smpp_config(
                configurationAttributes,
                "data_coding_alphabet",
                _type = Alphabet,
                optional = True,
                default_desc = self.DATA_CODING_ALPHABET
            )
        except SmppAttributeError as e:
            print(e)

        try:
            self.DATA_CODING_MESSAGE_CLASS = self.get_and_parse_smpp_config(
                configurationAttributes,
                "data_coding_alphabet",
                _type = MessageClass,
                optional = True,
                default_desc = self.DATA_CODING_MESSAGE_CLASS
            )
        except SmppAttributeError as e:
            print(e)

        print("SMPP Initialized successfully")
        return True

    def destroy(self, configurationAttributes):
        print("SMPP Destroy")
        print("SMPP Destroyed successfully")
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
        authenticationService = CdiUtil.bean(AuthenticationService)

        facesMessages = CdiUtil.bean(FacesMessages)
        facesMessages.setKeepMessages()

        session_attributes = self.identity.getSessionId().getSessionAttributes()
        form_passcode = ServerUtil.getFirstValue(requestParameters, "passcode")

        print("SMPP form_response_passcode: {}".format(str(form_passcode)))

        if step == 1:
            print("SMPP Step 1 Password Authentication")
            credentials = self.identity.getCredentials()

            user_name = credentials.getUsername()
            user_password = credentials.getPassword()

            logged_in = False
            if StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password):
                logged_in = authenticationService.authenticate(user_name, user_password)

            if not logged_in:
                return False

            # Get the Person's number and generate a code
            foundUser = None
            try:
                foundUser = authenticationService.getAuthenticatedUser()
            except:
                print("SMPP Error retrieving user {} from LDAP".format(user_name))
                return False

            mobile_number = None
            try:
                isVerified = foundUser.getAttribute("phoneNumberVerified")
                if isVerified:
                    mobile_number = foundUser.getAttribute("employeeNumber")
                if not mobile_number:
                    mobile_number = foundUser.getAttribute("mobile")
                if not mobile_number:
                    mobile_number = foundUser.getAttribute("telephoneNumber")
                if not mobile_number:
                    facesMessages.add(FacesMessage.SEVERITY_ERROR, "Failed to determine mobile phone number")
                    print("SMPP Error finding mobile number for user '{}'".format(user_name))
                    return False
            except Exception as e:
                facesMessages.add(FacesMessage.SEVERITY_ERROR, "Failed to determine mobile phone number")
                print("SMPP Error finding mobile number for {}: {}".format(user_name, e))
                return False

            # Generate Random six digit code
            code = random.randint(100000, 999999)

            # Get code and save it in LDAP temporarily with special session entry
            self.identity.setWorkingParameter("code", code)

            self.identity.setWorkingParameter("mobile_number", mobile_number)
            self.identity.getSessionId().getSessionAttributes().put("mobile_number", mobile_number)
            if not self.sendMessage(mobile_number, str(code)):
                facesMessages.add(FacesMessage.SEVERITY_ERROR, "Failed to send message to mobile phone")
                return False

            return True
        elif step == 2:
            # Retrieve the session attribute
            print("SMPP Step 2 SMS/OTP Authentication")
            code = session_attributes.get("code")
            print("SMPP Code: {}".format(str(code)))

            if code is None:
                print("SMPP Failed to find previously sent code")
                return False

            if form_passcode is None:
                print("SMPP Passcode is empty")
                return False

            if len(form_passcode) != 6:
                print("SMPP Passcode from response is not 6 digits: {}".format(form_passcode))
                return False

            if form_passcode == code:
                print("SMPP SUCCESS! User entered the same code!")
                return True

            print("SMPP failed, user entered the wrong code! {} != {}".format(form_passcode, code))
            facesMessages.add(facesMessage.SEVERITY_ERROR, "Incorrect SMS code, please try again.")
            return False

        print("SMPP ERROR: step param not found or != (1|2)")
        return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        if step == 1:
            print("SMPP Prepare for Step 1")
            return True
        elif step == 2:
            print("SMPP Prepare for Step 2")
            return True

        return False

    def getExtraParametersForStep(self, configurationAttributes, step):
        if step == 2:
            return Arrays.asList("code")

        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        return 2

    def getPageForStep(self, configurationAttributes, step):
        if step == 2:
            return "/auth/otp_sms/otp_sms.xhtml"

        return ""

    def getNextStep(self, configurationAttributes, requestParameters, step):
        return -1

    def getLogoutExternalUrl(self, configurationAttributes, requestParameters):
        print "Get external logout URL call"
        return None

    def logout(self, configurationAttributes, requestParameters):
        return True

    def sendMessage(self, number, code):
        status = False
        session = SMPPSession()
        session.setTransactionTimer(10000)

        # We only handle international destination number reformatting.
        # All others may vary by configuration decisions taken on SMPP
        # server side which we have no clue about.
        if self.DST_ADDR_TON == TypeOfNumber.INTERNATIONAL and number.startswith("+"):
            number = number[1:]

        try:
            print("SMPP Connecting")
            reference_id = session.connectAndBind(
                self.SMPP_SERVER,
                self.SMPP_PORT,
                BindParameter(
                    BindType.BIND_TX,
                    self.SYSTEM_ID,
                    self.PASSWORD,
                    None,
                    self.SRC_ADDR_TON,
                    self.SRC_ADDR_NPI,
                    None
                )
            )
            print("SMPP Connected to server with system id {}".format(reference_id))

            try:
                message_id = session.submitShortMessage(
                    "CMT",
                    self.SRC_ADDR_TON,
                    self.SRC_ADDR_NPI,
                    self.SRC_ADDR,
                    self.DST_ADDR_TON,
                    self.DST_ADDR_NPI,
                    number,
                    ESMClass(),
                    0,
                    self.PRIORITY_FLAG,
                    self.TIME_FORMATTER.format(Date()),
                    None,
                    RegisteredDelivery(SMSCDeliveryReceipt.DEFAULT),
                    0,
                    GeneralDataCoding(
                        self.DATA_CODING_ALPHABET,
                        self.DATA_CODING_MESSAGE_CLASS,
                        False
                    ),
                    0,
                    code
                )
                print("SMPP Message '{}' sent to #{} with message id {}".format(code, number, message_id))
                status = True
            except PDUException as e:
                print("SMPP Invalid PDU parameter: {}".format(e))
            except ResponseTimeoutException as e:
                print("SMPP Response timeout: {}".format(e))
            except InvalidResponseException as e:
                print("SMPP Receive invalid response: {}".format(e))
            except NegativeResponseException as e:
                print("SMPP Receive negative response: {}".format(e))
            except IOException as e:
                print("SMPP IO error occured: {}".format(e))
            finally:
                session.unbindAndClose()
        except IOException as e:
            print("SMPP Failed connect and bind to host: {}".format(e))

        return status
