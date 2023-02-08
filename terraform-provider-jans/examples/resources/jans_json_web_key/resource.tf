resource "jans_json_web_key" "test" {
	descr = "Encryption Key: ECDH-ES using Concat KDF and CEK wrapped with A256KW"
	kty 	= "EC"
	use 	= "enc"
	crv 	= "P-256"
	kid 	= "96cb1725-8fdb-4325-06d1-2ebf9adf4fa0_enc_ecdh-es+a256kw"
	x5c 	= [ "MIIBfTCCASSgAwIBAgIhAO6K4PoHUtIkuxtWASVQbBhP44Tq7Rxmf6OuqFb/gWEPMAoGCCqGSM49BAMCMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjIwODI5MDY0MjEwWhcNMjIwODMxMDc0MjE1WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEZ3xu2YigqjJPpvFvQs/gRe8r1AnCqrblmi9pPhYHauiSMxSjjwjSwZ3rmTWdE+owiEoNMZKFAPlc8aluGpdzzKMnMCUwIwYDVR0lBBwwGgYIKwYBBQUHAwEGCCsGAQUFBwMCBgRVHSUAMAoGCCqGSM49BAMCA0cAMEQCIBRvqiAghTo8stB3lLXVOV5wRJlWNXkMU3ij8CoHamHNAiBM1mFniBNLXbrJCoilBMxBnKVqbvCF/G1sz2xhYuGtDA==" ]
	name 	= "id_token ECDH-ES+A256KW Encryption Key"
	x 		= "Z3xu2YigqjJPpvFvQs_qMIhKDTGShQD5XPGpbhqXc8w"
	y 		= "kjMUo48I0sGd65k1nRPgRe8r1AnCqrblmi9pPhYHaug"
	exp 	= 1661931735188
	alg 	= "ECDH-ES+A256KW"
}