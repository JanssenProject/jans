/*
 * This software is available under the Apache-2.0 license. 
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2025, Gluu, Inc. 
 */

 #include<stdio.h>
 #include<stdlib.h>
 #include<string.h>
 #include<assert.h>
 
 #include "../target/include/cedarling_c.h"
 
// Test configuration - uses policy-store_ok_2.yaml for unsigned authorization
const char* TEST_CONFIG = "{\n"
    "    \"CEDARLING_APPLICATION_NAME\": \"TestApp\",\n"
    "    \"CEDARLING_POLICY_STORE_ID\": \"a1bf93115de86de760ee0bea1d529b521489e5a11747\",\n"
    "    \"CEDARLING_JWT_SIG_VALIDATION\": \"disabled\",\n"
    "    \"CEDARLING_JWT_STATUS_VALIDATION\": \"disabled\",\n"
    "    \"CEDARLING_LOG_TYPE\": \"std_out\",\n"
    "    \"CEDARLING_LOG_TTL\": 60,\n"
    "    \"CEDARLING_LOG_LEVEL\": \"DEBUG\",\n"
    "    \"CEDARLING_POLICY_STORE_LOCAL_FN\": \"../../../test_files/policy-store_ok_2.yaml\"\n"
    "}";
 
 static int test_run=0;
 static int test_passed=0;
 
 #define TEST_ASSERT(condition,message) do {\
 test_run++;\
 if (condition){\
     test_passed++;\
     printf(" [PASS] %s\n", message);\
 }else{\
     printf(" [FAIL] %s\n", message);\
    char* error = cedarling_get_last_error();\
     if (error) {\
         printf(" Error: %s\n", error);\
        cedarling_free_string(error);\
     }\
 }} while(0);
 
 
 void test_initialization(){
     printf("\nTEST: Library Initialization\n");
     printf("==============================\n");
 
     int result=cedarling_init();
     TEST_ASSERT(result==0,"Library Initialization");
 
     const char* version=cedarling_version();
     TEST_ASSERT(version!=NULL,"Version string available");
     TEST_ASSERT(strlen(version)>0,"Version string not empty");
 
     printf(" Version: %s\n",version);
 }
 
 void test_instance_creation(){
     printf("\nTest: Instance Creation\n");
     printf("=========================\n");
 
     CedarlingInstanceResult result;
     // Test valid configuration
     int ret = cedarling_new(TEST_CONFIG,&result);
 
     TEST_ASSERT(ret==0,"Create instance with valid config");
    TEST_ASSERT(result.instance_id>0,"Instance ID is valid");
    TEST_ASSERT(result.error_message==NULL,"No error message on success");
 
    uint64_t instance_id=result.instance_id;
     cedarling_free_instance_result(&result);
 
     // Test invalid configuration
     ret = cedarling_new("{invalid json}",&result);
     TEST_ASSERT(ret!=0,"Reject invalid JSON config");
    TEST_ASSERT(result.instance_id==0,"No Instance ID on error");
    TEST_ASSERT(result.error_message!=NULL,"Error message on provided");
 
     cedarling_free_instance_result(&result);
 
     // Test NULL parameters
     ret = cedarling_new(NULL,&result);
     TEST_ASSERT(ret!=0,"Reject NULL config");
 
     ret=cedarling_new(TEST_CONFIG,NULL);
     TEST_ASSERT(ret!=0,"Reject NULL result pointer");
 
     // Clean up
     cedarling_drop(instance_id);
 }
 
 void test_instance_with_env(){
     printf("\nTest: Instance Creation with Environment\n");
     printf("==========================================\n");
 
     CedarlingInstanceResult result;
 
     // Test with NULL config (environment only)
     int ret=cedarling_new_with_env(NULL,&result);
     // This might fail if environment variables are not set, which is expected
     if(ret==0){
        TEST_ASSERT(result.instance_id>0,"Instance created from environment");
        cedarling_drop(result.instance_id);
     }else{
        TEST_ASSERT(result.error_message!=NULL,"Error message for env failure");
         printf(" Note: Environment creation failed (expected if env vars not set)\n");
     }
     cedarling_free_instance_result(&result);
 
     // Test with a valid JSON config
     ret=cedarling_new_with_env(TEST_CONFIG,&result);
     TEST_ASSERT(ret==0,"Create instance with JSON config");
     if(ret==0){
        cedarling_drop(result.instance_id);
     }
     cedarling_free_instance_result(&result);
 }
 
