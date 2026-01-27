package provider

import (
	"context"
	"errors"
	"testing"

	"github.com/google/go-cmp/cmp"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/resource"
	"github.com/hashicorp/terraform-plugin-sdk/v2/terraform"
	"github.com/jans/terraform-provider-jans/jans"
)

func TestResourceKCSamlIDP_Mapping(t *testing.T) {

	schema := resourceKCSamlIDP()

	data := schema.Data(nil)

	idp := jans.IdentityProvider{
		DN:                      "inum=1234,ou=trusted-idp,o=jans",
		Inum:                    "1234",
		CreatorId:               "admin",
		Name:                    "test-idp",
		DisplayName:             "Test IDP",
		Description:             "A test identity provider",
		Realm:                   "master",
		Enabled:                 true,
		SigningCertificate:      "MIICert...",
		ValidateSignature:       "true",
		SingleLogoutServiceUrl:  "https://idp.example.com/logout",
		NameIDPolicyFormat:      "urn:oasis:names:tc:SAML:2.0:nameid-format:transient",
		PrincipalAttribute:      "uid",
		PrincipalType:           "ATTRIBUTE",
		IdpEntityId:             "https://idp.example.com",
		SingleSignOnServiceUrl:  "https://idp.example.com/sso",
		EncryptionPublicKey:     "MIIEnc...",
	}

	if err := toSchemaResource(data, idp); err != nil {
		t.Fatal(err)
	}

	newIdp := jans.IdentityProvider{}

	if err := fromSchemaResource(data, &newIdp); err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(idp, newIdp); diff != "" {
		t.Errorf("Got different IDP after mapping: %s", diff)
	}
}

func TestAccResourceKCSamlIDP_basic(t *testing.T) {

	resourceName := "jans_kc_saml_idp.test"

	resource.Test(t, resource.TestCase{
		PreCheck:     func() { testAccPreCheck(t) },
		Providers:    testAccProviders,
		CheckDestroy: testAccResourceCheckKCSamlIDPDestroy,
		Steps: []resource.TestStep{
			{
				Config: testAccResourceKCSamlIDPConfig_basic(),
				Check: resource.ComposeTestCheckFunc(
					testAccResourceCheckKCSamlIDPExists(resourceName),
					resource.TestCheckResourceAttr(resourceName, "name", "test-idp"),
					resource.TestCheckResourceAttr(resourceName, "display_name", "Test Identity Provider"),
					resource.TestCheckResourceAttr(resourceName, "enabled", "true"),
				),
			},
		},
	})
}

func testAccResourceKCSamlIDPConfig_basic() string {
	return `
resource "jans_kc_saml_idp" "test" {
	creator_id   = "admin"
	name         = "test-idp"
	display_name = "Test Identity Provider"
	description  = "A test SAML identity provider"
	realm        = "master"
	enabled      = true
}
`
}

func testAccResourceCheckKCSamlIDPExists(name string) resource.TestCheckFunc {
	return func(s *terraform.State) error {
		rs, ok := s.RootModule().Resources[name]
		if !ok {
			return errors.New("Not found: " + name)
		}

		inum := rs.Primary.ID
		c := testAccProvider.Meta().(*jans.Client)
		ctx := context.Background()

		_, err := c.GetIDP(ctx, inum)
		if err != nil {
			return err
		}

		return nil
	}
}

func testAccResourceCheckKCSamlIDPDestroy(s *terraform.State) error {
	c := testAccProvider.Meta().(*jans.Client)
	ctx := context.Background()

	for _, rs := range s.RootModule().Resources {
		if rs.Type != "jans_kc_saml_idp" {
			continue
		}

		inum := rs.Primary.ID
		_, err := c.GetIDP(ctx, inum)
		if err == nil {
			return errors.New("Resource still exists")
		}
	}

	return nil
}
