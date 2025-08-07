package io.jans.configapi.service.cedar;

import io.jans.configapi.util.AuthUtil;
import jakarta.inject.Inject;
import io.jans.cedarling.binding.wrapper.CedarlingAdapter;

import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import org.slf4j.Logger;


public class CedarlingService {


    @Inject
    Logger logger;

    @Inject
    CedarlingAdapter adapter;


//    private JWTCreator jwtCreator;
//    private AuthorizeResult result;
   
   private static final String AUTHENTICATION_SCHEME = "Bearer ";
   //public static final String BOOTSTRAP_JSON_PATH = ".src/main/resources/cedar/config-api-cedarling-bootstrap.json";
   

    public static final String JWT_SECRET = "-very-strong-shared-secret-of-at-least-32-bytes!";
    public static final String BOOTSTRAP_JSON_PATH = "./src/test/resources/config/bootstrap.json";
    public static final String ACCESS_TOKEN_FILE_PATH = "./src/test/resources/config/access_token_payload.json";
    public static final String ID_TOKEN_FILE_PATH = "./src/test/resources/config/id_token_payload.json";
    public static final String USER_INFO_FILE_PATH = "./src/test/resources/config/user_info_payload.json";

    public static final String ACTION_FILE_PATH = "./src/test/resources/config/action.txt";
    public static final String RESOURCE_FILE_PATH = "./src/test/resources/config/resource.json";
    public static final String CONTEXT_FILE_PATH = "./src/test/resources/config/context.json";
    public static final String PRINCIPALS_FILE_PATH = "./src/test/resources/config/principals.json";


    public void setUp() throws Exception {
        adapter = new CedarlingAdapter();
        jwtCreator = new JWTCreator(JWT_SECRET);
        // Load Cedarling bootstrap configuration from file
        String bootstrapJson = AppUtils.readFile(BOOTSTRAP_JSON_PATH);
        adapter.loadFromJson(bootstrapJson);
        assertNotNull(adapter.getCedarling());
    }


    public void authorize() throws Exception {
        // Get JWT 
        String accessTokend = AppUtils.readFile(ACCESS_TOKEN_FILE_PATH);
        String idTokenPayload = AppUtils.readFile(ID_TOKEN_FILE_PATH);
        String userInfoPayload = AppUtils.readFile(USER_INFO_FILE_PATH);
        // Read input files for authorization
        String action = AppUtils.readFile(ACTION_FILE_PATH);
        String resourceJson = AppUtils.readFile(RESOURCE_FILE_PATH);
        String contextJson = AppUtils.readFile(CONTEXT_FILE_PATH);
        // Generate signed JWTs from the payloads
        Map<String, String> tokens = Map.of(
                "access_token", jwtCreator.createJwtFromJson(accessTokenPayload),
                "id_token", jwtCreator.createJwtFromJson(idTokenPayload),
                "userinfo_token", jwtCreator.createJwtFromJson(userInfoPayload)
        );

        // Perform authorization
        result = adapter.authorize(tokens, action, new JSONObject(resourceJson), new JSONObject(contextJson));

        assertNotNull(result);
        assertNotNull(result.getPerson());
        assertNotNull(result.getWorkload());
        assertTrue(result.getDecision());
        assertNotEquals(adapter.getLogsByRequestIdAndTag(result.getRequestId(), "System").size(), 0);
        assertNotEquals(adapter.getLogsByRequestId(result.getRequestId()).size(), 0);
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

}
