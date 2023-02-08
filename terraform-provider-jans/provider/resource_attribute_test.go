package provider

import (
	"context"
	"errors"
	"fmt"
	"testing"

	"github.com/google/go-cmp/cmp"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/resource"
	"github.com/hashicorp/terraform-plugin-sdk/v2/terraform"
	"github.com/moabu/terraform-provider-jans/jans"
)

func TestResourceAttribute_Mapping(t *testing.T) {

	schema := resourceAttribute()

	data := schema.Data(nil)

	attr := jans.Attribute{
		Dn:                     "inum=98FC,ou=attributes,o=jans",
		Selected:               false,
		Inum:                   "98FC",
		Name:                   "birthdate",
		DisplayName:            "Birthdate",
		Description:            "End-User's birthday, represented as an ISO 8601:2004 [ISO8601-2004] YYYY-MM-DD format.",
		Origin:                 "jansPerson",
		DataType:               "string",
		EditType:               []string{"user", "admin"},
		ViewType:               []string{"user", "admin"},
		ClaimName:              "birthdate",
		Status:                 "active",
		Saml1Uri:               "urn:mace:dir:attribute-def:birthdate",
		Saml2Uri:               "urn:oid:1.3.6.1.4.1.48710.1.3.326",
		Urn:                    "http://openid.net/specs/openid-connect-core-1_0.html/StandardClaims/birthdate",
		OxMultiValuedAttribute: false,
		Custom:                 false,
		Required:               false,
		AdminCanAccess:         true,
		AdminCanView:           true,
		AdminCanEdit:           true,
		UserCanAccess:          true,
		UserCanView:            true,
		WhitePagesCanView:      false,
		UserCanEdit:            true,
		BaseDn:                 "inum=98FC,ou=attributes,o=jans",
	}

	if err := toSchemaResource(data, attr); err != nil {
		t.Fatal(err)
	}

	newAttr := jans.Attribute{}

	if err := fromSchemaResource(data, &newAttr); err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(attr, newAttr); diff != "" {
		t.Errorf("Got different attribute after mapping: %s", diff)
	}
}

func TestAccResourceAttribute_basic(t *testing.T) {

	var cfg jans.Attribute

	resourceName := "jans_attribute.test"

	resource.Test(t, resource.TestCase{
		PreCheck:     func() { testAccPreCheck(t) },
		Providers:    testAccProviders,
		CheckDestroy: testAccResourceCheckAttributeDestroy,
		Steps: []resource.TestStep{
			{
				Config: testAccResourceAttributeConfig_basic(),
				Check: resource.ComposeTestCheckFunc(
					testAccResourceCheckAttributeExists(resourceName, &cfg),
					resource.TestCheckResourceAttr(resourceName, "admin_can_access", "true"),
					resource.TestCheckResourceAttr(resourceName, "admin_can_edit", "true"),
					resource.TestCheckResourceAttr(resourceName, "admin_can_view", "true"),
					resource.TestCheckResourceAttr(resourceName, "claim_name", "locality"),
					resource.TestCheckResourceAttr(resourceName, "data_type", "string"),
					resource.TestCheckResourceAttr(resourceName, "description", "City"),
					resource.TestCheckResourceAttr(resourceName, "display_name", "City"),
					resource.TestCheckResourceAttr(resourceName, "saml1_uri", "urn:mace:dir:attribute-def:l"),
					resource.TestCheckResourceAttr(resourceName, "saml2_uri", "urn:oid:2.5.4.7"),
				),
			},
		},
	})
}

func testAccResourceAttributeConfig_basic() string {
	return `
resource "jans_attribute" "test" {
	admin_can_access          = true
	admin_can_edit            = true
	admin_can_view            = true
	claim_name                = "locality"
	data_type = "string"
	description = "City"
	display_name = "City"
	edit_type                 = [
			"user",
			"admin",
	]
	name                      = "l"
	origin                    = "jansCustomPerson"
	saml1_uri                 = "urn:mace:dir:attribute-def:l"
	saml2_uri                 = "urn:oid:2.5.4.7"
	urn                       = "urn:mace:dir:attribute-def:l"
	user_can_access           = true
	user_can_edit             = true
	user_can_view             = true
	view_type                 = [
			"user",
			"admin",
	]
	status = "inactive"
}
`
}

func testAccResourceCheckAttributeExists(name string, cfg *jans.Attribute) resource.TestCheckFunc {
	return func(s *terraform.State) error {
		rs, ok := s.RootModule().Resources[name]
		if !ok {
			return fmt.Errorf("Not found: %s", name)
		}

		c := testAccProvider.Meta().(*jans.Client)

		inum := rs.Primary.ID

		ctx := context.Background()

		out, err := c.GetAttribute(ctx, inum)
		if err != nil {
			return err
		}

		*cfg = *out
		return nil
	}
}

func testAccResourceCheckAttributeDestroy(s *terraform.State) error {

	c := testAccProvider.Meta().(*jans.Client)

	ctx := context.Background()

	for _, rs := range s.RootModule().Resources {
		if rs.Type != "jans_attribute" {
			continue
		}

		inum := rs.Primary.ID

		_, err := c.GetAttribute(ctx, inum)
		if !errors.Is(err, jans.ErrorNotFound) {
			return err
		}
	}

	return nil
}
