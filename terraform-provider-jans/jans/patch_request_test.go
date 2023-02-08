package jans

import (
	"testing"
)

func TestPatchCreation(t *testing.T) {

	entity := &JsonWebKey{
		Name:  "id_token ECDH-ES+A256KW Encryption Key",
		Descr: "Encryption Key: ECDH-ES using Concat KDF and CEK wrapped with A256KW",
		Kid:   "96cb1725-8fdb-4325-06d1-2ebf9adf4fa0_enc_ecdh-es+a256kw",
		Kty:   "EC",
		Use:   "enc",
		Alg:   "ECDH-ES+A256KW",
		Crv:   "P-256",
		Exp:   1661931735188,
		X5c:   []string{"MIIBfTCCASSgAwIBAgIhAO6K4PoHUtIkuxtWASVQbBhP44Tq7Rxmf6OuqFb/gWEPMAoGCCqGSM49BAMCMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjIwODI5MDY0MjEwWhcNMjIwODMxMDc0MjE1WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEZ3xu2YigqjJPpvFvQs/gRe8r1AnCqrblmi9pPhYHauiSMxSjjwjSwZ3rmTWdE+owiEoNMZKFAPlc8aluGpdzzKMnMCUwIwYDVR0lBBwwGgYIKwYBBQUHAwEGCCsGAQUFBwMCBgRVHSUAMAoGCCqGSM49BAMCA0cAMEQCIBRvqiAghTo8stB3lLXVOV5wRJlWNXkMU3ij8CoHamHNAiBM1mFniBNLXbrJCoilBMxBnKVqbvCF/G1sz2xhYuGtDA=="},
		N:     "n-value",
		E:     "e-value",
		X:     "Z3xu2YigqjJPpvFvQs_qMIhKDTGShQD5XPGpbhqXc8w",
		Y:     "kjMUo48I0sGd65k1nRPgRe8r1AnCqrblmi9pPhYHaug",
	}

	patches, err := createPatches(entity, &JsonWebKey{})
	if err != nil {
		t.Fatal(err)
	}

	if len(patches) != 13 {
		t.Fatalf("Expected 13 patches, got %d", len(patches))
	}

}

func TestPartialPatchCreation(t *testing.T) {

	orig := &JsonWebKey{
		Name:  "id_token ECDH-ES+A256KW Encryption Key",
		Descr: "Encryption Key: ECDH-ES using Concat KDF and CEK wrapped with A256KW",
		Kid:   "96cb1725-8fdb-4325-06d1-2ebf9adf4fa0_enc_ecdh-es+a256kw",
		Kty:   "EC",
		Use:   "enc",
		Alg:   "ECDH-ES+A256KW",
		Crv:   "P-256",
		Exp:   1661931735188,
		X5c:   []string{"MIIBfTCCASSgAwIBAgIhAO6K4PoHUtIkuxtWASVQbBhP44Tq7Rxmf6OuqFb/gWEPMAoGCCqGSM49BAMCMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjIwODI5MDY0MjEwWhcNMjIwODMxMDc0MjE1WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEZ3xu2YigqjJPpvFvQs/gRe8r1AnCqrblmi9pPhYHauiSMxSjjwjSwZ3rmTWdE+owiEoNMZKFAPlc8aluGpdzzKMnMCUwIwYDVR0lBBwwGgYIKwYBBQUHAwEGCCsGAQUFBwMCBgRVHSUAMAoGCCqGSM49BAMCA0cAMEQCIBRvqiAghTo8stB3lLXVOV5wRJlWNXkMU3ij8CoHamHNAiBM1mFniBNLXbrJCoilBMxBnKVqbvCF/G1sz2xhYuGtDA=="},
		N:     "n-value",
		E:     "e-value",
		X:     "Z3xu2YigqjJPpvFvQs_qMIhKDTGShQD5XPGpbhqXc8w",
		Y:     "kjMUo48I0sGd65k1nRPgRe8r1AnCqrblmi9pPhYHaug",
	}

	entity := &JsonWebKey{
		Name:  orig.Name,
		Descr: "ECDH-ES using Concat KDF and CEK wrapped with A256KW",
		Kid:   orig.Kid,
		Kty:   orig.Kty,
		Use:   orig.Use,
		Alg:   orig.Alg,
		Crv:   orig.Crv,
		Exp:   orig.Exp,
		X5c:   orig.X5c,
		N:     "new n-value",
		E:     "new e-value",
		X:     orig.X,
		Y:     orig.Y,
	}

	patches, err := createPatches(entity, orig)
	if err != nil {
		t.Fatal(err)
	}

	if len(patches) != 3 {
		t.Fatalf("Expected 3 patches, got %d", len(patches))
	}

}
