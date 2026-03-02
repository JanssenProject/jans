package cedarling_go

import (
	"encoding/json"
	"fmt"
	"reflect"
	"testing"
	"time"
)

var bootstrapConfig string = `
{
        "CEDARLING_APPLICATION_NAME": "TestApp",
        "CEDARLING_POLICY_STORE_ID": "a1bf93115de86de760ee0bea1d529b521489e5a11747",
        "CEDARLING_USER_AUTHZ": "enabled",
        "CEDARLING_WORKLOAD_AUTHZ": "enabled",
        "CEDARLING_JWT_SIG_VALIDATION": "disabled",
        "CEDARLING_JWT_STATUS_VALIDATION": "disabled",
        "CEDARLING_ID_TOKEN_TRUST_MODE": "never",
        "CEDARLING_LOG_TYPE": "off",
        "CEDARLING_LOG_TTL": 60,
        "CEDARLING_LOG_LEVEL": "DEBUG",
        "CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED": ["HS256"]
}
`

const (

	// JSON payload of access token
	// {
	//   "sub": "boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0",
	//   "code": "bf1934f6-3905-420a-8299-6b2e3ffddd6e",
	//   "iss": "https://test.jans.org",
	//   "token_type": "Bearer",
	//   "client_id": "5b4487c4-8db1-409d-a653-f907b8094039",
	//   "aud": "5b4487c4-8db1-409d-a653-f907b8094039",
	//   "acr": "basic",
	//   "x5t#S256": "",
	//   "scope": [
	//     "openid",
	//     "profile"
	//   ],
	//   "org_id": "some_long_id",
	//   "auth_time": 1724830746,
	//   "exp": 1724945978,
	//   "iat": 1724832259,
	//   "jti": "lxTmCVRFTxOjJgvEEpozMQ",
	//   "name": "Default Admin User",
	//   "status": {
	//     "status_list": {
	//       "idx": 201,
	//       "uri": "https://test.jans.org/jans-auth/restv1/status_list"
	//     }
	//   }
	// }
	accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJib0c4ZGZjNU1LVG4zN283Z3NkQ2V5cUw4THBXUXRnb080MW0xS1p3ZHEwIiwiY29kZSI6ImJmMTkzNGY2LTM5MDUtNDIwYS04Mjk5LTZiMmUzZmZkZGQ2ZSIsImlzcyI6Imh0dHBzOi8vdGVzdC5qYW5zLm9yZyIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJjbGllbnRfaWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhdWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhY3IiOiJiYXNpYyIsIng1dCNTMjU2IjoiIiwic2NvcGUiOlsib3BlbmlkIiwicHJvZmlsZSJdLCJvcmdfaWQiOiJzb21lX2xvbmdfaWQiLCJhdXRoX3RpbWUiOjE3MjQ4MzA3NDYsImV4cCI6MTcyNDk0NTk3OCwiaWF0IjoxNzI0ODMyMjU5LCJqdGkiOiJseFRtQ1ZSRlR4T2pKZ3ZFRXBvek1RIiwibmFtZSI6IkRlZmF1bHQgQWRtaW4gVXNlciIsInN0YXR1cyI6eyJzdGF0dXNfbGlzdCI6eyJpZHgiOjIwMSwidXJpIjoiaHR0cHM6Ly90ZXN0LmphbnMub3JnL2phbnMtYXV0aC9yZXN0djEvc3RhdHVzX2xpc3QifX19.7n4vE60lisFLnEFhVwYMOPh5loyLLtPc07sCvaFI-Ik"

	// JSON payload of id token
	// {
	//   "acr": "basic",
	//   "amr": "10",
	//   "aud": "5b4487c4-8db1-409d-a653-f907b8094039",
	//   "exp": 1724835859,
	//   "iat": 1724832259,
	//   "sub": "boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0",
	//   "iss": "https://test.jans.org",
	//   "jti": "sk3T40NYSYuk5saHZNpkZw",
	//   "nonce": "c3872af9-a0f5-4c3f-a1af-f9d0e8846e81",
	//   "sid": "6a7fe50a-d810-454d-be5d-549d29595a09",
	//   "jansOpenIDConnectVersion": "openidconnect-1.0",
	//   "c_hash": "pGoK6Y_RKcWHkUecM9uw6Q",
	//   "auth_time": 1724830746,
	//   "grant": "authorization_code",
	//   "status": {
	//     "status_list": {
	//       "idx": 202,
	//       "uri": "https://test.jans.org/jans-auth/restv1/status_list"
	//     }
	//   },
	//   "role": "Admin"
	// }
	idToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhY3IiOiJiYXNpYyIsImFtciI6IjEwIiwiYXVkIjpbIjViNDQ4N2M0LThkYjEtNDA5ZC1hNjUzLWY5MDdiODA5NDAzOSJdLCJleHAiOjE3MjQ4MzU4NTksImlhdCI6MTcyNDgzMjI1OSwic3ViIjoiYm9HOGRmYzVNS1RuMzdvN2dzZENleXFMOExwV1F0Z29PNDFtMUtad2RxMCIsImlzcyI6Imh0dHBzOi8vdGVzdC5qYW5zLm9yZyIsImp0aSI6InNrM1Q0ME5ZU1l1azVzYUhaTnBrWnciLCJub25jZSI6ImMzODcyYWY5LWEwZjUtNGMzZi1hMWFmLWY5ZDBlODg0NmU4MSIsInNpZCI6IjZhN2ZlNTBhLWQ4MTAtNDU0ZC1iZTVkLTU0OWQyOTU5NWEwOSIsImphbnNPcGVuSURDb25uZWN0VmVyc2lvbiI6Im9wZW5pZGNvbm5lY3QtMS4wIiwiY19oYXNoIjoicEdvSzZZX1JLY1dIa1VlY005dXc2USIsImF1dGhfdGltZSI6MTcyNDgzMDc0NiwiZ3JhbnQiOiJhdXRob3JpemF0aW9uX2NvZGUiLCJzdGF0dXMiOnsic3RhdHVzX2xpc3QiOnsiaWR4IjoyMDIsInVyaSI6Imh0dHBzOi8vdGVzdC5qYW5zLm9yZy9qYW5zLWF1dGgvcmVzdHYxL3N0YXR1c19saXN0In19LCJyb2xlIjoiQWRtaW4ifQ.RgCuWFUUjPVXmbW3ExQavJZH8Lw4q3kGhMFBRR0hSjA"

	// JSON payload of userinfo token
	// {
	//   "country": "US",
	//   "email": "user@example.com",
	//   "username": "UserNameExample",
	//   "sub": "boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0",
	//   "iss": "https://test.jans.org",
	//   "given_name": "Admin",
	//   "middle_name": "Admin",
	//   "inum": "8d1cde6a-1447-4766-b3c8-16663e13b458",
	//   "aud": "5b4487c4-8db1-409d-a653-f907b8094039",
	//   "updated_at": 1724778591,
	//   "name": "Default Admin User",
	//   "nickname": "Admin",
	//   "family_name": "User",
	//   "jti": "faiYvaYIT0cDAT7Fow0pQw",
	//   "jansAdminUIRole": [
	//     "api-admin"
	//   ],
	//   "exp": 1724945978
	// }
	userinfoToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb3VudHJ5IjoiVVMiLCJlbWFpbCI6InVzZXJAZXhhbXBsZS5jb20iLCJ1c2VybmFtZSI6IlVzZXJOYW1lRXhhbXBsZSIsInN1YiI6ImJvRzhkZmM1TUtUbjM3bzdnc2RDZXlxTDhMcFdRdGdvTzQxbTFLWndkcTAiLCJpc3MiOiJodHRwczovL3Rlc3QuamFucy5vcmciLCJnaXZlbl9uYW1lIjoiQWRtaW4iLCJtaWRkbGVfbmFtZSI6IkFkbWluIiwiaW51bSI6IjhkMWNkZTZhLTE0NDctNDc2Ni1iM2M4LTE2NjYzZTEzYjQ1OCIsImF1ZCI6IjViNDQ4N2M0LThkYjEtNDA5ZC1hNjUzLWY5MDdiODA5NDAzOSIsInVwZGF0ZWRfYXQiOjE3MjQ3Nzg1OTEsIm5hbWUiOiJEZWZhdWx0IEFkbWluIFVzZXIiLCJuaWNrbmFtZSI6IkFkbWluIiwiZmFtaWx5X25hbWUiOiJVc2VyIiwianRpIjoiZmFpWXZhWUlUMGNEQVQ3Rm93MHBRdyIsImphbnNBZG1pblVJUm9sZSI6WyJhcGktYWRtaW4iXSwiZXhwIjoxNzI0OTQ1OTc4fQ.t6p8fYAe1NkUt9mn9n9MYJlNCni8JYfhk-82hb_C1O4"
)

