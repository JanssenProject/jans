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

func TestResourceGroup_Mapping(t *testing.T) {

	schema := resourceGroup()

	data := schema.Data(nil)

	group := jans.Group{
		ID:          "1234",
		DisplayName: "test-group",
		Schemas:     []string{"urn:ietf:params:scim:schemas:core:2.0:Group"},
		Meta: jans.Meta{
			Location:     "https://localhost:9443/scim/v2/Groups/1234",
			ResourceType: "Group",
			Created:      "2021-03-01T00:00:00.000Z",
			LastModified: "2021-03-01T00:00:00.000Z",
		},
		Members: []jans.Member{
			{
				Ref:     "https://localhost:9443/scim/v2/Users/1234",
				Type:    "User",
				Display: "test-user",
				Value:   "1234",
			},
		},
	}

	if err := toSchemaResource(data, group); err != nil {
		t.Fatal(err)
	}

	newGroup := jans.Group{}

	if err := fromSchemaResource(data, &newGroup); err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(group, newGroup); diff != "" {
		t.Errorf("Got different entity after mapping: %s", diff)
	}
}

func TestAccResourceGroup_basic(t *testing.T) {

	resourceName := "jans_group.test"

	resource.Test(t, resource.TestCase{
		PreCheck:     func() { testAccPreCheck(t) },
		Providers:    testAccProviders,
		CheckDestroy: testAccResourceCheckGroupDestroy,
		Steps: []resource.TestStep{
			{
				Config: testAccResourceGroupConfig_basic(),
				Check: resource.ComposeTestCheckFunc(
					testAccResourceCheckGroupExists(resourceName),
					resource.TestCheckResourceAttr(resourceName, "display_name", "test-group"),
				),
			},
		},
	})
}

func testAccResourceGroupConfig_basic() string {
	return `
resource "jans_group" "test" {
	display_name 	= "test-group"
	schemas 			= ["urn:ietf:params:scim:schemas:core:2.0:Group"]
}
`
}

func testAccResourceCheckGroupExists(name string) resource.TestCheckFunc {
	return func(s *terraform.State) error {
		rs, ok := s.RootModule().Resources[name]
		if !ok {
			return fmt.Errorf("Not found: %s", name)
		}

		c := testAccProvider.Meta().(*jans.Client)

		id := rs.Primary.ID

		ctx := context.Background()

		_, err := c.GetGroup(ctx, id)
		if err != nil {
			return err
		}

		return nil
	}
}

func testAccResourceCheckGroupDestroy(s *terraform.State) error {

	c := testAccProvider.Meta().(*jans.Client)

	ctx := context.Background()

	for _, rs := range s.RootModule().Resources {
		if rs.Type != "jans_group" {
			continue
		}

		id := rs.Primary.ID

		_, err := c.GetGroup(ctx, id)
		if !errors.Is(err, jans.ErrorNotFound) {
			return err
		}
	}

	return nil
}
