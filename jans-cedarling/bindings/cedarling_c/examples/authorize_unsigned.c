/*
 * This software is available under the Apache-2.0 license. 
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2025, Gluu, Inc. 
 */

/**
 * Example: Unsigned Authorization
 * 
 * This example demonstrates how to use the Cedarling C bindings for
 * authorization with custom principals (not derived from JWTs).
 * 
 * Use case: When you have your own authentication system and want to
 * pass principal information directly without JWT tokens.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "../target/include/cedarling_c.h"

// Configuration JSON - uses policy-store_ok_2.yaml for unsigned authorization
const char* BOOTSTRAP_CONFIG = "{\n"
    "    \"CEDARLING_APPLICATION_NAME\": \"UnsignedAuthExample\",\n"
    "    \"CEDARLING_POLICY_STORE_ID\": \"a1bf93115de86de760ee0bea1d529b521489e5a11747\",\n"
    "    \"CEDARLING_LOG_TYPE\": \"std_out\",\n"
    "    \"CEDARLING_LOG_TTL\": 60,\n"
    "    \"CEDARLING_LOG_LEVEL\": \"DEBUG\",\n"
    "    \"CEDARLING_POLICY_STORE_LOCAL_FN\": \"../../../test_files/policy-store_ok_2.yaml\"\n"
    "}";

// Unsigned authorization request with custom principals
// Uses TestPrincipal1/TestPrincipal2 entity types defined in policy-store_ok_2.yaml
// Note: attributes must match the schema (is_ok: Bool)
const char* REQUEST_JSON = "{\n"
    "    \"principals\": [\n"
    "        {\n"
    "            \"cedar_entity_mapping\": {\n"
    "                \"entity_type\": \"Jans::TestPrincipal1\",\n"
    "                \"id\": \"test_principal_001\"\n"
    "            },\n"
    "            \"is_ok\": true\n"
    "        },\n"
    "        {\n"
    "            \"cedar_entity_mapping\": {\n"
    "                \"entity_type\": \"Jans::TestPrincipal2\",\n"
    "                \"id\": \"test_principal_002\"\n"
    "            },\n"
    "            \"is_ok\": true\n"
    "        }\n"
    "    ],\n"
    "    \"action\": \"Jans::Action::\\\"UpdateForTestPrincipals\\\"\",\n"
    "    \"resource\": {\n"
    "        \"cedar_entity_mapping\": {\n"
    "            \"entity_type\": \"Jans::Issue\",\n"
    "            \"id\": \"issue_12345\"\n"
    "        },\n"
    "        \"org_id\": \"org_engineering_001\",\n"
    "        \"country\": \"US\"\n"
    "    },\n"
    "    \"context\": {}\n"
    "}";

void print_error(const char* operation) {
    char* error_msg = cedarling_get_last_error();
    if (error_msg) {
        printf("Error in %s: %s\n", operation, error_msg);
        cedarling_free_string(error_msg);
    } else {
        printf("Error in %s: Unknown error\n", operation);
    }
}

int main() {
    printf("Cedarling C Bindings - Unsigned Authorization Example\n");
    printf("=====================================================\n\n");

    // Initialize the library
    printf("1. Initializing Cedarling library...\n");
    if (cedarling_init() != 0) {
        printf("   Failed to initialize Cedarling library\n");
        return 1;
    }
    printf("   Library initialized successfully\n\n");

    // Print version
    printf("2. Library version: %s\n\n", cedarling_version());

    // Create a new instance
    printf("3. Creating Cedarling instance...\n");
    CedarlingInstanceResult instance_result;
    int result = cedarling_new(BOOTSTRAP_CONFIG, &instance_result);

    if (result != 0) {
        printf("   Failed to create instance (error code: %d)\n", result);
        if (instance_result.error_message) {
            printf("   Error: %s\n", instance_result.error_message);
            cedarling_free_instance_result(&instance_result);
        }
        print_error("cedarling_new");
        return 1;
    }

    uint64_t instance_id = instance_result.instance_id;
    printf("   Instance created successfully (ID: %llu)\n\n", (unsigned long long)instance_id);
    cedarling_free_instance_result(&instance_result);

    // Perform unsigned authorization
    printf("4. Performing unsigned authorization...\n");
    printf("   Using custom principals (TestPrincipal1, TestPrincipal2)\n");
    
    CedarlingResult auth_result;
    result = cedarling_authorize_unsigned(instance_id, REQUEST_JSON, &auth_result);

    if (result != 0) {
        printf("   Authorization failed (error code: %d)\n", result);
        if (auth_result.error_message) {
            printf("   Error: %s\n", auth_result.error_message);
        }
        print_error("cedarling_authorize_unsigned");
        cedarling_free_result(&auth_result);
        cedarling_drop(instance_id);
        return 1;
    }

    printf("   Authorization completed successfully\n");
    char* response = (char*)auth_result.data;
    printf("   Response: %.200s%s\n\n", response, strlen(response) > 200 ? "..." : "");

    // Check the overall decision
    if (strstr(response, "\"decision\":true") != NULL) {
        printf("   >> ACCESS GRANTED (overall) <<\n\n");
    } else {
        printf("   >> ACCESS DENIED (overall) <<\n");
    }

    cedarling_free_result(&auth_result);

    // Get logs
    printf("5. Retrieving logs...\n");
    CedarlingStringArray logs;
    result = cedarling_pop_logs(instance_id, &logs);

    if (result == 0) {
        printf("   Retrieved %zu log entries\n", logs.count);
        for (size_t i = 0; i < logs.count && i < 3; i++) {
            printf("   Log %zu: %.100s%s\n", i + 1, logs.items[i], 
                   strlen(logs.items[i]) > 100 ? "..." : "");
        }
        if (logs.count > 3) {
            printf("   ... and %zu more\n", logs.count - 3);
        }
        cedarling_free_string_array(&logs);
    } else {
        printf("   Failed to retrieve logs\n");
    }
    printf("\n");

    // Shutdown and cleanup
    printf("6. Shutting down...\n");
    cedarling_shutdown(instance_id);
    cedarling_cleanup();
    printf("   Cleanup completed\n\n");

    printf("Example completed successfully!\n");
    return 0;
}