func loadTestConfig(configUpdate func(conf map[string]any)) (map[string]any, error) {
	policyStoreJsonPath := "../../test_files/policy-store_ok.yaml"

	var config map[string]any
	if err := json.Unmarshal([]byte(bootstrapConfig), &config); err != nil {
		return nil, err
	}
	config["CEDARLING_POLICY_STORE_LOCAL_FN"] = policyStoreJsonPath
	if configUpdate != nil {
		configUpdate(config)
	}
	return config, nil
}

func TestAuthorizeSuccess(t *testing.T) {
	config, err := loadTestConfig(nil)
	if err != nil {
		t.Fatalf("Failed to load test config: %v", err)
	}

	instance, err := NewCedarling(config)
	if err != nil {
		t.Fatalf("Failed to create Cedarling instance: %v", err)
	}

	resource := EntityData{
		CedarMapping: CedarEntityMapping{
			EntityType: "Jans::Issue",
			ID:         "random_id",
		},
		Payload: map[string]any{
			"org_id":  "some_long_id",
			"country": "US",
		},
	}

	request := Request{
		Tokens: map[string]string{
			"access_token":   accessToken,
			"id_token":       idToken,
			"userinfo_token": userinfoToken,
		},
		Action:   "Jans::Action::\"Update\"",
		Resource: resource,
		// will be mapped to {}
		Context: nil,
	}

	result, err := instance.Authorize(request)
	if err != nil {
		t.Fatalf("Authorization failed: %v", err)
	}

	if !result.Decision {
		t.Errorf("Expected allow decision, got DENY")
	}

	if result.Person.Decision() != DecisionAllow {
		t.Errorf("Expected allow decision, got DENY")
	}
	if !reflect.DeepEqual(result.Person.Reason(), []string{"444da5d85403f35ea76519ed1a18a33989f855bf1cf8"}) {
		t.Errorf("Expected reason for person to be 444da5d85403f35ea76519ed1a18a33989f855bf1cf8, got %v", result.Person.Reason())
	}

	if result.Workload.Decision() != DecisionAllow {
		t.Errorf("Expected allow decision, got %s", result.Workload.Decision().ToString())
	}
	if !reflect.DeepEqual(result.Workload.Reason(), []string{"840da5d85403f35ea76519ed1a18a33989f855bf1cf8"}) {
		t.Errorf("Expected reason for workload to be 840da5d85403f35ea76519ed1a18a33989f855bf1cf8, got %v", result.Workload.Reason())
	}
}

