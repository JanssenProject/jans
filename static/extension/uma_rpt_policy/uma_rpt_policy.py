# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2017, Gluu
#
# Author: Yuriy Zabrovarnyy
#

from org.xdi.model.custom.script.type.uma import UmaRptPolicyType
from org.xdi.model.uma import ClaimDefinitionBuilder
from java.lang import String

class UmaRptPolicy(UmaRptPolicyType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        print "RPT Policy. Initializing ..."
        print "RPT Policy. Initialized successfully"

        return True

    def destroy(self, configurationAttributes):
        print "RPT Policy. Destroying ..."
        print "RPT Policy. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 1

    # Returns required claims definitions.
    # This method must provide definition of all claims that is used in 'authorize' method.
    # Note : name in both places must match.
    def getRequiredClaims(self, context): # context is reference of org.xdi.oxauth.uma.authorization.UmaAuthorizationContext
        print context.getIssuer()
        json = """[
        {
            "issuer" : [ "%s" ],
            "name" : "country",
            "claim_token_format" : [ "http://openid.net/specs/openid-connect-core-1_0.html#IDToken" ],
            "claim_type" : "string",
            "friendly_name" : "country"
        },
        {
            "issuer" : [ "%s" ],
            "name" : "city",
            "claim_token_format" : [ "http://openid.net/specs/openid-connect-core-1_0.html#IDToken" ],
            "claim_type" : "string",
            "friendly_name" : "city"
        }
        ]"""

        json2 = """[
        {
            "issuer" : [ "{}" ],
            "name" : "country",
            "claim_token_format" : [ "http://openid.net/specs/openid-connect-core-1_0.html#IDToken" ],
            "claim_type" : "string",
            "friendly_name" : "country"
        },
        {
            "issuer" : [ "{}" ],
            "name" : "city",
            "claim_token_format" : [ "http://openid.net/specs/openid-connect-core-1_0.html#IDToken" ],
            "claim_type" : "string",
            "friendly_name" : "city"
        }
        ]"""
        print json
        print json2
        print json2.format(context.getIssuer(), context.getIssuer())

        string_format = String.format(json, context.getIssuer())

        print "string_format : " + string_format
        context.getLog().debug("string_format: " + string_format)

        return ClaimDefinitionBuilder.build(string_format)

    # Main authorization method. Must return True or False.
    def authorize(self, context): # context is reference of org.xdi.oxauth.uma.authorization.UmaAuthorizationContext
        print "RPT Policy. Authorizing ..."

        if (context.getClaim("country") == 'US' and context.getClaim("city") == 'NY'):
            print "Authorized successfully!"
            return True

        return False

    # Returns name of the Claims-Gathering script which will be invoked if need_info error is returned.
    def getClaimsGatheringScriptName(self, context): # context is reference of org.xdi.oxauth.uma.authorization.UmaAuthorizationContext
        return "sampleClaimsGathering"
