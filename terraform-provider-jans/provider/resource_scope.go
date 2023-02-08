package provider

import (
	"context"

	"github.com/hashicorp/go-cty/cty"
	"github.com/hashicorp/terraform-plugin-log/tflog"
	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/moabu/terraform-provider-jans/jans"
)

func resourceScope() *schema.Resource {

	return &schema.Resource{
		Description:   "Resource for managing OAuth scopes",
		CreateContext: resourceScopeCreate,
		ReadContext:   resourceScopeRead,
		UpdateContext: resourceScopeUpdate,
		DeleteContext: resourceScopeDelete,
		Importer: &schema.ResourceImporter{
			StateContext: schema.ImportStatePassthroughContext,
		},
		Schema: map[string]*schema.Schema{
			"dn": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "",
			},
			"inum": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "Unique id identifying the .",
			},
			"description": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "A human-readable string describing the scope.",
			},
			"display_name": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "A human-readable name of the scope.",
			},
			"scope_id": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "The base64url encoded id.",
			},
			"icon_url": {
				Type:     schema.TypeString,
				Optional: true,
				Description: `A URL for a graphic icon representing the scope. The referenced icon MAY be used by the authorization server 
						in any user interface it presents to the resource owner.`,
			},
			"scope_type": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "The scopes type associated with Access Tokens determine what resources will.",
				ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

					enums := []string{"openid", "dynamic", "uma", "spontaneous", "oauth"}
					return validateEnum(v, enums)
				},
			},
			"claims": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "Claim attributes associated with the scope.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"default_scope": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value to specify default scope.",
			},
			"group_claims": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Specifies if the scope is group claims.",
			},
			"dynamic_scope_scripts": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "Dynamic Scope Scripts associated with the scope.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"uma_authorization_policies": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "Policies associated with scopes.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"attributes": {
				Type:        schema.TypeList,
				MaxItems:    1,
				Optional:    true,
				Description: "ScopeAttributes",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"spontaneous_client_scopes": {
							Type:        schema.TypeList,
							Optional:    true,
							Description: "",
							Elem: &schema.Schema{
								Type: schema.TypeString,
							},
						},
						"show_in_configuration_endpoint": {
							Type:        schema.TypeBool,
							Optional:    true,
							Description: "",
						},
					},
				},
			},
			"creator_id": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Id of the scope creator. If creator is client then client_id if user then user_id",
			},
			"creator_type": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Scope creator type",
				ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

					enums := []string{"NONE", "CLIENT", "USER", "AUTO"}
					return validateEnum(v, enums)
				},
			},
			"creation_date": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Scope creation date time.",
			},
			"creator_attributes": {
				Type:        schema.TypeMap,
				Optional:    true,
				Description: "Stores creator attributes",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"uma_type": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Specifies if the scope is of type UMA.",
			},
			"deletable": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Specifies if the scope can be deleted.",
			},
			"expiration_date": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Expiry date of the Scope.",
			},
			"base_dn": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "Base distinguished name of the scope.",
			},
			"clients": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "Clients associated with the scope.",
				Elem:        resourceOidcClient(),
			},
		},
	}
}

func resourceScopeCreate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var scope jans.Scope
	if err := fromSchemaResource(d, &scope); err != nil {
		return diag.FromErr(err)
	}

	tflog.Debug(ctx, "Creating new Scope")
	newScope, err := c.CreateScope(ctx, &scope)
	if err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "New Scope created", map[string]interface{}{"inum": newScope.Inum})

	d.SetId(newScope.Inum)

	return resourceScopeRead(ctx, d, meta)
}

func resourceScopeRead(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var diags diag.Diagnostics

	inum := d.Id()
	scope, err := c.GetScope(ctx, inum)
	if err != nil {
		return handleNotFoundError(ctx, err, d)
	}

	if err := toSchemaResource(d, scope); err != nil {
		return diag.FromErr(err)
	}
	d.SetId(scope.Inum)

	return diags

}

func resourceScopeUpdate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var scope jans.Scope
	if err := fromSchemaResource(d, &scope); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "Updating Scope", map[string]interface{}{"inum": scope.Inum})
	if _, err := c.UpdateScope(ctx, &scope); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "Scope updated", map[string]interface{}{"inum": scope.Inum})

	return resourceScopeRead(ctx, d, meta)
}

func resourceScopeDelete(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	inum := d.Id()
	tflog.Debug(ctx, "Deleting Scope", map[string]interface{}{"inum": inum})
	if err := c.DeleteScope(ctx, inum); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "Scope deleted", map[string]interface{}{"inum": inum})

	return resourceScopeRead(ctx, d, meta)
}
