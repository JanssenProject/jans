package io.jans.chip.keyGen;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class DPoPProofFactory {
    public static String issueDPoPJWTToken(String httpMethod, String requestUrl) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {

        Map<String, Object> headers = new HashMap<>();
        headers.put("typ", "dpop+jwt");
        headers.put("alg", "RS256");
        headers.put("jwk", KeyManager.getPublicKeyJWK(KeyManager.getInstance().getPublicKey()).getRequiredParams());

        Map<String, Object> claims = new HashMap<>();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            LocalDateTime now = LocalDateTime.now();
            Instant instant = now.atZone(ZoneId.systemDefault()).toInstant();
            Date iat = Date.from(instant);
            claims.put("iat", iat);
        }

        claims.put("jti", UUID.randomUUID().toString());
        claims.put("htm", httpMethod); //POST
        claims.put("htu", requestUrl); //issuer

        String token = Jwts.builder()
                .setHeaderParams(headers)
                .setClaims(claims)
                .signWith(SignatureAlgorithm.RS256, KeyManager.getInstance().getPrivateKey())
                .compact();
        return token;
    }

    public static String issueJWTToken(Map<String, Object> claims) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {

        Map<String, Object> headers = new HashMap<>();
        headers.put("typ", "dpop+jwt");
        headers.put("alg", "RS256");
        headers.put("jwk", KeyManager.getPublicKeyJWK(KeyManager.getInstance().getPublicKey()).getRequiredParams());

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            LocalDateTime now = LocalDateTime.now();
            Instant instant = now.atZone(ZoneId.systemDefault()).toInstant();
            Date iat = Date.from(instant);
            claims.put("iat", iat);
        }

        claims.put("jti", UUID.randomUUID().toString());

        String token = Jwts.builder()
                .setHeaderParams(headers)
                .setClaims(claims)
                .signWith(SignatureAlgorithm.RS256, KeyManager.getInstance().getPrivateKey())
                .compact();
        return token;
    }
}
