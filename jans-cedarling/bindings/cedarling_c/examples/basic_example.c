#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "../target/include/cedarling_c.h"


// Sample configuration JSON
const char* BOOTSTRAP_CONFIG = "{\n"
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

// Sample Request JSON
const char* REQUEST_JSON = "{\n"
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

void print_error(const char* operation){
    const char* error_msg=cedarling_get_last_error();
    if(error_msg){
        printf("Error in %s: %s\n",operation,error_msg);
    }else{
        printf("Error in %s: Unknown error",operation);
    }
}

int main(){
    printf("Cedarling C Bindings Example\n");
    printf("============================\n\n");

    // Initialize the library
    printf("1. Initializing Cedarling library...\n");
    if(cedarling_init()!=0){
        printf("Failed to initialize Cedarling librar\n");
        return 1;
    }

    printf("  Library initialized successfully\n\n");

    // Print version
    printf("2. Library version: %s\n\n",cedarling_version());

    // Create a new instance
    printf("3. Creating Cedarling instance...\n");
    CedarlingInstanceResult instance_result;
    int result=cedarling_new(BOOTSTRAP_CONFIG,&instance_result);

    if(result!=0){
        printf(" Failed to create instance (error code: %d\n)",result);
        if(instance_result.ERROR_MESSAGE){
            printf(" Error:%s\n",instance_result.ERROR_MESSAGE);
            cedarling_free_instance_result(&instance_result);
        }
        print_error("cedarling_new");
        return 1;
    }

    uint64_t instance_id=instance_result.INSTANCE_ID;
    printf(" Instance created successfully (ID: %llu)\n\n",(unsigned long long)instance_id);

    // Free the instance result
    cedarling_free_instance_result(&instance_result);

    // Perform authorization
    printf("4. Performing authorization...\n");
    CedarlingResult auth_result;
    result=cedarling_authorize(instance_id,REQUEST_JSON,&auth_result);

    if(result!=0){
        printf(" Authorization failed (error code: %d)\n",result);

        if(auth_result.ERROR_MESSAGE){
            printf("Error: %s\n",auth_result.ERROR_MESSAGE);
            print_error("cedarling_authorize");
        }
        cedarling_free_result(&auth_result);
        cedarling_drop(instance_id);
        return 1;
    }

    printf(" Authorization completed successfully\n");
    printf(" Response: %s\n\n",auth_result.DATA?(char*)auth_result.DATA:"null");

    // Free authorization reult
    cedarling_free_result(&auth_result);

    // Get logs
    printf("5. Retrieving logs...\n");
    CedarlingStringArray logs;

    result = cedarling_pop_logs(instance_id,&logs);

    if(result==0){
        printf(" Log %zu log entries\n",logs.COUNT);
        for (size_t i=0;i<logs.COUNT;i++){
            printf("Log %zu: %.100s%s\n", i + 1, logs.ITEMS[i], strlen(logs.ITEMS[i]) > 100 ? "..." : "");
        }
        cedarling_free_string_array(&logs);
    }else{
        printf("Failed to retieve logs (error code: %d)\n",result);
        print_error("cedarling_pop_logs");
    }
    printf("\n");

    // Get log IDs
    printf("6. Retrieving log IDs...\n");
    CedarlingStringArray log_ids;
    result=cedarling_get_log_ids(instance_id,&log_ids);

    if(result==0){
        printf(" Retieved %zu log IDs\n",log_ids.COUNT);
        for(size_t i=0;i<log_ids.COUNT && i<5;i++){
            printf(" ID %zu: %s\n",i+1,log_ids.ITEMS[i]);
        }
        if(log_ids.COUNT>5){
            printf("  ... and %zu more\n",log_ids.COUNT-5);
        }

        cedarling_free_string_array(&log_ids);
    }else{
        printf("Failed to retrieve log IDs (error code: %d)\n",result);
        print_error("cedarling_get_log_ids");
    }

    printf("\n");

    // Shutdown the instance
    printf("7. Shutting down instance...\n");
    result=cedarling_shutdown(instance_id);

    if(result==0){
        printf(" Instance shutdown successfully");

    }else{
        printf("Failed to shutdown instance (error code: %d)\n",result);
        print_error("cedarling_shutdown");
    }

    // Drop the instance
    printf("8. Dropping instance...\n");
    cedarling_drop(instance_id);
    printf("Instance dropped\n\n");

    // Cleanup
    printf("9. Cleaning up...\n");
    cedarling_cleanup();
    printf("Cleanup completed \n\n");

    printf("Example completed successfully");
    return 0;
}