func BenchmarkAuthorize(b *testing.B) {
	config, err := loadTestConfig(nil)
	if err != nil {
		b.Fatalf("Failed to load test config: %v", err)
	}

	instance, err := NewCedarling(config)
	if err != nil {
		b.Fatalf("Failed to create Cedarling instance: %v", err)
	}

	resource := EntityData{
		CedarMapping: CedarEntityMapping{
			EntityType: "Jans::Issue",
			ID:         "random_id",
		},
		Payload: map[string]any{
			"org_id":  "some_long_id",
			"country": "US",
		},
	}

	request := Request{
		Tokens: map[string]string{
			"access_token":   accessToken,
			"id_token":       idToken,
			"userinfo_token": userinfoToken,
		},
		Action:   "Jans::Action::\"Update\"",
		Resource: resource,
		// will be mapped to {}
		Context: nil,
	}
	b.ResetTimer()
	result, err := instance.Authorize(request)
	if err != nil {
		b.Fatalf("Authorization failed: %v", err)
	}
	if !result.Decision {
		b.Fatalf("Decision false")
	}
}

func BenchmarkAuthorizeUnsigned(b *testing.B) {
	config, err := loadTestConfig(func(conf map[string]any) {
		conf["CEDARLING_PRINCIPAL_BOOLEAN_OPERATION"] = `{
            "and": [
                {"===": [{"var": "Jans::TestPrincipal1"}, "ALLOW"]},
                {"===": [{"var": "Jans::TestPrincipal2"}, "ALLOW"]},
                {"===": [{"var": "Jans::TestPrincipal3"}, "DENY"]}
            ]
        }`
		conf["CEDARLING_POLICY_STORE_LOCAL_FN"] = "../../test_files/policy-store_ok_2.yaml"
	})
	if err != nil {
		b.Fatalf("Failed to load test config: %v", err)
	}

	instance, err := NewCedarling(config)
	if err != nil {
		b.Fatalf("Failed to create Cedarling instance: %v", err)
	}

	resource := EntityData{
		CedarMapping: CedarEntityMapping{
			EntityType: "Jans::Issue",
			ID:         "random_id",
		},
		Payload: map[string]any{
			"org_id":  "some_long_id",
			"country": "US",
		},
	}

	principals := []EntityData{
		{
			CedarMapping: CedarEntityMapping{
				EntityType: "Jans::TestPrincipal1",
				ID:         "1",
			},
			Payload: map[string]any{
				"is_ok": true,
			},
		},
		{
			CedarMapping: CedarEntityMapping{
				EntityType: "Jans::TestPrincipal2",
				ID:         "2",
			},
			Payload: map[string]any{
				"is_ok": true,
			},
		},
		{
			CedarMapping: CedarEntityMapping{
				EntityType: "Jans::TestPrincipal3",
				ID:         "3",
			},
			Payload: map[string]any{
				"is_ok": false,
			},
		},
	}
	b.ResetTimer()
	request := RequestUnsigned{
		Principals: principals,
		Action:     "Jans::Action::\"UpdateForTestPrincipals\"",
		Resource:   resource,
		// will be mapped to {}
		Context: nil,
	}

	result, err := instance.AuthorizeUnsigned(request)
	if err != nil {
		b.Fatalf("Authorization failed: %v", err)
	}
	if !result.Decision {
		b.Fatalf("Decision false")
	}
}
func TestAuthorizeUnsignedSuccess(t *testing.T) {
	config, err := loadTestConfig(func(conf map[string]any) {
		conf["CEDARLING_PRINCIPAL_BOOLEAN_OPERATION"] = `{
            "and": [
                {"===": [{"var": "Jans::TestPrincipal1"}, "ALLOW"]},
                {"===": [{"var": "Jans::TestPrincipal2"}, "ALLOW"]},
                {"===": [{"var": "Jans::TestPrincipal3"}, "DENY"]}
            ]
        }`
		conf["CEDARLING_POLICY_STORE_LOCAL_FN"] = "../../test_files/policy-store_ok_2.yaml"
	})
	if err != nil {
		t.Fatalf("Failed to load test config: %v", err)
	}

	instance, err := NewCedarling(config)
	if err != nil {
		t.Fatalf("Failed to create Cedarling instance: %v", err)
	}

	resource := EntityData{
		CedarMapping: CedarEntityMapping{
			EntityType: "Jans::Issue",
			ID:         "random_id",
		},
		Payload: map[string]any{
			"org_id":  "some_long_id",
			"country": "US",
		},
	}

	principals := []EntityData{
		{
			CedarMapping: CedarEntityMapping{
				EntityType: "Jans::TestPrincipal1",
				ID:         "1",
			},
			Payload: map[string]any{
				"is_ok": true,
			},
		},
		{
			CedarMapping: CedarEntityMapping{
				EntityType: "Jans::TestPrincipal2",
				ID:         "2",
			},
			Payload: map[string]any{
				"is_ok": true,
			},
		},
		{
			CedarMapping: CedarEntityMapping{
				EntityType: "Jans::TestPrincipal3",
				ID:         "3",
			},
			Payload: map[string]any{
				"is_ok": false,
			},
		},
	}

	request := RequestUnsigned{
		Principals: principals,
		Action:     "Jans::Action::\"UpdateForTestPrincipals\"",
		Resource:   resource,
		// will be mapped to {}
		Context: nil,
	}

	result, err := instance.AuthorizeUnsigned(request)
	if err != nil {
		t.Fatalf("Authorization failed: %v", err)
	}

	if !result.Decision {
		t.Errorf("Expected allow decision, got %v", result.Decision)
	}

	if !result.Principals["Jans::TestPrincipal1"].IsAllowed() {
		t.Errorf("Expected allow decision for Jans::TestPrincipal1, got %s", result.Principals["Jans::TestPrincipal1"].Decision().ToString())
	}
	if !reflect.DeepEqual(result.Principals["Jans::TestPrincipal1"].Reason(), []string{"5"}) {
		t.Errorf("Expected Reason 5 for Jans::TestPrincipal1, got %s", result.Principals["Jans::TestPrincipal1"].Reason())
	}

	if !result.Principals["Jans::TestPrincipal2"].IsAllowed() {
		t.Errorf("Expected allow decision for Jans::TestPrincipal2, got %s", result.Principals["Jans::TestPrincipal2"].Decision().ToString())
	}
	if !reflect.DeepEqual(result.Principals["Jans::TestPrincipal2"].Reason(), []string{"5"}) {
		t.Errorf("Expected Reason 6 for Jans::TestPrincipal2, got %s", result.Principals["Jans::TestPrincipal2"].Reason())
	}

	if result.Principals["Jans::TestPrincipal3"].IsAllowed() {
		t.Errorf("Expected DENY decision for Jans::TestPrincipal3, got %s", result.Principals["Jans::TestPrincipal3"].Decision().ToString())
	}
	if !reflect.DeepEqual(result.Principals["Jans::TestPrincipal3"].Reason(), []string{}) {
		t.Errorf("Expected empty reason  for Jans::TestPrincipal3, got %s", result.Principals["Jans::TestPrincipal3"].Reason())
	}

}

