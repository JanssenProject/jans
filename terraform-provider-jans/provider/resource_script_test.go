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

func TestResourceScripts_Mapping(t *testing.T) {

	schema := resourceScript()

	data := schema.Data(nil)

	script := jans.Script{
		Dn:                  "inum=4A4E-4F3D,ou=scripts,o=jans",
		Inum:                "4A4E-4F3D",
		Name:                "test_script",
		Aliases:             []string{"test_alias"},
		Description:         "test_description",
		Script:              "// source code",
		ScriptType:          "introspection",
		ProgrammingLanguage: "python",
		ModuleProperties: []jans.SimpleCustomProperty{
			{
				Value1: "location_type",
				Value2: "ldap",
			},
		},
		ConfigurationProperties: []jans.SimpleExtendedCustomProperty{
			{
				Value1: "location_type",
				Value2: "ldap",
			},
		},
		Level:        1,
		Revision:     1,
		Enabled:      true,
		ScriptError:  jans.ScriptError{},
		Modified:     true,
		Internal:     true,
		LocationType: "ldap",
		BaseDN:       "inum=A44E-4F3D,ou=scripts,o=jans",
	}

	if err := toSchemaResource(data, script); err != nil {
		t.Fatal(err)
	}

	newScript := jans.Script{}

	if err := fromSchemaResource(data, &newScript); err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(script, newScript); diff != "" {
		t.Errorf("Got different script after mapping: %s", diff)
	}
}

func TestAccResourceScript_basic(t *testing.T) {

	resourceName := "jans_script.test"

	resource.Test(t, resource.TestCase{
		PreCheck:     func() { testAccPreCheck(t) },
		Providers:    testAccProviders,
		CheckDestroy: testAccResourceCheckScriptDestroy,
		Steps: []resource.TestStep{
			{
				Config: testAccResourceScriptConfig_basic(),
				Check: resource.ComposeTestCheckFunc(
					testAccResourceCheckScriptExists(resourceName),
					resource.TestCheckResourceAttr(resourceName, "script_type", "introspection"),
					resource.TestCheckResourceAttr(resourceName, "revision", "1"),
					resource.TestCheckResourceAttr(resourceName, "location_type", "ldap"),
					resource.TestCheckResourceAttr(resourceName, "module_properties.0.value1", "location_type"),
				),
			},
		},
	})
}

func testAccResourceScriptConfig_basic() string {
	return `
resource "jans_script" "test" {
	dn 												= "inum=4A4E-4F3D,ou=scripts,o=jans"
	inum 											= "4A4E-4F3D"
	name 											= "test_script"
	// aliases 									= 
	description 							= "Test description"
	script 										= ""
	script_type 							= "introspection"
	programming_language 			= "python"
	level 										= 1
	revision 									= 1
	enabled 									= true
	modified 									= false
	internal 									= false
	location_type 						= "ldap"
	base_dn 									= "inum=4A4E-4F3D,ou=scripts,o=jans"

	module_properties {
			value1 = "location_type"
			value2 = "ldap"
	}

	module_properties {
			value1 = "location_option"
			value2 = "foo"
	}
	
	// configuration_properties 	= 
}
`
}

func testAccResourceCheckScriptExists(name string) resource.TestCheckFunc {
	return func(s *terraform.State) error {
		rs, ok := s.RootModule().Resources[name]
		if !ok {
			return fmt.Errorf("Not found: %s", name)
		}

		c := testAccProvider.Meta().(*jans.Client)

		inum := rs.Primary.ID

		ctx := context.Background()

		_, err := c.GetScript(ctx, inum)
		if err != nil {
			return err
		}

		return nil
	}
}

func testAccResourceCheckScriptDestroy(s *terraform.State) error {

	c := testAccProvider.Meta().(*jans.Client)

	ctx := context.Background()

	for _, rs := range s.RootModule().Resources {
		if rs.Type != "jans_script" {
			continue
		}

		inum := rs.Primary.ID

		_, err := c.GetScript(ctx, inum)
		if !errors.Is(err, jans.ErrorNotFound) {
			return err
		}

	}

	return nil
}
