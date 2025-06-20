package provider

import (
	"context"

	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func dataSourceHealthStatus() *schema.Resource {
	return &schema.Resource{
		Description: "Data source for retrieving health status from Janssen server",
		ReadContext: dataSourceHealthStatusRead,
		Schema: map[string]*schema.Schema{
			"status": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "Overall health status",
			},
			"checks": {
				Type:        schema.TypeList,
				Computed:    true,
				Description: "Individual health check results",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"name": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "Check name",
						},
						"status": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "Check status",
						},
					},
				},
			},
			"db_type": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "Database type",
			},
			"last_update": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "Last update time",
			},
			"facter_data": {
				Type:        schema.TypeMap,
				Computed:    true,
				Description: "Facter data containing system information",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
		},
	}
}

func dataSourceHealthStatusRead(ctx context.Context, data *schema.ResourceData, meta interface{}) diag.Diagnostics {
	c := meta.(*jans.Client)

	healthStatus, err := c.GetHealthStatus(ctx)
	if err != nil {
		return diag.FromErr(err)
	}

	data.SetId("health-status")

	flattened := flattenHealthStatus(healthStatus)
	if err := toSchemaResource(data, flattened); err != nil {
		return diag.FromErr(err)
	}

	return nil
}

func flattenHealthStatus(healthStatus []jans.HealthStatus) map[string]interface{} {
	result := make(map[string]interface{})

	if len(healthStatus) > 0 {
		// Take the first health status entry
		status := healthStatus[0]
		result["status"] = status.Status
		result["checks"] = status.Checks
	}

	return result
}
