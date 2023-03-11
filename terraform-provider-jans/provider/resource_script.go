package provider

import (
	"context"

	"github.com/hashicorp/go-cty/cty"
	"github.com/hashicorp/terraform-plugin-log/tflog"
	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func resourceScript() *schema.Resource {

	return &schema.Resource{
		Description:   "Resource for managing custom scripts",
		CreateContext: resourceScriptCreate,
		ReadContext:   resourceScriptRead,
		UpdateContext: resourceScriptUpdate,
		DeleteContext: resourceScriptDelete,
		Importer: &schema.ResourceImporter{
			StateContext: schema.ImportStatePassthroughContext,
		},
		Schema: map[string]*schema.Schema{
			"dn": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "",
			},
			"inum": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "XRI i-number. Identifier to uniquely identify the script.",
			},
			"name": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "Custom script name. Should contain only letters, digits and underscores.",
				ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {
					return validateRegex(v, "^[a-zA-Z0-9_\\-\\:\\/\\.]{1,60}$")
				},
			},
			"aliases": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "List of possible aliases for the custom script.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"description": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Details describing the script.",
			},
			"script": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "Actual script.",
			},
			"script_type": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "Type of script.",
				ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

					enums := []string{
						"person_authentication", "introspection", "resource_owner_password_credentials",
						"application_session", "cache_refresh", "client_registration", "id_generator",
						"uma_rpt_policy", "uma_rpt_claims", "uma_claims_gathering", "consent_gathering",
						"dynamic_scope", "spontaneous_scope", "end_session", "post_authn", "scim",
						"ciba_end_user_notification", "revoke_token", "persistence_extension", "idp",
						"discovery", "update_token", "config_api",
					}
					return validateEnum(v, enums)
				},
			},
			"programming_language": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "Programming language of the custom script.",
				ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

					enums := []string{"python", "java"}
					return validateEnum(v, enums)
				},
			},
			"module_properties": {
				Type:        schema.TypeList,
				Required:    true,
				Description: "Module-level properties applicable to the script.",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"value1": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "",
						},
						"value2": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "",
						},
						"description": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "",
						},
					},
				},
			},
			"configuration_properties": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "Configuration properties applicable to the script.",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"value1": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "",
						},
						"value2": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "",
						},
						"description": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "",
						},
						"hide": {
							Type:        schema.TypeBool,
							Optional:    true,
							Description: "",
						},
					},
				},
			},
			"level": {
				Type:        schema.TypeInt,
				Required:    true,
				Description: "Script level.",
			},
			"revision": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "Update revision number of the script.",
			},
			"enabled": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "boolean value indicating if script enabled.",
			},
			"script_error": {
				Type:        schema.TypeList,
				Computed:    true,
				Description: "Possible errors assosiated with the script.",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"raised_at": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "",
						},
						"stack_trace": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "",
						},
					},
				},
			},
			"modified": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "boolean value indicating if the script is modified.",
			},
			"internal": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "boolean value indicating if the script is internal.",
			},
			"location_type": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "",
			},
			"base_dn": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "",
			},
		},
	}
}

func resourceScriptCreate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var script jans.Script
	if err := fromSchemaResource(d, &script); err != nil {
		return diag.FromErr(err)
	}

	tflog.Debug(ctx, "Creating new Script")
	newScript, err := c.CreateScript(ctx, &script)
	if err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "New Script created", map[string]interface{}{"inum": newScript.Inum})

	d.SetId(newScript.Inum)

	return resourceScriptRead(ctx, d, meta)
}

func resourceScriptRead(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var diags diag.Diagnostics

	inum := d.Id()
	script, err := c.GetScript(ctx, inum)
	if err != nil {
		return handleNotFoundError(ctx, err, d)
	}

	if err := toSchemaResource(d, script); err != nil {
		return diag.FromErr(err)
	}
	d.SetId(script.Inum)

	return diags

}

func resourceScriptUpdate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var script jans.Script
	if err := fromSchemaResource(d, &script); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "Updating Script", map[string]interface{}{"inum": script.Inum})
	if _, err := c.UpdateScript(ctx, &script); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "Script updated", map[string]interface{}{"inum": script.Inum})

	return resourceScriptRead(ctx, d, meta)
}

func resourceScriptDelete(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	inum := d.Id()
	tflog.Debug(ctx, "Deleting Script", map[string]interface{}{"inum": inum})
	if err := c.DeleteScript(ctx, inum); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "Script deleted", map[string]interface{}{"inum": inum})

	return resourceScriptRead(ctx, d, meta)
}
