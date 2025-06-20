package provider

import (
	"testing"

	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/resource"
)

func TestAccDataSourceCustomScriptTypes_basic(t *testing.T) {

	resource.Test(t, resource.TestCase{
		PreCheck:  func() { testAccPreCheck(t) },
		Providers: testAccProviders,
		Steps: []resource.TestStep{
			{
				Config: testAccDataSourceCustomScriptTypesConfig_basic(),
				Check: resource.ComposeTestCheckFunc(
					resource.TestCheckResourceAttrSet("data.jans_custom_script_types.test", "script_types.#"),
					resource.TestCheckResourceAttr("data.jans_custom_script_types.test", "script_types.0", "person_authentication"),
				),
			},
		},
	})
}

func testAccDataSourceCustomScriptTypesConfig_basic() string {
	return `
data "jans_custom_script_types" "test" {
}
`
}