func TestAuthorizeValidationError(t *testing.T) {
	config, err := loadTestConfig(nil)
	if err != nil {
		t.Fatalf("Failed to load test config: %v", err)
	}

	instance, err := NewCedarling(config)
	if err != nil {
		t.Fatalf("Failed to create Cedarling instance: %v", err)
	}

	// Invalid resource - org_id should be string but we set it to int
	resource := EntityData{
		CedarMapping: CedarEntityMapping{
			EntityType: "Jans::Issue",
			ID:         "random_id",
		},
		Payload: map[string]any{
			"org_id":  1, // Should be string
			"country": "US",
		},
	}

	request := Request{
		Tokens: map[string]string{
			"access_token":   accessToken,
			"id_token":       idToken,
			"userinfo_token": userinfoToken,
		},
		Action:   "Jans::Action::\"Update\"",
		Resource: resource,
		Context:  nil,
	}

	_, err = instance.Authorize(request)
	if err == nil {
		t.Fatal("Expected validation error, got nil")
	}
}

func ExampleNewCedarling() {
	// load bootstrap config
	config, err := loadTestConfig(nil)
	if err != nil {
		fmt.Printf("Failed to load bootstrap config: %v\n", err)
	}

	instance, err := NewCedarling(config)
	if err != nil {
		fmt.Printf("Failed to create Cedarling instance: %v\n", err)
	}
	instance.ShutDown()
}

