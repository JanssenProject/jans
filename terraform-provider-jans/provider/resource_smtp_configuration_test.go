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

func TestResourceSmtpConfiguration_Mapping(t *testing.T) {

	schema := resourceSmtpConfiguration()

	data := schema.Data(nil)

	cfg := jans.SMTPConfiguration{
		Host:                   "localhost",
		Port:                   25,
		RequiresSSL:            true,
		TrustHost:              true,
		FromName:               "Jans",
		FromEmailAddress:       "jans@jansen.io",
		RequiresAuthentication: true,
		UserName:               "jans",
		Password:               "password",
	}

	if err := toSchemaResource(data, cfg); err != nil {
		t.Fatal(err)
	}

	newCfg := jans.SMTPConfiguration{}

	if err := fromSchemaResource(data, &newCfg); err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(cfg, newCfg); diff != "" {
		t.Errorf("Got different configuration after mapping: %s", diff)
	}
}

func TestAccResourceSMTPConfiguration_basic(t *testing.T) {

	resource.Test(t, resource.TestCase{
		PreCheck:     func() { testAccPreCheck(t) },
		Providers:    testAccProviders,
		CheckDestroy: testAccReourceCheckSMTPConfigurationDestroy,
		Steps: []resource.TestStep{
			{
				Config:           testAccResourceSMTPConfigurationConfig_basic(),
				ResourceName:     "jans_smtp_configuration.global",
				ImportState:      true,
				ImportStateId:    "jans_smtp_configuration.jans_smtp_configuration",
				ImportStateCheck: testAccResourceCheckSMTPConfigurationImport,
			},
		},
	})
}

func testAccResourceSMTPConfigurationConfig_basic() string {
	return `
resource "jans_smtp_configuration" "global" {
}
`
}

func testAccResourceCheckSMTPConfigurationImport(states []*terraform.InstanceState) error {

	found := false
	for _, is := range states {

		if is.ID != "jans_smtp_configuration" {
			continue
		}

		found = true

		// if err := checkAttribute(is, "base_dn", "o=jans"); err != nil {
		// 	return err
		// }

		// if err := checkAttribute(is, "person_custom_object_class", "jansCustomPerson"); err != nil {
		// 	return err
		// }

		// if err := checkAttribute(is, "max_count", "200"); err != nil {
		// 	return err
		// }

		break
	}

	if !found {
		return errors.New("resource not found in states")
	}

	return nil
}

func testAccReourceCheckSMTPConfigurationDestroy(s *terraform.State) error {

	// since this is a global resource, delete should not have any effect

	c := testAccProvider.Meta().(*jans.Client)

	ctx := context.Background()

	for _, rs := range s.RootModule().Resources {
		if rs.Type != "jans_smtp_configuration" {
			continue
		}
		_, err := c.GetSMTPConfiguration(ctx)
		if err != nil {
			return err
		}
	}

	return nil
}
