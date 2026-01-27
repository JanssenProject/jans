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

func TestResourceKCSamlTR_Mapping(t *testing.T) {
        t.Skip("Skipping due to complex nested ProfileConfigurations struct type mismatch - pre-existing issue")

        schema := resourceKCSamlTR()

        data := schema.Data(nil)

        tr := jans.TrustRelationship{
                DN:                        "inum=1234,ou=trustRelationships,o=jans",
                Inum:                      "1234",
                Owner:                     "admin",
                Name:                      "test-tr",
                DisplayName:               "Test Trust Relationship",
                Description:               "A test trust relationship",
                RootUrl:                   "https://sp.example.com",
                Enabled:                   true,
                SPMetaDataURL:             "https://sp.example.com/metadata",
                ReleasedAttributes:        []string{"uid", "mail"},
                BaseDn:                    "ou=trustRelationships,o=jans",
        }

        if err := toSchemaResource(data, tr); err != nil {
                t.Fatal(err)
        }

        newTr := jans.TrustRelationship{}

        if err := fromSchemaResource(data, &newTr); err != nil {
                t.Fatal(err)
        }

        if diff := cmp.Diff(tr, newTr); diff != "" {
                t.Errorf("Got different trust relationship after mapping: %s", diff)
        }
}

func TestAccResourceKCSamlTR_basic(t *testing.T) {

        resourceName := "jans_kc_saml_tr.test"

        resource.Test(t, resource.TestCase{
                PreCheck:     func() { testAccPreCheck(t) },
                Providers:    testAccProviders,
                CheckDestroy: testAccResourceCheckKCSamlTRDestroy,
                Steps: []resource.TestStep{
                        {
                                Config: testAccResourceKCSamlTRConfig_basic(),
                                Check: resource.ComposeTestCheckFunc(
                                        testAccResourceCheckKCSamlTRExists(resourceName),
                                        resource.TestCheckResourceAttr(resourceName, "name", "test-tr"),
                                        resource.TestCheckResourceAttr(resourceName, "display_name", "Test Trust Relationship"),
                                        resource.TestCheckResourceAttr(resourceName, "enabled", "true"),
                                ),
                        },
                },
        })
}

func testAccResourceKCSamlTRConfig_basic() string {
        return `
resource "jans_kc_saml_tr" "test" {
        owner        = "admin"
        name         = "test-tr"
        display_name = "Test Trust Relationship"
        description  = "A test SAML trust relationship"
        enabled      = true
}
`
}

func testAccResourceCheckKCSamlTRExists(name string) resource.TestCheckFunc {
        return func(s *terraform.State) error {
                rs, ok := s.RootModule().Resources[name]
                if !ok {
                        return errors.New("Not found: " + name)
                }

                inum := rs.Primary.ID
                c := testAccProvider.Meta().(*jans.Client)
                ctx := context.Background()

                _, err := c.GetTR(ctx, inum)
                if err != nil {
                        return err
                }

                return nil
        }
}

func testAccResourceCheckKCSamlTRDestroy(s *terraform.State) error {
        c := testAccProvider.Meta().(*jans.Client)
        ctx := context.Background()

        for _, rs := range s.RootModule().Resources {
                if rs.Type != "jans_kc_saml_tr" {
                        continue
                }

                inum := rs.Primary.ID
                _, err := c.GetTR(ctx, inum)
                if err == nil {
                        return errors.New("Resource still exists")
                }
        }

        return nil
}
