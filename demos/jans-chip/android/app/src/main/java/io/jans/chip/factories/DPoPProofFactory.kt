package io.jans.chip.factories

import android.os.Build
import com.nimbusds.jose.JWSObject
import com.nimbusds.jwt.JWTClaimsSet
import io.jans.chip.factories.KeyManager.Companion.getPublicKeyJWK
import io.jans.chip.utils.AppConfig
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import java.security.InvalidAlgorithmParameterException
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.UUID

class DPoPProofFactory {
    companion object {
        var keyManager: KeyManager = KeyManager()

        @Throws(
            InvalidAlgorithmParameterException::class,
            NoSuchAlgorithmException::class,
            NoSuchProviderException::class
        )
        fun issueDPoPJWTToken(httpMethod: String, requestUrl: String): String? {

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

        @Throws(
            InvalidAlgorithmParameterException::class,
            NoSuchAlgorithmException::class,
            NoSuchProviderException::class
        )
        fun issueJWTToken(claims: MutableMap<String?, Any?>): String? {
            val headers: MutableMap<String, Any> =
                java.util.HashMap()
            headers["typ"] = "jwt"
            headers["alg"] = "RS256"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val now = LocalDateTime.now()
                val instant =
                    now.atZone(ZoneId.systemDefault()).toInstant()
                val iat = Date.from(instant)
                claims["iat"] = iat
            }
            claims["jti"] = UUID.randomUUID().toString()
            return Jwts.builder()
                .setHeaderParams(headers)
                .setClaims(claims)
                .signWith(SignatureAlgorithm.RS256, KeyManager.getPrivateKey())
                .compact()
        }

        fun getClaimsFromSSA(): JWTClaimsSet {
            val jwsObject: JWSObject = JWSObject.parse(AppConfig.SSA)
            val claims: JWTClaimsSet = JWTClaimsSet.parse(jwsObject.getPayload().toJSONObject());
            return claims
        }
    }
}