void test_authorization(){
    printf("\nTest: Unsigned Authorization\n");
    printf("=============================\n");

    CedarlingInstanceResult instance_result;

    int ret=cedarling_new(TEST_CONFIG, &instance_result);
    if(ret!=0){
        printf("[FAIL] Failed to create instance for authorization tests\n");
        cedarling_free_instance_result(&instance_result);
        return;
    }

   uint64_t instance_id=instance_result.instance_id;
    cedarling_free_instance_result(&instance_result);

    // Test valid unsigned authorization request with TestPrincipal1
    // Uses TestPrincipal1 entity type from policy-store_ok_2.yaml
    // Policy permits when principal.is_ok == true
    const char* valid_request = "{\n"
        "    \"principals\": [\n"
        "        {\n"
        "            \"cedar_entity_mapping\": {\n"
        "                \"entity_type\": \"Jans::TestPrincipal1\",\n"
        "                \"id\": \"test_principal_1\"\n"
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

    CedarlingResult auth_result;

    ret=cedarling_authorize_unsigned(instance_id,valid_request,&auth_result);

    // Test the authorization result
    TEST_ASSERT(ret==0,"Authorization request executed successfully");
    if(ret==0){
       TEST_ASSERT(auth_result.data!=NULL,"Authorization result data provided");
        
        // Cast uint32_t* to char* for string operations
       char* result_str = (char*)auth_result.data;
        printf(" Authorization result: %.200s...\n", result_str);
        
        // Verify the response structure contains expected fields
        // Note: The overall decision depends on principal_bool_operator config (not exposed to C)
        // Default operator requires Jans::Workload AND Jans::User, so decision will be false
        // when using TestPrincipal1. We verify the individual principal got "allow".
        TEST_ASSERT(strstr(result_str, "\"decision\":false") != NULL,
                   "Top-level decision should be false for this test policy setup");
        TEST_ASSERT(strstr(result_str, "\"decision\":\"allow\"") != NULL,
                   "Individual principal decision should be allow");
        TEST_ASSERT(strstr(result_str, "principals") != NULL,
                   "Response should contain principals field");
    }else{
       printf(" Authorization error: %s\n", auth_result.error_message ? auth_result.error_message : "unknown");
    }
    cedarling_free_result(&auth_result);

    // Test invalid JSON request
    ret=cedarling_authorize_unsigned(instance_id,"{Invalid Json}",&auth_result);
    TEST_ASSERT(ret!=0,"Reject invalid JSON request");
    TEST_ASSERT(auth_result.error_message!=NULL,"Error message for invalid JSON");
    cedarling_free_result(&auth_result);

    // Test NULL parameters
    ret=cedarling_authorize_unsigned(instance_id,NULL,&auth_result);
    TEST_ASSERT(ret!=0,"Reject NULL request");
    ret=cedarling_authorize_unsigned(instance_id,valid_request,NULL);
    TEST_ASSERT(ret!=0,"Reject NULL result pointer");

    // Test invalid instance ID
    ret=cedarling_authorize_unsigned(99999,valid_request,&auth_result);
    TEST_ASSERT(ret!=0,"Reject invalid instance ID");
    cedarling_free_result(&auth_result);

    // Clean up
    cedarling_drop(instance_id);
}
 
 void test_logging_functions(){
     printf("\nTest: Logging Functions\n");
     printf("=========================\n");
 
     CedarlingInstanceResult instance_result;
 
     int ret=cedarling_new(TEST_CONFIG,&instance_result);
     if(ret!=0){
         printf("[FAIL] Failed to create instance for logging tests\n");
         cedarling_free_instance_result(&instance_result);
         return;
     }
    uint64_t instance_id=instance_result.instance_id;
     cedarling_free_instance_result(&instance_result);
 
     // Test get log IDs
     CedarlingStringArray log_ids;
     ret=cedarling_get_log_ids(instance_id,&log_ids);
     TEST_ASSERT(ret==0,"Get log IDs function executes");
    // TEST_ASSERT(log_ids.count >= 0, "Log IDs count is valid");
    printf(" Found %zu log IDs\n", log_ids.count);
     cedarling_free_string_array(&log_ids);
 
     // Test pop logs
     CedarlingStringArray logs;
     ret=cedarling_pop_logs(instance_id,&logs);
     TEST_ASSERT(ret==0,"Pop logs function executes");
    // TEST_ASSERT(logs.count >= 0, "Logs count is valid");
    printf(" Found %zu logs\n", logs.count);
     cedarling_free_string_array(&logs);
 
     // Test get logs by Tag
     ret=cedarling_get_logs_by_tag(instance_id, "test_tag", &logs);
     TEST_ASSERT(ret==0,"Get logs by tag function executes");
     cedarling_free_string_array(&logs);
 
     // Test get logs by request ID
     ret=cedarling_get_logs_by_request_id(instance_id,"test_request_id", &logs);
     TEST_ASSERT(ret==0,"Get logs by request ID function executes");
     cedarling_free_string_array(&logs);
 
     // Test get log request ID and Tag
     ret=cedarling_get_logs_by_request_id_and_tag(instance_id,"test_request_id", "test_tag", &logs);
     TEST_ASSERT(ret==0,"Get logs by request ID and tag function executes");
     cedarling_free_string_array(&logs);
 
     // Test get log by ID — unknown id is an error, not empty success
     CedarlingResult log_result;
     ret = cedarling_get_log_by_id(instance_id, "nonexistent_log_id_xyz", &log_result);
     TEST_ASSERT(ret != 0, "Get log by ID returns error for unknown id");
     TEST_ASSERT(log_result.data == NULL, "No log data for unknown id");
     TEST_ASSERT(log_result.error_message != NULL, "Error message for unknown id");
     cedarling_free_result(&log_result);

     // If any logs remain, get by id succeeds for a real id
     ret = cedarling_get_log_ids(instance_id, &log_ids);
     TEST_ASSERT(ret == 0, "Get log IDs for get_log_by_id success path");
     if (log_ids.count > 0 && log_ids.items != NULL && log_ids.items[0] != NULL) {
         ret = cedarling_get_log_by_id(instance_id, log_ids.items[0], &log_result);
         TEST_ASSERT(ret == 0, "Get log by ID with real id succeeds");
         TEST_ASSERT(log_result.data != NULL, "Log data is not NULL for real id");
         printf(" Log data: %.200s...\n", (char*)log_result.data);
         cedarling_free_result(&log_result);
     }
     cedarling_free_string_array(&log_ids);
 
     // Test with invalid instance ID
    ret=cedarling_get_log_ids(99999, &logs);
    TEST_ASSERT(ret!=0,"Reject invalid instance ID for get log IDs");
    TEST_ASSERT(logs.count == 0, "No log IDs for invalid instance");
     cedarling_free_string_array(&logs);
 
    ret=cedarling_pop_logs(99999, &logs);
    TEST_ASSERT(ret!=0,"Reject invalid instance ID for pop logs");
    TEST_ASSERT(logs.count == 0, "No log IDs for invalid instance");
     cedarling_free_string_array(&logs);
 
    ret=cedarling_get_logs_by_tag(99999, "test_tag", &logs);
    TEST_ASSERT(ret!=0,"Reject invalid instance ID for get logs by tag");
    TEST_ASSERT(logs.count == 0, "No log IDs for invalid instance");
     cedarling_free_string_array(&logs);
 
    ret=cedarling_get_logs_by_request_id(99999, "test_request_id", &logs);
    TEST_ASSERT(ret!=0,"Reject invalid instance ID for get logs by request ID");
    TEST_ASSERT(logs.count == 0, "No log IDs for invalid instance");
     cedarling_free_string_array(&logs);
 
    ret=cedarling_get_logs_by_request_id_and_tag(99999, "test_request_id", "test_tag", &logs);
    TEST_ASSERT(ret!=0,"Reject invalid instance ID for get logs by request ID and tag");
    TEST_ASSERT(logs.count == 0, "No log IDs for invalid instance");
     cedarling_free_string_array(&logs);
 
     ret=cedarling_get_log_by_id(99999, "test_log_id", &log_result);
     TEST_ASSERT(ret!=0,"Reject invalid instance ID for get log by ID");
   TEST_ASSERT(log_result.data == NULL, "No log data for invalid instance");
   TEST_ASSERT(log_result.error_message != NULL, "Error message provided for invalid instance");
     cedarling_free_result(&log_result);
 
     // Clean up
     cedarling_drop(instance_id);
 }
 
