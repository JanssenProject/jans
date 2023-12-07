package provider

import (
	"testing"

	"github.com/google/go-cmp/cmp"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/resource"
	"github.com/jans/terraform-provider-jans/jans"
)

func TestDataSourceSchema_Mapping(t *testing.T) {

	schema := dataSourceSchema()

	data := schema.Data(nil)

	cfg := jans.Schema{
		Schemas: []string{"urn:ietf:params:scim:schemas:core:2.0:Schema"},
		ID:      "urn:ietf:params:scim:schemas:core:2.0:Group",
		Meta: jans.Meta{
			ResourceType: "Schema",
			Location:     "https://localhost:9443/scim/v2/Schemas/urn:ietf:params:scim:schemas:core:2.0:Group",
			Created:      "2021-03-16T15:00:00.000Z",
			LastModified: "2021-03-16T15:00:00.000Z",
		},
		Name:        "Group",
		Description: "Group",
		Attributes: []jans.SchemaAttribute{
			{
				Name:        "displayName",
				Type:        "string",
				MultiValued: false,
				Description: "A human-readable name for the Group",
				Required:    true,
				CaseExact:   false,
				Mutability:  "readWrite",
				Returned:    "default",
				Uniqueness:  "none",
			},
		},
	}

	if err := toSchemaResource(data, &cfg); err != nil {
		t.Fatal(err)
	}

	newCfg := jans.Schema{}

	if err := fromSchemaResource(data, &newCfg); err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(cfg, newCfg); diff != "" {
		t.Errorf("Got different configuration after mapping: %s", diff)
	}

}

func TestAccDataSourceSchema_basic(t *testing.T) {

	resource.Test(t, resource.TestCase{
		PreCheck:  func() { testAccPreCheck(t) },
		Providers: testAccProviders,
		Steps: []resource.TestStep{
			{
				Config: testAccDataSourceSchema_basic(),
				Check: resource.ComposeTestCheckFunc(
					resource.TestCheckResourceAttr("data.jans_schema.group", "name", "Group"),
					resource.TestCheckResourceAttr("data.jans_schema.group", "attributes.0.name", "displayName"),
				),
			},
		},
	})
}

func testAccDataSourceSchema_basic() string {
	return `
data "jans_schema" "group" {
	id = "urn:ietf:params:scim:schemas:core:2.0:Group"
}
`
}
