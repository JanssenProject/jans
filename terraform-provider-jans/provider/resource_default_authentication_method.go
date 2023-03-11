package provider

import (
	"context"

	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func resourceDefaultAuthenticationMethod() *schema.Resource {

	return &schema.Resource{
		CreateContext: resourceBlockCreate,
		ReadContext:   resourceDefaultAuthenticationMethodRead,
		UpdateContext: resourceDefaultAuthenticationMethodUpdate,
		DeleteContext: resourceUntrackOnDelete,
		Importer: &schema.ResourceImporter{
			StateContext: schema.ImportStatePassthroughContext,
		},
		Schema: map[string]*schema.Schema{
			"default_acr": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "This field controls the default authentication mechanism that is presented to users from all applications that leverage Janssen Server for authentication.",
			},
		},
	}
}

func resourceDefaultAuthenticationMethodRead(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var diags diag.Diagnostics

	defaultAuthMethod, err := c.GetDefaultAuthenticationMethod(ctx)
	if err != nil {
		return diag.FromErr(err)
	}

	if err := toSchemaResource(d, defaultAuthMethod); err != nil {
		return diag.FromErr(err)
	}

	d.SetId("jans_default_authentication_method")

	return diags
}

func resourceDefaultAuthenticationMethodUpdate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var defaulAuthMethod jans.DefaultAuthenticationMethod
	if err := fromSchemaResource(d, &defaulAuthMethod); err != nil {
		return diag.FromErr(err)
	}

	if _, err := c.UpdateDefaultAuthenticationMethod(ctx, &defaulAuthMethod); err != nil {
		return diag.FromErr(err)
	}

	return resourceDefaultAuthenticationMethodRead(ctx, d, meta)
}
