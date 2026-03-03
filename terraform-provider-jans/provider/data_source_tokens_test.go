package provider

import (
	"testing"

	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/resource"
)

func TestAccDataSourceTokens_basic(t *testing.T) {
	resource.Test(t, resource.TestCase{
		PreCheck:  func() { testAccPreCheck(t) },
		Providers: testAccProviders,
		Steps: []resource.TestStep{
			{
				Config: testAccDataSourceTokens_basic(),
				Check: resource.ComposeTestCheckFunc(
					resource.TestCheckResourceAttrSet("data.jans_tokens.test", "tokens.#"),
				),
			},
		},
	})
}

func TestAccDataSourceTokens_withFilters(t *testing.T) {
	resource.Test(t, resource.TestCase{
		PreCheck:  func() { testAccPreCheck(t) },
		Providers: testAccProviders,
		Steps: []resource.TestStep{
			{
				Config: testAccDataSourceTokens_withFilters(),
				Check: resource.ComposeTestCheckFunc(
					resource.TestCheckResourceAttrSet("data.jans_tokens.test", "tokens.#"),
				),
			},
		},
	})
}

func testAccDataSourceTokens_basic() string {
	return `
data "jans_tokens" "test" {
}
`
}

func testAccDataSourceTokens_withFilters() string {
	return `
data "jans_tokens" "test" {
  limit = 10
}
`
}
