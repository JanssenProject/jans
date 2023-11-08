# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2017, Janssen
#
# Author: Yuriy Zabrovarnyy
#
# Call sequence
# 1. First is call constructor of the Script __init__
# 2. Next init() method
# 3. Next getRequiredClaims() - method returns required claims, so UMA engine checks whether
#    in request RP provided all claims that are required. Pay attention that there can be
#    multiple scripts bound to the scopes, means that UMA engine will build set of required claims
#    from all scripts. If not all claims are provided need_info error is sent to RP.
#    During need_info construction getClaimsGatheringScriptName() method is called
# 4. authorize() method is called if all required claims are provided.
# 5. destroy()

from io.jans.model.custom.script.type.uma import UmaRptPolicyType
from io.jans.model.uma import ClaimDefinitionBuilder
from java.lang import String

class UmaRptPolicy(UmaRptPolicyType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "RPT Policy. Initializing ..."
        print "RPT Policy. Initialized successfully"

        return True

    def destroy(self, configurationAttributes):
        print "RPT Policy. Destroying ..."
        print "RPT Policy. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 11

    # Returns required claims definitions.
    # This method must provide definition of all claims that is used in 'authorize' method.
    # Note : name in both places must match.
    # %1$s - placeholder for issuer. It uses standard Java Formatter, docs : https://docs.oracle.com/javase/7/docs/api/java/util/Formatter.html
    def getRequiredClaims(self, context): # context is reference of io.jans.as.uma.authorization.UmaAuthorizationContext
        json = """[
        {
            "issuer" : [ "%1$s" ],
            "name" : "country",
            "claim_token_format" : [ "http://openid.net/specs/openid-connect-core-1_0.html#IDToken" ],
            "claim_type" : "string",
            "friendly_name" : "country"
        },
        {
            "issuer" : [ "%1$s" ],
            "name" : "city",
            "claim_token_format" : [ "http://openid.net/specs/openid-connect-core-1_0.html#IDToken" ],
            "claim_type" : "string",
            "friendly_name" : "city"
        }
        ]"""
        context.addRedirectUserParam("customUserParam1", "value1") # pass some custom parameters to need_info uri. It can be removed if you don't need custom parameters.
        return ClaimDefinitionBuilder.build(String.format(json, context.getIssuer()))

    # Main authorization method. Must return True or False.
    def authorize(self, context): # context is reference of io.jans.as.uma.authorization.UmaAuthorizationContext
        print "RPT Policy. Authorizing ..."

        if context.getClaim("country") == 'US' and context.getClaim("city") == 'NY':
            print "Authorized successfully!"
            return True

        return False

    # Returns name of the Claims-Gathering script which will be invoked if need_info error is returned.
    def getClaimsGatheringScriptName(self, context): # context is reference of io.jans.as.uma.authorization.UmaAuthorizationContext
        context.addRedirectUserParam("customUserParam2", "value2") # pass some custom parameters to need_info uri. It can be removed if you don't need custom parameters.
        return "sampleClaimsGathering"