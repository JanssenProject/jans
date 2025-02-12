package provider

import (
	"context"

	"github.com/hashicorp/terraform-plugin-log/tflog"
	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func resourceApiAppConfiguration() *schema.Resource {

	return &schema.Resource{
		Description:   "Resource for managing config-api configuration properties.",
		CreateContext: resourceBlockCreate,
		ReadContext:   resourceApiAppConfigurationRead,
		UpdateContext: resourceApiAppConfigurationUpdate,
		DeleteContext: resourceUntrackOnDelete,
		Importer: &schema.ResourceImporter{
			StateContext: schema.ImportStatePassthroughContext,
		},
		Schema: map[string]*schema.Schema{
			"config_oauth_enabled": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "",
			},
			"disable_logger_timer": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "",
			},
			"disable_audit_logger": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "",
			},
			"custom_attribute_validation_enabled": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "",
			},
			"acr_validation_enabled": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "",
			},
			"api_approved_issuer": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"api_protection_type": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "",
			},
			"api_client_id": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "",
			},
			"api_client_password": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "",
			},
			"endpoint_injection_enabled": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "",
			},
			"auth_issuer_url": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "",
			},
			"auth_openid_configuration_url": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "",
			},
			"auth_openid_introspection_url": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "",
			},
			"auth_openid_token_url": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "",
			},
			"auth_openid_revoke_url": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "",
			},
			"exclusive_auth_scopes": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"cors_configuration_filters": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"filter_name": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "",
						},
						"cors_enabled": {
							Type:        schema.TypeBool,
							Optional:    true,
							Description: "",
						},
						"cors_allowed_origins": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "",
						},
						"cors_allowed_methods": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "",
						},
						"cors_allowed_headers": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "",
						},
						"cors_exposed_headers": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "",
						},
						"cors_support_credentials": {
							Type:        schema.TypeBool,
							Optional:    true,
							Description: "",
						},
						"cors_logging_enabled": {
							Type:        schema.TypeBool,
							Optional:    true,
							Description: "",
						},
						"cors_preflight_max_age": {
							Type:        schema.TypeInt,
							Optional:    true,
							Description: "",
						},
						"cors_request_decorate": {
							Type:        schema.TypeBool,
							Optional:    true,
							Description: "",
						},
					},
				},
			},
			"logging_level": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "",
			},
			"logging_layout": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "",
			},
			"external_logger_configuration": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "",
			},
			"disable_jdk_logger": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "",
			},
			"max_count": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "",
			},
			"user_exclusion_attributes": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"user_mandatory_attributes": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"agama_configuration": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"mandatory_attributes": {
							Type:        schema.TypeList,
							Optional:    true,
							Description: "",
							Elem: &schema.Schema{
								Type: schema.TypeString,
							},
						},
						"optional_attributes": {
							Type:        schema.TypeList,
							Optional:    true,
							Description: "",
							Elem: &schema.Schema{
								Type: schema.TypeString,
							},
						},
					},
				},
			},
			"audit_log_conf": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"enabled": {
							Type:        schema.TypeBool,
							Optional:    true,
							Description: "",
						},
						"ignore_http_method": {
							Type:        schema.TypeList,
							Optional:    true,
							Description: "",
							Elem: &schema.Schema{
								Type: schema.TypeString,
							},
						},
						"header_attributes": {
							Type:        schema.TypeList,
							Optional:    true,
							Description: "",
							Elem: &schema.Schema{
								Type: schema.TypeString,
							},
						},
					},
				},
			},
			"data_format_conversion_conf": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"enabled": {
							Type:        schema.TypeBool,
							Optional:    true,
							Description: "",
						},
						"ignore_http_method": {
							Type:        schema.TypeList,
							Optional:    true,
							Description: "",
							Elem: &schema.Schema{
								Type: schema.TypeString,
							},
						},
					},
				},
			},
			"plugins": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"name": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "",
						},
						"description": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "",
						},
						"class_name": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "",
						},
					},
				},
			},
			"asset_mgt_configuration": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"asset_mgt_enabled": {
							Type:        schema.TypeBool,
							Optional:    true,
							Description: "",
						},
						"asset_server_upload_enabled": {
							Type:        schema.TypeBool,
							Optional:    true,
							Description: "",
						},
						"file_extension_validation_enabled": {
							Type:        schema.TypeBool,
							Optional:    true,
							Description: "",
						},
						"module_name_validation_enabled": {
							Type:        schema.TypeBool,
							Optional:    true,
							Description: "",
						},
						"asset_dir_mappings": {
							Type:        schema.TypeList,
							Optional:    true,
							Description: "",
							Elem: &schema.Resource{
								Schema: map[string]*schema.Schema{
									"directory": {
										Type:        schema.TypeString,
										Optional:    true,
										Description: "",
									},
									"type": {
										Type:        schema.TypeList,
										Optional:    true,
										Description: "",
										Elem: &schema.Schema{
											Type: schema.TypeString,
										},
									},
									"description": {
										Type:        schema.TypeString,
										Optional:    true,
										Description: "",
									},
								},
							},
						},
					},
				},
			},
		},
	}
}

func resourceApiAppConfigurationRead(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var diags diag.Diagnostics

	flow, err := c.GetApiAppConfiguration(ctx)
	if err != nil {
		return handleNotFoundError(ctx, err, d)
	}

	if err := toSchemaResource(d, flow); err != nil {
		return diag.FromErr(err)
	}
	d.SetId("jans_api_app_configuration")

	return diags

}

func resourceApiAppConfigurationUpdate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var config jans.ApiAppConfiguration
	patches, err := patchFromResourceData(d, &config)
	if err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "Updating ApiAppConfiguration")
	if _, err := c.PatchApiAppConfiguration(ctx, patches); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "ApiAppConfiguration updated")

	return resourceApiAppConfigurationRead(ctx, d, meta)
}
