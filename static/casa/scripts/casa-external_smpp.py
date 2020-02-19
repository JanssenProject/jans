# Author: Stefan Andersson

from java.util import Arrays, Date
from java.io import IOException
from java.lang import Enum

from javax.faces.application import FacesMessage

from org.gluu.jsf2.message import FacesMessages
from org.gluu.oxauth.security import Identity
from org.gluu.oxauth.service import UserService, AuthenticationService
from org.gluu.oxauth.util import ServerUtil
from org.gluu.model.custom.script.type.auth import PersonAuthenticationType
from org.gluu.service.cdi.util import CdiUtil
from org.gluu.util import StringHelper, ArrayHelper

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

    def init(self, configurationAttributes):
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
        self.SRC_ADDR = "Gluu OTP"

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
        print("SMPP Destroyed successfully")
        return True

    def getApiVersion(self):
        return 1

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def authenticate(self, configurationAttributes, requestParameters, step):
        print("SMPP Authenticate for step {}".format(step))

        identity = CdiUtil.bean(Identity)
        authenticationService = CdiUtil.bean(AuthenticationService)
        user = authenticationService.getAuthenticatedUser()

        if step == 1:
            if not user:
                credentials = identity.getCredentials()
                user_name = credentials.getUsername()
                user_password = credentials.getPassword()

                if StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password):
                    authenticationService.authenticate(user_name, user_password)
                    user = authenticationService.getAuthenticatedUser()

            if not user:
                return False

            numbers = self.getNumbers(user)
            if not numbers:
                return False
            else:
                # Generate Random six digit code
                code = random.randint(100000, 999999)
                identity.setWorkingParameter("randCode", code)

                if len(numbers) == 1:
                    return self.sendMessage(numbers[0], str(code))
                else:
                    chopped = [number[-4:] for number in numbers]

                    # converting to comma-separated list (identity does not remember lists)
                    identity.setWorkingParameter("numbers", ",".join(numbers))
                    identity.setWorkingParameter("choppedNos", ",".join(chopped))
                    return True
        else:
            if not user:
                return False

            session_attributes = identity.getSessionId().getSessionAttributes()
            code = session_attributes.get("randCode")
            numbers = session_attributes.get("numbers")

            if step == 2 and numbers:
                # Means that the selection number page was used
                idx = ServerUtil.getFirstValue(requestParameters, "OtpSmsloginForm:indexOfNumber")
                if idx and code:
                    number = numbers.split(",")[int(idx)]
                    return self.sendMessage(number, str(code))
                else:
                    return False

            form_passcode = ServerUtil.getFirstValue(requestParameters, "passcode")
            if form_passcode and code == form_passcode:
                print("SMPP authenticate. 6-digit code matches with code sent via SMS")
                return True
            else:
                facesMessages = CdiUtil.bean(FacesMessages)
                facesMessages.setKeepMessages()
                facesMessages.clear()
                facesMessages.add(FacesMessage.SEVERITY_ERROR, "Wrong code entered")
                return False

    def getNumbers(self, user):
        numbers = set()

        tmp = user.getAttributeValues("mobile")
        if tmp:
            for t in tmp:
                numbers.add(t)

        return list(numbers)

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        print("SMPP Prepare for Step {}".format(step))
        return True

    def getExtraParametersForStep(self, configurationAttributes, step):
        if step > 1:
            return Arrays.asList("randCode", "numbers", "choppedNos")
        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        if not CdiUtil.bean(Identity).getWorkingParameter("numbers"):
            return 2
        else:
            return 3

    def getPageForStep(self, configurationAttributes, step):
        print("SMPP getPageForStep called {}".format(step))
        print("SMPP Numbers are {}".format(CdiUtil.bean(Identity).getWorkingParameter("numbers")))

        def_page = "/casa/otp_sms.xhtml"
        if step == 2:
            if not CdiUtil.bean(Identity).getWorkingParameter("numbers"):
                return def_page
            else:
                return "/casa/otp_sms_prompt.xhtml"
        elif step == 3:
            return def_page

        return ""

    def logout(self, configurationAttributes, requestParameters):
        return True

    def hasEnrollments(self, configurationAttributes, user):
        return len(self.getNumbers(user)) > 0

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
                    code + " is your passcode to access your account"
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
