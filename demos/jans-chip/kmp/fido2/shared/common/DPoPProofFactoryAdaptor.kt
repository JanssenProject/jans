package com.example.fido2.shared.common

import android.content.Context
import android.os.Build
import com.example.fido2.authAdaptor.DPoPProofFactoryProvider
import com.example.fido2.shared.factories.DPoPProofFactory
import com.example.fido2.shared.factories.KeyManager
import com.example.fido2.shared.factories.KeyManager.Companion.getPublicKeyJWK
import com.nimbusds.jwt.JWTClaimsSet
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.UUID

class DPoPProofFactoryAdaptor(context: Context): DPoPProofFactoryProvider {

    private var obtainedContext: Context = context

    override fun getIssuerFromClaimsSet(): String {
        val jwtClaimsSet: JWTClaimsSet = DPoPProofFactory.getClaimsFromSSA()
        val issuer: String = jwtClaimsSet.getClaim("iss").toString()
        return issuer
    }

    override fun issueJWTToken(claims: MutableMap<String?, Any?>): String? {
        return DPoPProofFactory.issueJWTToken(claims)
    }

    override fun getJWKS(): String? {
        val jwks = JSONWebKeySet()
        jwks.addKey(getPublicKeyJWK(KeyManager.getPublicKey())?.requiredParams)
        return jwks.toJsonString()
    }

    override fun issueDPoPJWTToken(httpMethod: String, requestUrl: String): String? {

        val headers: MutableMap<String, Any> =
            mutableMapOf()
        headers["typ"] = "dpop+jwt"
        headers["alg"] = "RS256"
        val requiredParams = getPublicKeyJWK(KeyManager.getPublicKey())?.requiredParams
        headers["jwk"] = requiredParams!!

        val claims: MutableMap<String, Any> = HashMap()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val now = LocalDateTime.now()
            val instant =
                now.atZone(ZoneId.systemDefault()).toInstant()
            val iat = Date.from(instant)
            claims["iat"] = iat
        }
        claims["jti"] = UUID.randomUUID().toString()
        claims["htm"] = httpMethod //POST
        claims["htu"] = requestUrl //issuer
        return Jwts.builder()
            .setHeaderParams(headers)
            .setClaims(claims)
            .signWith(SignatureAlgorithm.RS256, KeyManager.getPrivateKey())
            .compact()
    }

    override fun getChecksum(): String {
        return AppUtil.getChecksum(obtainedContext) ?: ""
    }

    override fun getPackageName(): String {
        return obtainedContext.getPackageName()
    }
}