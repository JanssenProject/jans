package provider

import (
        "testing"

        "github.com/hashicorp/terraform-plugin-sdk/v2/helper/resource"
)

func TestAccDataSourceServiceStatus_basic(t *testing.T) {

        resource.Test(t, resource.TestCase{
                PreCheck:  func() { testAccPreCheck(t) },
                Providers: testAccProviders,
                Steps: []resource.TestStep{
                        {
                                Config: testAccDataSourceServiceStatusConfig_basic(),
                                Check: resource.ComposeTestCheckFunc(
                                        resource.TestCheckResourceAttrSet("data.jans_service_status.test", "status.%"),
                                ),
                        },
                },
        })
}

func testAccDataSourceServiceStatusConfig_basic() string {
        return `
data "jans_service_status" "test" {
}
`
}

func TestAccDataSourceServiceStatus_specificService(t *testing.T) {

        resource.Test(t, resource.TestCase{
                PreCheck:  func() { testAccPreCheck(t) },
                Providers: testAccProviders,
                Steps: []resource.TestStep{
                        {
                                Config: testAccDataSourceServiceStatusConfig_specific(),
                                Check: resource.ComposeTestCheckFunc(
                                        resource.TestCheckResourceAttrSet("data.jans_service_status.test", "status.%"),
                                ),
                        },
                },
        })
}

func testAccDataSourceServiceStatusConfig_specific() string {
        return `
data "jans_service_status" "test" {
  service = "jans-auth"
}
`
}
