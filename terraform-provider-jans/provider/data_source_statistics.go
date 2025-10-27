package provider

import (
        "context"
        "encoding/json"

        "github.com/hashicorp/terraform-plugin-sdk/v2/diag"
        "github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"

        "github.com/jans/terraform-provider-jans/jans"
)

func dataSourceStatistics() *schema.Resource {
        return &schema.Resource{
                Description: `Data source for retrieving statistics and metrics from Janssen server.

This data source allows you to retrieve server statistics and metrics for monitoring, alerting,
and performance tracking. You can query statistics for a specific month or a date range.

## Example Usage

` + "```hcl" + `
# Get current month statistics
data "jans_statistics" "current_month" {}

# Get specific month statistics
data "jans_statistics" "october_2025" {
  month = "202510"
}

# Get statistics for date range
data "jans_statistics" "quarterly" {
  start_month = "202507"
  end_month   = "202509"
}

# Output statistics
output "server_stats" {
  value = {
    month = data.jans_statistics.current_month.month
    data  = jsondecode(data.jans_statistics.current_month.statistics)
  }
}
` + "```" + `

## OAuth Scopes Required

- ` + "`https://jans.io/oauth/config/stats.readonly`" + `
`,
                ReadContext: dataSourceStatisticsRead,
                Schema: map[string]*schema.Schema{
                        "month": {
                                Type:        schema.TypeString,
                                Optional:    true,
                                Description: "Month for which the stat report is to be fetched (e.g., 202012). Required if start_month and end_month are not provided.",
                        },
                        "start_month": {
                                Type:        schema.TypeString,
                                Optional:    true,
                                Description: "Start month for which the stat report is to be fetched",
                        },
                        "end_month": {
                                Type:        schema.TypeString,
                                Optional:    true,
                                Description: "End month for which the stat report is to be fetched",
                        },
                        "format": {
                                Type:        schema.TypeString,
                                Optional:    true,
                                Description: "Report format",
                        },
                        "statistics": {
                                Type:        schema.TypeString,
                                Computed:    true,
                                Description: "Statistics data in JSON format",
                        },
                },
        }
}

func dataSourceStatisticsRead(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
        c := meta.(*jans.Client)

        month := d.Get("month").(string)
        startMonth := d.Get("start_month").(string)
        endMonth := d.Get("end_month").(string)
        format := d.Get("format").(string)

        // Validate that either month OR start_month/end_month are provided
        if month == "" && (startMonth == "" || endMonth == "") {
                return diag.Errorf("Either 'month' or both 'start_month' and 'end_month' must be provided")
        }

        stats, err := c.GetStatistics(ctx, month, startMonth, endMonth, format)
        if err != nil {
                return diag.FromErr(err)
        }

        // Convert the statistics to JSON string for storage
        statsJSON, err := json.Marshal(stats)
        if err != nil {
                return diag.FromErr(err)
        }

        d.SetId("statistics")
        d.Set("statistics", string(statsJSON))

        return nil
}
