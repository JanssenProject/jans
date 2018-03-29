# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2016, Gluu
#
# Author: Yuriy Movchan
#

from org.xdi.model.custom.script.type.scope import DynamicScopeType
from org.xdi.service.cdi.util import CdiUtil
from org.xdi.oxauth.service import UserService
from org.xdi.util import StringHelper, ArrayHelper
from java.util import Arrays, ArrayList

import java

class DynamicScope(DynamicScopeType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        print "Dynamic scope. Initialization"

        print "Dynamic scope. Initialized successfully"

        return True   

    def destroy(self, configurationAttributes):
        print "Dynamic scope. Destroy"
        print "Dynamic scope. Destroyed successfully"
        return True   

    # Update Json Web token before signing/encrypring it
    #   dynamicScopeContext is org.xdi.oxauth.service.external.context.DynamicScopeExternalContext
    #   configurationAttributes is java.util.Map<String, SimpleCustomProperty>
    def update(self, dynamicScopeContext, configurationAttributes):
        print "Dynamic scope. Update method"

        dynamicScopes = dynamicScopeContext.getDynamicScopes()
        authorizationGrant = dynamicScopeContext.getAuthorizationGrant()
        user = dynamicScopeContext.getUser()
        jsonWebResponse = dynamicScopeContext.getJsonWebResponse()
        claims = jsonWebResponse.getClaims()

        # Add work phone if there is scope = work_phone
        userService = CdiUtil.bean(UserService)
        workPhone = userService.getCustomAttribute(user, "telephoneNumber")
        if workPhone != None:
            claims.setClaim("work_phone", workPhone.getValues())

        return True

    def getSupportedClaims(self, configurationAttributes):
        return Arrays.asList("work_phone")

    def getApiVersion(self):
        return 2
