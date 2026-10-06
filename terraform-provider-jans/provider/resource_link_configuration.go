package provider

import (
	"context"

	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func linkLdapConfigurationSchema() *schema.Resource {
	return &schema.Resource{
		Schema: map[string]*schema.Schema{
			"config_id": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Identifier of the LDAP configuration.",
			},
			"bind_dn": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Bind DN used to connect to the LDAP server.",
			},
			"bind_password": {
				Type:        schema.TypeString,
				Optional:    true,
				Sensitive:   true,
				Description: "Password used to bind to the LDAP server.",
			},
			"servers": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "List of LDAP servers in host:port form.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"max_connections": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "Maximum number of connections in the pool.",
			},
			"use_ssl": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Whether to connect to the LDAP server over SSL.",
			},
			"base_dns": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "Base DNs to search.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"primary_key": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Primary key attribute on the source server.",
			},
			"local_primary_key": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Primary key attribute on the local server.",
			},
			"use_anonymous_bind": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Whether to bind anonymously.",
			},
			"enabled": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Whether this LDAP configuration is enabled.",
			},
			"version": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "Configuration version.",
			},
			"level": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "Configuration level.",
			},
		},
	}
}

func resourceLinkConfiguration() *schema.Resource {

	return &schema.Resource{
		Description: "Resource for managing the Jans Link plugin configuration. This " +
			"resource cannot be created or deleted, only imported and updated.",
		CreateContext: resourceBlockCreate,
		ReadContext:   resourceLinkConfigurationRead,
		UpdateContext: resourceLinkConfigurationUpdate,
		DeleteContext: resourceUntrackOnDelete,
		Importer: &schema.ResourceImporter{
			StateContext: schema.ImportStatePassthroughContext,
		},
		Schema: map[string]*schema.Schema{
			"source_configs": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "Source LDAP server configurations.",
				Elem:        linkLdapConfigurationSchema(),
			},
			"inum_config": {
				Type:        schema.TypeList,
				Optional:    true,
				MaxItems:    1,
				Description: "Inum LDAP server configuration.",
				Elem:        linkLdapConfigurationSchema(),
			},
			"target_config": {
				Type:        schema.TypeList,
				Optional:    true,
				MaxItems:    1,
				Description: "Target LDAP server configuration.",
				Elem:        linkLdapConfigurationSchema(),
			},
			"ldap_search_size_limit": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "LDAP search size limit.",
			},
			"key_attributes": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "Key attributes.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"key_object_classes": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "Key object classes.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"source_attributes": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "Source attributes.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"custom_ldap_filter": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Custom LDAP filter applied to the source search.",
			},
			"update_method": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Method used to update entries.",
			},
			"default_inum_server": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Whether this is the default inum server.",
			},
			"keep_external_person": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Whether to keep externally managed persons.",
			},
			"use_search_limit": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Whether to apply the LDAP search size limit.",
			},
			"attribute_mapping": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "Mapping of source to destination attributes.",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"source": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "Source attribute name.",
						},
						"destination": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "Destination attribute name.",
						},
					},
				},
			},
			"snapshot_folder": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Folder where snapshots are stored.",
			},
			"snapshot_max_count": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "Maximum number of snapshots to keep.",
			},
			"base_dn": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Base DN of the Link configuration.",
			},
			"person_object_class_types": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "Person object class types.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"person_custom_object_class": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Person custom object class.",
			},
			"contact_object_class_types": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "Contact object class types.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"allow_person_modification": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Whether person modification is allowed.",
			},
			"supported_user_status": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "Supported user status values.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"logging_level": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Logging level for the Link server.",
			},
			"logging_layout": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Logging layout used by the Link server.",
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
			"clean_service_interval": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "Time interval for the clean up service in seconds.",
			},
			"link_enabled": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Whether the Link service is enabled.",
			},
			"server_ip_address": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "IP address of the server running the Link service.",
			},
			"polling_interval": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Interval between synchronization runs.",
			},
			"last_update": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "Timestamp of the last synchronization run.",
			},
			"last_update_count": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "Number of entries processed in the last synchronization run.",
			},
			"problem_count": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "Number of problems encountered in the last synchronization run.",
			},
			"use_local_cache": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value to indicate if Local Cache is to be used.",
			},
		},
	}
}

func resourceLinkConfigurationRead(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var diags diag.Diagnostics

	config, err := c.GetLinkConfiguration(ctx)
	if err != nil {
		return diag.FromErr(err)
	}

	if err := toSchemaResource(d, config); err != nil {
		return diag.FromErr(err)
	}

	d.SetId("jans_link_configuration")

	return diags
}

func resourceLinkConfigurationUpdate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	config, err := c.GetLinkConfiguration(ctx)
	if err != nil {
		return diag.FromErr(err)
	}

	if err := mergeFromSchemaResource(d, config); err != nil {
		return diag.FromErr(err)
	}

	if _, err := c.UpdateLinkConfiguration(ctx, config); err != nil {
		return diag.FromErr(err)
	}

	return resourceLinkConfigurationRead(ctx, d, meta)
}
