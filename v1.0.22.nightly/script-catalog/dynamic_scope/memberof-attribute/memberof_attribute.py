# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2016, Janssen
#
# Author: Sahil Arora
#

from io.jans.model.custom.script.type.scope import DynamicScopeType
from io.jans.as.server.service import UserService
from io.jans.util import StringHelper, ArrayHelper
from java.util import Arrays, ArrayList
from io.jans.service.cdi.util import CdiUtil

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
    #   dynamicScopeContext is io.jans.as.service.external.context.DynamicScopeExternalContext
    #   configurationAttributes is java.util.Map<String, SimpleCustomProperty>
    def update(self, dynamicScopeContext, configurationAttributes):
        print "Dynamic scope. Update method"
        userService = CdiUtil.bean(UserService)
        print "-->userService: " + userService.toString()

        dynamicScopes = dynamicScopeContext.getDynamicScopes()
        authorizationGrant = dynamicScopeContext.getAuthorizationGrant()
        user = dynamicScopeContext.getUser()
        jsonWebResponse = dynamicScopeContext.getJsonWebResponse()
        claims = jsonWebResponse.getClaims()

        member_of_list= userService.getCustomAttribute(user, "memberof")
        if member_of_list == None:
            print "-->memberOf: is null"
            return None
        else:
             members_list = member_of_list.getValues()
             membersArray = []
             for members in members_list:
                 group = userService.getUserByDn(members, "displayName")
                 membersArray.append(group.getAttribute("displayName"))

             claims.setClaim("memberof", Arrays.asList(membersArray ) )

        return True

    def getSupportedClaims(self, configurationAttributes):
        return Arrays.asList("memberof")

    def getApiVersion(self):
        return 2
