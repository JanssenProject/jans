package provider

import (
	"context"
	"errors"
	"fmt"
	"testing"

	"github.com/google/go-cmp/cmp"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/resource"
	"github.com/hashicorp/terraform-plugin-sdk/v2/terraform"
	"github.com/jans/terraform-provider-jans/jans"
)

func TestReourceOidcClient_Mapping(t *testing.T) {

	schema := resourceOidcClient()

	data := schema.Data(nil)

	client := jans.OidcClient{
		Dn:                                "inum=1201.d52300ed-8193-510e-b31d-5829f4af346e,ou=clients,o=jans",
		ClientSecret:                      "SEw7VOX8m9ah",
		FrontChannelLogoutUri:             "null",
		FrontChannelLogoutSessionRequired: false,
		RedirectUris: []string{
			"https://moabu-21f13b7c-9069-ad58-5685-852e6d236020.gluu.info/.well-known/scim-configuration",
		},
		// ClaimRedirectUris: []string{},
		// ResponseTypes:     []string{},
		GrantTypes:      []string{"client_credentials"},
		ApplicationType: "native",
		// Contacts:                []string{},
		ClientName:              "",
		LogoUri:                 "",
		ClientUri:               "",
		PolicyUri:               "",
		TosUri:                  "",
		SubjectType:             "pairwise",
		TokenEndpointAuthMethod: "client_secret_basic",
		// DefaultAcrValues:        []string{},
		// PostLogoutRedirectUris:  []string{},
		// RequestUris:             []string{},
		Scopes: []string{
			"inum=1200.33AFBA,ou=scopes,o=jans",
			"inum=1200.939B32,ou=scopes,o=jans",
		},
		// Claims:                      []string{},
		TrustedClient:               false,
		PersistClientAuthorizations: false,
		IncludeClaimsInIdToken:      false,
		CustomAttributes: []jans.CustomAttribute{
			{
				Name:         "displayName",
				MultiValued:  false,
				Values:       []string{`"SCIM client"`},
				DisplayValue: "SCIM client",
				Value:        `"SCIM client"`,
			},
		},
		RptAsJwt:              false,
		AccessTokenAsJwt:      false,
		AccessTokenSigningAlg: "RS256",
		Disabled:              false,
		// AuthorizedOrigins:     []string{},
		Attributes: &jans.OidcClientAttribute{
			RunIntrospectionScriptBeforeJwtCreation: false,
			KeepClientAuthorizationAfterExpiration:  false,
			AllowSpontaneousScopes:                  false,
			BackchannelLogoutSessionRequired:        false,
			ParLifetime:                             600,
			RequirePar:                              false,
			JansDefaultPromptLogin:                  false,
		},
		AuthenticationMethod:  "client_secret_basic",
		TokenBindingSupported: false,
		BaseDn:                "inum=1201.d52300ed-8193-510e-b31d-5829f4af346e,ou=clients,o=jans",
		Inum:                  "1201.d52300ed-8193-510e-b31d-5829f4af346e",
	}

	if err := toSchemaResource(data, client); err != nil {
		t.Fatal(err)
	}

	newClient := jans.OidcClient{}

	if err := fromSchemaResource(data, &newClient); err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(client, newClient); diff != "" {
		t.Errorf("Got different client after mapping: %s", diff)
	}
}

func TestAccResourceOidcClient_basic(t *testing.T) {

	resourceName := "jans_oidc_client.test"

	resource.Test(t, resource.TestCase{
		PreCheck:     func() { testAccPreCheck(t) },
		Providers:    testAccProviders,
		CheckDestroy: testAccResourceCheckOidcClientDestroy,
		Steps: []resource.TestStep{
			{
				Config: testAccResourceOidcClientConfig_basic(),
				Check: resource.ComposeTestCheckFunc(
					testAccResourceCheckOidcClientExists(resourceName),
					resource.TestCheckResourceAttr(resourceName, "client_name", "SCIM client"),
					resource.TestCheckResourceAttr(resourceName, "grant_types.0", "client_credentials"),
					resource.TestCheckResourceAttr(resourceName, "front_channel_logout_uri", "https://jans.io/logout"),
				),
			},
		},
	})
}

func testAccResourceOidcClientConfig_basic() string {
	return `
resource "jans_oidc_client" "test" {
	inum 																	= "1201.d52300ed-8193-510e-b31d-5829f4af346e"
	authentication_method 								= "client_secret_basic"
	token_endpoint_auth_method						= "client_secret_basic"
	client_name 													= "SCIM client"
	client_secret 												= "SEw7VOX8m9ah"
	front_channel_logout_uri 							= "https://jans.io/logout"
	front_channel_logout_session_required = false
	redirect_uris 												= ["https://jans.io/.well-known/scim-configuration"]
	claim_redirect_uris 									= []
	response_types 												= []
	grant_types 													= ["client_credentials"]
	subject_type 													= "public"
	
	lifecycle {
		# ignore changes to secret, as it will be 
		# returned from the API as a hash
    ignore_changes = [ client_secret ]
  }
}
`
}

func testAccResourceCheckOidcClientExists(name string) resource.TestCheckFunc {
	return func(s *terraform.State) error {
		rs, ok := s.RootModule().Resources[name]
		if !ok {
			return fmt.Errorf("Not found: %s", name)
		}

		c := testAccProvider.Meta().(*jans.Client)

		inum := rs.Primary.ID

		ctx := context.Background()

		_, err := c.GetOidcClient(ctx, inum)
		if err != nil {
			return err
		}

		return nil
	}
}

func testAccResourceCheckOidcClientDestroy(s *terraform.State) error {

	c := testAccProvider.Meta().(*jans.Client)

	ctx := context.Background()

	for _, rs := range s.RootModule().Resources {
		if rs.Type != "jans_oidc_client" {
			continue
		}

		inum := rs.Primary.ID

		_, err := c.GetOidcClient(ctx, inum)
		if !errors.Is(err, jans.ErrorNotFound) {
			return err
		}

	}

	return nil
}
