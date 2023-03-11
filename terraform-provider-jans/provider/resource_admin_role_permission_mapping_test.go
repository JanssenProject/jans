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

func TestAdminUIRolePermissionMapping_Mapping(t *testing.T) {

	schema := resourceAdminUIRolePermissionMapping()

	data := schema.Data(nil)

	mapping := jans.AdminUIRolePermissionMapping{
		Role: "role",
		Permissions: []string{
			"permission-a",
			"permission-b",
		},
	}

	if err := toSchemaResource(data, mapping); err != nil {
		t.Fatal(err)
	}

	newMapping := jans.AdminUIRolePermissionMapping{}

	if err := fromSchemaResource(data, &newMapping); err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(mapping, newMapping); diff != "" {
		t.Errorf("Got different entity after mapping: %s", diff)
	}
}

func TestAccResourceAdminUIRolePermissionMapping_basic(t *testing.T) {

	resourceName := "jans_admin_ui_role_permission_mapping.custom-role-permissions"

	resource.Test(t, resource.TestCase{
		PreCheck:     func() { testAccPreCheck(t) },
		Providers:    testAccProviders,
		CheckDestroy: testAccResourceCheckAdminUIRolePermissionMappingDestroy,
		Steps: []resource.TestStep{
			{
				Config: testAccResourceAdminUIRolePermissionMappingConfig_basic(),
				Check: resource.ComposeTestCheckFunc(
					testAccResourceCheckAdminUIRolePermissionMappingExists(resourceName),
					resource.TestCheckResourceAttr(resourceName, "role", "custom-role"),
					resource.TestCheckResourceAttr(resourceName, "permissions.0", "permission-xy"),
				),
			},
		},
	})
}

func testAccResourceAdminUIRolePermissionMappingConfig_basic() string {
	return `
resource "jans_admin_ui_permission" "permission-xy" {
	permission 	= "permission-xy"
	description = "description "
}

resource "jans_admin_ui_role" "custom-role" {
	role 				= "custom-role"
	description = "custom-role-description"
	deletable 	= true
}

resource "jans_admin_ui_role_permission_mapping" "custom-role-permissions" {
	role 				= resource.jans_admin_ui_role.custom-role.role
	permissions = [
		resource.jans_admin_ui_permission.permission-xy.permission,
	]
}
`
}

func testAccResourceCheckAdminUIRolePermissionMappingExists(name string) resource.TestCheckFunc {
	return func(s *terraform.State) error {
		rs, ok := s.RootModule().Resources[name]
		if !ok {
			return fmt.Errorf("Not found: %s", name)
		}

		c := testAccProvider.Meta().(*jans.Client)

		name := rs.Primary.ID

		ctx := context.Background()

		_, err := c.GetAdminUIRolePermissionMapping(ctx, name)
		if err != nil {
			return err
		}

		return nil
	}
}

func testAccResourceCheckAdminUIRolePermissionMappingDestroy(s *terraform.State) error {

	c := testAccProvider.Meta().(*jans.Client)

	ctx := context.Background()

	for _, rs := range s.RootModule().Resources {
		if rs.Type != "jans_admin_ui_role_permission_mapping" {
			continue
		}

		mappingID := rs.Primary.ID

		_, err := c.GetAdminUIRolePermissionMapping(ctx, mappingID)
		if !errors.Is(err, jans.ErrorNotFound) {
			return err
		}

	}

	return nil
}
