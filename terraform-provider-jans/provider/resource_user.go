package provider

import (
	"context"

	"github.com/hashicorp/terraform-plugin-log/tflog"
	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func resourceUser() *schema.Resource {

	return &schema.Resource{
		Description:   "Resource represents a user resource. See section 4.1 of RFC 7643.",
		CreateContext: resourceUserCreate,
		ReadContext:   resourceUserRead,
		UpdateContext: resourceUserUpdate,
		DeleteContext: resourceUserDelete,
		Importer: &schema.ResourceImporter{
			StateContext: schema.ImportStatePassthroughContext,
		},
		Schema: map[string]*schema.Schema{
			"id": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "The unique identifier for the user.",
			},
			"schemas": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "A list of URIs of the schemas used to define the attributes of the user.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"meta": {
				Type:        schema.TypeList,
				Computed:    true,
				Description: "A complex type that contains meta attributes associated with the resource.",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"resource_type": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "The resource type of the user.",
						},
						"location": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "The URI of the user.",
						},
						"created": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "The date and time the user was created.",
						},
						"last_modified": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "The date and time the user was last modified.",
						},
					},
				},
			},
			"external_id": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Identifier of the resource useful from the perspective of the provisioning client. See section 3.1 of RFC 7643",
			},
			"user_name": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Identifier for the user, typically used by the user to directly authenticate (id and externalId are opaque identifiers generally not known by users)",
			},
			"name": {
				Type:        schema.TypeList,
				Optional:    true,
				MaxItems:    1,
				Description: "",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"family_name": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "",
						},
						"given_name": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "",
						},
						"middle_name": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "",
						},
						"honorific_prefix": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "A 'title' like 'Ms.', 'Mrs.'",
						},
						"honorific_suffix": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "Name suffix, like 'Junior', 'The great', 'III'",
						},
						"formatted": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "Full name, including all middle names, titles, and suffixes as appropriate",
						},
					},
				},
			},
			"display_name": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Name of the user suitable for display to end-users",
			},
			"nick_name": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Casual way to address the user in real life",
			},
			"profile_url": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "URI pointing to a location representing the User's online profile",
			},
			"title": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: " Example: Vice President",
			},
			"user_type": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Used to identify the relationship between the organization and the user Example: Contractor",
			},
			"preferred_language": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Preferred language as used in the Accept-Language HTTP header Example: en",
			},
			"locale": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Used for purposes of localizing items such as currency and dates Example: en-US",
			},
			"timezone": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: " Example: America/Los_Angeles",
			},
			"active": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "",
			},
			"password": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "",
			},
			"emails": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"value": {
							Type:        schema.TypeString,
							Required:    true,
							Description: " Example: johndoe@jans.io",
						},
						"type": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: " Example: work",
						},
						"display": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "",
						},
						"primary": {
							Type:        schema.TypeBool,
							Optional:    true,
							Description: "Denotes if this is the preferred e-mail among others, if any",
						},
					},
				},
			},
			"phone_numbers": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"value": {
							Type:        schema.TypeString,
							Required:    true,
							Description: " Example: +1-555-555-8377",
						},
						"type": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: " Example: fax",
						},
						"display": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "",
						},
						"primary": {
							Type:        schema.TypeBool,
							Optional:    true,
							Description: "Denotes if this is the preferred phone number among others, if any",
						},
					},
				},
			},
			"ims": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"value": {
							Type:        schema.TypeString,
							Required:    true,
							Description: "",
						},
						"type": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: " Example: gtalk",
						},
						"display": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "",
						},
						"primary": {
							Type:        schema.TypeBool,
							Optional:    true,
							Description: "Denotes if this is the preferred messaging addressed among others, if any",
						},
					},
				},
			},
			"photos": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"value": {
							Type:        schema.TypeString,
							Required:    true,
							Description: " Example: https://static.jans.io/profile.png",
						},
						"type": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: " Example: thumbnail",
						},
						"display": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "",
						},
						"primary": {
							Type:        schema.TypeBool,
							Optional:    true,
							Description: "Denotes if this is the preferred photo among others, if any",
						},
					},
				},
			},
			"addresses": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"formatted": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "Full mailing address, formatted for display or use with a mailing label",
						},
						"type": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "Example: home",
						},
						"street_address": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "Example: 56 Acacia Avenue",
						},
						"locality": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "City or locality of the address",
						},
						"region": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "State or region of the address",
						},
						"postal_code": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "Zip code",
						},
						"country": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "Country expressed in ISO 3166-1 'alpha-2' code format Example: UK",
						},
						"primary": {
							Type:        schema.TypeBool,
							Optional:    true,
							Description: "Denotes if this is the preferred address among others, if any",
						},
					},
				},
			},
			"groups": {
				Type:        schema.TypeList,
				Computed:    true,
				Description: "",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"value": {
							Type:        schema.TypeString,
							Required:    true,
							Description: "Group identifier Example: 180ee84f0671b1",
						},
						"type": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "Describes how the group membership was derived Example: direct",
						},
						"ref": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "URI associated to the group Example: https://nsfw.com/scim/restv1/v2/Groups/180ee84f0671b1",
						},
						"display": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: " Example: Cult managers",
						},
					},
				},
			},
			"entitlements": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"value": {
							Type:        schema.TypeString,
							Required:    true,
							Description: " Example: Stakeholder",
						},
						"type": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "",
						},
						"display": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "",
						},
						"primary": {
							Type:        schema.TypeBool,
							Optional:    true,
							Description: "Denotes if this is the preferred entitlement among others, if any",
						},
					},
				},
			},
			"roles": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"value": {
							Type:        schema.TypeString,
							Required:    true,
							Description: " Example: Project manager",
						},
						"type": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "",
						},
						"display": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "",
						},
						"primary": {
							Type:        schema.TypeBool,
							Optional:    true,
							Description: "Denotes if this is the preferred role among others, if any",
						},
					},
				},
			},
			"x509_certificates": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"value": {
							Type:        schema.TypeString,
							Required:    true,
							Description: "DER-encoded X.509 certificate",
						},
						"type": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "",
						},
						"display": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "",
						},
						"primary": {
							Type:        schema.TypeBool,
							Optional:    true,
							Description: "Denotes if this is the preferred certificate among others, if any",
						},
					},
				},
			},
			"extensions": {
				Type:        schema.TypeMap,
				Optional:    true,
				Description: "Extended attributes",
			},
		},
	}
}

