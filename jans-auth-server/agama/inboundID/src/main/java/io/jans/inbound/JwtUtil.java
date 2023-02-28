package io.jans.inbound;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jwt.*;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import java.text.ParseException;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.Map;

public class JwtUtil {

    private static final Base64.Decoder decoder = Base64.getDecoder();
    
    //ECDSA using P-256 (secp256r1) curve and SHA-256 hash algorithm
    public static String mkES256SignedJWT(String privateKeyPEM, String kid, String iss, String aud, String sub, int expGap)
            throws JOSEException, NoSuchAlgorithmException, InvalidKeySpecException {
        
        byte[] keyData = decoder.decode(privateKeyPEM);
        EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(keyData);
        KeyFactory kf = KeyFactory.getInstance("EC");
        PrivateKey privKey = kf.generatePrivate(privKeySpec);
        
        JWSSigner signer = new ECDSASigner(privKey, Curve.P_256);
        long now = System.currentTimeMillis();
        
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .issuer(iss)
                .issueTime(new Date(now))
                .expirationTime(new Date(now + expGap * 1000L))
                .audience(aud)
                .subject(sub)
                .build();
                
        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(kid).type(JOSEObjectType.JWT).build(),
                claimsSet);
        signedJWT.sign(signer);
        return signedJWT.serialize();
    
    }
    
    public static Map<String, Object> partialVerifyJWT(String jwt, String iss, String aud)
            throws ParseException, JOSEException {

        JWTClaimsSet claims = SignedJWT.parse(jwt).getJWTClaimsSet();

        //Apply some validations
        if (!iss.equals(claims.getIssuer())) throw new JOSEException("Unexpected issuer value in id_token");
        
        if (claims.getAudience().stream().filter(aud::equals).findFirst().isEmpty())
            throw new JOSEException("id_token does not contain the expected audience " + aud);

        long now = System.currentTimeMillis();
        if (Optional.ofNullable(claims.getExpirationTime()).map(Date::getTime).orElse(0L) < now)
            throw new JOSEException("Expired id_token");
        
        return claims.toJSONObject();

    }

    private JwtUtil() { }

}
