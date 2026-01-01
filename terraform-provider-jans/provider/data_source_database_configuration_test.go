package provider

import (
	"testing"

	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/resource"
)

func TestAccDataSourceDatabaseConfiguration_basic(t *testing.T) {

	resource.Test(t, resource.TestCase{
		PreCheck:  func() { testAccPreCheck(t) },
		Providers: testAccProviders,
		Steps: []resource.TestStep{
			{
				Config: testAccDataSourceDatabaseConfigurationConfig_basic(),
				Check: resource.ComposeTestCheckFunc(
					resource.TestCheckResourceAttrSet("data.jans_database_configuration.test", "tables.#"),
					resource.TestCheckResourceAttrSet("data.jans_database_configuration.test", "schema_json"),
				),
			},
		},
	})
}

func testAccDataSourceDatabaseConfigurationConfig_basic() string {
	return `
data "jans_database_configuration" "test" {
}
`
}
