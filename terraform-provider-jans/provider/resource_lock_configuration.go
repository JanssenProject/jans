package provider

import (
	"context"

	"github.com/hashicorp/go-cty/cty"
	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func lockCedarlingConfigurationSchema() *schema.Resource {
	return &schema.Resource{
		Schema: map[string]*schema.Schema{
			"enabled": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Whether the Cedarling engine is enabled.",
			},
			"policy_sources": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "Policy stores consumed by Cedarling.",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"enabled": {
							Type:        schema.TypeBool,
							Optional:    true,
							Description: "Whether this policy source is enabled.",
						},
						"authorization_token": {
							Type:        schema.TypeString,
							Optional:    true,
							Sensitive:   true,
							Description: "Bearer token used to retrieve the policy store.",
						},
						"policy_store_uri": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "URI of the policy store.",
						},
					},
				},
			},
			"log_type": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Cedarling log type.",
			},
			"log_level": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Cedarling log level.",
			},
			"external_policy_store_uri": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "URI of an external policy store.",
			},
			"max_entries": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "Maximum number of entries kept in the Cedarling log store.",
			},
		},
	}
}

func resourceLockConfiguration() *schema.Resource {

	return &schema.Resource{
		Description: "Resource for managing the Jans Lock plugin configuration. This " +
			"resource cannot be created or deleted, only imported and updated.",
		CreateContext: resourceBlockCreate,
		ReadContext:   resourceLockConfigurationRead,
		UpdateContext: resourceLockConfigurationUpdate,
		DeleteContext: resourceUntrackOnDelete,
		Importer: &schema.ResourceImporter{
			StateContext: schema.ImportStatePassthroughContext,
		},
		Schema: map[string]*schema.Schema{
			"base_dn": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Base DN of the Lock configuration.",
			},
			"base_endpoint": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Base endpoint of the Lock server.",
			},
			"open_id_issuer": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "OpenID issuer used by the Lock server.",
			},
			"protection_mode": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Token protection mode. Possible values are oauth and cedarling.",
				ValidateDiagFunc: func(v any, p cty.Path) diag.Diagnostics {
					return validateEnum(v, []string{"oauth", "cedarling"})
				},
			},
			"audit_persistence_mode": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Audit persistence mode. Possible values are internal and config-api.",
				ValidateDiagFunc: func(v any, p cty.Path) diag.Diagnostics {
					return validateEnum(v, []string{"internal", "config-api"})
				},
			},
			"cedarling_configuration": {
				Type:        schema.TypeList,
				Optional:    true,
				MaxItems:    1,
				Description: "Cedarling engine configuration.",
				Elem:        lockCedarlingConfigurationSchema(),
			},
			"grpc_configuration": {
				Type:        schema.TypeList,
				Optional:    true,
				MaxItems:    1,
				Description: "gRPC transport configuration of the Lock server.",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"server_mode": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "gRPC server mode. Possible values are disabled, bridge, plain_server and tls_server.",
							ValidateDiagFunc: func(v any, p cty.Path) diag.Diagnostics {
								return validateEnum(v, []string{"disabled", "bridge", "plain_server", "tls_server"})
							},
						},
						"grpc_port": {
							Type:        schema.TypeInt,
							Optional:    true,
							Description: "Port the gRPC server listens on.",
						},
						"use_tls": {
							Type:        schema.TypeBool,
							Optional:    true,
							Description: "Whether the gRPC server uses TLS.",
						},
						"tls_cert_chain_file_path": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "Path to the TLS certificate chain file.",
						},
						"tls_private_key_file_path": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "Path to the TLS private key file.",
						},
					},
				},
			},
			"stat_enabled": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Whether statistics collection is enabled.",
			},
			"stat_timer_interval_in_seconds": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "Interval of the statistics timer in seconds.",
			},
			"token_channels": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "Message channels the Lock server listens on for tokens.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"client_id": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Client id used by the Lock server.",
			},
			"client_password": {
				Type:        schema.TypeString,
				Optional:    true,
				Sensitive:   true,
				Description: "Client password used by the Lock server.",
			},
			"disable_jdk_logger": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value specifying whether to enable JDK Loggers.",
			},
			"disable_external_logger_configuration": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Whether to disable the external log4j2 configuration override.",
			},
			"logging_level": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Logging level for the Lock server.",
			},
			"logging_layout": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Logging layout used by the Lock server.",
			},
			"external_logger_configuration": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Path to an external logging configuration.",
			},
			"metric_reporter_interval": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "The interval for metric reporter in seconds.",
			},
			"metric_reporter_keep_data_days": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "The days to keep report data.",
			},
			"metric_reporter_enabled": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value specifying whether to enable Metric Reporter.",
			},
			"clean_service_interval": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "Time interval for the clean up service in seconds.",
			},
			"message_consumer_type": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Message consumer type.",
			},
			"error_reason_enabled": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Whether error reasons are returned in responses.",
			},
			"clean_service_batch_chunk_size": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "Each clean up iteration fetches a chunk of expired data per base dn and removes it.",
			},
		},
	}
}

func resourceLockConfigurationRead(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var diags diag.Diagnostics

	config, err := c.GetLockConfiguration(ctx)
	if err != nil {
		return diag.FromErr(err)
	}

	if err := toSchemaResource(d, config); err != nil {
		return diag.FromErr(err)
	}

	d.SetId("jans_lock_configuration")

	return diags
}

func resourceLockConfigurationUpdate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	config, err := c.GetLockConfiguration(ctx)
	if err != nil {
		return diag.FromErr(err)
	}

	if err := mergeFromSchemaResource(d, config); err != nil {
		return diag.FromErr(err)
	}

	if _, err := c.UpdateLockConfiguration(ctx, config); err != nil {
		return diag.FromErr(err)
	}

	return resourceLockConfigurationRead(ctx, d, meta)
}
