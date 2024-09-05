package provider

import (
	"context"
	"errors"
	"fmt"
	"path/filepath"
	"runtime"
	"testing"

	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/resource"
	"github.com/hashicorp/terraform-plugin-sdk/v2/terraform"
	"github.com/jans/terraform-provider-jans/jans"
)

func TestAccResourceAgamaDeployment_basic(t *testing.T) {

	resourceName := "jans_agama_deployment.test"

	resource.Test(t, resource.TestCase{
		PreCheck:     func() { testAccPreCheck(t) },
		Providers:    testAccProviders,
		CheckDestroy: testAccResourceCheckAgamaDeploymentDestroy,
		Steps: []resource.TestStep{
			{
				Config: testAccResourceAgamaDeploymentConfig_basic(),
				Check: resource.ComposeTestCheckFunc(
					testAccResourceCheckAgamaDeploymentExists(resourceName),
					resource.TestCheckResourceAttr(resourceName, "name", "test-deployment"),
				),
			},
		},
	})
}

func testAccResourceAgamaDeploymentConfig_basic() string {

	_, filename, _, _ := runtime.Caller(0)
	dir := filepath.Dir(filepath.Dir(filename))

	testFile := dir + "/testdata/agama_project.gama"

	return `
resource "jans_agama_deployment" "test" {
	name 						= "test-deployment"
	deployment_file = "` + testFile + `"
	autoconfigure   = true
}
`
}

func testAccResourceCheckAgamaDeploymentExists(name string) resource.TestCheckFunc {
	return func(s *terraform.State) error {
		rs, ok := s.RootModule().Resources[name]
		if !ok {
			return fmt.Errorf("Not found: %s", name)
		}

		c := testAccProvider.Meta().(*jans.Client)

		name := rs.Primary.ID

		ctx := context.Background()

		_, err := c.GetAgamaDeployment(ctx, name)
		if err != nil {
			return err
		}

		return nil
	}
}

func testAccResourceCheckAgamaDeploymentDestroy(s *terraform.State) error {

	c := testAccProvider.Meta().(*jans.Client)

	ctx := context.Background()

	for _, rs := range s.RootModule().Resources {
		if rs.Type != "jans_agama_deployment" {
			continue
		}

		deploymentName := rs.Primary.ID

		_, err := c.GetAgamaDeployment(ctx, deploymentName)
		if !errors.Is(err, jans.ErrorNotFound) {
			return err
		}

	}

	return nil
}
