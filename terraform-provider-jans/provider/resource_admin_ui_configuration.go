package provider

import (
	"context"

	"github.com/hashicorp/go-cty/cty"
	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func resourceAdminUIConfiguration() *schema.Resource {

	return &schema.Resource{
		Description: "Resource for managing the AdminUI application configuration. This " +
			"resource cannot be created or deleted, only imported and updated.",
		CreateContext: resourceBlockCreate,
		ReadContext:   resourceAdminUIConfigurationRead,
		UpdateContext: resourceAdminUIConfigurationUpdate,
		DeleteContext: resourceUntrackOnDelete,
		Importer: &schema.ResourceImporter{
			StateContext: schema.ImportStatePassthroughContext,
		},
		Schema: map[string]*schema.Schema{
			"auth_server_host": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "Host of the authorization server.",
			},
			"authz_base_url": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "Base URL of the authorization endpoint.",
			},
			"client_id": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "Client id used by the AdminUI.",
			},
			"response_type": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "OAuth response type.",
			},
			"scope": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "Scopes requested by the AdminUI.",
			},
			"redirect_url": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "Redirect URL of the AdminUI.",
			},
			"acr_values": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "ACR values requested by the AdminUI.",
			},
			"front_channel_logout_url": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "Front channel logout URL.",
			},
			"post_logout_redirect_uri": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "Post logout redirect URI.",
			},
			"end_session_endpoint": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "End session endpoint.",
			},
			"session_timeout_in_mins": {
				Type:        schema.TypeInt,
				Optional:    true,
				Computed:    true,
				Description: "Session timeout in minutes.",
			},
			"allow_smtp_keystore_edit": {
				Type:        schema.TypeBool,
				Optional:    true,
				Computed:    true,
				Description: "Whether editing the SMTP keystore is allowed from the AdminUI.",
			},
			"additional_parameters": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "Additional authentication parameters.",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"key": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "Parameter name.",
						},
						"value": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "Parameter value.",
						},
					},
				},
			},
			"cedarling_log_type": {
				Type:        schema.TypeString,
				Optional:    true,
				Computed:    true,
				Description: "Cedarling log type. Possible values are off and std_out.",
				ValidateDiagFunc: func(v any, p cty.Path) diag.Diagnostics {
					return validateEnum(v, []string{"off", "std_out"})
				},
			},
		},
	}
}

func resourceAdminUIConfigurationRead(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var diags diag.Diagnostics

	config, err := c.GetAdminUIConfiguration(ctx)
	if err != nil {
		return diag.FromErr(err)
	}

	if err := toSchemaResource(d, config); err != nil {
		return diag.FromErr(err)
	}

	d.SetId("jans_admin_ui_configuration")

	return diags
}

func resourceAdminUIConfigurationUpdate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	config, err := c.GetAdminUIConfiguration(ctx)
	if err != nil {
		return diag.FromErr(err)
	}

	if err := mergeFromSchemaResource(d, config); err != nil {
		return diag.FromErr(err)
	}

	if _, err := c.UpdateAdminUIConfiguration(ctx, config); err != nil {
		return diag.FromErr(err)
	}

	return resourceAdminUIConfigurationRead(ctx, d, meta)
}
