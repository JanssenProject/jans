package com.example.fido2.shared.common

import io.jans.webauthn.models.PublicKeyCredentialSource
import io.jans.webauthn.util.CredentialSelector

open class LocalCredentialSelector: CredentialSelector {
    override fun selectFrom(credentialList: MutableList<PublicKeyCredentialSource>?): PublicKeyCredentialSource? {
        return credentialList?.get(0)
    }
}