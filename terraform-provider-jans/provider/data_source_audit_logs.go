
package provider

import (
        "context"

        "github.com/hashicorp/terraform-plugin-sdk/v2/diag"
        "github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"

        "github.com/jans/terraform-provider-jans/jans"
)

func dataSourceAuditLogs() *schema.Resource {
        return &schema.Resource{
                Description: "Data source for retrieving audit logs from Janssen server",
                ReadContext: dataSourceAuditLogsRead,
                Schema: map[string]*schema.Schema{
                        "pattern": {
                                Type:        schema.TypeString,
                                Optional:    true,
                                Description: "Search pattern for filtering logs",
                        },
                        "start_index": {
                                Type:        schema.TypeInt,
                                Optional:    true,
                                Default:     0,
                                Description: "The 1-based index of the first query result",
                        },
                        "limit": {
                                Type:        schema.TypeInt,
                                Optional:    true,
                                Default:     50,
                                Description: "Search size - max size of the results to return",
                        },
                        "start_date": {
                                Type:        schema.TypeString,
                                Optional:    true,
                                Description: "Start date for which the log entries report is to be fetched",
                        },
                        "end_date": {
                                Type:        schema.TypeString,
                                Optional:    true,
                                Description: "End date for which the log entries is to be fetched",
                        },
                        "start": {
                                Type:        schema.TypeInt,
                                Computed:    true,
                                Description: "Start index of the returned results",
                        },
                        "total_entries_count": {
                                Type:        schema.TypeInt,
                                Computed:    true,
                                Description: "Total number of entries available",
                        },
                        "entries_count": {
                                Type:        schema.TypeInt,
                                Computed:    true,
                                Description: "Number of entries returned in this result",
                        },
                        "entries": {
                                Type:        schema.TypeList,
                                Computed:    true,
                                Description: "List of audit log entries",
                                Elem: &schema.Schema{
                                        Type: schema.TypeString,
                                },
                        },
                },
        }
}

func dataSourceAuditLogsRead(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
        c := meta.(*jans.Client)

        pattern := d.Get("pattern").(string)
        startIndex := d.Get("start_index").(int)
        limit := d.Get("limit").(int)
        startDate := d.Get("start_date").(string)
        endDate := d.Get("end_date").(string)

        logs, err := c.GetAuditLogs(ctx, pattern, startIndex, limit, startDate, endDate)
        if err != nil {
                return diag.FromErr(err)
        }

        d.SetId("audit_logs")
        d.Set("start", logs.Start)
        d.Set("total_entries_count", logs.TotalEntriesCount)
        d.Set("entries_count", logs.EntriesCount)
        d.Set("entries", logs.Entries)

        return nil
}
