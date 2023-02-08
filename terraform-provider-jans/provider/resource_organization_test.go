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

func TestResourceOrganization_Mapping(t *testing.T) {

	schema := resourceOrganization()

	data := schema.Data(nil)

	org := jans.Organization{
		Dn:           "o=janssen",
		DisplayName:  "TestDisplayName",
		Description:  "TestDescription",
		Member:       "yes",
		Organization: "TestOrga",
		ManagerGroup: "TestManagerGroup",
		ShortName:    "TestShortName",
		BaseDn:       "o=janssen",
	}

	if err := toSchemaResource(data, org); err != nil {
		t.Fatal(err)
	}

	newOrg := jans.Organization{}

	if err := fromSchemaResource(data, &newOrg); err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(org, newOrg); diff != "" {
		t.Errorf("Got different entity after mapping: %s", diff)
	}
}

func TestAccResourceOrganization_basic(t *testing.T) {

	resource.Test(t, resource.TestCase{
		PreCheck:     func() { testAccPreCheck(t) },
		Providers:    testAccProviders,
		CheckDestroy: testAccReourceCheckOrganizationDestroy,
		Steps: []resource.TestStep{
			{
				Config:           testAccResourceOrganizationConfig_basic(),
				ResourceName:     "jans_organization.global",
				ImportState:      true,
				ImportStateId:    "jans_organization.jans_organization",
				ImportStateCheck: testAccResourceCheckOrganizationImport,
			},
		},
	})
}

func testAccResourceOrganizationConfig_basic() string {
	return `
resource "jans_organization" "global" {
}
`
}

func testAccResourceCheckOrganizationImport(states []*terraform.InstanceState) error {

	found := false
	for _, is := range states {

		if is.ID != "jans_organization" {
			continue
		}

		found = true

		if err := checkAttribute(is, "display_name", "Gluu"); err != nil {
			return err
		}

		if err := checkAttribute(is, "manager_group", "inum=60B7,ou=groups,o=jans"); err != nil {
			return err
		}

		if err := checkAttribute(is, "theme_color", "166309"); err != nil {
			return err
		}

		break
	}

	if !found {
		return errors.New("resource not found in states")
	}

	return nil
}

func testAccReourceCheckOrganizationDestroy(s *terraform.State) error {

	// since this is a global resource, delete should not have any effect

	c := testAccProvider.Meta().(*jans.Client)

	ctx := context.Background()

	for _, rs := range s.RootModule().Resources {
		if rs.Type != "jans_organization" {
			continue
		}
		_, err := c.GetOrganization(ctx)
		if err != nil {
			return err
		}
	}

	return nil
}
