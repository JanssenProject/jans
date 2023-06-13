# Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
# Copyright (c) 2020, Janssen Project
#
# Author: Yuriy Zabrovarnyy
#
#
from __future__ import print_function

from io.jans.model.custom.script.type.postauthn import PostAuthnType

class PostAuthn(PostAuthnType):

    def __init__(self, currentTimeMillis):
        """Construct class.

        Args:
            currentTimeMillis (int): current time in miliseconds
        """
        self.currentTimeMillis = currentTimeMillis

    @classmethod
    def init(cls, customScript, configurationAttributes):
        print("Post Authn script. Initializing ...")
        print("Post Authn script. Initialized successfully")

        return True

    @classmethod
    def destroy(cls, configurationAttributes):
        print("Post Authn script. Destroying ...")
        print("Post Authn script. Destroyed successfully")
        return True

    @classmethod
    def getApiVersion(cls):
        return 11

    # This method is called during Authorization Request at Authorization Endpoint.
    # If True is returned, session is set as unauthenticated and user is send for authentication.
    # Note :
    # context is reference of io.jans.as.server.service.external.context.ExternalPostAuthnContext(in https://github.com/GluuFederation/oxauth project, )
    @classmethod
    def forceReAuthentication(cls, context):
        return False

    # This method is called during Authorization Request at Authorization Endpoint.
    # If True is returned user is send for Authorization. By default if client is "Pre-Authorized" or "Client Persist Authorizations" is on, authorization is skipped.
    # This script has higher priority and can cancel Pre-Authorization and persisted authorizations.
    # Note :
    # context is reference of io.jans.as.server.service.external.context.ExternalPostAuthnContext(in https://github.com/GluuFederation/oxauth project, )
    @classmethod
    def forceAuthorization(cls, context):
        return False
