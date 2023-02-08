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

func TestResourceFido2Config_Mapping(t *testing.T) {

	schema := resourceFido2Configuration()

	data := schema.Data(nil)

	cfg := jans.JansFido2DynConfiguration{
		Issuer:                      "https://moabu-21f13b7c-9069-ad58-5685-852e6d236020.gluu.info",
		BaseEndpoint:                "https://moabu-21f13b7c-9069-ad58-5685-852e6d236020.gluu.info/jans-fido2/restv1",
		CleanServiceInterval:        60,
		CleanServiceBatchChunkSize:  10000,
		UseLocalCache:               true,
		DisableJdkLogger:            true,
		LoggingLevel:                "INFO",
		LoggingLayout:               "text",
		ExternalLoggerConfiguration: "",
		MetricReporterEnabled:       true,
		MetricReporterInterval:      300,
		MetricReporterKeepDataDays:  15,
		PersonCustomObjectClassList: []string{"jansCustomPerson", "jansPerson"},
		Fido2Configuration: jans.Fido2Configuration{
			AuthenticatorCertsFolder: "/etc/jans/conf/fido2/authenticator_cert",
			MdsCertsFolder:           "/etc/jans/conf/fido2/mds/cert",
			MdsTocsFolder:            "/etc/jans/conf/fido2/mds/toc",
			ServerMetadataFolder:     "/etc/jans/conf/fido2/server_metadata",
			RequestedParties: []jans.RequestedParties{
				{
					Name:    "https://moabu-21f13b7c-9069-ad58-5685-852e6d236020.gluu.info",
					Domains: []string{"moabu-21f13b7c-9069-ad58-5685-852e6d236020.gluu.info"},
				},
			},
			UserAutoEnrollment:              false,
			UnfinishedRequestExpiration:     180,
			AuthenticationHistoryExpiration: 1296000,
			RequestedCredentialTypes:        []string{"RS256", "ES256"},
		},
	}

	if err := toSchemaResource(data, cfg); err != nil {
		t.Fatal(err)
	}

	newCfg := jans.JansFido2DynConfiguration{}

	if err := fromSchemaResource(data, &newCfg); err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(cfg, newCfg); diff != "" {
		t.Errorf("Got different configuration after mapping: %s", diff)
	}

}

func TestAccResourceFido2Configuration_basic(t *testing.T) {

	resource.Test(t, resource.TestCase{
		PreCheck:     func() { testAccPreCheck(t) },
		Providers:    testAccProviders,
		CheckDestroy: testAccResourceCheckFido2ConfigurationDestroy,
		Steps: []resource.TestStep{
			{
				Config:           testAccResourceFido2ConfigurationConfig_basic(),
				ResourceName:     "jans_fido2_configuration.global",
				ImportState:      true,
				ImportStateId:    "jans_fido2_configuration.jans_fido2_configuration",
				ImportStateCheck: testAccResourceCheckFido2ConfigurationImport,
			},
		},
	})
}

func testAccResourceFido2ConfigurationConfig_basic() string {
	return `
resource "jans_fido2_configuration" "global" {
}
`
}

func testAccResourceCheckFido2ConfigurationImport(states []*terraform.InstanceState) error {

	found := false
	for _, is := range states {

		if is.ID != "jans_fido2_configuration" {
			continue
		}

		found = true

		if err := checkAttribute(is, "clean_service_batch_chunk_size", "10000"); err != nil {
			return err
		}

		if err := checkAttribute(is, "logging_level", "INFO"); err != nil {
			return err
		}

		break
	}

	if !found {
		return errors.New("resource not found in states")
	}

	return nil
}

func testAccResourceCheckFido2ConfigurationDestroy(s *terraform.State) error {

	// since this is a global resource, delete should not have any effect

	c := testAccProvider.Meta().(*jans.Client)

	ctx := context.Background()

	for _, rs := range s.RootModule().Resources {
		if rs.Type != "jans_fido2_configuration" {
			continue
		}

		_, err := c.GetFido2Configuration(ctx)
		if err != nil {
			return err
		}
	}

	return nil
}
