package provider

import (
        "context"

        "github.com/hashicorp/go-cty/cty"
        "github.com/hashicorp/terraform-plugin-sdk/v2/diag"
        "github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
        "github.com/jans/terraform-provider-jans/jans"
)

func resourceFido2Configuration() *schema.Resource {

        return &schema.Resource{
                CreateContext: resourceBlockCreate,
                ReadContext:   resourceFido2ConfigurationRead,
                UpdateContext: resourceFido2ConfigurationUpdate,
                DeleteContext: resourceUntrackOnDelete,
                Importer: &schema.ResourceImporter{
                        StateContext: schema.ImportStatePassthroughContext,
                },
                Schema: map[string]*schema.Schema{
                        "issuer": {
                                Type:             schema.TypeString,
                                Optional:         true,
                                Description:      "URL using the https scheme for Issuer identifier. Example: https://server.example.com/",
                                ValidateDiagFunc: validateURL,
                        },
                        "base_endpoint": {
                                Type:             schema.TypeString,
                                Optional:         true,
                                Description:      "The base URL for Fido2 endpoints. Example: https://server.example.com/fido2/restv1",
                                ValidateDiagFunc: validateURL,
                        },
                        "clean_service_interval": {
                                Type:        schema.TypeInt,
                                Optional:    true,
                                Description: "Time interval for the clean up service in seconds.",
                        },
                        "clean_service_batch_chunk_size": {
                                Type:        schema.TypeInt,
                                Optional:    true,
                                Description: "Each clean up iteration fetches chunk of expired data per base dn and removes it.",
                        },
                        "use_local_cache": {
                                Type:        schema.TypeBool,
                                Optional:    true,
                                Description: "Boolean value to indicate if Local Cache is to be used.",
                        },
                        "disable_jdk_logger": {
                                Type:        schema.TypeBool,
                                Optional:    true,
                                Description: "Boolean value specifying whether to enable JDK Loggers.",
                        },
                        "logging_level": {
                                Type:        schema.TypeString,
                                Optional:    true,
                                Description: "Logging level for Fido2 logger.",
                        },
                        "logging_layout": {
                                Type:        schema.TypeString,
                                Optional:    true,
                                Description: "Logging layout used for Fido2.",
                        },
                        "external_logger_configuration": {
                                Type:        schema.TypeString,
                                Optional:    true,
                                Description: "Path to external Fido2 logging configuration.",
                        },
                        "metric_reporter_enabled": {
                                Type:        schema.TypeBool,
                                Optional:    true,
                                Description: "Boolean value specifying whether to enable Metric Reporter.",
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
                        "disable_external_logger_configuration": {
                                Type:        schema.TypeBool,
                                Optional:    true,
                                Description: "Choose whether to disable the external log4j2 configuration override.",
                        },
                        "fido2_metrics_enabled": {
                                Type:        schema.TypeBool,
                                Optional:    true,
                                Description: "Boolean value specifying whether FIDO2 passkey metrics collection is enabled.",
                        },
                        "fido2_metrics_retention_days": {
                                Type:        schema.TypeInt,
                                Optional:    true,
                                Description: "Number of days to keep FIDO2 passkey metrics data.",
                        },
                        "fido2_device_info_collection": {
                                Type:        schema.TypeBool,
                                Optional:    true,
                                Description: "Boolean value specifying whether to collect device information in FIDO2 metrics.",
                        },
                        "fido2_error_categorization": {
                                Type:        schema.TypeBool,
                                Optional:    true,
                                Description: "Boolean value specifying whether to categorize errors in FIDO2 metrics.",
                        },
                        "fido2_performance_metrics": {
                                Type:        schema.TypeBool,
                                Optional:    true,
                                Description: "Boolean value specifying whether to collect detailed performance metrics for FIDO2 operations.",
                        },
                        "fido2_metrics_aggregation_enabled": {
                                Type:        schema.TypeBool,
                                Optional:    true,
                                Description: "Boolean value specifying whether FIDO2 metrics aggregation is enabled.",
                        },
                        "trusted_proxy_enabled": {
                                Type:        schema.TypeBool,
                                Optional:    true,
                                Description: "Whether proxy headers may be trusted when recording the client IP in metrics. True trusts them only from the source addresses listed in trusted_proxy_ip_ranges.",
                        },
                        "trusted_proxy_ip_ranges": {
                                Type:        schema.TypeList,
                                Optional:    true,
                                Description: "Reverse-proxy source addresses whose forwarded headers are trusted, in CIDR notation. Only consulted when trusted_proxy_enabled is true; an empty list trusts nothing.",
                                Elem: &schema.Schema{
                                        Type: schema.TypeString,
                                },
                        },
                        "person_custom_object_class_list": {
                                Type:        schema.TypeList,
                                Optional:    true,
                                Description: "Custom object class list for dynamic person enrolment.",
                                Elem: &schema.Schema{
                                        Type: schema.TypeString,
                                },
                        },
                        "fido2_configuration": {
                                Type:        schema.TypeList,
                                Optional:    true,
                                MaxItems:    1,
                                Description: "Fido2Configuration.",
                                Elem: &schema.Resource{
                                        Schema: map[string]*schema.Schema{
                                                "authenticator_certs_folder": {
                                                        Type:        schema.TypeString,
                                                        Optional:    true,
                                                        Description: "Authenticators certificates fodler.",
                                                },
                                                "mds_certs_folder": {
                                                        Type:        schema.TypeString,
                                                        Optional:    true,
                                                        Description: "MDS TOC root certificates folder.",
                                                },
                                                "mds_tocs_folder": {
                                                        Type:        schema.TypeString,
                                                        Optional:    true,
                                                        Description: "MDS TOC files folder.",
                                                },
                                                "server_metadata_folder": {
                                                        Type:        schema.TypeString,
                                                        Optional:    true,
                                                        Description: "Authenticators metadata in json format.",
                                                },
                                                "requested_parties": {
                                                        Type:        schema.TypeList,
                                                        Optional:    true,
                                                        Description: "Authenticators metadata in json format.",
                                                        Elem: &schema.Resource{
                                                                Schema: map[string]*schema.Schema{
                                                                        "name": {
                                                                                Type:        schema.TypeString,
                                                                                Optional:    true,
                                                                                Description: "Name of the requested party.",
                                                                        },
                                                                        "domains": {
                                                                                Type:        schema.TypeList,
                                                                                Optional:    true,
                                                                                Description: "Requested Party domains.",
                                                                                Elem: &schema.Schema{
                                                                                        Type: schema.TypeString,
                                                                                },
                                                                        },
                                                                        "policy": {
                                                                                Type:        schema.TypeList,
                                                                                Optional:    true,
                                                                                MaxItems:    1,
                                                                                Description: "Per-relying-party assurance policy. Omitted falls back to the global configuration.",
                                                                                Elem: &schema.Resource{
                                                                                        Schema: map[string]*schema.Schema{
                                                                                                "attestation_mode": {
                                                                                                        Type:        schema.TypeString,
                                                                                                        Optional:    true,
                                                                                                        Description: "Attestation mode for this relying party. Possible values are disabled, monitor and enforced.",
                                                                                                        ValidateDiagFunc: func(v any, p cty.Path) diag.Diagnostics {
                                                                                                                return validateEnum(v, []string{"disabled", "monitor", "enforced"})
                                                                                                        },
                                                                                                },
                                                                                        },
                                                                                },
                                                                        },
                                                                },
                                                        },
                                                },
                                                "user_auto_enrollment": {
                                                        Type:        schema.TypeBool,
                                                        Optional:    true,
                                                        Description: "Allow to enroll users on enrollment/authentication requests.",
                                                },
                                                "unfinished_request_expiration": {
                                                        Type:        schema.TypeInt,
                                                        Optional:    true,
                                                        Description: "Expiration time in seconds for pending enrollment/authentication requests",
                                                },
                                                "authentication_history_expiration": {
                                                        Type:        schema.TypeInt,
                                                        Optional:    true,
                                                        Description: "Expiration time in seconds for approved authentication requests.",
                                                },
                                                "record_abandoned_assertions": {
                                                        Type:        schema.TypeBool,
                                                        Optional:    true,
                                                        Description: "Whether assertion ceremonies that lapse without being completed are relabelled as abandoned instead of being deleted unlabelled.",
                                                },
                                                "abandoned_request_expiration": {
                                                        Type:        schema.TypeInt,
                                                        Optional:    true,
                                                        Description: "Expiration time in seconds for abandoned assertion ceremonies.",
                                                },
                                                "abandoned_request_sweep_interval": {
                                                        Type:        schema.TypeInt,
                                                        Optional:    true,
                                                        Description: "Interval in seconds between sweeps for lapsed assertion ceremonies. Must stay below unfinished_request_expiration.",
                                                },
                                                "disable_metadata_service": {
                                                        Type:        schema.TypeBool,
                                                        Optional:    true,
                                                        Description: "Boolean value indicating whether the MDS download should be omitted.",
                                                },
                                                "mds_download_startup_retries": {
                                                        Type:        schema.TypeInt,
                                                        Optional:    true,
                                                        Description: "Number of times the MDS TOC download is retried at server startup when the TOC blob is missing.",
                                                },
                                                "mds_download_startup_retry_interval": {
                                                        Type:        schema.TypeInt,
                                                        Optional:    true,
                                                        Description: "Delay in seconds between MDS TOC download retries at server startup.",
                                                },
                                                "enterprise_attestation": {
                                                        Type:        schema.TypeBool,
                                                        Optional:    true,
                                                        Description: "Whether authenticators have been enabled for use in a specific protected environment.",
                                                },
                                                "attestation_mode": {
                                                        Type:        schema.TypeString,
                                                        Optional:    true,
                                                        Description: "Whether MDS validation should be omitted during attestation. Possible values are disabled, monitor and enforced.",
                                                        ValidateDiagFunc: func(v any, p cty.Path) diag.Diagnostics {
                                                                return validateEnum(v, []string{"disabled", "monitor", "enforced"})
                                                        },
                                                },
                                                "hints": {
                                                        Type:        schema.TypeList,
                                                        Optional:    true,
                                                        Description: "Hints to the relying party. Possible values are security-key, client-device and hybrid.",
                                                        Elem: &schema.Schema{
                                                                Type: schema.TypeString,
                                                        },
                                                },
                                                "allowed_top_origins": {
                                                        Type:        schema.TypeList,
                                                        Optional:    true,
                                                        Description: "Full origins permitted to frame a cross-origin ceremony. An empty list denies every framed ceremony.",
                                                        Elem: &schema.Schema{
                                                                Type: schema.TypeString,
                                                        },
                                                },
                                                "metadata_servers": {
                                                        Type:        schema.TypeList,
                                                        Optional:    true,
                                                        Description: "Sources of URLs with external metadata.",
                                                        Elem: &schema.Resource{
                                                                Schema: map[string]*schema.Schema{
                                                                        "url": {
                                                                                Type:        schema.TypeString,
                                                                                Optional:    true,
                                                                                Description: "URL of the metadata server.",
                                                                        },
                                                                        "root_cert": {
                                                                                Type:        schema.TypeString,
                                                                                Optional:    true,
                                                                                Description: "Root certificate of the metadata server.",
                                                                        },
                                                                },
                                                        },
                                                },
                                                "requested_credential_types": {
                                                        Type:        schema.TypeList,
                                                        Optional:    true,
                                                        Description: "List of Requested Credential Types.",
                                                        Elem: &schema.Schema{
                                                                Type: schema.TypeString,
                                                        },
                                                },
                                        },
                                },
                        },
                },
        }
}

func resourceFido2ConfigurationRead(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

        c := meta.(*jans.Client)

        var diags diag.Diagnostics

        fido2Config, err := c.GetFido2Configuration(ctx)
        if err != nil {
                return diag.FromErr(err)
        }

        if err := toSchemaResource(d, fido2Config); err != nil {
                return diag.FromErr(err)
        }

        d.SetId("jans_fido2_configuration")

        return diags
}

func resourceFido2ConfigurationUpdate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

        c := meta.(*jans.Client)

        fido2Config, err := c.GetFido2Configuration(ctx)
        if err != nil {
                return diag.FromErr(err)
        }

        if err := mergeFromSchemaResource(d, fido2Config); err != nil {
                return diag.FromErr(err)
        }

        if _, err := c.UpdateFido2Configuration(ctx, fido2Config); err != nil {
                return diag.FromErr(err)
        }

        return resourceFido2ConfigurationRead(ctx, d, meta)
}
