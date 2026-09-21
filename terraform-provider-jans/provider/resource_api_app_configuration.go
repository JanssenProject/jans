package provider

import (
	"context"

	"github.com/hashicorp/go-cty/cty"
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
			"service_name": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Config API service name.",
			},
			"protection_mode": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Protection mode for the Lock server. Possible values are oauth and cedarling.",
				ValidateDiagFunc: func(v any, p cty.Path) diag.Diagnostics {
					return validateEnum(v, []string{"oauth", "cedarling"})
				},
			},
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
			"user_role_permission_validation_enabled": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Flag to enable/disable the user role-permission mapping check during authentication.",
			},
			"validate_user_inum_in_introspection_flag": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Flag to enable/disable validating `User-inum` against the introspection response.",
			},
			"fetch_user_role_in_introspection_flag": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Flag to enable/disable returning the user role in the introspection response.",
			},
			"user_role_permission_excluded_clients": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "Client IDs exempt from the user role-permission mapping check. Only honoured for tokens that carry no user, so a user token is always checked. A client using the client_credentials grant, such as this provider, has to be listed here when user_role_permission_validation_enabled is true.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
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
			"return_client_secret_in_response": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Flag to enable/disable sending the client secret in the response.",
			},
			"return_encrypted_client_secret_in_response": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Flag to enable/disable sending the encrypted client secret in the response.",
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
			"disable_external_logger_configuration": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Choose whether to disable the external log4j2 configuration override.",
			},
			"max_count": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "",
			},
			"acr_exclusion_list": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "List of ACR values excluded from the active validation check.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
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
			"cedarling_configuration": {
				Type:        schema.TypeList,
				Optional:    true,
				MaxItems:    1,
				Description: "Cedar configuration used for authorization.",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"enabled": {
							Type:        schema.TypeBool,
							Optional:    true,
							Description: "Specify if Cedarling is enabled.",
						},
						"policy_sources": {
							Type:        schema.TypeList,
							Optional:    true,
							Description: "List of policy sources.",
							Elem: &schema.Resource{
								Schema: map[string]*schema.Schema{
									"enabled": {
										Type:        schema.TypeBool,
										Optional:    true,
										Description: "Specify if the policy source is enabled.",
									},
									"authorization_token": {
										Type:        schema.TypeString,
										Optional:    true,
										Sensitive:   true,
										Description: "Authorization token used to access the policy store URI.",
									},
									"policy_store_uri": {
										Type:        schema.TypeString,
										Optional:    true,
										Description: "URI of the policy store. The store can be either json or zip.",
									},
								},
							},
						},
						"log_type": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "Log type. Possible values are OFF, MEMORY and STD_OUT.",
							ValidateDiagFunc: func(v any, p cty.Path) diag.Diagnostics {
								return validateEnum(v, []string{"OFF", "MEMORY", "STD_OUT"})
							},
						},
						"log_level": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "System log level.",
							ValidateDiagFunc: func(v any, p cty.Path) diag.Diagnostics {
								return validateEnum(v, []string{"FATAL", "ERROR", "WARN", "INFO", "DEBUG", "TRACE"})
							},
						},
						"external_policy_store_uri": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "External policy store URI.",
						},
						"max_entries": {
							Type:        schema.TypeInt,
							Optional:    true,
							Description: "Maximum number of entries in the policy store file.",
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
