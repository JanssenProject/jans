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

func TestResourceUMAResource_Mapping(t *testing.T) {

	schema := resourceUMAResource()

	data := schema.Data(nil)

	umaResource := jans.UMAResource{
		Dn:              "dn",
		Inum:            "inum",
		ID:              "id",
		Name:            "name",
		IconURI:         "icon_uri",
		Scopes:          []string{"scopes"},
		ScopeExpression: "scope_expression",
		Clients:         []string{"clients"},
		Resources:       []string{"resources"},
		Creator:         "creator",
		Description:     "description",
		Type:            "type",
		CreationDate:    "creation_date",
		ExpirationDate:  "expiration_date",
		Deletable:       true,
	}

	if err := toSchemaResource(data, umaResource); err != nil {
		t.Fatal(err)
	}

	newResource := jans.UMAResource{}

	if err := fromSchemaResource(data, &newResource); err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(umaResource, newResource); diff != "" {
		t.Errorf("Got different resource after mapping: %s", diff)
	}
}

func TestAccResourceUmaResource_basic(t *testing.T) {

	resourceName := "jans_uma_resource.test"

	resource.Test(t, resource.TestCase{
		PreCheck:     func() { testAccPreCheck(t) },
		Providers:    testAccProviders,
		CheckDestroy: testAccResourceCheckUmaResourceDestroy,
		Steps: []resource.TestStep{
			{
				Config: testAccResourceUmaResourceConfig_basic(),
				Check: resource.ComposeTestCheckFunc(
					testAccResourceCheckUmaResourceExists(resourceName),
					resource.TestCheckResourceAttr(resourceName, "creator", "user"),
					resource.TestCheckResourceAttr(resourceName, "description", "test UMA resource"),
					resource.TestCheckResourceAttr(resourceName, "deletable", "true"),
				),
			},
		},
	})
}

func testAccResourceUmaResourceConfig_basic() string {
	return `
resource "jans_uma_resource" "test" {
	inum 							= "4A4E-4F3D"
	name 							= "test_uma_resource"
	scopes 						= []
	scope_expression 	= ""
	clients 				 	= []
	resources 				= []
	creator 					= "user"
	description 			= "test UMA resource"
	type 							= "uma_resource_type"
	deletable 				= "true"
}
`

}

func testAccResourceCheckUmaResourceExists(name string) resource.TestCheckFunc {
	return func(s *terraform.State) error {
		rs, ok := s.RootModule().Resources[name]
		if !ok {
			return fmt.Errorf("Not found: %s", name)
		}

		c := testAccProvider.Meta().(*jans.Client)

		id := rs.Primary.ID

		ctx := context.Background()

		_, err := c.GetUMAResource(ctx, id)
		if err != nil {
			return err
		}

		return nil
	}
}

func testAccResourceCheckUmaResourceDestroy(s *terraform.State) error {

	c := testAccProvider.Meta().(*jans.Client)

	ctx := context.Background()

	for _, rs := range s.RootModule().Resources {
		if rs.Type != "jans_uma_resource" {
			continue
		}

		id := rs.Primary.ID

		_, err := c.GetUMAResource(ctx, id)
		if !errors.Is(err, jans.ErrorNotFound) {
			return err
		}

	}

	return nil
}
