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

func TestResourceKCSamlConfiguration_Mapping(t *testing.T) {

        schema := resourceKCSamlConfiguration()

        data := schema.Data(nil)

        config := jans.KCSAMLConfiguration{
                ApplicationName:           "jans-saml",
                SamlTrustRelationshipDn:   "ou=trustRelationships,o=jans",
                TrustIdpDn:                "ou=trusted-idp,o=jans",
                Enabled:                   true,
                ServerUrl:                 "https://example.com",
                Realm:                     "master",
                ClientId:                  "test-client",
                ClientSecret:              "test-secret",
                GrantType:                 "client_credentials",
                Scope:                     "openid",
                Username:                  "admin",
                Password:                  "secret",
                SpMetadataUrl:             "https://example.com/sp/metadata",
                TokenUrl:                  "https://example.com/token",
                IdpUrl:                    "https://example.com/idp",
                IdpMetadataImportUrl:      "https://example.com/idp/metadata/import",
                IdpRootDir:                "/opt/jans/idp",
                IdpMetadataTempDir:        "/tmp/idp-metadata",
                IdpMetadataFile:           "idp-metadata.xml",
                IdpMetadataMandatoryAttributes: []string{"urn:oid:0.9.2342.19200300.100.1.1"},
                IgnoreValidation:          false,
        }

        if err := toSchemaResource(data, config); err != nil {
                t.Fatal(err)
        }

        newConfig := jans.KCSAMLConfiguration{}

        if err := fromSchemaResource(data, &newConfig); err != nil {
                t.Fatal(err)
        }

        if diff := cmp.Diff(config, newConfig); diff != "" {
                t.Errorf("Got different config after mapping: %s", diff)
        }
}

func TestAccResourceKCSamlConfiguration_basic(t *testing.T) {

        resource.Test(t, resource.TestCase{
                PreCheck:     func() { testAccPreCheck(t) },
                Providers:    testAccProviders,
                CheckDestroy: testAccResourceCheckKCSamlConfigurationDestroy,
                Steps: []resource.TestStep{
                        {
                                Config:           testAccResourceKCSamlConfigurationConfig_basic(),
                                ResourceName:     "jans_kc_saml_config.test",
                                ImportState:      true,
                                ImportStateId:    "jans_kc_saml_config",
                                ImportStateCheck: testAccResourceCheckKCSamlConfigurationImport,
                        },
                },
        })
}

func testAccResourceKCSamlConfigurationConfig_basic() string {
        return `
resource "jans_kc_saml_config" "test" {
        application_name = "jans-saml"
        enabled          = true
        realm            = "master"
}
`
}

func testAccResourceCheckKCSamlConfigurationImport(states []*terraform.InstanceState) error {
        found := false
        for _, state := range states {
                if state.Attributes["application_name"] != "" {
                        found = true
                        break
                }
        }

        if !found {
                return errors.New("KCSamlConfiguration resource not found in import state")
        }

        return nil
}

func testAccResourceCheckKCSamlConfigurationDestroy(s *terraform.State) error {
        c := testAccProvider.Meta().(*jans.Client)
        ctx := context.Background()

        for _, rs := range s.RootModule().Resources {
                if rs.Type != "jans_kc_saml_config" {
                        continue
                }

                _, err := c.GetKCSAMLConfiguration(ctx)
                if err == nil {
                        return fmt.Errorf("KCSAML configuration still exists")
                }
                if !errors.Is(err, jans.ErrorNotFound) {
                        return fmt.Errorf("unexpected error checking KCSAML configuration: %w", err)
                }
        }

        return nil
}
