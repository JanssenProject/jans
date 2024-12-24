from __future__ import print_function

from io.jans.service.cdi.util import CdiUtil
from io.jans.model.custom.script.type.token import UpdateTokenType
from io.jans.oxauth.service import SessionIdService
import sys


class UpdateToken(UpdateTokenType):

    def __init__(self, currentTimeMillis):
        """Construct class.

        Args:
            currentTimeMillis (int): current time in miliseconds
        """
        self.currentTimeMillis = currentTimeMillis

    @classmethod
    def init(cls, customScript, configurationAttributes):
        print("Update token obconnect script. Initializing ...")
        print("Update token obconnect  script. Initialized successfully")

        return True

    @classmethod
    def destroy(cls, configurationAttributes):
        print("Update token obconnect  script. Destroying ...")
        print("Update token  obconnect  script. Destroyed successfully")
        return True

    @classmethod
    def getApiVersion(cls):
        return 11

    # Returns boolean, true - indicates that script applied changes
    # This method is called after adding headers and claims. Hence script can override them
    # Note :
    # jsonWebResponse - is JwtHeader, you can use any method to manipulate JWT
    # context is reference of io.jans.oxauth.service.external.context.ExternalUpdateTokenContext (in https://github.com/GluuFederation/oxauth project, )
    @classmethod
    def modifyIdToken(cls, jsonWebResponse, context):
        print("Update token obconnect script. Modify idToken: %s" % jsonWebResponse)
        try :
            sessionIdService = CdiUtil.bean(SessionIdService)
            print("session id from context - %s" % context.getGrant().getSessionDn().strip("oxId="))

            sessionId = sessionIdService.getSessionByDn(context.getGrant().getSessionDn()) # fetch from persistence
            if sessionId is None:
                print("Session Id is none")
            else:
                print("session id -%s " % sessionId.getId())

            print("session id -%s " % sessionId.getSessionAttributes())
            openbanking_intent_id = sessionId.getSessionAttributes().get("openbanking_intent_id")
            acr = sessionId.getSessionAttributes().get("acr_ob")

            #jsonWebResponse.getHeader().setClaim("custom_header_name", "custom_header_value")

            #custom claims
            jsonWebResponse.getClaims().setClaim("openbanking_intent_id", openbanking_intent_id)
            jsonWebResponse.getClaims().setClaim("acr", acr)

            #regular claims
            jsonWebResponse.getClaims().setClaim("sub", openbanking_intent_id)

            print("Update token script. After modify idToken: %s" % jsonWebResponse)

            return True
        except Exception as e:
            print("update token failure" , e, sys.exc_info()[1])
            return None
