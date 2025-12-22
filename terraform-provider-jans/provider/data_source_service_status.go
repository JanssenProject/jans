package provider

import (
        "context"
        "fmt"

        "github.com/hashicorp/terraform-plugin-sdk/v2/diag"
        "github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
        "github.com/jans/terraform-provider-jans/jans"
)

func dataSourceServiceStatus() *schema.Resource {
        return &schema.Resource{
                Description: "Data source for retrieving Janssen service status.",
                ReadContext: dataSourceServiceStatusRead,
                Schema: map[string]*schema.Schema{
                        "service": {
                                Type:        schema.TypeString,
                                Optional:    true,
                                Default:     "all",
                                Description: "Service name to check status. Use 'all' to get status of all services.",
                        },
                        "status": {
                                Type:        schema.TypeMap,
                                Computed:    true,
                                Description: "Map of service names to their status (Running, Down, Not present).",
                                Elem: &schema.Schema{
                                        Type: schema.TypeString,
                                },
                        },
                },
        }
}

func dataSourceServiceStatusRead(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
        c := meta.(*jans.Client)

        service := d.Get("service").(string)

        status, err := c.GetServiceStatus(ctx, service)
        if err != nil {
                return diag.FromErr(err)
        }

        d.SetId(fmt.Sprintf("service_status_%s", service))

        statusMap := make(map[string]interface{})
        for k, v := range status {
                statusMap[k] = v
        }

        if err := d.Set("status", statusMap); err != nil {
                return diag.FromErr(err)
        }

        return nil
}
