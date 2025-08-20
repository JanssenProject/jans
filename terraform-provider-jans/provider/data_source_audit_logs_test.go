
package provider

import (
	"testing"

	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/resource"
)

func TestDataSourceAuditLogs_basic(t *testing.T) {
	resource.Test(t, resource.TestCase{
		PreCheck:  func() { testAccPreCheck(t) },
		Providers: testAccProviders,
		Steps: []resource.TestStep{
			{
				Config: testDataSourceAuditLogsConfig_basic(),
				Check: resource.ComposeTestCheckFunc(
					resource.TestCheckResourceAttrSet("data.jans_audit_logs.test", "entries_count"),
					resource.TestCheckResourceAttrSet("data.jans_audit_logs.test", "total_entries_count"),
				),
			},
		},
	})
}

func testDataSourceAuditLogsConfig_basic() string {
	return `
data "jans_audit_logs" "test" {
  limit = 10
}
`
}
