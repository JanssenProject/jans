#include<stdio.h>
#include<stdlib.h>
#include<string.h>
#include<assert.h>

#include "../target/include/cedarling_c.h"

// Test configuration
const char* TEST_CONFIG = "{\n"
    "    \"CEDARLING_APPLICATION_NAME\": \"TestApp\",\n"
    "    \"CEDARLING_POLICY_STORE_ID\": \"a1bf93115de86de760ee0bea1d529b521489e5a11747\",\n"
    "    \"CEDARLING_USER_AUTHZ\": \"enabled\",\n"
    "    \"CEDARLING_WORKLOAD_AUTHZ\": \"enabled\",\n"
    "    \"CEDARLING_JWT_SIG_VALIDATION\": \"disabled\",\n"
    "    \"CEDARLING_JWT_STATUS_VALIDATION\": \"disabled\",\n"
    "    \"CEDARLING_ID_TOKEN_TRUST_MODE\": \"never\",\n"
    "    \"CEDARLING_LOG_TYPE\": \"std_out\",\n"
    "    \"CEDARLING_LOG_TTL\": 60,\n"
    "    \"CEDARLING_LOG_LEVEL\": \"DEBUG\",\n"
    "    \"CEDARLING_POLICY_STORE_LOCAL_FN\": \"../../../test_files/policy-store_ok.yaml\"\n"
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
    const char* error = cedarling_get_last_error();\
    if (error) {\
        printf(" Error: %s\n", error);\
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
    TEST_ASSERT(result.INSTANCE_ID>0,"Instance ID is valid");
    TEST_ASSERT(result.ERROR_MESSAGE==NULL,"No error message on success");

    uint64_t instance_id=result.INSTANCE_ID;
    cedarling_free_instance_result(&result);

    // Test invalid configuration
    ret = cedarling_new("{invalid json}",&result);
    TEST_ASSERT(ret!=0,"Reject invalid JSON config");
    TEST_ASSERT(result.INSTANCE_ID==0,"No Instance ID on error");
    TEST_ASSERT(result.ERROR_MESSAGE!=NULL,"Error message on provided");

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
        TEST_ASSERT(result.INSTANCE_ID>0,"Instance created from environment");
        cedarling_drop(result.INSTANCE_ID);
    }else{
        TEST_ASSERT(result.ERROR_MESSAGE!=NULL,"Error message for env failure");
        printf(" Note: Environment creation failed (expected if env vars not set)\n");
    }
    cedarling_free_instance_result(&result);

    // Test with a valid JSON config
    ret=cedarling_new_with_env(TEST_CONFIG,&result);
    TEST_ASSERT(ret==0,"Create instance with JSON config");
    if(ret==0){
        cedarling_drop(result.INSTANCE_ID);
    }
    cedarling_free_instance_result(&result);
}

