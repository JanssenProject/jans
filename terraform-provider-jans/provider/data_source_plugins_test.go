package provider

import (
	"testing"

	"github.com/google/go-cmp/cmp"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/resource"
	"github.com/jans/terraform-provider-jans/jans"
)

func TestDatasourcePlugins_Mapping(t *testing.T) {

	schema := dataSourcePlugins()

	data := schema.Data(nil)

	plugins := jans.Plugins{
		Enabled: []jans.PluginConf{
			{
				Name: "fidoe2",
			},
		},
	}

	if err := toSchemaResource(data, &plugins); err != nil {
		t.Fatal(err)
	}

	newPlugins := jans.Plugins{}

	if err := fromSchemaResource(data, &newPlugins); err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(plugins, newPlugins); diff != "" {
		t.Errorf("Got different plugins after mapping: %s", diff)
	}

}

func TestAccDataSourcePlugins_basic(t *testing.T) {

	resource.Test(t, resource.TestCase{
		PreCheck:  func() { testAccPreCheck(t) },
		Providers: testAccProviders,
		Steps: []resource.TestStep{
			{
				Config: testAccDataSourcePlugins_basic(),
				Check: resource.ComposeTestCheckFunc(
					resource.TestCheckResourceAttr("data.jans_plugins.all", "enabled.0.name", "fido2"),
				),
			},
		},
	})
}

func testAccDataSourcePlugins_basic() string {
	return `
data "jans_plugins" "all" {
}
`
}
