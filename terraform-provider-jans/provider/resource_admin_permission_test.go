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

func TestAdminUIPermission_Mapping(t *testing.T) {

	schema := resourceAdminUIPermission()

	data := schema.Data(nil)

	permission := jans.AdminUIPermission{
		Permission:  "permission",
		Description: "description",
	}

	if err := toSchemaResource(data, permission); err != nil {
		t.Fatal(err)
	}

	newPermission := jans.AdminUIPermission{}

	if err := fromSchemaResource(data, &newPermission); err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(permission, newPermission); diff != "" {
		t.Errorf("Got different entity after mapping: %s", diff)
	}
}

func TestAccResourceAdminUIPermission_basic(t *testing.T) {

	resourceName := "jans_admin_ui_permission.test"

	resource.Test(t, resource.TestCase{
		PreCheck:     func() { testAccPreCheck(t) },
		Providers:    testAccProviders,
		CheckDestroy: testAccReourceCheckAdminUIPermissionDestroy,
		Steps: []resource.TestStep{
			{
				Config: testAccResourceAdminUIPermissionConfig_basic(),
				Check: resource.ComposeTestCheckFunc(
					testAccResourceCheckAdminUIPermissionExists(resourceName),
					resource.TestCheckResourceAttr(resourceName, "permission", "permission-xy"),
					resource.TestCheckResourceAttr(resourceName, "description", "description"),
				),
			},
		},
	})
}

func testAccResourceAdminUIPermissionConfig_basic() string {
	return `
resource "jans_admin_ui_permission" "test" {
	permission 	= "permission-xy"
	description = "description"
}
`
}

func testAccResourceCheckAdminUIPermissionExists(name string) resource.TestCheckFunc {
	return func(s *terraform.State) error {
		rs, ok := s.RootModule().Resources[name]
		if !ok {
			return fmt.Errorf("Not found: %s", name)
		}

		c := testAccProvider.Meta().(*jans.Client)

		permission := rs.Primary.ID

		ctx := context.Background()

		_, err := c.GetAdminUIPermission(ctx, permission)
		if err != nil {
			return err
		}

		return nil
	}
}

func testAccReourceCheckAdminUIPermissionDestroy(s *terraform.State) error {

	c := testAccProvider.Meta().(*jans.Client)

	ctx := context.Background()

	for _, rs := range s.RootModule().Resources {
		if rs.Type != "jans_admin_ui_permission" {
			continue
		}

		p := rs.Primary.ID

		_, err := c.GetAdminUIPermission(ctx, p)
		if !errors.Is(err, jans.ErrorNotFound) {
			return err
		}

	}

	return nil
}
