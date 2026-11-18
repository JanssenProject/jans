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
from io.jans.orm.exception import VersionMismatchException
from io.jans.orm.exception import UnsupportedOperationException as OrmUnsupportedOperationException
from jakarta.faces.application import FacesMessage
from io.jans.jsf2.message import FacesMessages
from io.jans.orm import PersistenceEntryManager
from java.util import Date

# Written to the invalid-login count attribute by lockUser: only inactive users carrying it are unlocked
LOCK_MARKER_PREFIX = "locked:"
UPDATE_ATTEMPTS = 5

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

                    locked_for = (Date().getTime() - locked_at) / 1000
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

                countInvalidLogin = self.incrementInvalidLoginCount(user_name)
                if countInvalidLogin is None:
                    return False

                remainingAttempts = self.maximumInvalidLoginAttemps - countInvalidLogin
                print "Remaining login count attempts '%s' for user '%s'" % ( remainingAttempts, user_name )
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

    def updateUser(self, user_name, change):
        # Compare-and-set on jansVersion: if another writer (lock, unlock, counter, admin) changed the
        # user since the read, re-read and let change() decide again on the current state
        userService = CdiUtil.bean(UserService)
        entryManager = CdiUtil.bean(PersistenceEntryManager)
        for _ in range(UPDATE_ATTEMPTS):
            user = userService.getUser(user_name)
            if user is None or not change(user):
                return None
            try:
                entryManager.updateWithVersion(user)
                return user
            except VersionMismatchException:
                print "Basic (lock account). User '%s' changed concurrently, retrying" % user_name
            except OrmUnsupportedOperationException:
                print "Basic (lock account). Backend has no versioned update, user '%s' updated without it" % user_name
                entryManager.merge(user)
                return user

        print "Basic (lock account). User '%s' kept changing, update skipped" % user_name
        return None

    def isLocked(self, user):
        current = CdiUtil.bean(UserService).getCustomAttribute(user, self.invalidLoginCountAttribute)
        return user.getStatus() == GluuStatus.INACTIVE and current is not None and self.getLockTime(current.getValue()) is not None

    def setInvalidLoginCount(self, user_name, count):
        if StringHelper.isEmpty(user_name):
            return None

        def change(user):
            if self.isLocked(user):
                print "Basic (lock account). Set invalid login count. User '%s' is locked, count not updated" % user_name
                return False
            CdiUtil.bean(UserService).setCustomAttribute(user, self.invalidLoginCountAttribute, count)
            user.setUpdatedAt(Date())
            return True

        updated_user = self.updateUser(user_name, change)
        if updated_user is not None:
            print "Basic (lock account). Set invalid login count. User '%s' count is '%s'" % (user_name, count)

        return updated_user

    def incrementInvalidLoginCount(self, user_name):
        # Increment on the current stored value so parallel failed logins are all counted
        result = {}

        def change(user):
            if self.isLocked(user):
                return False
            current = CdiUtil.bean(UserService).getCustomAttribute(user, self.invalidLoginCountAttribute)
            count = StringHelper.toInteger(None if current is None else current.getValue(), 0)
            result['count'] = count
            if count >= self.maximumInvalidLoginAttemps:
                return False
            result['count'] = count + 1
            CdiUtil.bean(UserService).setCustomAttribute(user, self.invalidLoginCountAttribute, StringHelper.toString(count + 1))
            user.setUpdatedAt(Date())
            return True

        self.updateUser(user_name, change)
        return result.get('count')

    # Marker format: locked:<epoch millis>:<jansVersion written by the lock>
    def parseLockMarker(self, count_attribute_value):
        if count_attribute_value is None or not count_attribute_value.startswith(LOCK_MARKER_PREFIX):
            return None
        parts = count_attribute_value[len(LOCK_MARKER_PREFIX):].split(":")
        try:
            return (long(parts[0]), long(parts[1]) if len(parts) > 1 else None)
        except ValueError:
            return None

    def getLockTime(self, count_attribute_value):
        marker = self.parseLockMarker(count_attribute_value)
        return None if marker is None else marker[0]

    def lockUser(self, user_name):
        if StringHelper.isEmpty(user_name):
            return None

        facesMessages = CdiUtil.bean(FacesMessages)
        facesMessages.setKeepMessages()

        def change(user):
            if user.getStatus() == GluuStatus.INACTIVE:
                print "Basic (lock account). Lock user. User '%s' locked already" % user_name
                return False
            # Status and marker go in one write, so a lock never exists without its marker. The marker
            # carries the version this write stores, so unlock can tell whether anyone wrote after it
            locked_at = Date()
            lock_version = (0 if user.getVersion() is None else user.getVersion()) + 1
            user.setStatus(GluuStatus.INACTIVE)
            user.setUpdatedAt(locked_at)
            CdiUtil.bean(UserService).setCustomAttribute(user, self.invalidLoginCountAttribute,
                    "%s%s:%s" % (LOCK_MARKER_PREFIX, locked_at.getTime(), lock_version))
            return True

        if self.updateUser(user_name, change) is None:
            return None

        facesMessages.add(FacesMessage.SEVERITY_ERROR, "Your account is locked. Please try again after " + StringHelper.toString(self.lockExpirationTime) + " secs")

        print "Basic (lock account). Lock user. User '%s' locked" % user_name

    def unLockUser(self, user_name, expected_marker):
        if StringHelper.isEmpty(user_name):
            return False

        marker = self.parseLockMarker(expected_marker)
        if marker is None:
            return False
        locked_at, lock_version = marker

        def change(user):
            current = CdiUtil.bean(UserService).getCustomAttribute(user, self.invalidLoginCountAttribute)
            current_marker = None if current is None else current.getValue()
            if user.getStatus() != GluuStatus.INACTIVE or current_marker != expected_marker:
                print "Basic (lock account). Unlock user. Lock of user '%s' changed, not unlocking" % user_name
                return False

            # Any write after the lock (config-api, SCIM, admin UI) bumps jansVersion or stamps updatedAt
            updated_at = user.getUpdatedAt()
            if (lock_version is not None and user.getVersion() != lock_version) or updated_at is None or updated_at.getTime() != locked_at:
                print "Basic (lock account). Unlock user. User '%s' was modified after the lock, not unlocking" % user_name
                return False

            user.setStatus(GluuStatus.ACTIVE)
            user.setUpdatedAt(Date())
            CdiUtil.bean(UserService).setCustomAttribute(user, self.invalidLoginCountAttribute, StringHelper.toString(0))
            return True

        if self.updateUser(user_name, change) is None:
            return False

        print "Basic (lock account). Unlock user. User '%s' unlocked" % user_name
        return True
