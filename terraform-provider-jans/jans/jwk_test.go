package jans

import (
	"context"
	"testing"
)

// func TestDeleteJWKs(t *testing.T) {

// 	client, err := NewInsecureClient(host, user, pass)
// 	if err != nil {
// 		t.Fatal(err)
// 	}

// 	ctx := context.Background()

// 	if err := client.DeleteJsonWebKey(ctx, "96cb1725-8fdb-4325-06d1-2ebf9adf4fa0_enc_ecdh-es+a256kw"); err != nil {
// 		t.Fatal(err)
// 	}
// }

func TestJWKs(t *testing.T) {

	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	jwks, err := client.GetWebKeysConfiguration(ctx)
	if err != nil {
		t.Fatal(err)
	}

	initialLength := len(jwks.Keys)

	newJWK := &JsonWebKey{
		Descr: "Encryption Key: ECDH-ES using Concat KDF and CEK wrapped with A256KW",
		Kty:   "EC",
		Use:   "enc",
		Crv:   "P-256",
		Kid:   "96cb1725-8fdb-4325-06d1-2ebf9adf4fa0_enc_ecdh-es+a256kw",
		X5c:   []string{"MIIBfTCCASSgAwIBAgIhAO6K4PoHUtIkuxtWASVQbBhP44Tq7Rxmf6OuqFb/gWEPMAoGCCqGSM49BAMCMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjIwODI5MDY0MjEwWhcNMjIwODMxMDc0MjE1WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEZ3xu2YigqjJPpvFvQs/gRe8r1AnCqrblmi9pPhYHauiSMxSjjwjSwZ3rmTWdE+owiEoNMZKFAPlc8aluGpdzzKMnMCUwIwYDVR0lBBwwGgYIKwYBBQUHAwEGCCsGAQUFBwMCBgRVHSUAMAoGCCqGSM49BAMCA0cAMEQCIBRvqiAghTo8stB3lLXVOV5wRJlWNXkMU3ij8CoHamHNAiBM1mFniBNLXbrJCoilBMxBnKVqbvCF/G1sz2xhYuGtDA=="},
		Name:  "id_token ECDH-ES+A256KW Encryption Key",
		X:     "Z3xu2YigqjJPpvFvQs_qMIhKDTGShQD5XPGpbhqXc8w",
		Y:     "kjMUo48I0sGd65k1nRPgRe8r1AnCqrblmi9pPhYHaug",
		Exp:   1661931735188,
		Alg:   "ECDH-ES+A256KW",
	}
	// if err := client.DeleteJsonWebKey(ctx, newJWK.Kid); err != nil {
	// 	t.Fatal(err)
	// }

	createdJWK, err := client.CreateJsonWebKey(ctx, newJWK)
	if err != nil {
		t.Fatal(err)
	}

	t.Cleanup(func() {
		_ = client.DeleteJsonWebKey(ctx, createdJWK.Kid)
	})

	if createdJWK.Descr != newJWK.Descr {
		t.Error("expected same description")
	}

	jwks, err = client.GetWebKeysConfiguration(ctx)
	if err != nil {
		t.Fatal(err)
	}

	initialLength++
	if len(jwks.Keys) != initialLength {
		t.Errorf("expected %v jwk entries, got %v", initialLength, len(jwks.Keys))
	}

	createdJWK, err = client.GetJsonWebKey(ctx, newJWK.Kid)
	if err != nil {
		t.Fatal(err)
	}

	if createdJWK.Descr != newJWK.Descr {
		t.Errorf("expected same description, got %s", createdJWK.Descr)
	}

	createdJWK.Descr = "new description"
	if _, err := client.UpdateJsonWebKey(ctx, createdJWK); err != nil {
		t.Fatal(err)
	}

	updatedJWK, err := client.GetJsonWebKey(ctx, newJWK.Kid)
	if err != nil {
		t.Fatal(err)
	}

	if updatedJWK.Descr != createdJWK.Descr {
		t.Errorf("expected updated description, got %s", updatedJWK.Descr)
	}

	if err := client.DeleteJsonWebKey(ctx, newJWK.Kid); err != nil {
		t.Fatal(err)
	}

	initialLength--

	jwks, err = client.GetWebKeysConfiguration(ctx)
	if err != nil {
		t.Fatal(err)
	}

	if len(jwks.Keys) != initialLength {
		t.Errorf("expected %v jwk entries, got %v", initialLength, len(jwks.Keys))
	}

}
