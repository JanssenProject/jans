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

func TestAgamaFlow_Mapping(t *testing.T) {

	schema := resourceAgamaFlow()

	data := schema.Data(nil)

	flow := jans.AgamaFlow{
		Dn:       "dn",
		Qname:    "qname",
		Revision: 1,
		Enabled:  true,
		Metadata: jans.FlowMetadata{
			FuncName:    "func_name",
			Inputs:      []string{"inputs"},
			Timeout:     60,
			DisplayName: "display_name",
			Author:      "author",
			Timestamp:   60,
			Description: "description",
			Properties:  map[string]string{"key": "value"},
		},
		Source:    "source",
		CodeError: "code_error",
	}

	if err := toSchemaResource(data, flow); err != nil {
		t.Fatal(err)
	}

	newFlow := jans.AgamaFlow{}

	if err := fromSchemaResource(data, &newFlow); err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(flow, newFlow); diff != "" {
		t.Errorf("Got different entity after mapping: %s", diff)
	}
}

func TestAccResourceAgamaFlow_basic(t *testing.T) {

	resourceName := "jans_agama_flow.test"

	resource.Test(t, resource.TestCase{
		PreCheck:     func() { testAccPreCheck(t) },
		Providers:    testAccProviders,
		CheckDestroy: testAccResourceCheckAgamaFlowDestroy,
		Steps: []resource.TestStep{
			{
				Config: testAccResourceAgamaFlowConfig_basic(),
				Check: resource.ComposeTestCheckFunc(
					testAccResourceCheckAgamaFlowExists(resourceName),
					resource.TestCheckResourceAttr(resourceName, "qname", "test"),
					resource.TestCheckResourceAttr(resourceName, "enabled", "true"),
				),
			},
		},
	})
}

func testAccResourceAgamaFlowConfig_basic() string {
	return `
resource "jans_agama_flow" "test" {
	qname 		 = "test"
	enabled 	 = true
	source 		 = <<EOF
Flow test
    Basepath "hello"

in = { name: "John" }
RRF "index.ftlh" in

Log "Done!"
Finish "john_doe"
EOF

	metadata {
		inputs     = []
		properties = {}
		timeout    = 0
	}

	lifecycle {
		ignore_changes = [source, revision]
	}
}
`
}

func testAccResourceCheckAgamaFlowExists(name string) resource.TestCheckFunc {
	return func(s *terraform.State) error {
		rs, ok := s.RootModule().Resources[name]
		if !ok {
			return fmt.Errorf("Not found: %s", name)
		}

		c := testAccProvider.Meta().(*jans.Client)

		qname := rs.Primary.ID

		ctx := context.Background()

		_, err := c.GetAgamaFlow(ctx, qname)
		if err != nil {
			return err
		}

		return nil
	}
}

func testAccResourceCheckAgamaFlowDestroy(s *terraform.State) error {

	c := testAccProvider.Meta().(*jans.Client)

	ctx := context.Background()

	for _, rs := range s.RootModule().Resources {
		if rs.Type != "jans_agama_flow" {
			continue
		}

		qname := rs.Primary.ID

		_, err := c.GetAgamaFlow(ctx, qname)
		if !errors.Is(err, jans.ErrorNotFound) {
			return err
		}

	}

	return nil
}
