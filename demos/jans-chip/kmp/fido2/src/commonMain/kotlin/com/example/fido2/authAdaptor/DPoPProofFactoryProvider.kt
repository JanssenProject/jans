package com.example.fido2.authAdaptor

interface DPoPProofFactoryProvider {
    fun getIssuerFromClaimsSet(): String
    fun issueJWTToken(claims: MutableMap<String?, Any?>): String?
    fun getJWKS(): String?
    fun issueDPoPJWTToken(httpMethod: String, requestUrl: String): String?
    fun getChecksum(): String
    fun getPackageName(): String
}