func ExampleNewCedarlingWithEnv() {
	instance, err := NewCedarlingWithEnv(nil)
	if err != nil {
		fmt.Printf("Failed to create Cedarling instance: %v\n", err)
	}
	instance.ShutDown()
}

func ExampleCedarling_Authorize() {
	// load bootstrap configuration
	config, err := loadTestConfig(nil)
	if err != nil {
		fmt.Printf("Failed to load bootstrap config: %v\n", err)
	}

	instance, err := NewCedarling(config)
	if err != nil {
		fmt.Printf("Failed to create Cedarling instance: %v\n", err)
	}

	resource := EntityData{
		CedarMapping: CedarEntityMapping{
			EntityType: "Jans::Issue",
			ID:         "random_id",
		},
		Payload: map[string]any{
			"org_id":  "some_long_id",
			"country": "US",
		},
	}

	request := Request{
		Tokens: map[string]string{
			"access_token":   "accessTokenJWT",
			"id_token":       "idTokenJWT",
			"userinfo_token": "userinfoTokenJWT",
		},
		Action:   "Jans::Action::\"Update\"",
		Resource: resource,
		// will be mapped to {}
		Context: nil,
	}

	result, err := instance.Authorize(request)
	if err != nil {
		fmt.Printf("Authorization failed: %v\n", err)
	}
	fmt.Println(result.Decision)

}

func ExampleCedarling_AuthorizeUnsigned() {
	// load bootstrap configuration
	config, err := loadTestConfig(nil)
	if err != nil {
		fmt.Printf("Failed to load bootstrap config: %v\n", err)
	}

	instance, err := NewCedarling(config)
	if err != nil {
		fmt.Printf("Failed to create Cedarling instance: %v\n", err)
	}

	resource := EntityData{
		CedarMapping: CedarEntityMapping{
			EntityType: "Jans::Issue",
			ID:         "random_id",
		},
		Payload: map[string]any{
			"org_id":  "some_long_id",
			"country": "US",
		},
	}

	principals := []EntityData{
		{
			CedarMapping: CedarEntityMapping{
				EntityType: "Jans::Principal",
				ID:         "1",
			},
			Payload: map[string]any{
				"is_ok": true,
			},
		},
	}

	request := RequestUnsigned{
		Principals: principals,
		Action:     "Jans::Action::\"Update\"",
		Resource:   resource,
		// will be mapped to {}
		Context: nil,
	}
	result, err := instance.AuthorizeUnsigned(request)
	if err != nil {
		fmt.Printf("Authorization failed: %v\n", err)
	}
	fmt.Println(result.Decision)
}

func ExampleCedarling_GetLogIds() {
	config, err := loadTestConfig(nil)
	if err != nil {
		fmt.Printf("Failed to load bootstrap config: %v\n", err)
	}
	config["CEDARLING_LOG_TYPE"] = "memory" // logs are only stored when running in memory logging mode

	instance, err := NewCedarling(config)
	if err != nil {
		fmt.Printf("Failed to create Cedarling instance: %v\n", err)
	}
	log_ids := instance.GetLogIds()
	fmt.Println(log_ids[0])
}

func ExampleCedarling_GetLogsByTag() {
	config, err := loadTestConfig(nil)
	if err != nil {
		fmt.Printf("Failed to load bootstrap config: %v\n", err)
	}
	config["CEDARLING_LOG_TYPE"] = "memory" // logs are only stored when running in memory logging mode

	instance, err := NewCedarling(config)
	if err != nil {
		fmt.Printf("Failed to create Cedarling instance: %v\n", err)
	}
	logs := instance.GetLogsByTag("debug")
	fmt.Println(logs[0])
}

