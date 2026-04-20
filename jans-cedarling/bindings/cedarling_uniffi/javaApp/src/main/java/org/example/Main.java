package org.example;

import org.example.utils.AppUtils;
import org.example.utils.JsonUtil;
import uniffi.cedarling_uniffi.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        try {
            // Load Cedarling bootstrap configuration from file
            String bootstrapJson = AppUtils.readFile("./src/main/resources/config/bootstrap.json");
            Cedarling cedarling = Cedarling.Companion.loadFromJson(bootstrapJson);

            // Optional principal: use first entry from principals.json if present (matches single-principal API)
            String principalsJson = AppUtils.readFile("./src/main/resources/config/principals.json");
            ObjectMapper mapper = new ObjectMapper();
            JsonNode principalsRoot = mapper.readTree(principalsJson);
            EntityData principal = null;
            if (principalsRoot.isArray() && principalsRoot.size() > 0) {
                principal =
                        EntityData.Companion.fromJson(
                                mapper.writeValueAsString(principalsRoot.get(0)));
            } else if (principalsRoot.isObject()) {
                principal = EntityData.Companion.fromJson(principalsJson);
            }

            // Read input files for authorization
            String action = AppUtils.readFile("./src/main/resources/config/action.txt");
            String resourceJson = AppUtils.readFile("./src/main/resources/config/resource.json");
            String contextJson = AppUtils.readFile("./src/main/resources/config/context.json");

            // Build EntityData from resource JSON
            EntityData resource = EntityData.Companion.fromJson(resourceJson);

            // Perform authorization (unsigned: optional principal + action + resource + context)
            AuthorizeResult result = cedarling.authorizeUnsigned(principal, action, resource, contextJson);

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
