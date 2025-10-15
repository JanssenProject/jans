package provider

import (
        "regexp"
        "testing"

        "github.com/hashicorp/terraform-plugin-sdk/v2/helper/resource"
)

func TestAccResourceTokenRevocation_basic(t *testing.T) {
        resource.Test(t, resource.TestCase{
                PreCheck:  func() { testAccPreCheck(t) },
                Providers: testAccProviders,
                Steps: []resource.TestStep{
                        {
                                Config: testAccResourceTokenRevocation_basic(),
                                // Expect 404 since test token doesn't exist - this validates the resource reaches the API
                                ExpectError: regexp.MustCompile("not found"),
                        },
                },
        })
}

func testAccResourceTokenRevocation_basic() string {
        return `
resource "jans_token_revocation" "test" {
  token_code = "test-token-code"
}
`
}
