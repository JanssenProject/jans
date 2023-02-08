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

func TestAdminUIRole_Mapping(t *testing.T) {

	schema := resourceAdminUIRole()

	data := schema.Data(nil)

	role := jans.AdminUIRole{
		Role:        "role",
		Description: "description",
		Deletable:   true,
	}

	if err := toSchemaResource(data, role); err != nil {
		t.Fatal(err)
	}

	newRole := jans.AdminUIRole{}

	if err := fromSchemaResource(data, &newRole); err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(role, newRole); diff != "" {
		t.Errorf("Got different entity after mapping: %s", diff)
	}
}

func TestAccResourceAdminUIRole_basic(t *testing.T) {

	resourceName := "jans_admin_ui_role.test"

	resource.Test(t, resource.TestCase{
		PreCheck:     func() { testAccPreCheck(t) },
		Providers:    testAccProviders,
		CheckDestroy: testAccResourceCheckAdminUIRoleDestroy,
		Steps: []resource.TestStep{
			{
				Config: testAccResourceAdminUIRoleConfig_basic(),
				Check: resource.ComposeTestCheckFunc(
					testAccResourceCheckAdminUIRoleExists(resourceName),
					resource.TestCheckResourceAttr(resourceName, "role", "custom-role"),
					resource.TestCheckResourceAttr(resourceName, "description", "custom-role-description"),
					resource.TestCheckResourceAttr(resourceName, "deletable", "true"),
				),
			},
		},
	})
}

func testAccResourceAdminUIRoleConfig_basic() string {
	return `
resource "jans_admin_ui_role" "test" {
	role 				= "custom-role"
	description = "custom-role-description"
	deletable 	= true
}
`
}

func testAccResourceCheckAdminUIRoleExists(name string) resource.TestCheckFunc {
	return func(s *terraform.State) error {
		rs, ok := s.RootModule().Resources[name]
		if !ok {
			return fmt.Errorf("Not found: %s", name)
		}

		c := testAccProvider.Meta().(*jans.Client)

		name := rs.Primary.ID

		ctx := context.Background()

		_, err := c.GetAdminUIRole(ctx, name)
		if err != nil {
			return err
		}

		return nil
	}
}

func testAccResourceCheckAdminUIRoleDestroy(s *terraform.State) error {

	c := testAccProvider.Meta().(*jans.Client)

	ctx := context.Background()

	for _, rs := range s.RootModule().Resources {
		if rs.Type != "jans_admin_ui_role" {
			continue
		}

		roleID := rs.Primary.ID

		_, err := c.GetAdminUIRole(ctx, roleID)
		if !errors.Is(err, jans.ErrorNotFound) {
			return err
		}

	}

	return nil
}
