package provider

import (
	"testing"

	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/resource"
)

func TestAccDataSourceFeatureFlags_basic(t *testing.T) {

	resource.Test(t, resource.TestCase{
		PreCheck:  func() { testAccPreCheck(t) },
		Providers: testAccProviders,
		Steps: []resource.TestStep{
			{
				Config: testAccDataSourceFeatureFlagsConfig_basic(),
				Check: resource.ComposeTestCheckFunc(
					resource.TestCheckResourceAttrSet("data.jans_feature_flags.test", "flags.#"),
				),
			},
		},
	})
}

func testAccDataSourceFeatureFlagsConfig_basic() string {
	return `
data "jans_feature_flags" "test" {
}
`
}
