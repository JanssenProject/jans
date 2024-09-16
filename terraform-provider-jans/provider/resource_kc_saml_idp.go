package provider

import (
	"context"

	"github.com/hashicorp/go-cty/cty"
	"github.com/hashicorp/terraform-plugin-log/tflog"
	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func resourceKCSamlIDP() *schema.Resource {
	return &schema.Resource{
		Description:   "Resource for managing Keycloak SAML Identity Provider.",
		CreateContext: resourceKCSamlIDPCreate,
		ReadContext:   resourceKCSamlIDPRead,
		UpdateContext: resourceKCSamlIDPUpdate,
		DeleteContext: resourceKCSamlIDPDelete,
		Schema: map[string]*schema.Schema{
			"dn": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "DN of the identity provider.",
			},
			"inum": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "Inum of the identity provider.",
			},
			"creator_id": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "Creator ID of the identity provider.",
			},
			"name": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "Name of the identity provider.",
			},
			"display_name": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "Display name of the identity provider.",
			},
			"description": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "Description of the identity provider.",
			},
			"realm": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "Realm of the identity provider.",
			},
			"enabled": {
				Type:        schema.TypeBool,
				Optional:    true,
				Default:     true,
				Description: "Status of the identity provider.",
			},
			"signing_certificate": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Signing certificate of the identity provider.",
			},
			"validate_signature": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Validate signature of the identity provider.",
			},
			"single_logout_service_url": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Single logout service URL of the identity provider.",
			},
			"name_id_policy_format": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Name ID policy format of the identity provider.",
			},
			"principal_attribute": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Principal attribute of the identity provider.",
			},
			"principal_type": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Principal type of the identity provider.",
			},
			"idp_entity_id": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "IDP entity ID of the identity provider.",
			},
			"single_sign_on_service_url": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Single sign on service URL of the identity provider.",
			},
			"encryption_public_key": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Encryption public key of the identity provider.",
			},
			"provider_id": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Provider ID of the identity provider.",
			},
			"trust_email": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Trust email of the identity provider.",
			},
			"store_token": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Store token of the identity provider.",
			},
			"add_read_token_role_on_create": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Add read token role on create of the identity provider.",
			},
			"authenticate_by_default": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Authenticate by default of the identity provider.",
			},
			"link_only": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Link only of the identity provider.",
			},
			"first_broker_login_flow_alias": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "First broker login flow alias of the identity provider.",
			},
			"post_broker_login_flow_alias": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Post broker login flow alias of the identity provider.",
			},
			"sp_meta_data_url": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "SP metadata URL of the identity provider.",
			},
			"sp_meta_data_location": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "SP metadata location of the identity provider.",
			},
			"idp_meta_data_url": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "IDP metadata URL of the identity provider.",
			},
			"idp_meta_data_location": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "IDP metadata location of the identity provider.",
			},
			"status": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Status of the identity provider.",
				ValidateDiagFunc: func(v interface{}, _ cty.Path) diag.Diagnostics {
					enums := []string{"active", "inactive", "expired", "register"}
					return validateEnum(v, enums)
				},
			},
			"validation_status": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Validation status of the identity provider.",
				ValidateDiagFunc: func(v interface{}, _ cty.Path) diag.Diagnostics {
					enums := []string{"In Progress", "Success", "Scheduled", "Failed"}
					return validateEnum(v, enums)
				},
			},
			"validation_log": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "Validation log of the identity provider.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"base_dn": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Base DN of the identity provider.",
			},
			"valid_until": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Valid until of the identity provider.",
			},
			"cache_duration": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Cache duration of the identity provider.",
			},
			"metadata_file": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Metadata file location for the trust relationship.",
			},
		},
	}
}

func resourceKCSamlIDPCreate(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {

	c := meta.(*jans.Client)

	idp, f, err := handleMetadataFile[jans.IdentityProvider](d)
	if err != nil {
		return diag.FromErr(err)
	}

	ip, err := c.CreateIDP(ctx, idp, f)
	if err != nil {
		return diag.FromErr(err)
	}

	tflog.Debug(ctx, "New Identity Provider created", map[string]interface{}{"inum": ip.Inum})

	return resourceKCSamlIDPRead(ctx, d, meta)
}

func resourceKCSamlIDPRead(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
	c := meta.(*jans.Client)

	ip, err := c.GetIDP(ctx, d.Get("inum").(string))
	if err != nil {
		return handleNotFoundError(ctx, err, d)
	}

	if err := toSchemaResource(d, ip); err != nil {
		return diag.FromErr(err)
	}

	tflog.Debug(ctx, "Identity Provider read")

	return nil
}

func resourceKCSamlIDPDelete(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {
	c := meta.(*jans.Client)

	inum := d.Get("inum").(string)
	tflog.Debug(ctx, "Deleting Identity Provider", map[string]any{"inum": inum})
	if err := c.DeleteIDP(ctx, inum); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "Identity Provider deleted", map[string]any{"inum": inum})

	return resourceKCSamlIDPRead(ctx, d, meta)
}

func resourceKCSamlIDPUpdate(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
	c := meta.(*jans.Client)

	idp, f, err := handleMetadataFile[jans.IdentityProvider](d)
	if err != nil {
		return diag.FromErr(err)
	}

	ip, err := c.UpdateIDP(ctx, idp, f)
	if err != nil {
		return diag.FromErr(err)
	}

	tflog.Debug(ctx, "Identity Provider updated", map[string]interface{}{"inum": ip.Inum})

	return resourceKCSamlIDPRead(ctx, d, meta)
}
