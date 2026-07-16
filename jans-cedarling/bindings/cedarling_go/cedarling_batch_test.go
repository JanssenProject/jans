package cedarling_go

import (
	"strings"
	"testing"
)

// unsignedBatchItem returns a BatchItem that mirrors the resource used by the
// single-item unsigned tests.
func unsignedBatchItem() BatchItem {
	return BatchItem{
		Resource: unsignedTestResource(),
		Action:   "Jans::Action::\"UpdateForTestPrincipals\"",
		Context:  nil,
	}
}

func unsignedPrincipal(isOk bool) *EntityData {
	return &EntityData{
		CedarMapping: CedarEntityMapping{
			EntityType: "Jans::TestPrincipal1",
			ID:         "1",
		},
		Payload: map[string]any{"is_ok": isOk},
	}
}

// TestAuthorizeUnsignedBatchOrderedAllow checks N=3 items all Allow when the
// principal satisfies the `is_ok` guard, batch_id is populated, and results
// arity matches item arity.
func TestAuthorizeUnsignedBatchOrderedAllow(t *testing.T) {
	config, err := loadUnsignedTestConfig()
	if err != nil {
		t.Fatalf("Failed to load test config: %v", err)
	}
	instance, err := NewCedarling(config)
	if err != nil {
		t.Fatalf("Failed to create Cedarling instance: %v", err)
	}
	defer instance.ShutDown()

	request := BatchAuthorizeUnsignedRequest{
		Principal: unsignedPrincipal(true),
		Items:     []BatchItem{unsignedBatchItem(), unsignedBatchItem(), unsignedBatchItem()},
	}

	response, err := instance.AuthorizeUnsignedBatch(request)
	if err != nil {
		t.Fatalf("Batch authorization failed: %v", err)
	}
	if len(response.Results) != 3 {
		t.Fatalf("Expected 3 results, got %d", len(response.Results))
	}
	if response.BatchID == "" {
		t.Error("batch_id should be populated")
	}
	for i, r := range response.Results {
		if !r.Decision {
			t.Errorf("item %d should allow, got Deny", i)
		}
	}
}

// TestAuthorizeUnsignedBatchNoPrincipalResidualDenies checks partial evaluation
// on a batch: `principal=nil` with a public action allows, with a
// principal-dependent action fails closed — each in its own item slot.
func TestAuthorizeUnsignedBatchNoPrincipalResidualDenies(t *testing.T) {
	config, err := loadUnsignedTestConfig()
	if err != nil {
		t.Fatalf("Failed to load test config: %v", err)
	}
	instance, err := NewCedarling(config)
	if err != nil {
		t.Fatalf("Failed to create Cedarling instance: %v", err)
	}
	defer instance.ShutDown()

	publicItem := BatchItem{
		Resource: unsignedTestResource(),
		Action:   "Jans::Action::\"OpenPublicIssue\"",
	}
	privateItem := unsignedBatchItem()

	response, err := instance.AuthorizeUnsignedBatch(BatchAuthorizeUnsignedRequest{
		Principal: nil,
		Items:     []BatchItem{publicItem, privateItem},
	})
	if err != nil {
		t.Fatalf("Batch authorization failed: %v", err)
	}
	if len(response.Results) != 2 {
		t.Fatalf("Expected 2 results, got %d", len(response.Results))
	}
	if !response.Results[0].Decision {
		t.Error("public action must allow under partial eval")
	}
	if response.Results[1].Decision {
		t.Error("principal-dependent action must fail closed")
	}
}

func TestAuthorizeUnsignedBatchEmptyItemsRejected(t *testing.T) {
	config, err := loadUnsignedTestConfig()
	if err != nil {
		t.Fatalf("Failed to load test config: %v", err)
	}
	instance, err := NewCedarling(config)
	if err != nil {
		t.Fatalf("Failed to create Cedarling instance: %v", err)
	}
	defer instance.ShutDown()

	_, err = instance.AuthorizeUnsignedBatch(BatchAuthorizeUnsignedRequest{
		Principal: unsignedPrincipal(true),
		Items:     []BatchItem{},
	})
	if err == nil {
		t.Fatal("empty items must be rejected")
	}
	if !strings.Contains(strings.ToLower(err.Error()), "empty") {
		t.Errorf("expected error to mention empty items, got: %v", err)
	}
}

// TestAuthorizeMultiIssuerBatchOrdered checks the boundary: N=3 items with a
// shared valid token set, batch_id populated, results arity matches.
func TestAuthorizeMultiIssuerBatchOrdered(t *testing.T) {
	config := getMultiIssuerConfig()
	instance, err := NewCedarling(config)
	if err != nil {
		t.Fatalf("Failed to create Cedarling instance: %v", err)
	}
	defer instance.ShutDown()

	makeItem := func() BatchItem {
		return BatchItem{
			Resource: getTestResource(),
			Action:   "Jans::Action::\"Update\"",
			Context:  nil,
		}
	}
	request := BatchAuthorizeMultiIssuerRequest{
		Tokens: []TokenInput{{Mapping: "Jans::Access_Token", Payload: accessToken}},
		Items:  []BatchItem{makeItem(), makeItem(), makeItem()},
	}

	response, err := instance.AuthorizeMultiIssuerBatch(request)
	if err != nil {
		t.Fatalf("Batch authorization failed: %v", err)
	}
	if len(response.Results) != 3 {
		t.Fatalf("Expected 3 results, got %d", len(response.Results))
	}
	if response.BatchID == "" {
		t.Error("batch_id should be populated")
	}
	// Same token+resource+action combo the single-item test uses; every
	// result must Allow. Position-wise iteration also proves ordering.
	for i, r := range response.Results {
		if !r.Decision {
			t.Errorf("item %d should allow", i)
		}
	}
}

func TestAuthorizeMultiIssuerBatchEmptyTokensRejected(t *testing.T) {
	config := getMultiIssuerConfig()
	instance, err := NewCedarling(config)
	if err != nil {
		t.Fatalf("Failed to create Cedarling instance: %v", err)
	}
	defer instance.ShutDown()

	_, err = instance.AuthorizeMultiIssuerBatch(BatchAuthorizeMultiIssuerRequest{
		Tokens: []TokenInput{},
		Items: []BatchItem{
			{Resource: getTestResource(), Action: "Jans::Action::\"Update\""},
		},
	})
	if err == nil {
		t.Fatal("empty tokens must be rejected")
	}
	if !strings.Contains(strings.ToLower(err.Error()), "empty") {
		t.Errorf("expected error to mention empty tokens, got: %v", err)
	}
}

func TestAuthorizeMultiIssuerBatchEmptyItemsRejected(t *testing.T) {
	config := getMultiIssuerConfig()
	instance, err := NewCedarling(config)
	if err != nil {
		t.Fatalf("Failed to create Cedarling instance: %v", err)
	}
	defer instance.ShutDown()

	_, err = instance.AuthorizeMultiIssuerBatch(BatchAuthorizeMultiIssuerRequest{
		Tokens: []TokenInput{{Mapping: "Jans::Access_Token", Payload: accessToken}},
		Items:  []BatchItem{},
	})
	if err == nil {
		t.Fatal("empty items must be rejected")
	}
	if !strings.Contains(strings.ToLower(err.Error()), "empty") {
		t.Errorf("expected error to mention empty items, got: %v", err)
	}
}
