package provider

import (
        "testing"

        "github.com/hashicorp/terraform-plugin-sdk/v2/helper/resource"
)

func TestAccDataSourceStatistics_basic(t *testing.T) {
        resource.Test(t, resource.TestCase{
                PreCheck:  func() { testAccPreCheck(t) },
                Providers: testAccProviders,
                Steps: []resource.TestStep{
                        {
                                Config: testAccDataSourceStatistics_basic(),
                                Check: resource.ComposeTestCheckFunc(
                                        resource.TestCheckResourceAttrSet("data.jans_statistics.test", "id"),
                                ),
                        },
                },
        })
}

func TestAccDataSourceStatistics_withMonth(t *testing.T) {
        resource.Test(t, resource.TestCase{
                PreCheck:  func() { testAccPreCheck(t) },
                Providers: testAccProviders,
                Steps: []resource.TestStep{
                        {
                                Config: testAccDataSourceStatistics_withMonth(),
                                Check: resource.ComposeTestCheckFunc(
                                        resource.TestCheckResourceAttr("data.jans_statistics.test", "month", "202410"),
                                ),
                        },
                },
        })
}

func testAccDataSourceStatistics_basic() string {
        return `
data "jans_statistics" "test" {
  month = "202410"
}
`
}

func testAccDataSourceStatistics_withMonth() string {
        return `
data "jans_statistics" "test" {
  month = "202410"
}
`
}
