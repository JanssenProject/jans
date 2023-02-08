package provider

import (
	"context"

	"github.com/hashicorp/terraform-plugin-log/tflog"
	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/moabu/terraform-provider-jans/jans"
)

func resourceLDAPDatabaseConfiguration() *schema.Resource {

	return &schema.Resource{
		CreateContext: resourceLDAPDatabaseConfigurationCreate,
		ReadContext:   resourceLDAPDatabaseConfigurationRead,
		UpdateContext: resourceLDAPDatabaseConfigurationUpdate,
		DeleteContext: resourceLDAPDatabaseConfigurationDelete,
		Importer: &schema.ResourceImporter{
			StateContext: schema.ImportStatePassthroughContext,
		},
		Schema: map[string]*schema.Schema{
			"config_id": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "Unique identifier - Name Example: auth_ldap_server",
			},
			"bind_dn": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "This contains the username to connect to the backend server. You need to use full DN here. As for example, cn=jans,dc=company,dc=org.",
			},
			"bind_password": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "Ldap password for binding.",
			},
			"servers": {
				Type:        schema.TypeList,
				Required:    true,
				Description: "List of LDAP authentication servers.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"max_connections": {
				Type:        schema.TypeInt,
				Required:    true,
				Description: "This value defines the maximum number of connections that are allowed to read the backend Active Directory/LDAP server.",
			},
			"use_ssl": {
				Type:        schema.TypeBool,
				Required:    true,
				Description: "Enable SSL communication between Jans Server and LDAP server.",
			},
			"base_dns": {
				Type:        schema.TypeList,
				Required:    true,
				Description: "List contains the location of the Active Directory/LDAP tree from where the Gluu Server shall read the user information.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"primary_key": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "Used to search and bind operations in configured LDAP server. Example: SAMAccountName,uid, email",
			},
			"local_primary_key": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "Used to search local user entry in Gluu Server’s internal LDAP directory. Example: uid, email",
			},
			"use_anonymous_bind": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value used to indicate if the LDAP Server will allow anonymous bind request.",
			},
			"enabled": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value used to indicate if the LDAP Server is enabled. Do not use this unless the server administrator has entered all the required values.",
			},
			"version": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "LDAP server version.",
			},
			"level": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "A string that indicates the level.",
			},
		},
	}
}

func resourceLDAPDatabaseConfigurationCreate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	var cfg jans.LDAPDBConfiguration
	if err := fromSchemaResource(d, &cfg); err != nil {
		return diag.FromErr(err)
	}

	c := meta.(*jans.Client)

	tflog.Debug(ctx, "Creating new LDAP database configuration")
	newCfg, err := c.CreateLDAPDBConfiguration(ctx, &cfg)
	if err != nil {
		return diag.FromErr(err)
	}

	d.SetId(newCfg.ConfigId)
	tflog.Debug(ctx, "New LDAP database configuration created", map[string]interface{}{"id": newCfg.ConfigId})

	return resourceLDAPDatabaseConfigurationRead(ctx, d, meta)
}

func resourceLDAPDatabaseConfigurationRead(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	var diags diag.Diagnostics

	c := meta.(*jans.Client)

	configId := d.Id()
	config, err := c.GetLDAPDBConfiguration(ctx, configId)
	if err != nil {
		return handleNotFoundError(ctx, err, d)
	}

	if err := toSchemaResource(d, config); err != nil {
		return diag.FromErr(err)
	}

	d.SetId(config.ConfigId)

	return diags
}

func resourceLDAPDatabaseConfigurationUpdate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var cfg jans.LDAPDBConfiguration
	if err := fromSchemaResource(d, &cfg); err != nil {
		return diag.FromErr(err)
	}

	tflog.Debug(ctx, "Updating existing LDAP database configuration", map[string]interface{}{"id": cfg.ConfigId})
	if _, err := c.UpdateLDAPDBConfiguration(ctx, &cfg); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "LDAP database configuration updated", map[string]interface{}{"id": cfg.ConfigId})

	return resourceLDAPDatabaseConfigurationRead(ctx, d, meta)
}

func resourceLDAPDatabaseConfigurationDelete(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	configId := d.Id()
	tflog.Debug(ctx, "Deleting existing LDAP database configuration", map[string]interface{}{"id": configId})
	if err := c.DeleteLDAPDBConfiguration(ctx, configId); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "LDAP database configuration deleted", map[string]interface{}{"id": configId})

	return resourceLDAPDatabaseConfigurationRead(ctx, d, meta)
}
