package jans

import (
        "context"

        "github.com/hashicorp/terraform-plugin-log/tflog"
        "github.com/hashicorp/terraform-plugin-sdk/v2/diag"
        "github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
)

func ResourceShibbolethTrustedSP() *schema.Resource {
        return &schema.Resource{
                CreateContext: resourceShibbolethTrustedSPCreate,
                ReadContext:   resourceShibbolethTrustedSPRead,
                UpdateContext: resourceShibbolethTrustedSPUpdate,
                DeleteContext: resourceShibbolethTrustedSPDelete,
                Importer: &schema.ResourceImporter{
                        StateContext: schema.ImportStatePassthroughContext,
                },
                Schema: map[string]*schema.Schema{
                        "entity_id": {
                                Type:        schema.TypeString,
                                Required:    true,
                                ForceNew:    true,
                                Description: "The entity ID of the Service Provider",
                        },
                        "name": {
                                Type:        schema.TypeString,
                                Required:    true,
                                Description: "Display name for the Service Provider",
                        },
                        "description": {
                                Type:        schema.TypeString,
                                Optional:    true,
                                Description: "Description of the Service Provider",
                        },
                        "metadata_url": {
                                Type:        schema.TypeString,
                                Optional:    true,
                                Description: "URL to fetch SP metadata from",
                        },
                        "metadata_xml": {
                                Type:        schema.TypeString,
                                Optional:    true,
                                Description: "SP metadata XML content",
                        },
                        "enabled": {
                                Type:        schema.TypeBool,
                                Optional:    true,
                                Default:     true,
                                Description: "Whether the trust relationship is enabled",
                        },
                        "released_attributes": {
                                Type:        schema.TypeList,
                                Optional:    true,
                                Description: "List of attribute names to release to this SP",
                                Elem: &schema.Schema{
                                        Type: schema.TypeString,
                                },
                        },
                        "name_id_format": {
                                Type:        schema.TypeString,
                                Optional:    true,
                                Default:     "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified",
                                Description: "SAML NameID format to use",
                        },
                },
        }
}

func resourceShibbolethTrustedSPCreate(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
        c := meta.(*Client)

        sp := &TrustedServiceProvider{
                EntityId:     d.Get("entity_id").(string),
                Name:         d.Get("name").(string),
                Description:  d.Get("description").(string),
                MetadataUrl:  d.Get("metadata_url").(string),
                MetadataXml:  d.Get("metadata_xml").(string),
                Enabled:      d.Get("enabled").(bool),
                NameIdFormat: d.Get("name_id_format").(string),
        }

        if v, ok := d.GetOk("released_attributes"); ok {
                attrs := make([]string, 0)
                for _, a := range v.([]interface{}) {
                        attrs = append(attrs, a.(string))
                }
                sp.ReleasedAttributes = attrs
        }

        tflog.Debug(ctx, "Creating Shibboleth trusted service provider", map[string]interface{}{
                "entity_id": sp.EntityId,
        })

        result, err := c.CreateTrustedServiceProvider(ctx, sp)
        if err != nil {
                return diag.FromErr(err)
        }

        d.SetId(result.EntityId)

        return resourceShibbolethTrustedSPRead(ctx, d, meta)
}

func resourceShibbolethTrustedSPRead(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
        c := meta.(*Client)

        entityId := d.Id()

        tflog.Debug(ctx, "Reading Shibboleth trusted service provider", map[string]interface{}{
                "entity_id": entityId,
        })

        sp, err := c.GetTrustedServiceProvider(ctx, entityId)
        if err != nil {
                return handleNotFoundError(err, d)
        }

        if err := d.Set("entity_id", sp.EntityId); err != nil {
                return diag.FromErr(err)
        }
        if err := d.Set("name", sp.Name); err != nil {
                return diag.FromErr(err)
        }
        if err := d.Set("description", sp.Description); err != nil {
                return diag.FromErr(err)
        }
        if err := d.Set("metadata_url", sp.MetadataUrl); err != nil {
                return diag.FromErr(err)
        }
        if err := d.Set("metadata_xml", sp.MetadataXml); err != nil {
                return diag.FromErr(err)
        }
        if err := d.Set("enabled", sp.Enabled); err != nil {
                return diag.FromErr(err)
        }
        if err := d.Set("released_attributes", sp.ReleasedAttributes); err != nil {
                return diag.FromErr(err)
        }
        if err := d.Set("name_id_format", sp.NameIdFormat); err != nil {
                return diag.FromErr(err)
        }

        return nil
}

func resourceShibbolethTrustedSPUpdate(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
        c := meta.(*Client)

        sp := &TrustedServiceProvider{
                EntityId:     d.Get("entity_id").(string),
                Name:         d.Get("name").(string),
                Description:  d.Get("description").(string),
                MetadataUrl:  d.Get("metadata_url").(string),
                MetadataXml:  d.Get("metadata_xml").(string),
                Enabled:      d.Get("enabled").(bool),
                NameIdFormat: d.Get("name_id_format").(string),
        }

        if v, ok := d.GetOk("released_attributes"); ok {
                attrs := make([]string, 0)
                for _, a := range v.([]interface{}) {
                        attrs = append(attrs, a.(string))
                }
                sp.ReleasedAttributes = attrs
        }

        tflog.Debug(ctx, "Updating Shibboleth trusted service provider", map[string]interface{}{
                "entity_id": sp.EntityId,
        })

        _, err := c.UpdateTrustedServiceProvider(ctx, sp)
        if err != nil {
                return diag.FromErr(err)
        }

        return resourceShibbolethTrustedSPRead(ctx, d, meta)
}

func resourceShibbolethTrustedSPDelete(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
        c := meta.(*Client)

        entityId := d.Id()

        tflog.Debug(ctx, "Deleting Shibboleth trusted service provider", map[string]interface{}{
                "entity_id": entityId,
        })

        if err := c.DeleteTrustedServiceProvider(ctx, entityId); err != nil {
                return diag.FromErr(err)
        }

        d.SetId("")
        return nil
}