// getMultiIssuerConfig returns configuration for multi-issuer tests
func getMultiIssuerConfig() map[string]interface{} {
	return map[string]interface{}{
		"CEDARLING_APPLICATION_NAME":                     "TestApp",
		"CEDARLING_POLICY_STORE_ID":                      "a1bf93115de86de760ee0bea1d529b521489e5a11747",
		"CEDARLING_JWT_SIG_VALIDATION":                   "disabled",
		"CEDARLING_JWT_STATUS_VALIDATION":                "disabled",
		"CEDARLING_ID_TOKEN_TRUST_MODE":                  "never",
		"CEDARLING_POLICY_STORE_LOCAL_FN":                "../../test_files/policy-store-multi-issuer-test.yaml",
		"CEDARLING_AUTHORIZATION_USE_WORKLOAD_PRINCIPAL": "false",
		"CEDARLING_AUTHORIZATION_USE_USER_PRINCIPAL":     "false",
		"CEDARLING_USER_AUTHZ":                           "enabled",
		"CEDARLING_WORKLOAD_AUTHZ":                       "enabled",
		"CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED":   []string{"HS256"},
	}
}

// getTestResource returns a standard test resource
func getTestResource() EntityData {
	return EntityData{
		CedarMapping: CedarEntityMapping{
			EntityType: "Jans::Issue",
			ID:         "random_id",
		},
		Payload: map[string]interface{}{
			"country": "US",
			"org_id":  "some_long_id",
		},
	}
}

// TestSingleTokenAuthorization tests single token authorization
// (matches Rust test_single_acme_access_token_authorization)
func TestSingleTokenAuthorization(t *testing.T) {
	config := getMultiIssuerConfig()
	instance, err := NewCedarling(config)
	if err != nil {
		t.Fatalf("Failed to create Cedarling instance: %v", err)
	}

	request := AuthorizeMultiIssuerRequest{
		Tokens:   []TokenInput{{Mapping: "Jans::Access_Token", Payload: accessToken}},
		Action:   "Jans::Action::\"Update\"",
		Context:  map[string]interface{}{},
		Resource: getTestResource(),
	}

	result, err := instance.AuthorizeMultiIssuer(request)
	if err != nil {
		t.Fatalf("Failed to authorize multi-issuer request: %v", err)
	}

	if !result.Decision {
		t.Error("Authorization should be ALLOW for single token")
	}
	if result.RequestID == "" {
		t.Error("request_id should be present")
	}
}

// TestMultipleTokensFromDifferentIssuers tests multiple tokens from different issuers
// (matches Rust test_multiple_tokens_from_different_issuers)
func TestMultipleTokensFromDifferentIssuers(t *testing.T) {
	config := getMultiIssuerConfig()
	instance, err := NewCedarling(config)
	if err != nil {
		t.Fatalf("Failed to create Cedarling instance: %v", err)
	}

	tokens := []TokenInput{
		{Mapping: "Jans::Access_Token", Payload: accessToken},
		{Mapping: "Jans::Id_Token", Payload: idToken},
		{Mapping: "Jans::Userinfo_Token", Payload: userinfoToken},
	}

	request := AuthorizeMultiIssuerRequest{
		Tokens:   tokens,
		Action:   "Jans::Action::\"Update\"",
		Context:  map[string]interface{}{},
		Resource: getTestResource(),
	}

	result, err := instance.AuthorizeMultiIssuer(request)
	if err != nil {
		t.Fatalf("Failed to authorize multi-issuer request: %v", err)
	}

	if !result.Decision {
		t.Error("Authorization should be ALLOW for multi-token request")
	}
	if result.RequestID == "" {
		t.Error("request_id should be present")
	}
}

// TestValidationEmptyTokenArray tests validation - empty token array
// (matches Rust test_validation_empty_token_array)
func TestValidationEmptyTokenArray(t *testing.T) {
	config := getMultiIssuerConfig()
	instance, err := NewCedarling(config)
	if err != nil {
		t.Fatalf("Failed to create Cedarling instance: %v", err)
	}

	request := AuthorizeMultiIssuerRequest{
		Tokens:   []TokenInput{},
		Action:   "Jans::Action::\"Update\"",
		Context:  map[string]interface{}{},
		Resource: getTestResource(),
	}

	_, err = instance.AuthorizeMultiIssuer(request)
	if err == nil {
		t.Fatal("Should fail with empty token array")
	}
}

// TestValidationGracefulDegradationInvalidToken tests validation - graceful degradation when invalid token is present
// (matches Rust test_validation_graceful_degradation_invalid_token)
func TestValidationGracefulDegradationInvalidToken(t *testing.T) {
	config := getMultiIssuerConfig()
	instance, err := NewCedarling(config)
	if err != nil {
		t.Fatalf("Failed to create Cedarling instance: %v", err)
	}

	// Create request with both valid and invalid tokens
	tokens := []TokenInput{
		{Mapping: "Jans::Access_Token", Payload: accessToken},   // Valid token
		{Mapping: "Invalid::Token", Payload: "not-a-valid-jwt"}, // Invalid JWT
	}

	request := AuthorizeMultiIssuerRequest{
		Tokens:   tokens,
		Action:   "Jans::Action::\"Update\"",
		Context:  map[string]interface{}{},
		Resource: getTestResource(),
	}

	// Graceful degradation: invalid tokens are ignored, valid tokens are processed
	result, err := instance.AuthorizeMultiIssuer(request)
	if err != nil {
		t.Fatalf("Should succeed gracefully when some tokens are invalid: %v", err)
	}

	if !result.Decision {
		t.Error("Should be ALLOW - valid token has required attributes despite invalid token")
	}
	if result.RequestID == "" {
		t.Error("request_id should be present")
	}
}

