package io.jans.configapi.service.cedar;

import io.jans.configapi.util.AuthUtil;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import io.jans.cedarling.binding.wrapper.CedarlingAdapter;

import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import org.slf4j.Logger;
import uniffi.cedarling_uniffi.*;

@ApplicationScoped
@Named
public class CedarlingService {

    @Inject
    Logger logger;


    private CedarlingAdapter adapter;    
    private AuthorizeResult result;
    
    private static final String AUTHENTICATION_SCHEME = "Bearer ";
    public static final String PROTECTION_CONFIGURATION_FILE_NAME = "config-api-rs-protect.json";
    public static final String BOOTSTRAP_JSON_PATH = "/opt/jans/jetty/jans-config-api/custom/static/config-api-cedarling-bootstrap.json";

    @PostConstruct
    public void setUp() throws Exception {
        logger.error(" BOOTSTRAP_JSON_PATH:{}", BOOTSTRAP_JSON_PATH);
        adapter = new CedarlingAdapter();
        // Load Cedarling bootstrap configuration from file
        String bootstrapJson = AuthUtil.readFile(BOOTSTRAP_JSON_PATH);
        logger.error(bootstrapJson);
        adapter.loadFromJson(bootstrapJson);
        logger.error(" adapter:{}", adapter);
    }

    public void authorize(String accessToken, String idToken, String userInfo, String action, String resource,
            String context) throws Exception {
        logger.error("\n *********************************************\n");
        logger.error(" accessToken:{}, idToken:{}, userInfo:{}, action:{}, resource:{}, context:{} ", accessToken,
                idToken, userInfo, action, resource, context);

        // Generate signed JWTs from the payloads
        Map<String, String> tokens = Map.of("access_token", accessToken, "id_token", idToken, "userinfo_token",
                userInfo);

        // Perform authorization
        result = adapter.authorize(tokens, action, new JSONObject(resource), new JSONObject(context));

        logger.error(" result:{} ", result);
        logger.error("\n *********************************************\n");

    }

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
            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);

            // Apply the HMAC signature
            signedJWT.sign(signer);

            // Serialize to compact JWT format
            return signedJWT.serialize();
        }
    }

}
