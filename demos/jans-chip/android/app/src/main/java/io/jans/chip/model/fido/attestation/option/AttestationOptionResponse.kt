package io.jans.chip.model.fido.attestation.option

import androidx.room.Ignore

class AttestationOptionResponse (
     var attestation: String?,
     var authenticatorSelection: AuthenticatorSelection?,
     var challenge: String?,
     var pubKeyCredParams: List<PubKeyCredParam>?,
     var rp: RP?,
     var user: User?,
) {
     @Ignore
     var isSuccessful: Boolean? = true

     @Ignore
     var errorMessage: String? = null
}