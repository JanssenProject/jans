package provider

import (
	"context"

	"github.com/hashicorp/go-cty/cty"
	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func resourceScimAppConfiguration() *schema.Resource {

	return &schema.Resource{
		CreateContext: resourceBlockCreate,
		ReadContext:   resourceScimAppConfigurationRead,
		UpdateContext: resourceScimAppConfigurationUpdate,
		DeleteContext: resourceUntrackOnDelete,
		Importer: &schema.ResourceImporter{
			StateContext: schema.ImportStatePassthroughContext,
		},
		Schema: map[string]*schema.Schema{
			"base_dn": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Application config Base DN",
			},
			"application_url": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Application base URL",
			},
			"base_endpoint": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "SCIM base endpoint URL",
			},
			"person_custom_object_class": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Person Object Class",
			},
			"ox_auth_issuer": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Jans Auth - Issuer identifier.",
			},
			"protection_mode": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "SCIM Protection Mode",
				ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

					enums := []string{"OAUTH", "BYPASS"}
					return validateEnum(v, enums)
				},
			},
			"max_count": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: " Example: Maximum number of results per page",
			},
			"user_extension_schema_uri": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "User Extension Schema URI",
			},
			"logging_level": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Logging level for scim logger.",
				ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

					enums := []string{"TRACE", "DEBUG", "INFO", "WARN", "ERROR", "FATAL", "false"}
					return validateEnum(v, enums)
				},
			},
			"logging_layout": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Logging layout used for Server loggers.",
			},
			"external_logger_configuration": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Path to external log4j2 logging configuration.",
			},
			"metric_reporter_interval": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "The interval for metric reporter in seconds.",
			},
			"metric_reporter_keep_data_days": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "The days to keep metric reported data.",
			},
			"metric_reporter_enabled": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Metric reported data enabled flag.",
			},
			"disable_jdk_logger": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value specifying whether to enable JDK Loggers.",
			},
			"use_local_cache": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value specifying whether to enable local in-memory cache.",
			},
			"bulk_max_operations": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "Specifies maximum bulk operations.",
			},
			"bulk_max_payload_size": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "Specifies maximum payload size of bulk operations.",
			},
		},
	}
}

func resourceScimAppConfigurationRead(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var diags diag.Diagnostics

	scimAppConfig, err := c.GetScimAppConfiguration(ctx)
	if err != nil {
		return diag.FromErr(err)
	}

	if err := toSchemaResource(d, scimAppConfig); err != nil {
		return diag.FromErr(err)
	}

	d.SetId("jans_scim_app_configuration")

	return diags
}

func resourceScimAppConfigurationUpdate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var scimAppConfig jans.ScimAppConfigurations
	if err := fromSchemaResource(d, &scimAppConfig); err != nil {
		return diag.FromErr(err)
	}

	if _, err := c.UpdateScimAppConfiguration(ctx, &scimAppConfig); err != nil {
		return diag.FromErr(err)
	}

	return resourceScimAppConfigurationRead(ctx, d, meta)
}
