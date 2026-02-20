package jans

import (
        "context"

        "github.com/hashicorp/terraform-plugin-log/tflog"
        "github.com/hashicorp/terraform-plugin-sdk/v2/diag"
        "github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
)

func ResourceShibbolethConfiguration() *schema.Resource {
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
        c := meta.(*Client)

        config := &ShibbolethIdpConfiguration{
                EntityId: d.Get("entity_id").(string),
                Scope:    d.Get("scope").(string),
                Enabled:  d.Get("enabled").(bool),
        }

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
        c := meta.(*Client)

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
        c := meta.(*Client)

        config := &ShibbolethIdpConfiguration{
                EntityId: d.Get("entity_id").(string),
                Scope:    d.Get("scope").(string),
                Enabled:  d.Get("enabled").(bool),
                Revision: d.Get("revision").(int),
        }

        if v, ok := d.GetOk("metadata_providers"); ok {
                providers := make([]string, 0)
                for _, p := range v.([]interface{}) {
                        providers = append(providers, p.(string))
                }
                config.MetadataProviders = providers
        }

        tflog.Debug(ctx, "Updating Shibboleth configuration")
        _, err := c.UpdateShibbolethConfiguration(ctx, config)
        if err != nil {
                return diag.FromErr(err)
        }

        return resourceShibbolethConfigurationRead(ctx, d, meta)
}

func resourceShibbolethConfigurationDelete(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
        tflog.Debug(ctx, "Shibboleth configuration cannot be deleted, disabling instead")

        c := meta.(*Client)

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
