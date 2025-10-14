package provider

import (
        "regexp"
        "testing"

        "github.com/hashicorp/terraform-plugin-sdk/v2/helper/resource"
)

func TestAccResourceSessionRevocation_basic(t *testing.T) {
        resource.Test(t, resource.TestCase{
                PreCheck:  func() { testAccPreCheck(t) },
                Providers: testAccProviders,
                Steps: []resource.TestStep{
                        {
                                Config: testAccResourceSessionRevocation_basic(),
                                // Expect 404 since test user doesn't exist - this validates the resource reaches the API
                                ExpectError: regexp.MustCompile("not found"),
                        },
                },
        })
}

func testAccResourceSessionRevocation_basic() string {
        return `
resource "jans_session_revocation" "test" {
  user_dn = "inum=test-user,ou=people,o=jans"
}
`
}
