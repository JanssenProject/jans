/*
 * This software is available under the Apache-2.0 license. 
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2025, Gluu, Inc. 
 */

/**
 * Basic Example: Unsigned Authorization
 * 
 * This example demonstrates the basic usage of Cedarling C bindings
 * using unsigned authorization with custom principals.
 * 
 * For more detailed examples, see:
 * - authorize_unsigned.c - Detailed unsigned authorization example
 * - authorize_multi_issuer.c - JWT token-based authorization example
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "../target/include/cedarling_c.h"


// Sample configuration JSON - uses policy-store_ok_2.yaml for unsigned authorization
const char* BOOTSTRAP_CONFIG = "{\n"
    "    \"CEDARLING_APPLICATION_NAME\": \"TestApp\",\n"
    "    \"CEDARLING_POLICY_STORE_ID\": \"a1bf93115de86de760ee0bea1d529b521489e5a11747\",\n"
    "    \"CEDARLING_LOG_TYPE\": \"std_out\",\n"
    "    \"CEDARLING_LOG_TTL\": 60,\n"
    "    \"CEDARLING_LOG_LEVEL\": \"DEBUG\",\n"
    "    \"CEDARLING_POLICY_STORE_LOCAL_FN\": \"../../../test_files/policy-store_ok_2.yaml\"\n"
    "}";

// Sample unsigned authorization request with custom principals
// Uses TestPrincipal1 entity type defined in policy-store_ok_2.yaml schema
const char* REQUEST_JSON = "{\n"
    "    \"principals\": [\n"
    "        {\n"
    "            \"cedar_entity_mapping\": {\n"
    "                \"entity_type\": \"Jans::TestPrincipal1\",\n"
    "                \"id\": \"principal_1\"\n"
    "            },\n"
    "            \"is_ok\": true\n"
    "        }\n"
    "    ],\n"
    "    \"action\": \"Jans::Action::\\\"UpdateForTestPrincipals\\\"\",\n"
    "    \"resource\": {\n"
    "        \"cedar_entity_mapping\": {\n"
    "            \"entity_type\": \"Jans::Issue\",\n"
    "            \"id\": \"random_id\"\n"
    "        },\n"
    "        \"org_id\": \"some_long_id\",\n"
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
    printf("Cedarling C Bindings Example\n");
    printf("============================\n\n");

    // Initialize the library
    printf("1. Initializing Cedarling library...\n");
    if (cedarling_init() != 0) {
        printf("Failed to initialize Cedarling library\n");
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

    // Free the instance result
    cedarling_free_instance_result(&instance_result);

    // Perform authorization
    printf("4. Performing unsigned authorization...\n");
    CedarlingResult auth_result;
    result = cedarling_authorize_unsigned(instance_id, REQUEST_JSON, &auth_result);

    if (result != 0) {
        printf("   Authorization failed (error code: %d)\n", result);

        if (auth_result.error_message) {
            printf("   Error: %s\n", auth_result.error_message);
            print_error("cedarling_authorize_unsigned");
        }
        cedarling_free_result(&auth_result);
        cedarling_drop(instance_id);
        return 1;
    }

    printf("   Authorization completed successfully\n");
    printf("   Response: %s\n\n", auth_result.data ? (char*)auth_result.data : "null");

    // Free authorization result
    cedarling_free_result(&auth_result);

    // Get logs
    printf("5. Retrieving logs...\n");
    CedarlingStringArray logs;

    result = cedarling_pop_logs(instance_id, &logs);

    if (result == 0) {
        printf("   Retrieved %zu log entries\n", logs.count);
        for (size_t i = 0; i < logs.count; i++) {
            printf("   Log %zu: %.100s%s\n", i + 1, logs.items[i], strlen(logs.items[i]) > 100 ? "..." : "");
        }
        cedarling_free_string_array(&logs);
    } else {
        printf("   Failed to retrieve logs (error code: %d)\n", result);
        print_error("cedarling_pop_logs");
    }
    printf("\n");

    // Get log IDs
    printf("6. Retrieving log IDs...\n");
    CedarlingStringArray log_ids;
    result = cedarling_get_log_ids(instance_id, &log_ids);

    if (result == 0) {
        printf("   Retrieved %zu log IDs\n", log_ids.count);
        for (size_t i = 0; i < log_ids.count && i < 5; i++) {
            printf("   ID %zu: %s\n", i + 1, log_ids.items[i]);
        }
        if (log_ids.count > 5) {
            printf("   ... and %zu more\n", log_ids.count - 5);
        }

        cedarling_free_string_array(&log_ids);
    } else {
        printf("   Failed to retrieve log IDs (error code: %d)\n", result);
        print_error("cedarling_get_log_ids");
    }

    printf("\n");

    // Shutdown the instance
    printf("7. Shutting down instance...\n");
    result = cedarling_shutdown(instance_id);

    if (result == 0) {
        printf("   Instance shutdown successfully\n");
    } else {
        printf("   Failed to shutdown instance (error code: %d)\n", result);
        print_error("cedarling_shutdown");
    }

    // Drop the instance
    printf("8. Dropping instance...\n");
    cedarling_drop(instance_id);
    printf("   Instance dropped\n\n");

    // Cleanup
    printf("9. Cleaning up...\n");
    cedarling_cleanup();
    printf("   Cleanup completed\n\n");

    printf("Example completed successfully!\n");
    return 0;
}
