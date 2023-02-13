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

func TestResourceAuthServiceConfig_Mapping(t *testing.T) {

	schema := resourceAppConfiguration()

	data := schema.Data(nil)

	authConfig := jans.AppConfiguration{
		Issuer: "https://accounts.google.com",
		AuthenticationProtectionConfiguration: jans.AuthenticationProtectionConfiguration{
			DelayTime: 16,
		},
		AgamaConfiguration: jans.AgamaConfiguration{
			DefaultResponseHeaders: map[string]string{
				"X-Frame-Options": "SAMEORIGIN",
			},
		},
		// ResponseTypesSupported: [][]string{
		// 	{"code", "token"},
		// 	{"code", "id_token"},
		// },
		AuthenticationFilters: []jans.AuthenticationFilter{
			{
				Filter:                "authn.filter.basic",
				Bind:                  true,
				BaseDn:                "ou=users,dc=example,dc=com",
				BindPasswordAttribute: "userPassword",
			},
		},
		GrantTypesSupported: []string{
			"password",
			"authorization_code",
			"implicit",
			"refresh_token",
			"client_credentials",
		},
	}

	if err := toSchemaResource(data, authConfig); err != nil {
		t.Fatal(err)
	}

	newConfig := jans.AppConfiguration{}

	if err := fromSchemaResource(data, &newConfig); err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(authConfig, newConfig); diff != "" {
		t.Errorf("Got different config after mapping: %s", diff)
	}
}

func TestAccResourceAppConfiguration_basic(t *testing.T) {

	resource.Test(t, resource.TestCase{
		PreCheck:     func() { testAccPreCheck(t) },
		Providers:    testAccProviders,
		CheckDestroy: testAccReourceCheckAppConfigurationDestroy,
		Steps: []resource.TestStep{
			{
				Config:           testAccResourceAppConfigurationConfig_basic(),
				ResourceName:     "jans_app_configuration.global",
				ImportState:      true,
				ImportStateId:    "jans_app_configuration.jans_app_configuration",
				ImportStateCheck: testAccResourceCheckAppConfigurationImport,
			},
		},
	})
}

func testAccResourceAppConfigurationConfig_basic() string {
	return `
resource "jans_app_configuration" "global" {
}
`
}

func testAccResourceCheckAppConfigurationImport(states []*terraform.InstanceState) error {

	found := false
	for _, is := range states {

		if is.ID != "jans_app_configuration" {
			continue
		}

		found = true

		if err := checkAttribute(is, "openid_sub_attribute", "inum"); err != nil {
			return err
		}

		if err := checkAttribute(is, "claims_parameter_supported", "false"); err != nil {
			return err
		}

		if err := checkAttribute(is, "dynamic_registration_expiration_time", "-1"); err != nil {
			return err
		}

		break
	}

	if !found {
		return errors.New("resource not found in states")
	}

	return nil
}

func testAccReourceCheckAppConfigurationDestroy(s *terraform.State) error {

	// since this is a global resource, delete should not have any effect

	c := testAccProvider.Meta().(*jans.Client)

	ctx := context.Background()

	for _, rs := range s.RootModule().Resources {
		if rs.Type != "jans_app_configuration" {
			continue
		}
		_, err := c.GetAppConfiguration(ctx)
		if err != nil {
			return err
		}
	}

	return nil
}
