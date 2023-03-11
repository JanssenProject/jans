package provider

import (
	"context"

	"github.com/hashicorp/terraform-plugin-log/tflog"
	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func resourceAgamaFlow() *schema.Resource {

	return &schema.Resource{
		Description:   "Resource for managing authentication flows via the Agama engine.",
		CreateContext: resourceAgamaFlowCreate,
		ReadContext:   resourceAgamaFlowRead,
		UpdateContext: resourceAgamaFlowUpdate,
		DeleteContext: resourceAgamaFlowDelete,
		Importer: &schema.ResourceImporter{
			StateContext: schema.ImportStatePassthroughContext,
		},
		Schema: map[string]*schema.Schema{
			"dn": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "Flow distinguished name",
			},
			"qname": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Flow qualified name",
			},
			"revision": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "Revision number of the flow",
			},
			"enabled": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Whether the flow can be launched directly from an authentication request",
			},
			"metadata": {
				Type:        schema.TypeList,
				Optional:    true,
				MaxItems:    1,
				Description: "Flow metadata",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"func_name": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "For internal use only. This property should not be modified",
						},
						"inputs": {
							Type:        schema.TypeList,
							Optional:    true,
							Description: "For internal use only. This property should not be modified",
							Elem: &schema.Schema{
								Type: schema.TypeString,
							},
						},
						"timeout": {
							Type:        schema.TypeInt,
							Optional:    true,
							Description: "For internal use only. This property should not be modified",
						},
						"display_name": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "Name of the flow for displaying purposes",
						},
						"author": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "Author of the flow",
						},
						"timestamp": {
							Type:        schema.TypeInt,
							Computed:    true,
							Description: "Flow creation timestamp relative to UNIX epoch",
						},
						"description": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "Descriptive details of the flow",
						},
						"properties": {
							Type:        schema.TypeMap,
							Optional:    true,
							Description: "Configuration parameters of the flow",
							Elem: &schema.Schema{
								Type: schema.TypeString,
							},
						},
					},
				},
			},
			"source": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Source code",
			},
			"code_error": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Errors in the flow source detected by Agama transpiler",
			},
		},
	}
}

func resourceAgamaFlowCreate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var flow jans.AgamaFlow
	if err := fromSchemaResource(d, &flow); err != nil {
		return diag.FromErr(err)
	}

	tflog.Debug(ctx, "Creating new agama flow")
	newFlow, err := c.CreateAgamaFlow(ctx, &flow)
	if err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "New agama flow created", map[string]interface{}{"dn": newFlow.Dn})

	d.SetId(newFlow.Qname)

	return resourceAgamaFlowRead(ctx, d, meta)
}

func resourceAgamaFlowRead(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var diags diag.Diagnostics

	dn := d.Id()
	flow, err := c.GetAgamaFlow(ctx, dn)
	if err != nil {
		return handleNotFoundError(ctx, err, d)
	}

	if err := toSchemaResource(d, flow); err != nil {
		return diag.FromErr(err)
	}
	d.SetId(flow.Qname)

	return diags

}

func resourceAgamaFlowUpdate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var flow jans.AgamaFlow
	if err := fromSchemaResource(d, &flow); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "Updating agama flow", map[string]interface{}{"dn": flow.Dn})
	if err := c.UpdateAgamaFlow(ctx, &flow); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "Agama flow updated", map[string]interface{}{"dn": flow.Dn})

	return resourceAgamaFlowRead(ctx, d, meta)
}

func resourceAgamaFlowDelete(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	dn := d.Id()
	tflog.Debug(ctx, "Deleting agama flow", map[string]interface{}{"dn": dn})
	if err := c.DeleteAgamaFlow(ctx, dn); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "Agama flow deleted", map[string]interface{}{"dn": dn})

	return resourceAgamaFlowRead(ctx, d, meta)
}
