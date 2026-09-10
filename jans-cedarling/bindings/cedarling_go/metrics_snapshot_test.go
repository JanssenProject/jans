// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package cedarling_go

import (
	"strings"
	"testing"
)

// loadMetricsConfig loads the bootstrap config with metrics collection toggled
// using the same policy store as the unsigned authorization tests. Metrics
// collection is disabled unless enabled is true.
func loadMetricsConfig(enabled bool) (map[string]any, error) {
	config, err := loadUnsignedTestConfig()
	if err != nil {
		return nil, err
	}
	value := "disabled"
	if enabled {
		value = "enabled"
	}
	config["CEDARLING_METRICS_COLLECTION"] = value
	return config, nil
}

// TestDrainMetricsDisabledReturnsError ensures DrainMetrics fails with the
// "telemetry-not-enabled" error when metrics collection is disabled.
func TestDrainMetricsDisabledReturnsError(t *testing.T) {
	config, err := loadMetricsConfig(false)
	if err != nil {
		t.Fatalf("Failed to load test config: %v", err)
	}
	instance, err := NewCedarling(config)
	if err != nil {
		t.Fatalf("Failed to create Cedarling instance: %v", err)
	}
	defer instance.ShutDown()

	_, err = instance.DrainMetrics()
	if err == nil {
		t.Fatal("DrainMetrics should fail when metrics collection is disabled")
	}
	if !strings.Contains(err.Error(), "telemetry-not-enabled") {
		t.Errorf("Expected 'telemetry-not-enabled' error, got: %v", err)
	}
}

// TestDrainMetricsEnabledCollectsAndResets ensures DrainMetrics returns a
// local snapshot with populated counters and resets them for the next interval.
func TestDrainMetricsEnabledCollectsAndResets(t *testing.T) {
	config, err := loadMetricsConfig(true)
	if err != nil {
		t.Fatalf("Failed to load test config: %v", err)
	}
	instance, err := NewCedarling(config)
	if err != nil {
		t.Fatalf("Failed to create Cedarling instance: %v", err)
	}
	defer instance.ShutDown()

	// First snapshot should report the loaded policy count and no authz traffic yet.
	first, err := instance.DrainMetrics()
	if err != nil {
		t.Fatalf("Failed to drain metrics: %v", err)
	}
	policyCount, ok := first.OperationalStats["instance.policy_count"]
	if !ok || policyCount <= 0 {
		t.Errorf(
			"Expected positive instance.policy_count, got %v (present: %v)",
			policyCount,
			ok,
		)
	}

	// Perform one authorization to register a request counter.
	principal := EntityData{
		CedarMapping: CedarEntityMapping{
			EntityType: "Jans::TestPrincipal1",
			ID:         "1",
		},
		Payload: map[string]any{"is_ok": true},
	}
	request := RequestUnsigned{
		Principal: &principal,
		Action:    "Jans::Action::\"UpdateForTestPrincipals\"",
		Resource:  unsignedTestResource(),
		Context:   nil,
	}
	result, err := instance.AuthorizeUnsigned(request)
	if err != nil {
		t.Fatalf("Authorization failed: %v", err)
	}
	if !result.Decision {
		t.Error("Expected allow decision")
	}

	// Second snapshot should include the single request and reset the counters.
	second, err := instance.DrainMetrics()
	if err != nil {
		t.Fatalf("Failed to drain metrics: %v", err)
	}
	if got := second.OperationalStats["authz.requests_total"]; got != 1 {
		t.Errorf("Expected authz.requests_total == 1, got %d", got)
	}

	// Third snapshot should show the counters were reset to zero.
	third, err := instance.DrainMetrics()
	if err != nil {
		t.Fatalf("Failed to drain metrics: %v", err)
	}
	if got := third.OperationalStats["authz.requests_total"]; got != 0 {
		t.Errorf("Expected authz.requests_total reset to 0, got %d", got)
	}
}
