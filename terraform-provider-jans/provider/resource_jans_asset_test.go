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

func TestResourceJansAsset_Mapping(t *testing.T) {

        schema := resourceAsset()

        data := schema.Data(nil)

        doc := jans.Document{
                Dn:           "inum=1234,ou=assets,o=jans",
                Inum:         "1234",
                FileName:     "test.txt",
                FilePath:     "/opt/jans/assets",
                Description:  "A test document",
                Document:     "Test content",
                CreationDate: "2024-01-15T10:30:00",
                Service:      "jans-auth",
                Level:        "1",
                Revision:     "001",
                Enabled:      true,
                Alias:        "test-alias",
                BaseDn:       "ou=assets,o=jans",
        }

        if err := toSchemaResource(data, doc); err != nil {
                t.Fatal(err)
        }

        newDoc := jans.Document{}

        if err := fromSchemaResource(data, &newDoc); err != nil {
                t.Fatal(err)
        }

        if diff := cmp.Diff(doc, newDoc); diff != "" {
                t.Errorf("Got different document after mapping: %s", diff)
        }
}

func TestAccResourceJansAsset_basic(t *testing.T) {

        resourceName := "jans_asset.test"

        resource.Test(t, resource.TestCase{
                PreCheck:     func() { testAccPreCheck(t) },
                Providers:    testAccProviders,
                CheckDestroy: testAccResourceCheckJansAssetDestroy,
                Steps: []resource.TestStep{
                        {
                                Config: testAccResourceJansAssetConfig_basic(),
                                Check: resource.ComposeTestCheckFunc(
                                        testAccResourceCheckJansAssetExists(resourceName),
                                        resource.TestCheckResourceAttr(resourceName, "description", "Test asset"),
                                        resource.TestCheckResourceAttr(resourceName, "enabled", "true"),
                                ),
                        },
                },
        })
}

func testAccResourceJansAssetConfig_basic() string {
        return `
resource "jans_asset" "test" {
        file_name   = "test_asset.txt"
        description = "Test asset"
        enabled     = true
        service     = "jans-auth"
        level       = "1"
        asset       = "${path.module}/testdata/test_asset.txt"
}
`
}

func testAccResourceCheckJansAssetExists(name string) resource.TestCheckFunc {
        return func(s *terraform.State) error {
                rs, ok := s.RootModule().Resources[name]
                if !ok {
                        return errors.New("Not found: " + name)
                }

                inum := rs.Primary.ID
                c := testAccProvider.Meta().(*jans.Client)
                ctx := context.Background()

                _, err := c.GetJansAsset(ctx, inum)
                if err != nil {
                        return err
                }

                return nil
        }
}

func testAccResourceCheckJansAssetDestroy(s *terraform.State) error {
        c := testAccProvider.Meta().(*jans.Client)
        ctx := context.Background()

        for _, rs := range s.RootModule().Resources {
                if rs.Type != "jans_asset" {
                        continue
                }

                inum := rs.Primary.ID
                _, err := c.GetJansAsset(ctx, inum)
                if err == nil {
                        return errors.New("Resource still exists")
                }
                if !errors.Is(err, jans.ErrorNotFound) {
                        return fmt.Errorf("unexpected error checking asset %s: %w", inum, err)
                }
        }

        return nil
}
