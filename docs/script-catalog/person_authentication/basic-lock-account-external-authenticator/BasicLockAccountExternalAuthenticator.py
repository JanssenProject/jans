# Janssen Project software is available under the Apache 2.0 License (2004). See http://www.apache.org/licenses/ for full text.
# Copyright (c) 2020, Janssen Project
#
# Author: Yuriy Movchan
# Author: Gasmyr Mougang
#

from io.jans.service.cdi.util import CdiUtil
from io.jans.as.server.security import Identity
from io.jans.model.custom.script.type.auth import PersonAuthenticationType
from io.jans.as.server.service import AuthenticationService
from io.jans.as.server.service import UserService
from io.jans.util import StringHelper
from io.jans.model import GluuStatus
from io.jans.orm.exception import AuthenticationException
from jakarta.faces.application import FacesMessage
from io.jans.jsf2.message import FacesMessages
from java.lang import System

# Written to the invalid-login count attribute by lockUser: only inactive users carrying it are unlocked
LOCK_MARKER_PREFIX = "locked:"
# Max gap between the marker time and the updatedAt that updateUser stamps on the lock write
LOCK_WRITE_SLACK_MILLIS = 5000

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript,  configurationAttributes):
        print "Basic (lock account). Initialization"

        self.invalidLoginCountAttribute = "jansCountInvalidLogin"
        if configurationAttributes.containsKey("invalid_login_count_attribute"):
            self.invalidLoginCountAttribute = configurationAttributes.get("invalid_login_count_attribute").getValue2()
        else:
            print "Basic (lock account). Initialization. Using default attribute"

        self.maximumInvalidLoginAttemps = 3
        if configurationAttributes.containsKey("maximum_invalid_login_attemps"):
            self.maximumInvalidLoginAttemps = StringHelper.toInteger(configurationAttributes.get("maximum_invalid_login_attemps").getValue2())
        else:
            print "Basic (lock account). Initialization. Using default number attempts"

        self.lockExpirationTime = 180
        if configurationAttributes.containsKey("lock_expiration_time"):
            self.lockExpirationTime = StringHelper.toInteger(configurationAttributes.get("lock_expiration_time").getValue2())
        else:
            print "Basic (lock account). Initialization. Using default lock expiration time"


        print "Basic (lock account). Initialized successfully. invalid_login_count_attribute: '%s', maximum_invalid_login_attemps: '%s', lock_expiration_time: '%s'" % (self.invalidLoginCountAttribute, self.maximumInvalidLoginAttemps, self.lockExpirationTime)

        return True   

    def destroy(self, configurationAttributes):
        print "Basic (lock account). Destroy"
        print "Basic (lock account). Destroyed successfully"
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
        authenticationService = CdiUtil.bean(AuthenticationService)

        if step == 1:
            print "Basic (lock account). Authenticate for step 1"
            facesMessages = CdiUtil.bean(FacesMessages)
            facesMessages.setKeepMessages()
            identity = CdiUtil.bean(Identity)
            credentials = identity.getCredentials()
            user_name = credentials.getUsername()
            user_password = credentials.getPassword()

            # user names are logged below; reject line breaks to keep log records intact
            if user_name is not None and ("\r" in user_name or "\n" in user_name):
                return False

            logged_in = False
            if (StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password)):
                try:
                    logged_in = authenticationService.authenticate(user_name, user_password)
                except AuthenticationException:
                    print "Basic (lock account). Authenticate. Failed to authenticate user '%s'" % user_name

            if logged_in:
                self.setInvalidLoginCount(user_name, StringHelper.toString(0))
            else:
                countInvalidLoginArributeValue = self.getUserAttributeValue(user_name, self.invalidLoginCountAttribute)
                userSatus = self.getUserStatus(user_name)
                print "Current user '%s' status is '%s'" % ( user_name, userSatus )

                if userSatus == "inactive":
                    locked_at = self.getLockTime(countInvalidLoginArributeValue)
                    if locked_at is None:
                        print "Basic (lock account). User '%s' is inactive and was not locked by this script" % user_name
                        return False

                    locked_for = (System.currentTimeMillis() - locked_at) / 1000
                    if locked_for < self.lockExpirationTime:
                        print "Basic (lock account). User '%s' is locked for '%s' more seconds" % ( user_name, self.lockExpirationTime - locked_for )
                        return False

                    print "Basic (lock account). Unlocking user '%s' after lock expiration" % user_name
                    if not self.unLockUser(user_name, countInvalidLoginArributeValue):
                        return False

                    logged_in = authenticationService.authenticate(user_name, user_password)
                    if not logged_in:
                        self.setInvalidLoginCount(user_name, StringHelper.toString(1))
                        if self.maximumInvalidLoginAttemps == 1:
                            self.lockUser(user_name)
                    return logged_in

                countInvalidLogin = StringHelper.toInteger(countInvalidLoginArributeValue, 0)

                if countInvalidLogin < self.maximumInvalidLoginAttemps:
                    countInvalidLogin = countInvalidLogin + 1
                    remainingAttempts = self.maximumInvalidLoginAttemps - countInvalidLogin

                    print "Remaining login count attempts '%s' for user '%s'" % ( remainingAttempts, user_name )

                    self.setInvalidLoginCount(user_name, StringHelper.toString(countInvalidLogin))
                    if remainingAttempts > 0 and userSatus == "active":
                        facesMessages.add(FacesMessage.SEVERITY_INFO, StringHelper.toString(remainingAttempts)+" more attempt(s) before account is LOCKED!")

                if (countInvalidLogin >= self.maximumInvalidLoginAttemps) and ((userSatus is None) or (userSatus == "active")):
                    print "Basic (lock account). Locking '%s' for '%s' seconds" % ( user_name, self.lockExpirationTime)
                    self.lockUser(user_name)
                    return False

            return logged_in
        else:
            return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        if step == 1:
            print "Basic (lock account). Prepare for Step 1"
            return True
        else:
            return False

    def getExtraParametersForStep(self, configurationAttributes, step):
        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        return 1

    def getPageForStep(self, configurationAttributes, step):
        return ""
        
    def getNextStep(self, configurationAttributes, requestParameters, step):
        return -1

    def getLogoutExternalUrl(self, configurationAttributes, requestParameters):
        print "Get external logout URL call"
        return None

    def logout(self, configurationAttributes, requestParameters):
        return True

    # jansStatus is the mapped status field, not a custom attribute
    def getUserStatus(self, user_name):
        find_user_by_uid = CdiUtil.bean(UserService).getUser(user_name)
        if find_user_by_uid is None or find_user_by_uid.getStatus() is None:
            return None
        return find_user_by_uid.getStatus().getValue()

    def getUserAttributeValue(self, user_name, attribute_name):
        if StringHelper.isEmpty(user_name):
            return None

        userService = CdiUtil.bean(UserService)

        find_user_by_uid = userService.getUser(user_name, attribute_name)
        if find_user_by_uid is None:
            return None

        custom_attribute_value = userService.getCustomAttribute(find_user_by_uid, attribute_name)
        if custom_attribute_value is None:
            return None
        
        attribute_value = custom_attribute_value.getValue()

        print "Basic (lock account). Get user attribute. User's '%s' attribute '%s' value is '%s'" % (user_name, attribute_name, attribute_value)

        return attribute_value

    def setInvalidLoginCount(self, user_name, count):
        if StringHelper.isEmpty(user_name):
            return None

        userService = CdiUtil.bean(UserService)

        find_user_by_uid = userService.getUser(user_name)
        if find_user_by_uid is None:
            return None

        # A concurrent request may have locked the user since the caller read it: keep that lock's marker
        current = userService.getCustomAttribute(find_user_by_uid, self.invalidLoginCountAttribute)
        if find_user_by_uid.getStatus() == GluuStatus.INACTIVE and current is not None and self.getLockTime(current.getValue()) is not None:
            print "Basic (lock account). Set invalid login count. User '%s' is locked, count not updated" % user_name
            return None

        userService.setCustomAttribute(find_user_by_uid, self.invalidLoginCountAttribute, count)
        updated_user = userService.updateUser(find_user_by_uid)

        print "Basic (lock account). Set invalid login count. User '%s' count is '%s'" % (user_name, count)

        return updated_user

    def getLockTime(self, count_attribute_value):
        if count_attribute_value is None or not count_attribute_value.startswith(LOCK_MARKER_PREFIX):
            return None
        try:
            return long(count_attribute_value[len(LOCK_MARKER_PREFIX):])
        except ValueError:
            return None

    def lockUser(self, user_name):
        if StringHelper.isEmpty(user_name):
            return None

        userService = CdiUtil.bean(UserService)
        facesMessages = CdiUtil.bean(FacesMessages)
        facesMessages.setKeepMessages()

        find_user_by_uid = userService.getUser(user_name)
        if (find_user_by_uid is None):
            return None

        if find_user_by_uid.getStatus() == GluuStatus.INACTIVE:
            print "Basic (lock account). Lock user. User '%s' locked already" % user_name
            return

        # Status and marker go in one update, so a lock never exists without its marker
        find_user_by_uid.setStatus(GluuStatus.INACTIVE)
        userService.setCustomAttribute(find_user_by_uid, self.invalidLoginCountAttribute, LOCK_MARKER_PREFIX + str(System.currentTimeMillis()))
        userService.updateUser(find_user_by_uid)
        facesMessages.add(FacesMessage.SEVERITY_ERROR, "Your account is locked. Please try again after " + StringHelper.toString(self.lockExpirationTime) + " secs")

        print "Basic (lock account). Lock user. User '%s' locked" % user_name

    def unLockUser(self, user_name, expected_marker):
        if StringHelper.isEmpty(user_name):
            return False

        userService = CdiUtil.bean(UserService)

        find_user_by_uid = userService.getUser(user_name)
        if (find_user_by_uid is None):
            return False

        # Re-read right before the write: skip if another request already unlocked or re-locked the user
        current = userService.getCustomAttribute(find_user_by_uid, self.invalidLoginCountAttribute)
        current_marker = None if current is None else current.getValue()
        if find_user_by_uid.getStatus() != GluuStatus.INACTIVE or current_marker != expected_marker:
            print "Basic (lock account). Unlock user. Lock of user '%s' changed, not unlocking" % user_name
            return False

        # Every UserService.updateUser (config-api, SCIM, admin UI) stamps updatedAt, and the script does
        # not write a locked user, so a later updatedAt means an administrator changed the user while locked
        updated_at = find_user_by_uid.getUpdatedAt()
        if updated_at is None or abs(updated_at.getTime() - self.getLockTime(expected_marker)) > LOCK_WRITE_SLACK_MILLIS:
            print "Basic (lock account). Unlock user. User '%s' was modified after the lock, not unlocking" % user_name
            return False

        find_user_by_uid.setStatus(GluuStatus.ACTIVE)
        userService.setCustomAttribute(find_user_by_uid, self.invalidLoginCountAttribute, StringHelper.toString(0))
        userService.updateUser(find_user_by_uid)

        print "Basic (lock account). Unlock user. User '%s' unlocked" % user_name
        return True
