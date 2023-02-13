package provider

import (
	"context"
	"errors"
	"fmt"
	"testing"

	"github.com/google/go-cmp/cmp"
	"github.com/jans/terraform-provider-jans/jans"

	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/resource"
	"github.com/hashicorp/terraform-plugin-sdk/v2/terraform"
)

func TestResourceLDAPDatabaseConfiguration_Mapping(t *testing.T) {

	schema := resourceLDAPDatabaseConfiguration()

	data := schema.Data(nil)

	ldapConfig := jans.LDAPDBConfiguration{
		ConfigId:         "auth_ldap_server",
		BindDN:           "cn=directory manager",
		BindPassword:     "CLcHdW8FPW40PByaxmcXaQ==",
		Servers:          []string{"localhost:1636"},
		MaxConnections:   1000,
		UseSSL:           true,
		BaseDNs:          []string{"ou=people,o=jans"},
		PrimaryKey:       "uid",
		LocalPrimaryKey:  "uid",
		UseAnonymousBind: false,
		Enabled:          false,
		Version:          0,
		Level:            0,
	}

	if err := toSchemaResource(data, ldapConfig); err != nil {
		t.Fatal(err)
	}

	newConfig := jans.LDAPDBConfiguration{}

	if err := fromSchemaResource(data, &newConfig); err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(ldapConfig, newConfig); diff != "" {
		t.Errorf("Got different config after mapping: %s", diff)
	}
}

func TestAccResourceLDAPDatabaseConfiguration_basic(t *testing.T) {

	var cfg jans.LDAPDBConfiguration

	resource.Test(t, resource.TestCase{
		PreCheck:     func() { testAccPreCheck(t) },
		Providers:    testAccProviders,
		CheckDestroy: testAccResourceCheckLDAPDatabaseConfigurationDestroy,
		Steps: []resource.TestStep{
			{
				Config: testAccResourceLDAPDatabaseConfigurationConfig_basic(),
				Check: resource.ComposeTestCheckFunc(
					testAccResourceCheckLDAPDatabaseConfigurationExists("jans_ldap_database_configuration.test", &cfg),
				),
			},
		},
	})
}

func testAccResourceLDAPDatabaseConfigurationConfig_basic() string {
	return `
resource "jans_ldap_database_configuration" "test" {
  local_primary_key = "uid"
  config_id 				= "test"
  max_connections 	= 200
  servers 					= ["ldap.default:1636"]
  use_ssl 					= true
  bind_dn 					= "cn=directory manager"
  bind_password 		= "CLcHdW8FPW40PByaxmcXaQ=="
  primary_key 			= "uid"
  base_dns 					= ["ou=people,o=jans"]

	lifecycle {
		# ignore changes to passowrd, as it will be returned as a hash
		# from the API
    ignore_changes = [ bind_password ]
  }
} 
`
}

func testAccResourceCheckLDAPDatabaseConfigurationExists(name string, cfg *jans.LDAPDBConfiguration) resource.TestCheckFunc {
	return func(s *terraform.State) error {
		rs, ok := s.RootModule().Resources[name]
		if !ok {
			return fmt.Errorf("Not found: %s", name)
		}

		c := testAccProvider.Meta().(*jans.Client)

		name := rs.Primary.ID

		ctx := context.Background()

		out, err := c.GetLDAPDBConfiguration(ctx, name)
		if err != nil {
			return err
		}

		*cfg = *out
		return nil
	}
}

func testAccResourceCheckLDAPDatabaseConfigurationDestroy(s *terraform.State) error {

	c := testAccProvider.Meta().(*jans.Client)

	ctx := context.Background()

	for _, rs := range s.RootModule().Resources {
		if rs.Type != "jans_ldap_database_configuration" {
			continue
		}

		configID := rs.Primary.ID

		_, err := c.GetLDAPDBConfiguration(ctx, configID)
		if !errors.Is(err, jans.ErrorNotFound) {
			return err
		}
	}

	return nil
}
