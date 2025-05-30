package cedarling_go

import (
	"encoding/json"
	"reflect"
	"testing"
)

var bootstrapConfig string = `
{
        "CEDARLING_APPLICATION_NAME": "TestApp",
        "CEDARLING_POLICY_STORE_ID": "a1bf93115de86de760ee0bea1d529b521489e5a11747",
        "CEDARLING_USER_AUTHZ": "enabled",
        "CEDARLING_WORKLOAD_AUTHZ": "enabled",
        "CEDARLING_JWT_SIG_VALIDATION": "disabled",
        "CEDARLING_JWT_STATUS_VALIDATION": "disabled",
        "CEDARLING_ID_TOKEN_TRUST_MODE": "none",
        "CEDARLING_LOG_TYPE": "std_out",
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
	idToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhY3IiOiJiYXNpYyIsImFtciI6IjEwIiwiYXVkIjoiNWI0NDg3YzQtOGRiMS00MDlkLWE2NTMtZjkwN2I4MDk0MDM5IiwiZXhwIjoxNzI0ODM1ODU5LCJpYXQiOjE3MjQ4MzIyNTksInN1YiI6ImJvRzhkZmM1TUtUbjM3bzdnc2RDZXlxTDhMcFdRdGdvTzQxbTFLWndkcTAiLCJpc3MiOiJodHRwczovL3Rlc3QuamFucy5vcmciLCJqdGkiOiJzazNUNDBOWVNZdWs1c2FIWk5wa1p3Iiwibm9uY2UiOiJjMzg3MmFmOS1hMGY1LTRjM2YtYTFhZi1mOWQwZTg4NDZlODEiLCJzaWQiOiI2YTdmZTUwYS1kODEwLTQ1NGQtYmU1ZC01NDlkMjk1OTVhMDkiLCJqYW5zT3BlbklEQ29ubmVjdFZlcnNpb24iOiJvcGVuaWRjb25uZWN0LTEuMCIsImNfaGFzaCI6InBHb0s2WV9SS2NXSGtVZWNNOXV3NlEiLCJhdXRoX3RpbWUiOjE3MjQ4MzA3NDYsImdyYW50IjoiYXV0aG9yaXphdGlvbl9jb2RlIiwic3RhdHVzIjp7InN0YXR1c19saXN0Ijp7ImlkeCI6MjAyLCJ1cmkiOiJodHRwczovL3Rlc3QuamFucy5vcmcvamFucy1hdXRoL3Jlc3R2MS9zdGF0dXNfbGlzdCJ9fSwicm9sZSI6IkFkbWluIn0.RgCuWFUUjPVXmbW3ExQavJZH8Lw4q3kGhMFBRR0hSjA"

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
		EntityType: "Jans::Issue",
		ID:         "random_id",
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
		EntityType: "Jans::Issue",
		ID:         "random_id",
		Payload: map[string]any{
			"org_id":  "some_long_id",
			"country": "US",
		},
	}

	principals := []EntityData{
		{
			EntityType: "Jans::TestPrincipal1",
			ID:         "1",
			Payload: map[string]any{
				"is_ok": true,
			},
		},
		{
			EntityType: "Jans::TestPrincipal2",
			ID:         "2",
			Payload: map[string]any{
				"is_ok": true,
			},
		},
		{
			EntityType: "Jans::TestPrincipal3",
			ID:         "3",
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
		EntityType: "Jans::Issue",
		ID:         "random_id",
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
