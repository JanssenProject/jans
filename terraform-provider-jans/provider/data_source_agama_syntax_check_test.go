package provider

import (
	"testing"

	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/resource"
)

func TestAccDataSourceAgamaSyntaxCheck_basic(t *testing.T) {

	resource.Test(t, resource.TestCase{
		PreCheck:  func() { testAccPreCheck(t) },
		Providers: testAccProviders,
		Steps: []resource.TestStep{
			{
				Config: testAccDataSourceAgamaSyntaxCheckConfig_basic(),
				Check: resource.ComposeTestCheckFunc(
					resource.TestCheckResourceAttrSet("data.jans_agama_syntax_check.test", "valid"),
					resource.TestCheckResourceAttrSet("data.jans_agama_syntax_check.test", "message"),
				),
			},
		},
	})
}

func testAccDataSourceAgamaSyntaxCheckConfig_basic() string {
	return `
data "jans_agama_syntax_check" "test" {
  flow_name = "test_flow"
  code      = "Flow test_flow\n  Finish true"
}
`
}
