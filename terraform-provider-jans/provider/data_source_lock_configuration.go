package provider

import (
	"context"
	"strconv"
	"time"

	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func dataSourceLockConfiguration() *schema.Resource {

	return &schema.Resource{
		Description: "Data source for retrieving the Jans Lock plugin configuration.",
		ReadContext: dataSourceLockConfigurationRead,
		Schema:      computedSchemaFromResource(resourceLockConfiguration().Schema),
	}
}

func dataSourceLockConfigurationRead(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	config, err := c.GetLockConfiguration(ctx)
	if err != nil {
		return diag.FromErr(err)
	}

	if err := toSchemaResource(d, config); err != nil {
		return diag.FromErr(err)
	}

	d.SetId(strconv.FormatInt(time.Now().Unix(), 10))

	return nil
}
