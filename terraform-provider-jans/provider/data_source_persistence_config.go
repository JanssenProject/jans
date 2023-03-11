package provider

import (
	"context"
	"strconv"
	"time"

	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func dataSourcePersistenceConfiguration() *schema.Resource {

	return &schema.Resource{
		Description: "Data source for retrieving the persistence configured in the Janssen server",
		ReadContext: dataSourcePersistenceConfigurationRead,
		Schema: map[string]*schema.Schema{
			"persistence_type": {
				Type:     schema.TypeString,
				Computed: true,
			},
		},
	}
}

func dataSourcePersistenceConfigurationRead(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
	c := meta.(*jans.Client)

	persistenceConfig, err := c.GetPersistenceConfiguration(ctx)
	if err != nil {
		return diag.FromErr(err)
	}

	if err := toSchemaResource(d, persistenceConfig); err != nil {
		return diag.FromErr(err)
	}

	d.SetId(strconv.FormatInt(time.Now().Unix(), 10))

	return nil
}
