package org.example;

import org.example.utils.AppUtils;
import org.example.utils.JsonUtil;
import uniffi.cedarling_uniffi.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        try {
            // Load Cedarling bootstrap configuration from file
            String bootstrapJson = AppUtils.readFile("./src/main/resources/config/bootstrap.json");
            Cedarling cedarling = Cedarling.Companion.loadFromJson(bootstrapJson);

            // Read principals from JSON array
            String principalsJson = AppUtils.readFile("./src/main/resources/config/principals.json");
            List<EntityData> principals = new ArrayList<>();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode array = mapper.readTree(principalsJson);
            for (JsonNode element : array) {
                principals.add(EntityData.Companion.fromJson(mapper.writeValueAsString(element)));
            }

            // Read input files for authorization
            String action = AppUtils.readFile("./src/main/resources/config/action.txt");
            String resourceJson = AppUtils.readFile("./src/main/resources/config/resource.json");
            String contextJson = AppUtils.readFile("./src/main/resources/config/context.json");

            // Build EntityData from resource JSON
            EntityData resource = EntityData.Companion.fromJson(resourceJson);

            // Perform authorization (unsigned: principals + action + resource + context)
            AuthorizeResult result = cedarling.authorizeUnsigned(principals, action, resource, contextJson);

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
