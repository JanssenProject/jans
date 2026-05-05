// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

// Mirrors cedarling/benches/authz_authorize_unsigned_benchmark.rs and
// authz_authorize_multi_issuer_benchmark.rs:
//   - build the Cedarling instance once outside the timed region
//   - run a single validation call asserting the expected decision
//   - keep the hot loop tight; sink the result so the compiler can't elide it
package cedarling_go

import "testing"

// benchSink is a package-level variable used to keep the AuthorizeResult alive
// across benchmark iterations, mirroring criterion's `black_box`.
// Without it the Go compiler is free to discard the call's return value.
var benchSink any

func BenchmarkAuthorizeUnsigned(b *testing.B) {
	config, err := loadUnsignedTestConfig()
	if err != nil {
		b.Fatalf("failed to load config: %v", err)
	}
	instance, err := NewCedarling(config)
	if err != nil {
		b.Fatalf("failed to create instance: %v", err)
	}
	defer instance.ShutDown()

	principal := EntityData{
		CedarMapping: CedarEntityMapping{
			EntityType: "Jans::TestPrincipal1",
			ID:         "boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0",
		},
		Payload: map[string]any{"is_ok": true},
	}
	request := RequestUnsigned{
		Principal: &principal,
		Action:    `Jans::Action::"UpdateForTestPrincipals"`,
		Resource:  unsignedTestResource(),
	}

	// Pre-measurement validation (matches `validate_cedarling_works` in Rust).
	result, err := instance.AuthorizeUnsigned(request)
	if err != nil {
		b.Fatalf("validation call failed: %v", err)
	}
	if !result.Decision {
		b.Fatalf("expected allow for benchmark validation, got deny")
	}

	b.ResetTimer()
	b.ReportAllocs()
	var r AuthorizeResult
	for i := 0; i < b.N; i++ {
		r, err = instance.AuthorizeUnsigned(request)
		if err != nil {
			b.Fatalf("authorization failed: %v", err)
		}
	}
	benchSink = r
}

func BenchmarkAuthorizeMultiIssuer(b *testing.B) {
	instance, err := NewCedarling(getMultiIssuerConfig())
	if err != nil {
		b.Fatalf("failed to create instance: %v", err)
	}
	defer instance.ShutDown()

	request := AuthorizeMultiIssuerRequest{
		Tokens: []TokenInput{
			{Mapping: "Jans::Access_Token", Payload: accessToken},
			{Mapping: "Jans::Id_Token", Payload: idToken},
			{Mapping: "Jans::Userinfo_Token", Payload: userinfoToken},
		},
		Action:   `Jans::Action::"Update"`,
		Context:  map[string]interface{}{},
		Resource: getTestResource(),
	}

	// Pre-measurement validation: matches the assert in Rust's multi-issuer bench
	// that the request returns Allow before entering the iter loop.
	result, err := instance.AuthorizeMultiIssuer(request)
	if err != nil {
		b.Fatalf("validation call failed: %v", err)
	}
	if !result.Decision {
		b.Fatalf("expected allow for benchmark validation, got deny")
	}

	b.ResetTimer()
	b.ReportAllocs()
	var r MultiIssuerAuthorizeResult
	for i := 0; i < b.N; i++ {
		r, err = instance.AuthorizeMultiIssuer(request)
		if err != nil {
			b.Fatalf("authorization failed: %v", err)
		}
	}
	benchSink = r
}
