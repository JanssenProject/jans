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

    private static DPoPProofFactory single_instance = null;
    private DPoPProofFactory(){}

    public static synchronized DPoPProofFactory getInstance() {
        if (single_instance == null)
            single_instance = new DPoPProofFactory();

        return single_instance;
    }
    /**
     * The function `issueDPoPJWTToken` generates a DPoP (Distributed Proof of Possession) JWT (JSON Web Token) with the
     * specified HTTP method and request URL.
     *
     * @param httpMethod The `httpMethod` parameter represents the HTTP method used in the request, such as "GET", "POST",
     * "PUT", etc.
     * @param requestUrl The `requestUrl` parameter is the URL of the HTTP request that you want to issue a DPoP JWT token
     * for. It represents the target resource or endpoint that you want to access.
     * @return The method is returning a JWT (JSON Web Token) token.
     */
    public String issueDPoPJWTToken(String httpMethod, String requestUrl) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {

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

    /**
     * The function generates a JWT token with specified claims and signs it using an RSA256 algorithm.
     *
     * @param claims A map containing the claims to be included in the JWT token. Claims are key-value pairs that provide
     * information about the token, such as the issuer, subject, expiration time, etc.
     * @return The method returns a JWT (JSON Web Token) token as a String.
     */
    public String issueJWTToken(Map<String, Object> claims) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {

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