// TestDataAPIPushAndGetCtx tests basic push and get operations
func TestDataAPIPushAndGetCtx(t *testing.T) {
	config, err := loadTestConfig(nil)
	if err != nil {
		t.Fatalf("Failed to load test config: %v", err)
	}
	instance, err := NewCedarling(config)
	if err != nil {
		t.Fatalf("Failed to create Cedarling instance: %v", err)
	}
	defer instance.ShutDown()

	// Push data without TTL
	value := map[string]interface{}{
		"level":   "premium",
		"country": "US",
	}
	err = instance.PushDataCtx("user_profile", value, nil)
	if err != nil {
		t.Fatalf("Failed to push data: %v", err)
	}

	// Get data
	resultProfile, err := instance.GetDataCtx("user_profile")
	if err != nil {
		t.Fatalf("Failed to get data: %v", err)
	}
	if resultProfile == nil {
		t.Fatal("Expected data to be present")
	}

	// Test TTL expiration
	ttlValue := map[string]interface{}{
		"temp": "data",
	}
	// Push with 100ms TTL
	d := 100 * time.Millisecond
	err = instance.PushDataCtx("temp_key", ttlValue, &d)
	if err != nil {
		t.Fatalf("Failed to push data with TTL: %v", err)
	}

	// Verify data exists immediately
	resultTemp, errTemp := instance.GetDataCtx("temp_key")
	if errTemp != nil {
		t.Fatalf("Failed to get data: %v", errTemp)
	}
	if resultTemp == nil {
		t.Fatal("Expected data to be present immediately after push")
	}

	// Wait for TTL to expire (200ms)
	time.Sleep(200 * time.Millisecond)

	// Verify data is expired
	resultTemp, errTemp = instance.GetDataCtx("temp_key")
	if errTemp != nil {
		t.Fatalf("Failed to get data: %v", errTemp)
	}
	if resultTemp != nil {
		t.Fatal("Expected data to be expired after TTL")
	}

	// Verify value structure from original result
	resultMap, ok := resultProfile.(map[string]interface{})
	if !ok {
		t.Fatalf("Expected result to be a map, got %T", resultProfile)
	}
	if resultMap["level"] != "premium" {
		t.Errorf("Expected level to be 'premium', got %v", resultMap["level"])
	}
}

// TestDataAPIGetDataEntryCtx tests getting data with metadata
func TestDataAPIGetDataEntryCtx(t *testing.T) {
	config, err := loadTestConfig(nil)
	if err != nil {
		t.Fatalf("Failed to load test config: %v", err)
	}
	instance, err := NewCedarling(config)
	if err != nil {
		t.Fatalf("Failed to create Cedarling instance: %v", err)
	}
	defer instance.ShutDown()

	// Push data
	err = instance.PushDataCtx("test_key", "test_value", nil)
	if err != nil {
		t.Fatalf("Failed to push data: %v", err)
	}

	// Get entry with metadata
	entry, err := instance.GetDataEntryCtx("test_key")
	if err != nil {
		t.Fatalf("Failed to get data entry: %v", err)
	}
	if entry.Key == "" {
		t.Fatal("Expected entry to be present")
	}

	if entry.Key != "test_key" {
		t.Errorf("Expected key to be 'test_key', got %v", entry.Key)
	}
	if entry.CreatedAt == "" {
		t.Error("Expected created_at to be set")
	}
}

// TestDataAPIRemoveDataCtx tests removing data
func TestDataAPIRemoveDataCtx(t *testing.T) {
	config, err := loadTestConfig(nil)
	if err != nil {
		t.Fatalf("Failed to load test config: %v", err)
	}
	instance, err := NewCedarling(config)
	if err != nil {
		t.Fatalf("Failed to create Cedarling instance: %v", err)
	}
	defer instance.ShutDown()

	// Push data
	err = instance.PushDataCtx("to_remove", "value", nil)
	if err != nil {
		t.Fatalf("Failed to push data: %v", err)
	}

	// Remove data
	removed, err := instance.RemoveDataCtx("to_remove")
	if err != nil {
		t.Fatalf("Failed to remove data: %v", err)
	}
	if !removed {
		t.Error("Expected remove to return true")
	}

	// Verify it's gone
	result, err := instance.GetDataCtx("to_remove")
	if err != nil {
		t.Fatalf("Failed to get data: %v", err)
	}
	if result != nil {
		t.Error("Expected data to be removed")
	}
}

