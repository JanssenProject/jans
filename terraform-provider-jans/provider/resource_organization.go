package provider

import (
	"context"

	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func resourceOrganization() *schema.Resource {

	return &schema.Resource{
		Description: "Resource for managing the organization information maintained in the Janssen Server. This " +
			"resource cannot be created or deleted, only imported and updated.",
		CreateContext: resourceBlockCreate,
		ReadContext:   resourceOrganizationRead,
		UpdateContext: resourceOrganizationUpdate,
		DeleteContext: resourceUntrackOnDelete,
		Importer: &schema.ResourceImporter{
			StateContext: schema.ImportStatePassthroughContext,
		},
		Schema: map[string]*schema.Schema{
			"dn": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "",
			},
			"display_name": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Organization name",
			},
			"description": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Organization description",
			},
			"member": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "String describing memberOf",
			},
			// "country_name": {
			// 	Type:        schema.TypeString,
			// 	Optional:    true,
			// 	Description: "Organization country name",
			// },
			"organization": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "",
			},
			// "status": {
			// 	Type:        schema.TypeString,
			// 	Optional:    true,
			// 	Description: "",
			// },
			"manager_group": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "qualified id of the group Example: inum=60B7,ou=groups,o=jans",
			},
			"theme_color": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "color of the theme Example: 166309",
			},
			"short_name": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "",
			},
			// "custom_messages": {
			// 	Type:        schema.TypeList,
			// 	Optional:    true,
			// 	Description: "",
			// 	Elem: &schema.Schema{
			// 		Type: schema.TypeString,
			// 	},
			// },
			// "title": {
			// 	Type:        schema.TypeString,
			// 	Optional:    true,
			// 	Description: "",
			// },
			"organization_title": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "",
			},
			// "js_logo_path": {
			// 	Type:        schema.TypeString,
			// 	Optional:    true,
			// 	Description: "Path to organization logo image",
			// },
			// "js_favicon_path": {
			// 	Type:        schema.TypeString,
			// 	Optional:    true,
			// 	Description: "Path to organization favicon image",
			// },
			"base_dn": {
				Type:        schema.TypeString,
				Computed:    true, // XXX: is this really computed?
				Description: "",
			},
		},
	}
}

func resourceOrganizationRead(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var diags diag.Diagnostics

	orga, err := c.GetOrganization(ctx)
	if err != nil {
		return diag.FromErr(err)
	}

	if err := toSchemaResource(d, orga); err != nil {
		return diag.FromErr(err)
	}

	d.SetId("jans_organization")

	return diags
}

func resourceOrganizationUpdate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var loggingConfig jans.LoggingConfiguration
	if err := fromSchemaResource(d, &loggingConfig); err != nil {
		return diag.FromErr(err)
	}

	if _, err := c.UpdateLoggingConfiguration(ctx, &loggingConfig); err != nil {
		return diag.FromErr(err)
	}

	return resourceLoggingConfigurationRead(ctx, d, meta)
}
