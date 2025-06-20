package provider

import (
	"context"
	"fmt"
	"testing"

	"github.com/google/go-cmp/cmp"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/resource"
	"github.com/hashicorp/terraform-plugin-sdk/v2/terraform"
	"github.com/jans/terraform-provider-jans/jans"
)

func TestResourceMessage_Mapping(t *testing.T) {
	schema := resourceMessage()
	data := schema.Data(nil)

	message := jans.Message{
		ID:          "test-id",
		Key:         "test.message.key",
		Value:       "Test message value",
		Language:    "en",
		Application: "test-app",
	}

	if err := toSchemaResource(data, message); err != nil {
		t.Fatal(err)
	}

	newMessage := jans.Message{}
	if err := fromSchemaResource(data, &newMessage); err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(message, newMessage); diff != "" {
		t.Errorf("Got different entity after mapping: %s", diff)
	}
}

func TestAccResourceMessage_basic(t *testing.T) {
	resource.Test(t, resource.TestCase{
		PreCheck:  func() { testAccPreCheck(t) },
		Providers: testAccProviders,
		Steps: []resource.TestStep{
			{
				Config: testAccResourceMessageConfig_basic(),
				Check: resource.ComposeTestCheckFunc(
					testAccCheckMessageExists("jans_message.test"),
					resource.TestCheckResourceAttr("jans_message.test", "key", "test.message.key"),
					resource.TestCheckResourceAttr("jans_message.test", "value", "Test message value"),
				),
			},
		},
	})
}

func testAccCheckMessageExists(resourceName string) resource.TestCheckFunc {
	return func(s *terraform.State) error {
		rs, ok := s.RootModule().Resources[resourceName]
		if !ok {
			return fmt.Errorf("not found: %s", resourceName)
		}

		if rs.Primary.ID == "" {
			return fmt.Errorf("no ID is set")
		}

		client := testAccProvider.Meta().(*jans.Client)
		ctx := context.Background()

		_, err := client.GetMessage(ctx, rs.Primary.ID)
		return err
	}
}

func testAccResourceMessageConfig_basic() string {
	return `
resource "jans_message" "test" {
	key = "test.message.key"
	value = "Test message value"
	language = "en"
	application = "test-app"
}
`
}