// TestDataAPIClearDataCtx tests clearing all data
func TestDataAPIClearDataCtx(t *testing.T) {
	config, err := loadTestConfig(nil)
	if err != nil {
		t.Fatalf("Failed to load test config: %v", err)
	}
	instance, err := NewCedarling(config)
	if err != nil {
		t.Fatalf("Failed to create Cedarling instance: %v", err)
	}
	defer instance.ShutDown()

	// Push multiple entries
	err = instance.PushDataCtx("key1", "value1", nil)
	if err != nil {
		t.Fatalf("Failed to push data: %v", err)
	}
	err = instance.PushDataCtx("key2", "value2", nil)
	if err != nil {
		t.Fatalf("Failed to push data: %v", err)
	}

	// Clear all data
	err = instance.ClearDataCtx()
	if err != nil {
		t.Fatalf("Failed to clear data: %v", err)
	}

	// Verify all data is gone
	stats, err := instance.GetStatsCtx()
	if err != nil {
		t.Fatalf("Failed to get stats: %v", err)
	}
	if stats.EntryCount != 0 {
		t.Errorf("Expected entry count to be 0, got %v", stats.EntryCount)
	}
}

// TestDataAPIListDataCtx tests listing all data entries
func TestDataAPIListDataCtx(t *testing.T) {
	config, err := loadTestConfig(nil)
	if err != nil {
		t.Fatalf("Failed to load test config: %v", err)
	}
	instance, err := NewCedarling(config)
	if err != nil {
		t.Fatalf("Failed to create Cedarling instance: %v", err)
	}
	defer instance.ShutDown()

	// Clear any existing data
	err = instance.ClearDataCtx()
	if err != nil {
		t.Fatalf("Failed to clear data: %v", err)
	}

	// Push multiple entries
	err = instance.PushDataCtx("list_key1", "value1", nil)
	if err != nil {
		t.Fatalf("Failed to push data: %v", err)
	}
	err = instance.PushDataCtx("list_key2", "value2", nil)
	if err != nil {
		t.Fatalf("Failed to push data: %v", err)
	}

	// List all entries
	entries, err := instance.ListDataCtx()
	if err != nil {
		t.Fatalf("Failed to list data: %v", err)
	}

	if len(entries) != 2 {
		t.Errorf("Expected 2 entries, got %v", len(entries))
	}
}

// TestDataAPIGetStatsCtx tests getting data store statistics
func TestDataAPIGetStatsCtx(t *testing.T) {
	config, err := loadTestConfig(nil)
	if err != nil {
		t.Fatalf("Failed to load test config: %v", err)
	}
	instance, err := NewCedarling(config)
	if err != nil {
		t.Fatalf("Failed to create Cedarling instance: %v", err)
	}
	defer instance.ShutDown()

	// Clear and add some data
	err = instance.ClearDataCtx()
	if err != nil {
		t.Fatalf("Failed to clear data: %v", err)
	}
	err = instance.PushDataCtx("stats_key", "stats_value", nil)
	if err != nil {
		t.Fatalf("Failed to push data: %v", err)
	}

	// Get stats
	stats, err := instance.GetStatsCtx()
	if err != nil {
		t.Fatalf("Failed to get stats: %v", err)
	}

	if stats.EntryCount != 1 {
		t.Errorf("Expected entry count to be 1, got %v", stats.EntryCount)
	}
	if stats.MaxEntries == 0 {
		t.Error("Expected max_entries to be set")
	}
}

// TestDataAPIInstanceIsolationCtx tests that data is isolated between instances
func TestDataAPIInstanceIsolationCtx(t *testing.T) {
	config, err := loadTestConfig(nil)
	if err != nil {
		t.Fatalf("Failed to load test config: %v", err)
	}

	instance1, err := NewCedarling(config)
	if err != nil {
		t.Fatalf("Failed to create first Cedarling instance: %v", err)
	}
	defer instance1.ShutDown()

	instance2, err := NewCedarling(config)
	if err != nil {
		t.Fatalf("Failed to create second Cedarling instance: %v", err)
	}
	defer instance2.ShutDown()

	// Push data to instance1
	err = instance1.PushDataCtx("isolated_key", "instance1_value", nil)
	if err != nil {
		t.Fatalf("Failed to push data to instance1: %v", err)
	}

	// Verify data is NOT in instance2
	result, err := instance2.GetDataCtx("isolated_key")
	if err != nil {
		t.Fatalf("Failed to get data from instance2: %v", err)
	}
	if result != nil {
		t.Error("Data should be isolated between instances")
	}

	// Verify data IS in instance1
	result, err = instance1.GetDataCtx("isolated_key")
	if err != nil {
		t.Fatalf("Failed to get data from instance1: %v", err)
	}
	if result == nil {
		t.Error("Data should be present in instance1")
	}
}