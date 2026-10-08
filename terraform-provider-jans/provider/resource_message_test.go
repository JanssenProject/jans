package provider

import (
	"errors"
	"testing"

	"github.com/google/go-cmp/cmp"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/resource"
	"github.com/hashicorp/terraform-plugin-sdk/v2/terraform"
	"github.com/jans/terraform-provider-jans/jans"
)

func TestResourceMessageConfiguration_Mapping(t *testing.T) {
	schema := resourceMessageConfiguration()
	data := schema.Data(nil)

	cfg := jans.MessageConfiguration{
		MessageProviderType: "POSTGRES",
		RedisConfiguration: &jans.RedisMessageConfiguration{
			RedisProviderType: "STANDALONE",
			Servers:           "localhost:6379",
		},
		PostgresConfiguration: &jans.PostgresMessageConfiguration{
			DriverClassName: "org.postgresql.Driver",
			DbSchemaName:    "public",
			ConnectionUri:   "jdbc:postgresql://localhost:5432/jans",
		},
	}

	if err := toSchemaResource(data, cfg); err != nil {
		t.Fatal(err)
	}

	newCfg := jans.MessageConfiguration{}
	if err := fromSchemaResource(data, &newCfg); err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(cfg, newCfg); diff != "" {
		t.Errorf("Got different configuration after mapping: %s", diff)
	}
}

// TestAccResourceMessage_basic imports the singleton message configuration and
// verifies it exposes a provider type. It uses import (not apply) so the
// server's messaging configuration is never mutated.
func TestAccResourceMessage_basic(t *testing.T) {
	resourceName := "jans_message.global"

	resource.Test(t, resource.TestCase{
		PreCheck:  func() { testAccPreCheck(t) },
		Providers: testAccProviders,
		Steps: []resource.TestStep{
			{
				Config:           testAccResourceMessageConfig_basic(),
				ResourceName:     resourceName,
				ImportState:      true,
				ImportStateId:    "jans_message",
				ImportStateCheck: testAccResourceCheckMessageImport,
			},
		},
	})
}

func testAccResourceMessageConfig_basic() string {
	return `
resource "jans_message" "global" {
	message_provider_type = "DISABLED"
	postgres_configuration {}
	redis_configuration {}
}
`
}

func testAccResourceCheckMessageImport(states []*terraform.InstanceState) error {
	for _, is := range states {
		if is.ID != "jans_message" {
			continue
		}

		if is.Attributes["message_provider_type"] == "" {
			return errors.New("message_provider_type is not set")
		}

		return nil
	}

	return errors.New("resource not found in states")
}
