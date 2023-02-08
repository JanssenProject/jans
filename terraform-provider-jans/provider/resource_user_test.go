package provider

import (
	"context"
	"errors"
	"fmt"
	"testing"

	"github.com/google/go-cmp/cmp"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/resource"
	"github.com/hashicorp/terraform-plugin-sdk/v2/terraform"
	"github.com/moabu/terraform-provider-jans/jans"
)

func TestResourceUser_Mapping(t *testing.T) {

	schema := resourceUser()

	data := schema.Data(nil)

	user := jans.User{
		ID:          "1234",
		DisplayName: "test-user",
		Schemas:     []string{"urn:ietf:params:scim:schemas:core:2.0:User"},
		Meta: jans.Meta{
			Location:     "https://localhost:9443/scim/v2/Users/1234",
			ResourceType: "User",
			Created:      "2021-03-01T00:00:00.000Z",
			LastModified: "2021-03-01T00:00:00.000Z",
		},
		ExternalId:        "ext1234",
		UserName:          "test-user",
		NickName:          "test-user",
		ProfileUrl:        "https://localhost:9443/scim/v2/Users/1234",
		Title:             "Mr",
		UserType:          "Employee",
		PreferredLanguage: "en",
		Locale:            "en_US",
		Timezone:          "UTC",
		Active:            true,
		Password:          "password",
		Name: jans.Name{
			FamilyName:      "Doe",
			GivenName:       "John",
			MiddleName:      "M",
			HonorificPrefix: "Dr",
			HonorificSuffix: "Jr",
		},
		Emails: []jans.Email{
			{
				Value:   "john.doe@jans.io",
				Display: "Work",
				Type:    "work",
				Primary: true,
			},
		},
		PhoneNumbers: []jans.PhoneNumber{
			{
				Value:   "1234567890",
				Display: "Mobile",
				Type:    "work",
				Primary: true,
			},
		},
		Ims: []jans.InstantMessagingAddress{
			{
				Value:   "@john.doe",
				Display: "Messenger",
				Type:    "Messenger",
				Primary: true,
			},
		},
		Photos: []jans.Photo{
			{
				Value:   "https://localhost:9443/scim/v2/Users/1234/photo",
				Display: "Photo",
				Type:    "photo",
				Primary: true,
			},
		},
		Addresses: []jans.Address{
			{
				Formatted:     "123 Main St",
				StreetAddress: "123 Main St",
				Locality:      "New York",
				Region:        "NY",
				PostalCode:    "12345",
				Country:       "US",
				Type:          "work",
				Primary:       true,
			},
		},
		Groups: []jans.GroupReference{
			{
				Value:   "1234",
				Display: "test-group",
				Type:    "Group",
				Ref:     "https://localhost:9443/scim/v2/Groups/1234",
			},
		},
		Entitlements: []jans.Entitlement{
			{
				Value:   "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:entitlement",
				Display: "Entitlement",
				Type:    "entitlement",
				Primary: true,
			},
		},
		Roles: []jans.Role{
			{
				Value:   "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:role",
				Display: "Role",
				Type:    "role",
				Primary: true,
			},
		},
		X509Certificates: []jans.X509Certificate{
			{
				Value:   "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:x509Certificates",
				Display: "X509Certificates",
				Type:    "PEM",
				Primary: true,
			},
		},
	}

	if err := toSchemaResource(data, user); err != nil {
		t.Fatal(err)
	}

	newUser := jans.User{}

	if err := fromSchemaResource(data, &newUser); err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(user, newUser); diff != "" {
		t.Errorf("Got different user after mapping: %s", diff)
	}
}

func TestAccResourceUser_basic(t *testing.T) {

	resourceName := "jans_user.user"

	resource.Test(t, resource.TestCase{
		PreCheck:     func() { testAccPreCheck(t) },
		Providers:    testAccProviders,
		CheckDestroy: testAccResourceCheckUserDestroy,
		Steps: []resource.TestStep{
			{
				Config: testAccResourceUserConfig_basic(),
				Check: resource.ComposeTestCheckFunc(
					testAccResourceCheckUserExists(resourceName),
					resource.TestCheckResourceAttr(resourceName, "display_name", "test-user"),
				),
			},
		},
	})
}

func testAccResourceUserConfig_basic() string {
	return `
resource jans_user "user" {
	display_name        = "test-user"
	schemas             = [ "urn:ietf:params:scim:schemas:core:2.0:User" ]
	external_id         = "ext1234"
	user_name           = "test-user"
	nick_name           = "test-user"
	profile_url         = "https://localhost:9443/scim/v2/Users/1234"
	title               = "Mr"
	user_type           = "Employee"
	preferred_language  = "en"
	locale              = "en_US"
	timezone            = "UTC"
	active              = true
	password            = "password"
	
	name {
		family_name     = "Doe"
		given_name       = "John"
		middle_name      = "M"
		honorific_prefix = "Dr"
		honorific_suffix = "Jr"
		// formatted				 = "Dr John M Doe Jr"
	}

	emails {
		value   = "john.doe@jans.io"
		display = "Work"
		type    = "work"
		primary = true
	}
	
	phone_numbers {
		value   = "1234567890"
		display = "Mobile"
		type    = "work"
		primary = true
	}

	ims {
		value   = "@john.doe"
		display = "Messenger"
		type    = "Messenger"
		primary = true
	}

	photos {
		value   = "https://localhost:9443/scim/v2/Users/1234/photo"
		display = "Photo"
		type    = "photo"
		primary = true
	}

	addresses {
		formatted       = "123 Main St"
		street_address  = "123 Main St"
		locality        = "New York"
		region          = "NY"
		postal_code     = "12345"
		country         = "US"
		type            = "work"
		primary         = true
	}

	entitlements {
		value   = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:entitlement"
		display = "Entitlement"
		type    = "entitlement"
		primary = true
	}

	roles {
		value   = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:role"
		display = "Role"
		type    = "role"
		primary = true
	}

	x509_certificates {
		value   = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:x509Certificates"
		display = "X509Certificates"
		type    = "PEM"
		primary = true
	}

	lifecycle {
		ignore_changes = [ "password" ]
	}
}
`
}

func testAccResourceCheckUserExists(name string) resource.TestCheckFunc {
	return func(s *terraform.State) error {
		rs, ok := s.RootModule().Resources[name]
		if !ok {
			return fmt.Errorf("Not found: %s", name)
		}

		c := testAccProvider.Meta().(*jans.Client)

		id := rs.Primary.ID

		ctx := context.Background()

		_, err := c.GetUser(ctx, id)
		if err != nil {
			return err
		}

		return nil
	}
}

func testAccResourceCheckUserDestroy(s *terraform.State) error {

	c := testAccProvider.Meta().(*jans.Client)

	ctx := context.Background()

	for _, rs := range s.RootModule().Resources {
		if rs.Type != "jans_user" {
			continue
		}

		id := rs.Primary.ID

		_, err := c.GetUser(ctx, id)
		if !errors.Is(err, jans.ErrorNotFound) {
			return err
		}
	}

	return nil
}
