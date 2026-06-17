package provider

import (
	"context"

	"github.com/hashicorp/terraform-plugin-log/tflog"
	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func resourceShibbolethConfiguration() *schema.Resource {
	return &schema.Resource{
		CreateContext: resourceShibbolethConfigurationCreate,
		ReadContext:   resourceShibbolethConfigurationRead,
		UpdateContext: resourceShibbolethConfigurationUpdate,
		DeleteContext: resourceShibbolethConfigurationDelete,
		Importer: &schema.ResourceImporter{
			StateContext: schema.ImportStatePassthroughContext,
		},
		Schema: map[string]*schema.Schema{
			"entity_id": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "The entity ID of the Shibboleth Identity Provider",
			},
			"scope": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "The scope used for attribute release",
			},
			"enabled": {
				Type:        schema.TypeBool,
				Optional:    true,
				Default:     false,
				Description: "Whether the Shibboleth IDP is enabled",
			},
			"metadata_providers": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "List of metadata provider URLs",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"revision": {
				Type:        schema.TypeInt,
				Computed:    true,
				Description: "Configuration revision number",
			},
		},
	}
}

func resourceShibbolethConfigurationCreate(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
	c := meta.(*jans.Client)

	// Merge onto the current server config so fields managed elsewhere (trusted
	// service providers, attribute mappings) are preserved on the PUT.
	config, err := c.GetShibbolethConfiguration(ctx)
	if err != nil {
		return diag.FromErr(err)
	}

	config.EntityId = d.Get("entity_id").(string)
	config.Scope = d.Get("scope").(string)
	config.Enabled = d.Get("enabled").(bool)

	if v, ok := d.GetOk("metadata_providers"); ok {
		providers := make([]string, 0)
		for _, p := range v.([]interface{}) {
			providers = append(providers, p.(string))
		}
		config.MetadataProviders = providers
	}

	tflog.Debug(ctx, "Creating Shibboleth configuration")
	result, err := c.UpdateShibbolethConfiguration(ctx, config)
	if err != nil {
		return diag.FromErr(err)
	}

	d.SetId("shibboleth-idp-config")
	d.Set("revision", result.Revision)

	return resourceShibbolethConfigurationRead(ctx, d, meta)
}

func resourceShibbolethConfigurationRead(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
	c := meta.(*jans.Client)

	tflog.Debug(ctx, "Reading Shibboleth configuration")
	config, err := c.GetShibbolethConfiguration(ctx)
	if err != nil {
		return diag.FromErr(err)
	}

	if err := d.Set("entity_id", config.EntityId); err != nil {
		return diag.FromErr(err)
	}
	if err := d.Set("scope", config.Scope); err != nil {
		return diag.FromErr(err)
	}
	if err := d.Set("enabled", config.Enabled); err != nil {
		return diag.FromErr(err)
	}
	if err := d.Set("metadata_providers", config.MetadataProviders); err != nil {
		return diag.FromErr(err)
	}
	if err := d.Set("revision", config.Revision); err != nil {
		return diag.FromErr(err)
	}

	return nil
}

func resourceShibbolethConfigurationUpdate(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
	c := meta.(*jans.Client)

	// Merge onto the current server config so fields managed elsewhere (trusted
	// service providers, attribute mappings) are preserved on the PUT.
	config, err := c.GetShibbolethConfiguration(ctx)
	if err != nil {
		return diag.FromErr(err)
	}

	config.EntityId = d.Get("entity_id").(string)
	config.Scope = d.Get("scope").(string)
	config.Enabled = d.Get("enabled").(bool)

	if v, ok := d.GetOk("metadata_providers"); ok {
		providers := make([]string, 0)
		for _, p := range v.([]interface{}) {
			providers = append(providers, p.(string))
		}
		config.MetadataProviders = providers
	}

	tflog.Debug(ctx, "Updating Shibboleth configuration")
	if _, err := c.UpdateShibbolethConfiguration(ctx, config); err != nil {
		return diag.FromErr(err)
	}

	return resourceShibbolethConfigurationRead(ctx, d, meta)
}

func resourceShibbolethConfigurationDelete(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
	tflog.Debug(ctx, "Shibboleth configuration cannot be deleted, disabling instead")

	c := meta.(*jans.Client)

	config, err := c.GetShibbolethConfiguration(ctx)
	if err != nil {
		return diag.FromErr(err)
	}

	config.Enabled = false
	_, err = c.UpdateShibbolethConfiguration(ctx, config)
	if err != nil {
		return diag.FromErr(err)
	}

	d.SetId("")
	return nil
}
