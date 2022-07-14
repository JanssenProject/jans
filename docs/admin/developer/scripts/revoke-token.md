# Revoke Token Script Guide

## Overview

Revoke Token scripts allow inject custom logic during token revoking.

## Interface

### Methods

    # This method is called during Revoke Token call.
    # If True is returned, token is revoked. If False is returned, revoking is skipped.
    # Note :
    # context is reference of org.gluu.oxauth.service.external.context.RevokeTokenContext(in https://github.com/GluuFederation/oxauth project, )
    def revoke(self, context):
        return True
        
Full version of the script example can be found [here.](https://github.com/GluuFederation/community-edition-setup/blob/version_4.3.0/static/extension/revoke_token/revoke_token.py)

**Note** `RevokeTokenContext` allows to access response builder (`context.getResponseBuilder()`) which allows to customer response if needed.        

### Objects

## Common Use Cases

Descriptions of common use cases for this script, including a code snippet for each