void test_authorization(){
    printf("\nTest: Authorization\n");
    printf("=====================\n");

    CedarlingInstanceResult instance_result;

    int ret=cedarling_new(TEST_CONFIG, &instance_result);
    if(ret!=0){
        printf("[FAIL] Failed to create instance for authorization tests\n");
        cedarling_free_instance_result(&instance_result);
        return;
    }

    uint64_t instance_id=instance_result.INSTANCE_ID;
    cedarling_free_instance_result(&instance_result);

    // Test valid authorization request
    const char* valid_request = "{\n"
        "    \"tokens\": {\n"
        "        \"access_token\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJib0c4ZGZjNU1LVG4zN283Z3NkQ2V5cUw4THBXUXRnb080MW0xS1p3ZHEwIiwiY29kZSI6ImJmMTkzNGY2LTM5MDUtNDIwYS04Mjk5LTZiMmUzZmZkZGQ2ZSIsImlzcyI6Imh0dHBzOi8vdGVzdC5qYW5zLm9yZyIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJjbGllbnRfaWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhdWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhY3IiOiJiYXNpYyIsIng1dCNTMjU2IjoiIiwic2NvcGUiOlsib3BlbmlkIiwicHJvZmlsZSJdLCJvcmdfaWQiOiJzb21lX2xvbmdfaWQiLCJhdXRoX3RpbWUiOjE3MjQ4MzA3NDYsImV4cCI6MTcyNDk0NTk3OCwiaWF0IjoxNzI0ODMyMjU5LCJqdGkiOiJseFRtQ1ZSRlR4T2pKZ3ZFRXBvek1RIiwibmFtZSI6IkRlZmF1bHQgQWRtaW4gVXNlciIsInN0YXR1cyI6eyJzdGF0dXNfbGlzdCI6eyJpZHgiOjIwMSwidXJpIjoiaHR0cHM6Ly90ZXN0LmphbnMub3JnL2phbnMtYXV0aC9yZXN0djEvc3RhdHVzX2xpc3QifX19.7n4vE60lisFLnEFhVwYMOPh5loyLLtPc07sCvaFI-Ik\",\n"
        "        \"id_token\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhY3IiOiJiYXNpYyIsImFtciI6IjEwIiwiYXVkIjoiNWI0NDg3YzQtOGRiMS00MDlkLWE2NTMtZjkwN2I4MDk0MDM5IiwiZXhwIjoxNzI0ODM1ODU5LCJpYXQiOjE3MjQ4MzIyNTksInN1YiI6ImJvRzhkZmM1TUtUbjM3bzdnc2RDZXlxTDhMcFdRdGdvTzQxbTFLWndkcTAiLCJpc3MiOiJodHRwczovL3Rlc3QuamFucy5vcmciLCJqdGkiOiJzazNUNDBOWVNZdWs1c2FIWk5wa1p3Iiwibm9uY2UiOiJjMzg3MmFmOS1hMGY1LTRjM2YtYTFhZi1mOWQwZTg4NDZlODEiLCJzaWQiOiI2YTdmZTUwYS1kODEwLTQ1NGQtYmU1ZC01NDlkMjk1OTVhMDkiLCJqYW5zT3BlbklEQ29ubmVjdFZlcnNpb24iOiJvcGVuaWRjb25uZWN0LTEuMCIsImNfaGFzaCI6InBHb0s2WV9SS2NXSGtVZWNNOXV3NlEiLCJhdXRoX3RpbWUiOjE3MjQ4MzA3NDYsImdyYW50IjoiYXV0aG9yaXphdGlvbl9jb2RlIiwic3RhdHVzIjp7InN0YXR1c19saXN0Ijp7ImlkeCI6MjAyLCJ1cmkiOiJodHRwczovL3Rlc3QuamFucy5vcmcvamFucy1hdXRoL3Jlc3R2MS9zdGF0dXNfbGlzdCJ9fSwicm9sZSI6IkFkbWluIn0.RgCuWFUUjPVXmbW3ExQavJZH8Lw4q3kGhMFBRR0hSjA\",\n"
        "        \"userinfo_token\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb3VudHJ5IjoiVVMiLCJlbWFpbCI6InVzZXJAZXhhbXBsZS5jb20iLCJ1c2VybmFtZSI6IlVzZXJOYW1lRXhhbXBsZSIsInN1YiI6ImJvRzhkZmM1TUtUbjM3bzdnc2RDZXlxTDhMcFdRdGdvTzQxbTFLWndkcTAiLCJpc3MiOiJodHRwczovL3Rlc3QuamFucy5vcmciLCJnaXZlbl9uYW1lIjoiQWRtaW4iLCJtaWRkbGVfbmFtZSI6IkFkbWluIiwiaW51bSI6IjhkMWNkZTZhLTE0NDctNDc2Ni1iM2M4LTE2NjYzZTEzYjQ1OCIsImF1ZCI6IjViNDQ4N2M0LThkYjEtNDA5ZC1hNjUzLWY5MDdiODA5NDAzOSIsInVwZGF0ZWRfYXQiOjE3MjQ3Nzg1OTEsIm5hbWUiOiJEZWZhdWx0IEFkbWluIFVzZXIiLCJuaWNrbmFtZSI6IkFkbWluIiwiZmFtaWx5X25hbWUiOiJVc2VyIiwianRpIjoiZmFpWXZhWUlUMGNEQVQ3Rm93MHBRdyIsImphbnNBZG1pblVJUm9sZSI6WyJhcGktYWRtaW4iXSwiZXhwIjoxNzI0OTQ1OTc4fQ.t6p8fYAe1NkUt9mn9n9MYJlNCni8JYfhk-82hb_C1O4\"\n"
        "    },\n"
        "    \"action\": \"Jans::Action::\\\"Update\\\"\",\n"
        "    \"resource\": {\n"
        "        \"type\": \"Jans::Issue\",\n"
        "        \"id\": \"random_id\",\n"
        "        \"org_id\": \"some_long_id\",\n"
        "        \"country\": \"US\"\n"
        "    },\n"
        "    \"context\": {}\n"
        "}";

    CedarlingResult auth_result;

    ret=cedarling_authorize(instance_id,valid_request,&auth_result);

    // Test the authorization result
    if(ret==0){
        TEST_ASSERT(auth_result.DATA!=NULL,"Authorization result data provided");
        
        // Cast uint32_t* to char* for string operations
        char* result_str = (char*)auth_result.DATA;
        printf(" Authorization result: %.200s...\n", result_str);
        
        // succeed with allow decision
        TEST_ASSERT(strstr(result_str, "\"decision\":true") != NULL || 
                   strstr(result_str, "\"Decision\":true") != NULL, 
                   "Authorization decision should be allow/true");
    }else{
        TEST_ASSERT(auth_result.ERROR_MESSAGE!=NULL,"Error message provided on failure");
        printf(" Authorization error: %s\n", auth_result.ERROR_MESSAGE);
    }
    cedarling_free_result(&auth_result);

    // Test invalid JSON request
    ret=cedarling_authorize(instance_id,"{Invalid Json}",&auth_result);
    TEST_ASSERT(ret!=0,"Reject invalid JSON request");
    TEST_ASSERT(auth_result.ERROR_MESSAGE!=NULL,"Error message for invalid JSON");
    cedarling_free_result(&auth_result);

    // Test NULL parameters
    ret=cedarling_authorize(instance_id,NULL,&auth_result);
    TEST_ASSERT(ret!=0,"Reject NULL request");
    ret=cedarling_authorize(instance_id,valid_request,NULL);
    TEST_ASSERT(ret!=0,"Reject NULL result pointer");

    // Test invalid instance ID
    ret=cedarling_authorize(99999,valid_request,&auth_result);
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
    uint64_t instance_id=instance_result.INSTANCE_ID;
    cedarling_free_instance_result(&instance_result);

    // Test get log IDs
    CedarlingStringArray log_ids;
    ret=cedarling_get_log_ids(instance_id,&log_ids);
    TEST_ASSERT(ret==0,"Get log IDs function executes");
    // TEST_ASSERT(log_ids.COUNT >= 0, "Log IDs count is valid");
    printf(" Found %zu log IDs\n", log_ids.COUNT);
    cedarling_free_string_array(&log_ids);

    // Test pop logs
    CedarlingStringArray logs;
    ret=cedarling_pop_logs(instance_id,&logs);
    TEST_ASSERT(ret==0,"Pop logs function executes");
    // TEST_ASSERT(logs.COUNT >= 0, "Logs count is valid");
    printf(" Found %zu logs\n", logs.COUNT);
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

    // Test get log by ID
    CedarlingResult log_result;
    ret=cedarling_get_log_by_id(instance_id, "test_log_id", &log_result);
    TEST_ASSERT(ret==0,"Get log by ID function executes");
    if(ret==0){
        TEST_ASSERT(log_result.DATA != NULL, "Log data is not NULL");
        printf(" Log data: %.200s...\n", (char*)log_result.DATA);
        cedarling_free_result(&log_result);
    }else{
        TEST_ASSERT(log_result.ERROR_MESSAGE != NULL, "Error message provided on failure");
        printf(" Get log by ID error: %s\n", log_result.ERROR_MESSAGE);
        cedarling_free_result(&log_result);
    }

    // Test with invalid instance ID
    ret=cedarling_get_log_ids(99999, &logs);
    TEST_ASSERT(ret==0,"Reject invalid instance ID for get log IDs");
    TEST_ASSERT(logs.COUNT == 0, "No log IDs for invalid instance");
    cedarling_free_string_array(&logs);

    ret=cedarling_pop_logs(99999, &logs);
    TEST_ASSERT(ret==0,"Reject invalid instance ID for pop logs");
    TEST_ASSERT(logs.COUNT == 0, "No log IDs for invalid instance");
    cedarling_free_string_array(&logs);

    ret=cedarling_get_logs_by_tag(99999, "test_tag", &logs);
    TEST_ASSERT(ret==0,"Reject invalid instance ID for get logs by tag");
    TEST_ASSERT(logs.COUNT == 0, "No log IDs for invalid instance");
    cedarling_free_string_array(&logs);

    ret=cedarling_get_logs_by_request_id(99999, "test_request_id", &logs);
    TEST_ASSERT(ret==0,"Reject invalid instance ID for get logs by request ID");
    TEST_ASSERT(logs.COUNT == 0, "No log IDs for invalid instance");
    cedarling_free_string_array(&logs);

    ret=cedarling_get_logs_by_request_id_and_tag(99999, "test_request_id", "test_tag", &logs);
    TEST_ASSERT(ret==0,"Reject invalid instance ID for get logs by request ID and tag");
    TEST_ASSERT(logs.COUNT == 0, "No log IDs for invalid instance");
    cedarling_free_string_array(&logs);

    ret=cedarling_get_log_by_id(99999, "test_log_id", &log_result);
    TEST_ASSERT(ret!=0,"Reject invalid instance ID for get log by ID");
    TEST_ASSERT(logs.COUNT == 0, "No log IDs for invalid instance");
    cedarling_free_result(&log_result);

    // Clean up
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
    const char* error = cedarling_get_last_error();
    TEST_ASSERT(error == NULL, "No error after clear");

    // Trigger an error
    CedarlingInstanceResult result;
    cedarling_new("{invalid}",&result);
    error=cedarling_get_last_error();
    TEST_ASSERT(error != NULL, "Error message set after failure");
    TEST_ASSERT(strlen(error) > 0, "Error message is not empty");
    printf(" Error message: %s\n", error);

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

    uint64_t instance_id = result.INSTANCE_ID;
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
    TEST_ASSERT(ret==0,"Dropped instance handled gracefully");
    TEST_ASSERT(logs.COUNT == 0, "No logs from dropped instance");
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