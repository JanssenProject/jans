package provider

import (
	"context"

	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func dataSourceAgamaSyntaxCheck() *schema.Resource {
	return &schema.Resource{
		Description: "Data source for checking Agama flow code syntax.",
		ReadContext: dataSourceAgamaSyntaxCheckRead,
		Schema: map[string]*schema.Schema{
			"flow_name": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "The name of the Agama flow to check.",
			},
			"code": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "The Agama DSL code to validate.",
			},
			"valid": {
				Type:        schema.TypeBool,
				Computed:    true,
				Description: "Whether the Agama code syntax is valid.",
			},
			"message": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "Syntax check result message. Empty or 'Syntax is OK' for valid code.",
			},
		},
	}
}

func dataSourceAgamaSyntaxCheckRead(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
	c := meta.(*jans.Client)

	flowName := d.Get("flow_name").(string)
	code := d.Get("code").(string)

	result, err := c.CheckAgamaSyntax(ctx, flowName, code)
	if err != nil {
		return diag.FromErr(err)
	}

	d.SetId("agama_syntax_check_" + flowName)

	if err := d.Set("valid", result.Valid); err != nil {
		return diag.FromErr(err)
	}

	if err := d.Set("message", result.Message); err != nil {
		return diag.FromErr(err)
	}

	return nil
}
