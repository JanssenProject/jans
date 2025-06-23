package org.example;

import org.example.jwt.JWTCreator;
import org.example.utils.AppUtils;
import org.example.utils.JsonUtil;
import uniffi.cedarling_uniffi.*;

import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        try {
            // Load Cedarling bootstrap configuration from file
            String bootstrapJson = AppUtils.readFile("./src/main/resources/config/bootstrap.json");
            Cedarling cedarling = Cedarling.Companion.loadFromJson(bootstrapJson);

            // Create a JWT creator with a secure secret (must be at least 32 bytes)
            JWTCreator jwtCreator = new JWTCreator("-very-strong-shared-secret-of-at-least-32-bytes!");

            // Read JWT payloads from files
            String accessTokenPayload = AppUtils.readFile("./src/main/resources/config/access_token_payload.json");
            String idTokenPayload = AppUtils.readFile("./src/main/resources/config/id_token_payload.json");
            String userInfoPayload = AppUtils.readFile("./src/main/resources/config/user_info_payload.json");

            // Generate signed JWTs from the payloads
            Map<String, String> tokens = Map.of(
                    "access_token", jwtCreator.createJwtFromJson(accessTokenPayload),
                    "id_token", jwtCreator.createJwtFromJson(idTokenPayload),
                    "userinfo_token", jwtCreator.createJwtFromJson(userInfoPayload)
            );

            // Read input files for authorization
            String action = AppUtils.readFile("./src/main/resources/config/action.txt");
            String resourceJson = AppUtils.readFile("./src/main/resources/config/resource.json");
            String contextJson = AppUtils.readFile("./src/main/resources/config/context.json");

            // Build EntityData from resource JSON
            EntityData resource = EntityData.Companion.fromJson(resourceJson);

            // Perform authorization
            AuthorizeResult result = cedarling.authorize(tokens, action, resource, contextJson);

            // Print decision and full result as pretty-formatted JSON
            System.out.println("Decision:\n" + JsonUtil.toPrettyJson(result.getDecision()));
            System.out.println("Result:\n" + JsonUtil.toPrettyJson(result));

            // Fetch and print logs associated with the request
            List<String> logs = cedarling.getLogsByRequestId(result.getRequestId());
            System.out.println("============ Logs ============");
            logs.forEach(System.out::println);

        } catch (CedarlingException e) {
            System.err.println("Authorization failed: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}