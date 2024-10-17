package com.example.fido2.model

class KtPublicKeyCredentialSource(
    var roomUid: Int,
    var id: ByteArray,
    var keyPairAlias: String?,
    var rpId: String?,
    var userHandle: ByteArray,
    var userDisplayName: String?,
    var otherUI: String?,
    var keyUseCounter: Int
)