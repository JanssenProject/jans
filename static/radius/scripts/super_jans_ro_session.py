# Super Janssen Radius Dynamic Scope 
# Copyright (c) 2019 Janssen Inc.

from org.jans.model.custom.script.type.scope import DynamicScopeType
from org.jans.oxauth.security import Identity
from org.jans.service.cdi.util import CdiUtil

import java

class DynamicScope(DynamicScopeType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Super-Janssen-DynScope init"
        self.sessionIdClaimName = "__session_id"
        if configurationAttributes.containsKey("session_id_claim_name"):
            self.sessionIdClaimName = configurationAttributes.get("session_id_claim_name").getValue2()
        
        print "Super-Janssen-DynScope init complete"
        return True
    
    def destroy(self, configurationAttributes):
        print "Super-Janssen-DynScope destroy"
        print "Super-Janssen-DynScope destroy complete"
        return True
    
    def update(self, dynamicScopeContext, configurationAttributes):
        # Todo implement this
        print "Super-Janssen-DynScope update"
        updated = False
        identity = CdiUtil.bean(Identity)
        if (identity is not None) and (identity.getSessionId() is not None):
            session_id = identity.getSessionId().getId()
            jsonWebResponse  = dynamicScopeContext.getJsonWebResponse()
            claims = jsonWebResponse.getClaims()
            claims.setClaim(self.sessionIdClaimName,session_id)
            updated = True
        else:
            print "Super-Janssen-DynScope. No session id found. Skipping"
        print "Super-Janssen-DynScope update complete"
        return updated
    
    def getApiVersion(self):
        return 11
    
    def getSupportedClaims(self,arg):
         return None