void test_trusted_issuer_loading_info() {
    printf("\nTest: Trusted Issuer Loading Info\n");
    printf("===================================\n");

    CedarlingInstanceResult instance_result;
    int ret = cedarling_new(TEST_CONFIG, &instance_result);
    if (ret != 0) {
        printf("[FAIL] Failed to create instance for trusted issuer tests\n");
        cedarling_free_instance_result(&instance_result);
        return;
    }

   uint64_t instance_id = instance_result.instance_id;
    cedarling_free_instance_result(&instance_result);

    bool loaded_by_name = true;
    ret = cedarling_is_trusted_issuer_loaded_by_name(instance_id, "missing_issuer", &loaded_by_name);
    TEST_ASSERT(ret == 0, "Trusted issuer by name call succeeds");
    TEST_ASSERT(loaded_by_name == false, "Trusted issuer by name returns false for missing issuer");

    bool loaded_by_iss = true;
    ret = cedarling_is_trusted_issuer_loaded_by_iss(instance_id, "https://missing.example.org", &loaded_by_iss);
    TEST_ASSERT(ret == 0, "Trusted issuer by iss call succeeds");
    TEST_ASSERT(loaded_by_iss == false, "Trusted issuer by iss returns false for missing issuer");

    size_t total = 0;
    size_t loaded_count = 0;
    ret = cedarling_total_issuers(instance_id, &total);
    TEST_ASSERT(ret == 0, "Total issuers call succeeds");
    ret = cedarling_loaded_trusted_issuers_count(instance_id, &loaded_count);
    TEST_ASSERT(ret == 0, "Loaded trusted issuers count call succeeds");
    TEST_ASSERT(total >= loaded_count, "Total issuers is greater than or equal to loaded count");

    CedarlingStringArray loaded_ids;
    ret = cedarling_loaded_trusted_issuer_ids(instance_id, &loaded_ids);
    TEST_ASSERT(ret == 0, "Get loaded trusted issuer IDs executes");
   TEST_ASSERT(loaded_ids.count == loaded_count, "Loaded IDs count matches loaded trusted issuers count");
    cedarling_free_string_array(&loaded_ids);

    CedarlingStringArray failed_ids;
    ret = cedarling_failed_trusted_issuer_ids(instance_id, &failed_ids);
    TEST_ASSERT(ret == 0, "Get failed trusted issuer IDs executes");
   TEST_ASSERT(total == loaded_count + failed_ids.count, "Total issuers equals loaded + failed issuer counts");
    cedarling_free_string_array(&failed_ids);

    // NULL parameter handling for issuer lookup methods
    loaded_by_name = true;
    ret = cedarling_is_trusted_issuer_loaded_by_name(instance_id, NULL, &loaded_by_name);
    TEST_ASSERT(ret != 0, "Reject NULL issuer_id parameter");
    TEST_ASSERT(loaded_by_name == false, "Output reset on NULL issuer_id");

    loaded_by_iss = true;
    ret = cedarling_is_trusted_issuer_loaded_by_iss(instance_id, NULL, &loaded_by_iss);
    TEST_ASSERT(ret != 0, "Reject NULL iss_claim parameter");
    TEST_ASSERT(loaded_by_iss == false, "Output reset on NULL iss_claim");

    // Invalid instance handling
    loaded_by_name = true;
    ret = cedarling_is_trusted_issuer_loaded_by_name(99999, "any", &loaded_by_name);
    TEST_ASSERT(ret != 0, "Invalid instance is rejected for issuer-by-name lookup");
    TEST_ASSERT(loaded_by_name == false, "Invalid instance returns false for issuer-by-name lookup");

    loaded_by_iss = true;
    ret = cedarling_is_trusted_issuer_loaded_by_iss(99999, "https://example.org", &loaded_by_iss);
    TEST_ASSERT(ret != 0, "Invalid instance is rejected for issuer-by-iss lookup");
    TEST_ASSERT(loaded_by_iss == false, "Invalid instance returns false for issuer-by-iss lookup");

    total = 999;
    ret = cedarling_total_issuers(99999, &total);
    TEST_ASSERT(ret != 0, "Invalid instance rejects total issuers");
    TEST_ASSERT(total == 0, "Invalid instance resets total issuers output to 0");

    loaded_count = 999;
    ret = cedarling_loaded_trusted_issuers_count(99999, &loaded_count);
    TEST_ASSERT(ret != 0, "Invalid instance rejects loaded issuers count");
    TEST_ASSERT(loaded_count == 0, "Invalid instance resets loaded issuers count output to 0");

    ret = cedarling_is_trusted_issuer_loaded_by_name(instance_id, "any", NULL);
    TEST_ASSERT(ret != 0, "Reject NULL out_result for issuer-by-name lookup");

    ret = cedarling_is_trusted_issuer_loaded_by_iss(instance_id, "https://example.org", NULL);
    TEST_ASSERT(ret != 0, "Reject NULL out_result for issuer-by-iss lookup");

    ret = cedarling_total_issuers(instance_id, NULL);
    TEST_ASSERT(ret != 0, "Reject NULL out_count for total issuers");

    ret = cedarling_loaded_trusted_issuers_count(instance_id, NULL);
    TEST_ASSERT(ret != 0, "Reject NULL out_count for loaded issuers count");

    ret = cedarling_loaded_trusted_issuer_ids(99999, &loaded_ids);
    TEST_ASSERT(ret != 0, "Loaded trusted issuer IDs rejects invalid instance");
   TEST_ASSERT(loaded_ids.count == 0, "No loaded trusted issuer IDs for invalid instance");
    cedarling_free_string_array(&loaded_ids);

    ret = cedarling_failed_trusted_issuer_ids(99999, &failed_ids);
    TEST_ASSERT(ret != 0, "Failed trusted issuer IDs rejects invalid instance");
   TEST_ASSERT(failed_ids.count == 0, "No failed trusted issuer IDs for invalid instance");
    cedarling_free_string_array(&failed_ids);

    // NULL result pointers for array-returning methods
    ret = cedarling_loaded_trusted_issuer_ids(instance_id, NULL);
    TEST_ASSERT(ret != 0, "Reject NULL result pointer for loaded trusted issuer IDs");

    ret = cedarling_failed_trusted_issuer_ids(instance_id, NULL);
    TEST_ASSERT(ret != 0, "Reject NULL result pointer for failed trusted issuer IDs");

    cedarling_drop(instance_id);
}

 void test_memory_management(){
     printf("\nTest: Memory Management\n");
     printf("=========================\n");
 
     // Test string freeing (should not crash)
     char* test_str=NULL;
     cedarling_free_string(test_str);
     TEST_ASSERT(1,"Free NULL string pointer");
 
     // Test Array freeing (should not crash)
     CedarlingStringArray empty_array = {NULL, 0};
     cedarling_free_string_array(&empty_array);
     TEST_ASSERT(1,"Free empty string array");
 
     // Test result freeing (should not crash)
     CedarlingResult empty_result = {0, NULL, NULL};
     cedarling_free_result(&empty_result);
     TEST_ASSERT(1,"Free empty result");
 
     // Test instance result freeing (should not crash)
     CedarlingInstanceResult empty_instance_result = {0, 0, NULL};
     cedarling_free_instance_result(&empty_instance_result);
     TEST_ASSERT(1,"Free empty instance result");
 }
 
 void test_error_handling(){
     printf("\nTest: Error Handling\n");
     printf("======================\n");
 
     cedarling_clear_last_error();
    char* error = cedarling_get_last_error();
     TEST_ASSERT(error == NULL, "No error after clear");
 
     // Trigger an error
     CedarlingInstanceResult result;
     cedarling_new("{invalid}",&result);
     error=cedarling_get_last_error();
     TEST_ASSERT(error != NULL, "Error message set after failure");
    if (error) {
        TEST_ASSERT(strlen(error) > 0, "Error message is not empty");
        printf(" Error message: %s\n", error);
        cedarling_free_string(error);
    }
 
     cedarling_free_instance_result(&result);
 
     // Clear error
     cedarling_clear_last_error();
     error = cedarling_get_last_error();
     TEST_ASSERT(error == NULL, "Error cleared successfully");
 }
 
 void test_instance_lifecycle(){
     printf("\nTest: Instance Lifecycle\n");
     printf("=========================\n");
 
     // Create an instance
     CedarlingInstanceResult result;
     int ret = cedarling_new(TEST_CONFIG, &result);
     TEST_ASSERT(ret == 0, "Create instance successfully");
 
     if(ret!=0){
         cedarling_free_instance_result(&result);
         return;
     }
 
    uint64_t instance_id = result.instance_id;
     cedarling_free_instance_result(&result);
 
     // Shutdown the instance
     ret = cedarling_shutdown(instance_id);
     TEST_ASSERT(ret == 0, "Shutdown instance successfully");
 
     // Drop the instance
     cedarling_drop(instance_id);
     TEST_ASSERT(1,"Drop instance (should not crash)");
 
     // Try to use dropped instance (should fail gracefully)
     CedarlingStringArray logs;
    ret=cedarling_get_log_ids(instance_id, &logs);
    TEST_ASSERT(ret!=0,"Dropped instance reports not found");
    TEST_ASSERT(logs.count == 0, "No logs from dropped instance");
     cedarling_free_string_array(&logs);
 
 }
 
 int main() {
     printf("Starting Cedarling C Binding Tests\n");
     printf("==================================\n");
     
     test_initialization();
     
     test_instance_creation();
 
     test_instance_with_env();
 
     test_authorization();
 
     test_logging_functions();

    test_trusted_issuer_loading_info();
 
     test_memory_management();
 
     test_error_handling();
 
     test_instance_lifecycle();
 
     // Final cleanup
     cedarling_cleanup();
 
     printf("\n");
     printf("Test Results\n");
     printf("============\n");
     printf("Tests run: %d\n", test_run);
     printf("Tests passed: %d\n", test_passed);
     printf("Tests failed: %d\n", test_run - test_passed);
 
     if(test_passed==test_run){
         printf("All tests passed successfully!\n");
         return 0;
     } else {
         printf("Some tests failed. Please check the output above for details.\n");
         return 1;
     }
 
 }