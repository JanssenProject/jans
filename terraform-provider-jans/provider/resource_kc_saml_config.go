package provider

import (
	"context"

	"github.com/hashicorp/terraform-plugin-log/tflog"
	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func resourceKCSamlConfiguration() *schema.Resource {

	return &schema.Resource{
		Description:   "Resource for managing Keycloak SAML Configuration.",
		CreateContext: resourceKCSamlConfigurationCreate,
		ReadContext:   resourceKCSamlConfigurationRead,
		UpdateContext: resourceKCSamlConfigurationUpdate,
		DeleteContext: resourceUntrackOnDelete,
		Importer: &schema.ResourceImporter{
			StateContext: schema.ImportStatePassthroughContext,
		},
		Schema: map[string]*schema.Schema{
			"application_name": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Application name.",
			},
			"saml_trust_relationship_dn": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "SAML trust relationship DN.",
			},
			"trusted_idp_dn": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Trusted IDP DN.",
			},
			"enabled": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Whether the configuration should be enabled or not.",
			},
			"slected_idp": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Selected IDP.",
			},
			"server_url": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Server URL.",
			},
			"realm": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Realm.",
			},
			"client_id": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Client ID.",
			},
			"client_secret": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Client Secret.",
			},
			"grant_type": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Grant Type.",
			},
			"scope": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Scope.",
			},
			"username": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Username.",
			},
			"password": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Password.",
			},
			"sp_metadata_url": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "SP Metadata URL.",
			},
			"token_url": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Token URL.",
			},
			"idp_url": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "IDP URL.",
			},
			"ext_idp_token_url": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Ext IDP Token URL.",
			},
			"ext_idp_redirect_url": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Ext IDP Redirect URL.",
			},
			"idp_metadata_import_url": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "IDP Metadata Import URL.",
			},
			"idp_root_dir": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "IDP Root Directory.",
			},
			"idp_metadata_dir": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "IDP Metadata Directory.",
			},
			"idp_metadata_temp_dir": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "IDP Metadata Temporary Directory.",
			},
			"idp_metadata_file": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "IDP Metadata File.",
			},
			"sp_metadata_dir": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "SP Metadata Directory.",
			},
			"sp_metadata_temp_dir": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "SP Metadata Temporary Directory.",
			},
			"sp_metadata_file": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "SP Metadata File.",
			},
			"ignore_validation": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Ignore Validation.",
			},
			"set_config_default_value": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Set Config Default Value.",
			},
			"idp_metadata_mandatory_attributes": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "IDP Metadata Mandatory Attributes.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"kc_attributes": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "KC Attributes.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"kc_saml_config": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "KC SAML Config..",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
		},
	}
}

func resourceKCSamlConfigurationCreate(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {

	c := meta.(*jans.Client)
	var saml jans.KCSAMLConfiguration

	if err := fromSchemaResource(d, &saml); err != nil {
		return diag.Errorf("failed to read resource: %s", err.Error())
	}

	tflog.Debug(ctx, "Creating new KCSAMLConfiguration", map[string]interface{}{"message": saml})
	if _, err := c.CreateKCSAMLConfiguration(ctx, &saml); err != nil {
		return diag.Errorf("failed to create KCSAMLConfiguration: %s", err.Error())
	}

	return resourceKCSamlConfigurationRead(ctx, d, meta)
}

func resourceKCSamlConfigurationRead(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {

	c := meta.(*jans.Client)

	saml, err := c.GetKCSAMLConfiguration(ctx)
	if err != nil {
		return diag.Errorf("failed to get KCSAMLConfiguration: %s", err.Error())
	}

	if err := toSchemaResource(d, saml); err != nil {
		return diag.Errorf("failed to write resource: %s", err.Error())
	}

	tflog.Debug(ctx, "KCSAMLConfiguration read")

	return nil
}

func resourceKCSamlConfigurationUpdate(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {

	c := meta.(*jans.Client)

	var saml jans.KCSAMLConfiguration
	patches, err := patchFromResourceData(d, &saml)
	if err != nil {
		return diag.Errorf("failed to read resource: %s", err.Error())
	}

	tflog.Debug(ctx, "Updating KCSAMLConfiguration", map[string]interface{}{"message": saml})
	if _, err := c.PatchKCSAMLConfiguration(ctx, patches); err != nil {
		return diag.Errorf("failed to update KCSAMLConfiguration: %s", err.Error())
	}

	tflog.Debug(ctx, "KCSAMLConfiguration updated")

	return resourceKCSamlConfigurationRead(ctx, d, meta)
}
