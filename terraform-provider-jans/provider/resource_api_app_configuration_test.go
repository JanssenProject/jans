package provider

import (
	"context"
	"errors"
	"testing"

	"github.com/google/go-cmp/cmp"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/resource"
	"github.com/hashicorp/terraform-plugin-sdk/v2/terraform"
	"github.com/jans/terraform-provider-jans/jans"
)

func TestResourceApiAppConfiguration_Mapping(t *testing.T) {

	schema := resourceApiAppConfiguration()

	data := schema.Data(nil)

	authConfig := jans.ApiAppConfiguration{
		ConfigOauthEnabled:         true,
		ApiApprovedIssuer:          []string{"https://demoexample.gluu.org"},
		ApiProtectionType:          "oauth2",
		ApiClientId:                "1800.e6a05744-194f-4549-b1b8-f8a9df885582",
		ApiClientPassword:          "/s09uYMMjav2MOVqElIYDw==",
		EndpointInjectionEnabled:   false,
		AuthIssuerUrl:              "https://demoexample.gluu.org",
		AuthOpenidConfigurationUrl: "https://demoexample.gluu.org/.well-known/openid-configuration",
		AuthOpenidIntrospectionUrl: "https://demoexample.gluu.org/jans-auth/restv1/introspection",
		AuthOpenidTokenUrl:         "https://demoexample.gluu.org/jans-auth/restv1/token",
		AuthOpenidRevokeUrl:        "https://demoexample.gluu.org/jans-auth/restv1/revoke",
		ExclusiveAuthScopes:        []string{"jans_stat", "https://jans.io/scim/users.read", "https://jans.io/scim/users.write"},
		CorsConfigurationFilters: []jans.CorsConfigurationFilter{
			{
				FilterName:             "CorsFilter",
				CorsEnabled:            true,
				CorsAllowedOrigins:     "*",
				CorsAllowedMethods:     "GET,PUT,POST,DELETE,PATCH,HEAD,OPTIONS",
				CorsSupportCredentials: true,
				CorsLoggingEnabled:     false,
				CorsPreflightMaxAge:    1800,
				CorsRequestDecorate:    true,
			},
		},
		LoggingLevel:            "INFO",
		LoggingLayout:           "text",
		DisableJdkLogger:        true,
		MaxCount:                0,
		UserExclusionAttributes: []string{"userPassword"},
		UserMandatoryAttributes: []string{"mail", "displayName", "jansStatus", "userPassword", "givenName"},
		AgamaConfiguration: jans.AgamaConfiguration{
			MandatoryAttributes: []string{"qname", "source"},
			OptionalAttributes:  []string{"serialVersionUID", "enabled"},
		},
		AuditLogConf: jans.AuditLogConf{
			Enabled:          true,
			HeaderAttributes: []string{"User-inum"},
		},
		DataFormatConversionConf: jans.DataFormatConversionConf{
			Enabled:          true,
			IgnoreHttpMethod: []string{"@jakarta.ws.rs.GET()"},
		},
		Plugins: []jans.PluginConf{
			{
				Name:        "admin",
				Description: "admin-ui plugin",
				ClassName:   "io.jans.ca.plugin.adminui.rest.ApiApplication",
			},
			{
				Name:        "fido2",
				Description: "fido2 plugin",
				ClassName:   "io.jans.configapi.plugin.fido2.rest.ApiApplication",
			},
			{
				Name:        "scim",
				Description: "scim plugin",
				ClassName:   "io.jans.configapi.plugin.scim.rest.ApiApplication",
			},
			{
				Name:        "user-management",
				Description: "user-management plugin",
				ClassName:   "io.jans.configapi.plugin.mgt.rest.ApiApplication",
			},
		},
	}

	if err := toSchemaResource(data, authConfig); err != nil {
		t.Fatal(err)
	}

	newConfig := jans.ApiAppConfiguration{}

	patches, err := patchFromResourceData(data, &newConfig)
	if err != nil {
		t.Fatal(err)
	}

	if len(patches) != 24 {
		t.Errorf("Got %d patches, expected 24", len(patches))
	}

	if err := fromSchemaResource(data, &newConfig); err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(authConfig, newConfig); diff != "" {
		t.Errorf("Got different config after mapping: %s", diff)
	}
}

func TestAccResourceApiAppConfiguration_basic(t *testing.T) {

	resource.Test(t, resource.TestCase{
		PreCheck:     func() { testAccPreCheck(t) },
		Providers:    testAccProviders,
		CheckDestroy: testAccReourceCheckApiAppConfigurationDestroy,
		Steps: []resource.TestStep{
			{
				Config:           testAccResourceApiAppConfigurationConfig_basic(),
				ResourceName:     "jans_api_app_configuration.global",
				ImportState:      true,
				ImportStateId:    "jans_api_app_configuration.jans_api_app_configuration",
				ImportStateCheck: testAccResourceCheckApiAppConfigurationImport,
			},
		},
	})
}

func testAccResourceApiAppConfigurationConfig_basic() string {
	return `
resource "jans_api_app_configuration" "global" {
}
`
}

func testAccResourceCheckApiAppConfigurationImport(states []*terraform.InstanceState) error {

	found := false
	for _, is := range states {

		if is.ID != "jans_api_app_configuration" {
			continue
		}

		found = true

		if err := checkAttribute(is, "api_protection_type", "oauth2"); err != nil {
			return err
		}

		if err := checkAttribute(is, "user_exclusion_attributes.0", "userPassword"); err != nil {
			return err
		}

		if err := checkAttribute(is, "endpoint_injection_enabled", "false"); err != nil {
			return err
		}

		break
	}

	if !found {
		return errors.New("resource not found in states")
	}

	return nil
}

func testAccReourceCheckApiAppConfigurationDestroy(s *terraform.State) error {

	// since this is a global resource, delete should not have any effect

	c := testAccProvider.Meta().(*jans.Client)

	ctx := context.Background()

	for _, rs := range s.RootModule().Resources {
		if rs.Type != "jans_api_app_configuration" {
			continue
		}
		_, err := c.GetApiAppConfiguration(ctx)
		if err != nil {
			return err
		}
	}

	return nil
}
