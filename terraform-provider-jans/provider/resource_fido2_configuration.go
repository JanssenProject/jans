package provider

import (
	"context"

	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/moabu/terraform-provider-jans/jans"
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
				Description: "Time interval for the Clean Service in seconds.",
			},
			"clean_service_batch_chunk_size": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "Each clean up iteration fetches chunk of expired data per base dn and removes it from storage.",
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

	var fido2Config jans.JansFido2DynConfiguration
	if err := fromSchemaResource(d, &fido2Config); err != nil {
		return diag.FromErr(err)
	}

	if err := c.UpdateFido2Configuration(ctx, &fido2Config); err != nil {
		return diag.FromErr(err)
	}

	return resourceFido2ConfigurationRead(ctx, d, meta)
}
