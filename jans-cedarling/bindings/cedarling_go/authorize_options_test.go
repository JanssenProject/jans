package cedarling_go

import (
	"encoding/json"
	"strings"
	"testing"
)

// AuthorizeOptions travel with a request but never take part in the Cedar
// evaluation, so a request gives the same decision with and without them.

func traceOptions() *AuthorizeOptions {
	instance := "workload-1-a"
	return &AuthorizeOptions{Trace: &TraceContext{
		ExecutionID:        "exec-1",
		ExecutionAuthority: "https://gateway.example.com",
		WorkloadID:         "workload-1",
		WorkloadInstanceID: &instance,
	}}
}

func TestAuthorizeUnsignedWithTraceOptions(t *testing.T) {
	config, err := loadUnsignedTestConfig()
	if err != nil {
		t.Fatalf("Failed to load test config: %v", err)
	}
	instance, err := NewCedarling(config)
	if err != nil {
		t.Fatalf("Failed to create Cedarling instance: %v", err)
	}
	defer instance.ShutDown()

	for _, isOk := range []bool{true, false} {
		request := func(options *AuthorizeOptions) RequestUnsigned {
			return RequestUnsigned{
				Principal: unsignedPrincipal(isOk),
				Action:    "Jans::Action::\"UpdateForTestPrincipals\"",
				Resource:  unsignedTestResource(),
				Options:   options,
			}
		}
		plain, err := instance.AuthorizeUnsigned(request(nil))
		if err != nil {
			t.Fatalf("plain authorization failed: %v", err)
		}
		traced, err := instance.AuthorizeUnsigned(request(traceOptions()))
		if err != nil {
			t.Fatalf("authorization with options failed: %v", err)
		}
		if plain.Decision != isOk {
			t.Errorf("baseline decision should follow is_ok=%v, got %v", isOk, plain.Decision)
		}
		if traced.Decision != plain.Decision {
			t.Errorf("options must not change the decision (is_ok=%v)", isOk)
		}
	}
}

func TestAuthorizeBatchWithTraceOptions(t *testing.T) {
	unsignedConfig, err := loadUnsignedTestConfig()
	if err != nil {
		t.Fatalf("Failed to load test config: %v", err)
	}
	unsigned, err := NewCedarling(unsignedConfig)
	if err != nil {
		t.Fatalf("Failed to create Cedarling instance: %v", err)
	}
	defer unsigned.ShutDown()

	unsignedResponse, err := unsigned.AuthorizeUnsignedBatch(BatchAuthorizeUnsignedRequest{
		Principal: unsignedPrincipal(true),
		Items:     []BatchItem{unsignedBatchItem()},
		Options:   traceOptions(),
	})
	if err != nil {
		t.Fatalf("unsigned batch with options failed: %v", err)
	}
	if len(unsignedResponse.Results) != 1 || !unsignedResponse.Results[0].IsOk() ||
		!unsignedResponse.Results[0].Ok.Decision {
		t.Errorf("unsigned batch item with options should allow: %+v", unsignedResponse.Results)
	}

	multiIssuer, err := NewCedarling(getMultiIssuerConfig())
	if err != nil {
		t.Fatalf("Failed to create Cedarling instance: %v", err)
	}
	defer multiIssuer.ShutDown()

	multiResponse, err := multiIssuer.AuthorizeMultiIssuerBatch(BatchAuthorizeMultiIssuerRequest{
		Tokens:  []TokenInput{{Mapping: "Jans::Access_Token", Payload: accessToken}},
		Items:   []BatchItem{{Resource: getTestResource(), Action: "Jans::Action::\"Update\""}},
		Options: traceOptions(),
	})
	if err != nil {
		t.Fatalf("multi-issuer batch with options failed: %v", err)
	}
	if len(multiResponse.Results) != 1 || !multiResponse.Results[0].IsOk() ||
		!multiResponse.Results[0].Ok.Decision {
		t.Errorf("multi-issuer batch item with options should allow: %+v", multiResponse.Results)
	}

	single, err := multiIssuer.AuthorizeMultiIssuer(AuthorizeMultiIssuerRequest{
		Tokens:   []TokenInput{{Mapping: "Jans::Access_Token", Payload: accessToken}},
		Resource: getTestResource(),
		Action:   "Jans::Action::\"Update\"",
		Options:  traceOptions(),
	})
	if err != nil {
		t.Fatalf("multi-issuer request with options failed: %v", err)
	}
	if !single.Decision {
		t.Error("multi-issuer request with options should allow")
	}
}

// TestRequestsMarshalOptions checks the JSON the binding hands to Rust: the
// `options` member is there only when set, in the shape the core reads.
func TestRequestsMarshalOptions(t *testing.T) {
	const wantTrace = `"options":{"trace":{"execution_id":"exec-1",` +
		`"execution_authority":"https://gateway.example.com","workload_id":"workload-1",` +
		`"workload_instance_id":"workload-1-a"}}`

	requests := map[string]func(*AuthorizeOptions) any{
		"unsigned": func(o *AuthorizeOptions) any {
			return RequestUnsigned{Resource: unsignedTestResource(), Options: o}
		},
		"multi_issuer": func(o *AuthorizeOptions) any {
			return AuthorizeMultiIssuerRequest{Resource: getTestResource(), Options: o}
		},
		"batch_unsigned": func(o *AuthorizeOptions) any {
			return BatchAuthorizeUnsignedRequest{Options: o}
		},
		"batch_multi_issuer": func(o *AuthorizeOptions) any {
			return BatchAuthorizeMultiIssuerRequest{Options: o}
		},
	}
	for name, build := range requests {
		with, err := json.Marshal(build(traceOptions()))
		if err != nil {
			t.Fatalf("%s: marshal with options failed: %v", name, err)
		}
		if !strings.Contains(string(with), wantTrace) {
			t.Errorf("%s: JSON should carry the trace options, got %s", name, with)
		}
		without, err := json.Marshal(build(nil))
		if err != nil {
			t.Fatalf("%s: marshal without options failed: %v", name, err)
		}
		if strings.Contains(string(without), `"options"`) {
			t.Errorf("%s: JSON without options should have no options member, got %s", name, without)
		}
	}
}
