package provider

import (
	"testing"

	"github.com/google/go-cmp/cmp"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/resource"
	"github.com/moabu/terraform-provider-jans/jans"
)

func TestDatasourcePersistenceConfiguration_Mapping(t *testing.T) {

	schema := dataSourcePersistenceConfiguration()

	data := schema.Data(nil)

	cfg := jans.PersistenceConfiguration{
		PersistenceType: "ldap",
	}

	if err := toSchemaResource(data, &cfg); err != nil {
		t.Fatal(err)
	}

	newCfg := jans.PersistenceConfiguration{}

	if err := fromSchemaResource(data, &newCfg); err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(cfg, newCfg); diff != "" {
		t.Errorf("Got different configuration after mapping: %s", diff)
	}

}

func TestAccDataSourcePersistenceConfiguration_basic(t *testing.T) {

	resource.Test(t, resource.TestCase{
		PreCheck:  func() { testAccPreCheck(t) },
		Providers: testAccProviders,
		Steps: []resource.TestStep{
			{
				Config: testAccDataSourcePersistenceConfigurationConfig_basic(),
				Check: resource.ComposeTestCheckFunc(
					resource.TestCheckResourceAttr("data.jans_persistence_config.pc", "persistence_type", "sql"),
				),
			},
		},
	})
}

func testAccDataSourcePersistenceConfigurationConfig_basic() string {
	return `
data "jans_persistence_config" "pc" {
}
`
}
