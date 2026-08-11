package provider

import (
	"testing"

	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/resource"
)

// TestAccDataSourceClientAuthorization_basic reads the client authorizations for
// an existing user (the default admin). A user with no seeded authorizations is
// a valid result, so the test only asserts the read succeeds and the collection
// attribute is present.
func TestAccDataSourceClientAuthorization_basic(t *testing.T) {
	resource.Test(t, resource.TestCase{
		PreCheck:  func() { testAccPreCheck(t) },
		Providers: testAccProviders,
		Steps: []resource.TestStep{
			{
				Config: testAccDataSourceClientAuthorization_basic(),
				Check: resource.ComposeTestCheckFunc(
					resource.TestCheckResourceAttrSet("data.jans_client_authorization.test", "authorizations.#"),
				),
			},
		},
	})
}

func testAccDataSourceClientAuthorization_basic() string {
	return `
data "jans_client_authorization" "test" {
	user_id = "admin"
}
`
}
