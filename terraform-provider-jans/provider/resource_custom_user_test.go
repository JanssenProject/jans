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

func TestResourceCustomUser_Mapping(t *testing.T) {

	schema := resourceCustomUser()

	data := schema.Data(nil)

	user := jans.CustomUser{
		UserID: "exampleUsr1",
		CustomAttributes: []jans.CustomAttribute{
			{
				Name:         "birthdate",
				MultiValued:  false,
				Values:       []string{`"2000-12-31T00:00:00"`},
				Value:        `"2000-12-31T00:00:00"`,
				DisplayValue: "Sun Dec 31 00:00:00 UTC 2000",
			},
			{
				Name:         "emailVerified",
				MultiValued:  false,
				Values:       []string{"true"},
				Value:        "true",
				DisplayValue: "true",
			},
			{
				Name:         "jansAdminUIRole",
				MultiValued:  true,
				Values:       []string{`"api-admin"`},
				Value:        `"api-admin"`,
				DisplayValue: "api-admin",
			},
			{
				Name:         "memberOf",
				MultiValued:  true,
				Values:       []string{`"inum=60B7,ou=groups,o=jans"`},
				Value:        `"inum=60B7,ou=groups,o=jans"`,
				DisplayValue: "inum=60B7,ou=groups,o=jans",
			},
			{
				Name:         "middleName",
				MultiValued:  false,
				Values:       []string{`"Test USer 1"`},
				Value:        `"Test USer 1"`,
				DisplayValue: "Test USer 1",
			},
			{
				Name:         "nickname",
				MultiValued:  false,
				Values:       []string{`"Test USer 1"`},
				Value:        `"Test USer 1"`,
				DisplayValue: "Test USer 1",
			},
			{
				Name:         "sn",
				MultiValued:  false,
				Values:       []string{`"exampleUsr1"`},
				Value:        `"exampleUsr1"`,
				DisplayValue: "exampleUsr1",
			},
		},
		CustomObjectClasses: []string{
			"top",
			"jansCustomPerson",
		},
		Mail:         "exampleUsr1@jans.io",
		DisplayName:  "Default Test User",
		JansStatus:   "active",
		UserPassword: "pwd123",
		GivenName:    "exampleUsr1",
	}

	if err := toSchemaResource(data, user); err != nil {
		t.Fatal(err)
	}

	newUser := jans.CustomUser{}

	if err := fromSchemaResource(data, &newUser); err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(user, newUser); diff != "" {
		t.Errorf("Got different entity after mapping: %s", diff)
	}
}

func TestAccResourceCustomUser_basic(t *testing.T) {

	resourceName := "jans_custom_user.test"

	resource.Test(t, resource.TestCase{
		PreCheck:     func() { testAccPreCheck(t) },
		Providers:    testAccProviders,
		CheckDestroy: testAccResourceCheckCustomUserDestroy,
		Steps: []resource.TestStep{
			{
				Config: testAccResourceCustomUserConfig_basic(),
				Check: resource.ComposeTestCheckFunc(
					testAccResourceCheckCustomUserExists(resourceName),
					resource.TestCheckResourceAttr(resourceName, "jans_status", "active"),
					resource.TestCheckResourceAttr(resourceName, "user_id", "test"),
					resource.TestCheckResourceAttr(resourceName, "ox_auth_persistent_jwt.0", "jwt1"),
					resource.TestCheckResourceAttr(resourceName, "mail", "test@jans.io"),
					resource.TestCheckResourceAttr(resourceName, "given_name", "given-name-test"),
				),
			},
		},
	})
}

func testAccResourceCustomUserConfig_basic() string {
	return `
resource "jans_custom_user" "test" {

	jans_status 						= "active"
	user_id 								= "test"
	ox_auth_persistent_jwt 	= ["jwt1", "jwt2"]
	mail 										= "test@jans.io"
	display_name 						= "display-test"
	given_name 							= "given-name-test"
	user_password 					= "password"

	custom_attributes {
		name 					= "nickname"
		multi_valued 	= false
		value 				= "\"the-tester\""
		values 				= [ "\"the-tester\"" ]
		display_value = "the-tester"
	}

	lifecycle {
		# ignore changes to password, as it will not be 
		# returned from the API
    ignore_changes = [ user_password ]
  }
}
`
}

func testAccResourceCheckCustomUserExists(name string) resource.TestCheckFunc {
	return func(s *terraform.State) error {
		rs, ok := s.RootModule().Resources[name]
		if !ok {
			return fmt.Errorf("Not found: %s", name)
		}

		c := testAccProvider.Meta().(*jans.Client)

		inum := rs.Primary.ID

		ctx := context.Background()

		_, err := c.GetCustomUser(ctx, inum)
		if err != nil {
			return err
		}

		return nil
	}
}

func testAccResourceCheckCustomUserDestroy(s *terraform.State) error {

	c := testAccProvider.Meta().(*jans.Client)

	ctx := context.Background()

	for _, rs := range s.RootModule().Resources {
		if rs.Type != "jans_custom_user" {
			continue
		}

		inum := rs.Primary.ID

		_, err := c.GetCustomUser(ctx, inum)
		if !errors.Is(err, jans.ErrorNotFound) {
			return err
		}
	}

	return nil
}
