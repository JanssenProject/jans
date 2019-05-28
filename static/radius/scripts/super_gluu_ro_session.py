# Super Gluu Radius Dynamic Scope 
# Copyright (c) 2019 Gluu Inc.

from org.gluu.model.custom.script.type.scope import DynamicScopeType
from org.gluu.oxauth.security import Identity
from org.gluu.service.cdi.util import CdiUtil

import java

class DynamicScope(DynamicScopeType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self,configurationAttributes):
        print "Super-Gluu-DynScope init"
        self.sessionIdClaimName = "__session_id"
        if configurationAttributes.containsKey("session_id_claim_name"):
            self.sessionIdClaimName = configurationAttributes.get("session_id_claim_name").getValue2()
        
        print "Super-Gluu-DynScope init complete"
        return True
    
    def destroy(self, configurationAttributes):
        print "Super-Gluu-DynScope destroy"
        print "Super-Gluu-DynScope destroy complete"
        return True
    
    def update(self, dynamicScopeContext, configurationAttributes):
        # Todo implement this
        print "Super-Gluu-DynScope update"
        updated = False
        identity = CdiUtil.bean(Identity)
        if (identity is not None) and (identity.getSessionId() is not None):
            session_id = identity.getSessionId().getId()
            jsonWebResponse  = dynamicScopeContext.getJsonWebResponse()
            claims = jsonWebResponse.getClaims()
            claims.setClaim(self.sessionIdClaimName,session_id)
            updated = True
        else:
            print "Super-Gluu-DynScope. No session id found. Skipping"
        print "Super-Gluu-DynScope update complete"
        return updated
    
    def getApiVersion(self):
        return 1
