# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2016, Janssen
#
# Author: Yuriy Movchan
#

from io.jans.model.custom.script.type.scope import DynamicScopeType
from java.util import Arrays


class DynamicScope(DynamicScopeType):
    def __init__(self, current_time_millis):
        self.currentTimeMillis = current_time_millis

    def init(self, custom_script, configuration_attributes):
        print("Dynamic scope. Initialization")

        print("Dynamic scope. Initialized successfully")

        return True   

    def destroy(self, configuration_attributes):
        print("Dynamic scope. Destroy")
        print("Dynamic scope. Destroyed successfully")
        return True   

    # Update Json Web token before signing/encrypring it
    #   dynamic_scope_context is io.jans.as.service.external.context.DynamicScopeExternalContext
    #   configuration_attributes is java.util.Map<String, SimpleCustomProperty>
    def update(self, dynamic_scope_context, configuration_attributes):
        print("Dynamic scope. Update method")

        json_web_response = dynamic_scope_context.getJsonWebResponse()
        claims = json_web_response.getClaims()

        # Add organization name if there is scope = org_name
        claims.setClaim("org_name", "Janssen, Inc.")

        return True

    def getSupportedClaims(self, configuration_attributes):
        return Arrays.asList("org_name")

    def getApiVersion(self):
        return 11
