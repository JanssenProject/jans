package provider

import (
	"fmt"
	"os"
	"testing"

	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/hashicorp/terraform-plugin-sdk/v2/terraform"
)

var (
	testAccProvider  *schema.Provider
	testAccProviders map[string]*schema.Provider
)

var requiredEnvironmentVariables = []string{
	"JANS_CLIENT_ID",
	"JANS_CLIENT_SECRET",
	"JANS_URL",
}

func init() {
	testAccProvider = Provider()
	testAccProviders = map[string]*schema.Provider{
		"jans": testAccProvider,
	}
}

func testAccPreCheck(t *testing.T) {
	for _, requiredEnvironmentVariable := range requiredEnvironmentVariables {
		if value := os.Getenv(requiredEnvironmentVariable); value == "" {
			t.Fatalf("%s must be set before running acceptance tests.", requiredEnvironmentVariable)
		}
	}
}

// Following needs to be commented out for the tests to run locally
// func TestMain(m *testing.M) {

// 	os.Setenv("TF_ACC", "1")
// 	os.Setenv("JANS_CLIENT_ID", "1800.c014a54b-b068-4ff0-b094-1cce64b994b9")
// 	os.Setenv("JANS_CLIENT_SECRET", "UJGea5ABGTKp")
// 	os.Setenv("JANS_URL", "https://127.0.0.1")
// 	os.Setenv("JANS_INSECURE_CLIENT", "true")

// 	os.Exit(m.Run())
// }

func checkAttribute(is *terraform.InstanceState, attributeName, desiredValue string) error {

	if is.Attributes[attributeName] != desiredValue {
		return fmt.Errorf("%q is not correct, expected '%s', got '%s'", attributeName, desiredValue, is.Attributes[attributeName])
	}

	return nil
}
