package provider

import (
	"context"
	"errors"
	"testing"

	"github.com/google/go-cmp/cmp"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/resource"
	"github.com/hashicorp/terraform-plugin-sdk/v2/terraform"
	"github.com/moabu/terraform-provider-jans/jans"
)

func TestLoggingConfiguration_Mapping(t *testing.T) {

	schema := resourceLoggingConfiguration()

	data := schema.Data(nil)

	cfg := jans.LoggingConfiguration{}

	if err := toSchemaResource(data, cfg); err != nil {
		t.Fatal(err)
	}

	newCfg := jans.LoggingConfiguration{}

	if err := fromSchemaResource(data, &newCfg); err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(cfg, newCfg); diff != "" {
		t.Errorf("Got different configuration after mapping: %s", diff)
	}
}

func TestLoggingConfiguration_basic(t *testing.T) {

	resourceName := "jans_logging_configuration.global"

	resource.Test(t, resource.TestCase{
		PreCheck:     func() { testAccPreCheck(t) },
		Providers:    testAccProviders,
		CheckDestroy: testAccResourceCheckLoggingConfigurationDestroy,
		Steps: []resource.TestStep{
			{
				Config:           testAccResourceLoggingConfigurationConfig_basic(),
				ResourceName:     resourceName,
				ImportState:      true,
				ImportStateId:    "jans_logging_configuration.jans_logging_configuration",
				ImportStateCheck: testAccResourceCheckLoggingConfigurationImport,
			},
		},
	})
}

func testAccResourceLoggingConfigurationConfig_basic() string {
	return `
resource "jans_logging_configuration" "global" {
}
`
}

func testAccResourceCheckLoggingConfigurationImport(states []*terraform.InstanceState) error {

	found := false
	for _, is := range states {

		if is.ID != "jans_logging_configuration" {
			continue
		}

		found = true

		if is.Attributes["logging_level"] != "INFO" {
			return errors.New("logging_level is not equal")
		}

		if is.Attributes["logging_layout"] != "text" {
			return errors.New("logging_layout is not equal")
		}

		if is.Attributes["http_logging_enabled"] != "false" {
			return errors.New("http_logging_enabled is not equal")
		}

		if is.Attributes["disable_jdk_logger"] != "true" {
			return errors.New("disable_jdk_logger is not equal")
		}

		if is.Attributes["enabled_oauth_audit_logging"] != "false" {
			return errors.New("enabled_oauth_audit_logging is not equal")
		}

		break
	}

	if !found {
		return errors.New("resource not found in states")
	}

	return nil
}

func testAccResourceCheckLoggingConfigurationDestroy(s *terraform.State) error {

	// since this is a global resource, delete should not have any effect

	c := testAccProvider.Meta().(*jans.Client)

	ctx := context.Background()

	_, err := c.GetLoggingConfiguration(ctx)
	if err != nil {
		return err
	}

	return nil
}
