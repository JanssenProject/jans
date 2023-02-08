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

func TestResourceScimAppConfiguration_Mapping(t *testing.T) {

	schema := resourceScimAppConfiguration()

	data := schema.Data(nil)

	cfg := jans.ScimAppConfigurations{
		MaxCount:                   200,
		DisableJdkLogger:           true,
		UseLocalCache:              true,
		BaseDN:                     "o=jans",
		ApplicationUrl:             "https://moabu-21f13b7c-9069-ad58-5685-852e6d236020.gluu.info",
		BaseEndpoint:               "https://moabu-21f13b7c-9069-ad58-5685-852e6d236020.gluu.info/jans-scim/restv1",
		PersonCustomObjectClass:    "jansCustomPerson",
		OxAuthIssuer:               "https://moabu-21f13b7c-9069-ad58-5685-852e6d236020.gluu.info",
		UserExtensionSchemaURI:     "urn:ietf:params:scim:schemas:extension:gluu:2.0:User",
		LoggingLevel:               "INFO",
		LoggingLayout:              "text",
		MetricReporterInterval:     300,
		MetricReporterKeepDataDays: 15,
		MetricReporterEnabled:      true,
		BulkMaxOperations:          30,
		BulkMaxPayloadSize:         3072000,
	}

	if err := toSchemaResource(data, cfg); err != nil {
		t.Fatal(err)
	}

	newCfg := jans.ScimAppConfigurations{}

	if err := fromSchemaResource(data, &newCfg); err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(cfg, newCfg); diff != "" {
		t.Errorf("Got different configuration after mapping: %s", diff)
	}
}

func TestAccResourceScimAppConfiguration_basic(t *testing.T) {

	resource.Test(t, resource.TestCase{
		PreCheck:     func() { testAccPreCheck(t) },
		Providers:    testAccProviders,
		CheckDestroy: testAccReourceCheckScimAppConfigurationDestroy,
		Steps: []resource.TestStep{
			{
				Config:           testAccResourceScimAppConfigurationConfig_basic(),
				ResourceName:     "jans_scim_app_configuration.global",
				ImportState:      true,
				ImportStateId:    "jans_scim_app_configuration.jans_scim_app_configuration",
				ImportStateCheck: testAccResourceCheckScimAppConfigurationImport,
			},
		},
	})
}

func testAccResourceScimAppConfigurationConfig_basic() string {
	return `
resource "jans_scim_app_configuration" "global" {
}
`
}

func testAccResourceCheckScimAppConfigurationImport(states []*terraform.InstanceState) error {

	found := false
	for _, is := range states {

		if is.ID != "jans_scim_app_configuration" {
			continue
		}

		found = true

		if err := checkAttribute(is, "base_dn", "o=jans"); err != nil {
			return err
		}

		if err := checkAttribute(is, "person_custom_object_class", "jansCustomPerson"); err != nil {
			return err
		}

		if err := checkAttribute(is, "max_count", "200"); err != nil {
			return err
		}

		break
	}

	if !found {
		return errors.New("resource not found in states")
	}

	return nil
}

func testAccReourceCheckScimAppConfigurationDestroy(s *terraform.State) error {

	// since this is a global resource, delete should not have any effect

	c := testAccProvider.Meta().(*jans.Client)

	ctx := context.Background()

	for _, rs := range s.RootModule().Resources {
		if rs.Type != "jans_scim_app_configuration" {
			continue
		}
		_, err := c.GetScimAppConfiguration(ctx)
		if err != nil {
			return err
		}
	}

	return nil
}
