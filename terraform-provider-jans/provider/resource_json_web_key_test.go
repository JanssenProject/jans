package provider

import (
	"context"
	"errors"
	"fmt"
	"testing"

	"github.com/google/go-cmp/cmp"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/resource"
	"github.com/hashicorp/terraform-plugin-sdk/v2/terraform"
	"github.com/jans/terraform-provider-jans/jans"
)

func TestResourceJsonWebKey_Mapping(t *testing.T) {

	schema := resourceJsonWebKey()

	data := schema.Data(nil)

	key := jans.JsonWebKey{
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

	if err := toSchemaResource(data, key); err != nil {
		t.Fatal(err)
	}

	newKey := jans.JsonWebKey{}

	if err := fromSchemaResource(data, &newKey); err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(key, newKey); diff != "" {
		t.Errorf("Got different key after mapping: %s", diff)
	}
}

func TestAccResourceJsonWebKey_basic(t *testing.T) {

	resourceName := "jans_json_web_key.test"

	resource.Test(t, resource.TestCase{
		PreCheck:     func() { testAccPreCheck(t) },
		Providers:    testAccProviders,
		CheckDestroy: testAccResourceCheckJsonWebKeyDestroy,
		Steps: []resource.TestStep{
			{
				Config: testAccResourceJsonWebKeyConfig_basic(),
				Check: resource.ComposeTestCheckFunc(
					testAccResourceCheckJsonWebKeyExists(resourceName),
					resource.TestCheckResourceAttr(resourceName, "descr", "Encryption Key: ECDH-ES using Concat KDF and CEK wrapped with A256KW"),
					resource.TestCheckResourceAttr(resourceName, "kty", "EC"),
					resource.TestCheckResourceAttr(resourceName, "crv", "P-256"),
				),
			},
		},
	})
}

func testAccResourceJsonWebKeyConfig_basic() string {
	return `
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
`
}

func testAccResourceCheckJsonWebKeyExists(name string) resource.TestCheckFunc {
	return func(s *terraform.State) error {
		rs, ok := s.RootModule().Resources[name]
		if !ok {
			return fmt.Errorf("Not found: %s", name)
		}

		c := testAccProvider.Meta().(*jans.Client)

		kid := rs.Primary.ID

		ctx := context.Background()

		_, err := c.GetJsonWebKey(ctx, kid)
		if err != nil {
			return err
		}

		return nil
	}
}

func testAccResourceCheckJsonWebKeyDestroy(s *terraform.State) error {

	c := testAccProvider.Meta().(*jans.Client)

	ctx := context.Background()

	for _, rs := range s.RootModule().Resources {
		if rs.Type != "jans_json_web_key" {
			continue
		}

		kid := rs.Primary.ID

		_, err := c.GetJsonWebKey(ctx, kid)
		if !errors.Is(err, jans.ErrorNotFound) {
			return err
		}

	}

	return nil
}
