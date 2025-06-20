package provider

import (
	"context"
	"strconv"
	"time"

	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func dataSourceServerStats() *schema.Resource {
	return &schema.Resource{
		Description: "Data source for retrieving server statistics from Janssen server",
		ReadContext: dataSourceServerStatsRead,
		Schema: map[string]*schema.Schema{
			"start_time": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "Server start time",
			},
			"current_time": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "Current server time",
			},
			"uptime": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "Server uptime",
			},
			"memory_usage": {
				Type:        schema.TypeFloat,
				Computed:    true,
				Description: "Current memory usage",
			},
			"cpu_usage": {
				Type:        schema.TypeFloat,
				Computed:    true,
				Description: "Current CPU usage",
			},
			"db_type": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "Database type (sql, ldap, etc.)",
			},
			"last_update": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "Last update timestamp",
			},
			"facter_data": {
				Type:        schema.TypeMap,
				Computed:    true,
				Description: "Server statistics including memory, disk, hostname, etc.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
		},
	}
}

func dataSourceServerStatsRead(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
	c := meta.(*jans.Client)

	serverStats, err := c.GetServerStats(ctx)
	if err != nil {
		return diag.FromErr(err)
	}

	if err := toSchemaResource(d, serverStats); err != nil {
		return diag.FromErr(err)
	}

	d.SetId(strconv.FormatInt(time.Now().Unix(), 10))

	return nil
}
