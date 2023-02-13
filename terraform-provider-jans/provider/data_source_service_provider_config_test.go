package provider

import (
	"testing"

	"github.com/google/go-cmp/cmp"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/resource"
	"github.com/jans/terraform-provider-jans/jans"
)

func TestDatasourceServiceProviderConfig_Mapping(t *testing.T) {

	schema := dataSourceServiceProviderConfig()

	data := schema.Data(nil)

	cfg := jans.ServiceProviderConfig{
		Schemas: []string{
			"urn:ietf:params:scim:schemas:core:2.0:ServiceProviderConfig",
		},
		Meta: jans.Meta{
			ResourceType: "ServiceProviderConfig",
			Location:     "https://moabu-national-colt.gluu.info/jans-scim/restv1/v2/ServiceProviderConfig",
		},
		DocumentationUri: "https://gluu.org/docs/ce/user-management/scim2/",
		Patch: jans.Supported{
			Supported: true,
		},
		Bulk: jans.Bulk{
			Supported:      true,
			MaxOperations:  30,
			MaxPayloadSize: 3072000,
		},
		Filter: jans.Filter{
			Supported:  true,
			MaxResults: 200,
		},
		ChangePassword: jans.Supported{
			Supported: true,
		},
		Sort: jans.Supported{
			Supported: true,
		},
		Etag: jans.Supported{
			Supported: false,
		},
		AuthenticationSchemes: []jans.AuthenticationSchemes{
			{
				Type:        "oauth2",
				Name:        "OAuth 2.0",
				Description: "OAuth2 Bearer Token Authentication Scheme.",
				SpecURI:     "http://tools.ietf.org/html/rfc6749",
				DocumentURI: "http://tools.ietf.org/html/rfc6749",
				Primary:     true,
			},
		},
	}

	if err := toSchemaResource(data, &cfg); err != nil {
		t.Fatal(err)
	}

	newCfg := jans.ServiceProviderConfig{}

	if err := fromSchemaResource(data, &newCfg); err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(cfg, newCfg); diff != "" {
		t.Errorf("Got different configuration after mapping: %s", diff)
	}

}

func TestAccDataSourceServiceProviderConfig_basic(t *testing.T) {

	resource.Test(t, resource.TestCase{
		PreCheck:  func() { testAccPreCheck(t) },
		Providers: testAccProviders,
		Steps: []resource.TestStep{
			{
				Config: testAccDataSourceServiceProviderConfig_basic(),
				Check: resource.ComposeTestCheckFunc(
					resource.TestCheckResourceAttr("data.jans_service_provider_config.config", "documentation_uri", "https://gluu.org/docs/ce/user-management/scim2/"),
				),
			},
		},
	})
}

func testAccDataSourceServiceProviderConfig_basic() string {
	return `
data "jans_service_provider_config" "config" {
}
`
}
