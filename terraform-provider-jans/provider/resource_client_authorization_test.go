package provider

import (
	"context"
	"fmt"
	"testing"

	"github.com/google/go-cmp/cmp"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/resource"
	"github.com/hashicorp/terraform-plugin-sdk/v2/terraform"
	"github.com/jans/terraform-provider-jans/jans"
)

func TestResourceClientAuthorization_Mapping(t *testing.T) {
	schema := resourceClientAuthorization()
	data := schema.Data(nil)

	auth := jans.ClientAuthorization{
		ID:           "test-id",
		ClientId:     "test-client-id",
		Scopes:       []string{"openid", "profile"},
		RedirectURIs: []string{"https://example.com/callback"},
		GrantTypes:   []string{"authorization_code"},
	}

	if err := toSchemaResource(data, auth); err != nil {
		t.Fatal(err)
	}

	newAuth := jans.ClientAuthorization{}
	if err := fromSchemaResource(data, &newAuth); err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(auth, newAuth); diff != "" {
		t.Errorf("Got different entity after mapping: %s", diff)
	}
}

func TestAccResourceClientAuthorization_basic(t *testing.T) {
	resource.Test(t, resource.TestCase{
		PreCheck:  func() { testAccPreCheck(t) },
		Providers: testAccProviders,
		Steps: []resource.TestStep{
			{
				Config: testAccResourceClientAuthorizationConfig_basic(),
				Check: resource.ComposeTestCheckFunc(
					testAccCheckClientAuthorizationExists("jans_client_authorization.test"),
					resource.TestCheckResourceAttr("jans_client_authorization.test", "client_id", "test-client-id"),
				),
			},
		},
	})
}

func testAccCheckClientAuthorizationExists(resourceName string) resource.TestCheckFunc {
	return func(s *terraform.State) error {
		rs, ok := s.RootModule().Resources[resourceName]
		if !ok {
			return fmt.Errorf("not found: %s", resourceName)
		}

		if rs.Primary.ID == "" {
			return fmt.Errorf("no ID is set")
		}

		client := testAccProvider.Meta().(*jans.Client)
		ctx := context.Background()

		_, err := client.GetClientAuthorization(ctx, rs.Primary.ID)
		return err
	}
}

func testAccResourceClientAuthorizationConfig_basic() string {
	return `
resource "jans_client_authorization" "test" {
	client_id = "test-client-id"
	scopes = ["openid", "profile"]
	redirect_uris = ["https://example.com/callback"]
	grant_types = ["authorization_code"]
}
`
}
