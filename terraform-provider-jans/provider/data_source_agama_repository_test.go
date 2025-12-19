package provider

import (
	"testing"

	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/resource"
)

func TestAccDataSourceAgamaRepository_basic(t *testing.T) {

	resource.Test(t, resource.TestCase{
		PreCheck:  func() { testAccPreCheck(t) },
		Providers: testAccProviders,
		Steps: []resource.TestStep{
			{
				Config: testAccDataSourceAgamaRepositoryConfig_basic(),
				Check: resource.ComposeTestCheckFunc(
					resource.TestCheckResourceAttrSet("data.jans_agama_repository.test", "repositories.#"),
				),
			},
		},
	})
}

func testAccDataSourceAgamaRepositoryConfig_basic() string {
	return `
data "jans_agama_repository" "test" {
}
`
}
