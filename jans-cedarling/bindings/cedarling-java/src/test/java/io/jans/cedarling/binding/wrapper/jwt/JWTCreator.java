package io.jans.cedarling.binding.wrapper.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.util.Date;
import java.util.Map;

public class JWTCreator {

    private final byte[] secret;

    public JWTCreator(String secret) {
        this.secret = secret.getBytes(); // Convert secret to bytes for HMAC
    }

    public String createJwtFromJson(String jsonString) throws Exception {
        // Convert JSON to Map
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> claimsMap = objectMapper.readValue(jsonString, Map.class);

        // Build JWT claims
        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder();
        for (Map.Entry<String, Object> entry : claimsMap.entrySet()) {
            claimsBuilder.claim(entry.getKey(), entry.getValue());
        }

        // Optionally add iat/exp (example: token valid for 1 hour)
        claimsBuilder.issueTime(new Date());
        claimsBuilder.expirationTime(new Date(System.currentTimeMillis() + 3600_000));

        JWTClaimsSet claimsSet = claimsBuilder.build();

        // Create HMAC signer
        JWSSigner signer = new MACSigner(secret);

        // Prepare JWS object
        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS256),
                claimsSet
        );

        // Apply the HMAC signature
        signedJWT.sign(signer);

        // Serialize to compact JWT format
        return signedJWT.serialize();
    }
}