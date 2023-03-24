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

func TestResourceDefaultAuthenticationMethod_Mapping(t *testing.T) {

	schema := resourceDefaultAuthenticationMethod()

	data := schema.Data(nil)

	acr := jans.DefaultAuthenticationMethod{
		DefaultAcr: "pwd",
	}

	if err := toSchemaResource(data, acr); err != nil {
		t.Fatal(err)
	}

	newAcr := jans.DefaultAuthenticationMethod{}

	if err := fromSchemaResource(data, &newAcr); err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(acr, newAcr); diff != "" {
		t.Errorf("Got different entity after mapping: %s", diff)
	}
}

func TestAccResourceDefaultAuthenticationMethod_basic(t *testing.T) {

	resource.Test(t, resource.TestCase{
		PreCheck:     func() { testAccPreCheck(t) },
		Providers:    testAccProviders,
		CheckDestroy: testAccResourceCheckDefaultAuthenticationMethodDestroy,
		Steps: []resource.TestStep{
			{
				Config:           testAccResourceDefaultAuthenticationMethodConfig_basic(),
				ResourceName:     "jans_default_authentication_method.global",
				ImportState:      true,
				ImportStateId:    "jans_default_authentication_method.jans_default_authentication_method",
				ImportStateCheck: testAccResourceCheckDefaultAuthenticationMethodImport,
			},
		},
	})
}

func testAccResourceDefaultAuthenticationMethodConfig_basic() string {
	return `
resource "jans_default_authentication_method" "global" {
}
`
}

func testAccResourceCheckDefaultAuthenticationMethodImport(states []*terraform.InstanceState) error {

	found := false
	for _, is := range states {

		if is.ID != "jans_default_authentication_method" {
			continue
		}

		found = true

		if err := checkAttribute(is, "default_acr", "token"); err != nil {
			return err
		}

		break
	}

	if !found {
		return errors.New("resource not found in states")
	}

	return nil
}

func testAccResourceCheckDefaultAuthenticationMethodDestroy(s *terraform.State) error {

	// since this is a global resource, delete should not have any effect

	c := testAccProvider.Meta().(*jans.Client)

	ctx := context.Background()

	for _, rs := range s.RootModule().Resources {
		if rs.Type != "jans_default_authentication_method" {
			continue
		}

		_, err := c.GetDefaultAuthenticationMethod(ctx)
		if err != nil {
			return err
		}
	}

	return nil
}