func resourceUserCreate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var user jans.User
	if err := fromSchemaResource(d, &user); err != nil {
		return diag.FromErr(err)
	}

	tflog.Debug(ctx, "Creating new User")
	newUser, err := c.CreateUser(ctx, &user)
	if err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "New User created", map[string]interface{}{"id": newUser.ID})

	d.SetId(newUser.ID)

	return resourceUserRead(ctx, d, meta)
}

func resourceUserRead(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var diags diag.Diagnostics

	id := d.Id()
	user, err := c.GetUser(ctx, id)
	if err != nil {
		return handleNotFoundError(ctx, err, d)
	}

	if err := toSchemaResource(d, user); err != nil {
		return diag.FromErr(err)
	}
	d.SetId(user.ID)

	return diags

}

func resourceUserUpdate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var user jans.User
	if err := fromSchemaResource(d, &user); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "Updating User", map[string]interface{}{"id": user.ID})
	if _, err := c.UpdateUser(ctx, &user); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "User updated", map[string]interface{}{"id": user.ID})

	return resourceUserRead(ctx, d, meta)
}

func resourceUserDelete(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	id := d.Id()
	tflog.Debug(ctx, "Deleting User", map[string]interface{}{"id": id})
	if err := c.DeleteUser(ctx, id); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "User deleted", map[string]interface{}{"id": id})

	return resourceUserRead(ctx, d, meta)
}
