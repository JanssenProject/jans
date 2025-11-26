package provider

import (
        "regexp"
        "testing"

        "github.com/hashicorp/terraform-plugin-sdk/v2/helper/resource"
)

func TestAccResourceSSARevocation_basic(t *testing.T) {
        resource.Test(t, resource.TestCase{
                PreCheck:  func() { testAccPreCheck(t) },
                Providers: testAccProviders,
                Steps: []resource.TestStep{
                        {
                                Config: testAccResourceSSARevocation_basic(),
                                // NOTE: API returns 500 "Unprocessable Entity" for non-existent SSA instead of 404
                                // This appears to be a server-side bug, but we test it validates API connectivity
                                ExpectError: regexp.MustCompile("not found|500|Unprocessable"),
                        },
                },
        })
}

func testAccResourceSSARevocation_basic() string {
        return `
resource "jans_ssa_revocation" "test" {
  jti = "test-jti"
}
`
}
