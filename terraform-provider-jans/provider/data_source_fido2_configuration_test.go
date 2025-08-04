package provider

import (
	"testing"

	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/resource"
)

func TestAccDataSourceFido2Configuration_basic(t *testing.T) {
	resource.Test(t, resource.TestCase{
		PreCheck:  func() { testAccPreCheck(t) },
		Providers: testAccProviders,
		Steps: []resource.TestStep{
			{
				Config: testAccDataSourceFido2Configuration_basic(),
				Check: resource.ComposeTestCheckFunc(
					resource.TestCheckResourceAttrSet("data.jans_fido2_configuration.test", "id"),
				),
			},
		},
	})
}

func testAccDataSourceFido2Configuration_basic() string {
	return `
data "jans_fido2_configuration" "test" {
}
`
}
