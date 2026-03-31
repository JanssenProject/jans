/*
 * This software is available under the Apache-2.0 license. 
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2025, Gluu, Inc. 
 */

/**
 * Example: Multi-Issuer Authorization
 * 
 * This example demonstrates how to use the Cedarling C bindings for
 * authorization with JWT tokens from multiple issuers.
 * 
 * Use case: When you have JWT tokens (access_token, id_token, etc.) from
 * OIDC providers and want to authorize based on token claims.
 * 
 * Note: In production, you should enable JWT signature validation.
 * This example uses disabled validation for demonstration purposes only.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "../target/include/cedarling_c.h"

// Configuration JSON - JWT validation disabled for demo
// Uses policy-store-multi-issuer-basic.yaml which has schema supporting tokens in context
const char* BOOTSTRAP_CONFIG = "{\n"
    "    \"CEDARLING_APPLICATION_NAME\": \"MultiIssuerExample\",\n"
    "    \"CEDARLING_POLICY_STORE_ID\": \"multi_issuer_basic_store\",\n"
    "    \"CEDARLING_JWT_SIG_VALIDATION\": \"disabled\",\n"
    "    \"CEDARLING_JWT_STATUS_VALIDATION\": \"disabled\",\n"
    "    \"CEDARLING_LOG_TYPE\": \"memory\",\n"
    "    \"CEDARLING_LOG_TTL\": 60,\n"
    "    \"CEDARLING_LOG_LEVEL\": \"DEBUG\",\n"
    "    \"CEDARLING_POLICY_STORE_LOCAL_FN\": \"../../../test_files/policy-store-multi-issuer-basic.yaml\"\n"
    "}";

// Multi-issuer authorization request with a Dolphin userinfo token
// The token mapping "Dolphin::Userinfo_token" maps to the trusted issuer's token metadata.
// NOTE: This is a sample JWT - validation is disabled so the signature doesn't matter.
// The JWT payload contains: {"iss":"https://idp.dolphin.sea","sub":"dolphin_user_123",...,"role":["admin","user"]}
// This matches the policy basic_token_query_4 which checks context.tokens.dolphin_userinfo_token.role.contains("admin")
const char* REQUEST_JSON = "{\n"
    "    \"tokens\": [\n"
    "        {\n"
    "            \"mapping\": \"Dolphin::Userinfo_token\",\n"
    "            \"payload\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJodHRwczovL2lkcC5kb2xwaGluLnNlYSIsInN1YiI6ImRvbHBoaW5fdXNlcl8xMjMiLCJqdGkiOiJkb2xwaGluX3VzZXJfMTIzIiwiY2xpZW50X2lkIjoiZG9scGhpbl9jbGllbnRfMTIzIiwiYXVkIjoiZG9scGhpbl9hdWRpZW5jZSIsImV4cCI6MjAwMDAwMDAwMCwiaWF0IjoxNTE2MjM5MDIyLCJyb2xlIjpbImFkbWluIiwidXNlciJdfQ.placeholder_signature\"\n"
    "        }\n"
    "    ],\n"
    "    \"action\": \"Acme::Action::\\\"CheckRoleFoodApprover\\\"\",\n"
    "    \"resource\": {\n"
    "        \"cedar_entity_mapping\": {\n"
    "            \"entity_type\": \"Acme::Resource\",\n"
    "            \"id\": \"ApprovedDolphinFoods\"\n"
    "        },\n"
    "        \"name\": \"Approved Dolphin Foods\"\n"
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
    printf("Cedarling C Bindings - Multi-Issuer Authorization Example\n");
    printf("=========================================================\n\n");

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

    // Perform multi-issuer authorization
    printf("4. Performing multi-issuer authorization...\n");
    printf("   Using token: Dolphin::Userinfo_token with role=[admin, user]\n");
    
    CedarlingResult auth_result;
    result = cedarling_authorize_multi_issuer(instance_id, REQUEST_JSON, &auth_result);

    if (result != 0) {
        printf("   Authorization failed (error code: %d)\n", result);
        if (auth_result.error_message) {
            printf("   Error: %s\n", auth_result.error_message);
        }
        print_error("cedarling_authorize_multi_issuer");
        cedarling_free_result(&auth_result);
        cedarling_drop(instance_id);
        return 1;
    }

    printf("   Authorization completed successfully\n");
    char* response = (char*)auth_result.data;
    printf("   Response: %.200s%s\n\n", response, strlen(response) > 200 ? "..." : "");

    // Check the decision
    if (strstr(response, "\"decision\":true") != NULL) {
        printf("   >> ACCESS GRANTED <<\n\n");
    } else {
        printf("   >> ACCESS DENIED <<\n\n");
    }

    cedarling_free_result(&auth_result);

    // Get logs
    printf("5. Retrieving logs...\n");
    CedarlingStringArray logs;
    result = cedarling_pop_logs(instance_id, &logs);

    if (result == 0) {
        printf("   Retrieved %zu log entries\n", logs.count);
        for (size_t i = 0; i < logs.count && i < 5; i++) {
            printf("   Log %zu: %.100s%s\n", i + 1, logs.items[i], 
                   strlen(logs.items[i]) > 100 ? "..." : "");
        }
        if (logs.count > 5) {
            printf("   ... and %zu more\n", logs.count - 5);
        }
        cedarling_free_string_array(&logs);
    } else {
        printf("   Failed to retrieve logs\n");
    }
    printf("\n");

    // Shutdown and cleanup
    printf("6. Shutting down...\n");
    cedarling_shutdown(instance_id);
    cedarling_drop(instance_id);
    cedarling_cleanup();
    printf("   Cleanup completed\n\n");

    printf("Example completed successfully!\n");
    return 0;
}
