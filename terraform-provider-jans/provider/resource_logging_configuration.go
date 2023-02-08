package provider

import (
	"context"

	"github.com/hashicorp/go-cty/cty"
	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/moabu/terraform-provider-jans/jans"
)

func resourceLoggingConfiguration() *schema.Resource {

	return &schema.Resource{
		Description: "Resource for managing the logging configurations for the Janssen Server. This resource cannot " +
			"be created or deleted, only imported and updated.",
		CreateContext: resourceBlockCreate,
		ReadContext:   resourceLoggingConfigurationRead,
		UpdateContext: resourceLoggingConfigurationUpdate,
		DeleteContext: resourceUntrackOnDelete,
		Importer: &schema.ResourceImporter{
			StateContext: schema.ImportStatePassthroughContext,
		},
		Schema: map[string]*schema.Schema{
			"logging_level": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Logging level for Jans Authorization Server logger.",
				ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

					enums := []string{"TRACE", "DEBUG", "INFO", "WARN", "ERROR", "FATAL", "OFF"}
					return validateEnum(v, enums)
				},
			},
			"logging_layout": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Logging layout used for Jans Authorization Server loggers.",
				ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

					enums := []string{"text", "json"}
					return validateEnum(v, enums)
				},
			},
			"http_logging_enabled": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "To enable http request/response logging.",
			},
			"disable_jdk_logger": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "To enable/disable Jdk logging.",
			},
			"enabled_oauth_audit_logging": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "To enable/disable OAuth audit logging.",
			},
			"external_logger_configuration": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Path to external log4j2 configuration file.",
			},
			"http_logging_exclude_paths": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "List of paths to exclude from logger. Example: [/auth/img /auth/stylesheet]",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"last_updated": {
				Type:     schema.TypeString,
				Optional: true,
				Computed: true,
			},
		},
	}
}

func resourceLoggingConfigurationRead(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var diags diag.Diagnostics

	var loggingConfigOld jans.LoggingConfiguration
	if err := fromSchemaResource(d, &loggingConfigOld); err != nil {
		return diag.FromErr(err)
	}

	loggingConfig, err := c.GetLoggingConfiguration(ctx)
	if err != nil {
		return diag.FromErr(err)
	}

	if err := toSchemaResource(d, loggingConfig); err != nil {
		return diag.FromErr(err)
	}

	d.SetId("jans_logging_configuration")

	return diags
}

func resourceLoggingConfigurationUpdate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

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
