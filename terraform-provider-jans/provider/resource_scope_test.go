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

func TestResourceScope_Mapping(t *testing.T) {

	schema := resourceScope()

	data := schema.Data(nil)

	scope := jans.Scope{}

	if err := toSchemaResource(data, scope); err != nil {
		t.Fatal(err)
	}

	newScope := jans.Scope{}

	if err := fromSchemaResource(data, &newScope); err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(scope, newScope); diff != "" {
		t.Errorf("Got different scope after mapping: %s", diff)
	}
}

func TestAccResourceScope_basic(t *testing.T) {

	resourceName := "jans_scope.test"

	resource.Test(t, resource.TestCase{
		PreCheck:     func() { testAccPreCheck(t) },
		Providers:    testAccProviders,
		CheckDestroy: testAccResourceCheckScopeDestroy,
		Steps: []resource.TestStep{
			{
				Config: testAccResourceScopeConfig_basic(),
				Check: resource.ComposeTestCheckFunc(
					testAccResourceCheckScopeExists(resourceName),
					resource.TestCheckResourceAttr(resourceName, "display_name", "test groups.read"),
					resource.TestCheckResourceAttr(resourceName, "scope_type", "oauth"),
					resource.TestCheckResourceAttr(resourceName, "attributes.0.show_in_configuration_endpoint", "true"),
				),
			},
		},
	})
}

func testAccResourceScopeConfig_basic() string {
	return `
resource "jans_scope" "test" {
	display_name  = "test groups.read"
	scope_id     	= "https://jans.io/test/groups.read"
	description 	= "Query test group resources"
	scope_type    = "oauth"
	creation_date = "2022-09-01T13:42:58"
	uma_type      = false
	
	attributes {
		show_in_configuration_endpoint = true
	}
}
`
}

func testAccResourceCheckScopeExists(name string) resource.TestCheckFunc {
	return func(s *terraform.State) error {
		rs, ok := s.RootModule().Resources[name]
		if !ok {
			return fmt.Errorf("Not found: %s", name)
		}

		c := testAccProvider.Meta().(*jans.Client)

		inum := rs.Primary.ID

		ctx := context.Background()

		_, err := c.GetScope(ctx, inum)
		if err != nil {
			return err
		}

		return nil
	}
}

func testAccResourceCheckScopeDestroy(s *terraform.State) error {

	c := testAccProvider.Meta().(*jans.Client)

	ctx := context.Background()

	for _, rs := range s.RootModule().Resources {
		if rs.Type != "jans_scope" {
			continue
		}

		inum := rs.Primary.ID

		_, err := c.GetScope(ctx, inum)
		if !errors.Is(err, jans.ErrorNotFound) {
			return err
		}

	}

	return nil
}
