package provider

import (
	"context"

	"github.com/hashicorp/go-cty/cty"
	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func resourceAttribute() *schema.Resource {

	return &schema.Resource{
		Description:   "Resource for managing attributes.",
		CreateContext: resourceAttributeCreate,
		ReadContext:   resourceAttributeRead,
		UpdateContext: resourceAttributeUpdate,
		DeleteContext: resourceAttributeDelete,
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
				Description: "XRI i-number. Identifier to uniquely identify the attribute.",
			},
			"name": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "Name of the attribute. Example: name, displayName, birthdate, email",
			},
			"display_name": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "",
			},
			"description": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "User friendly descriptive detail of attribute.",
			},
			"data_type": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "Data Type of attribute.",
				ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

					enums := []string{"string", "numeric", "boolean", "binary", "certificate", "date", "json"}
					return validateEnum(v, enums)
				},
			},
			"status": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "Attrubute status",
				ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

					enums := []string{"active", "inactive", "expired", "registered"}
					return validateEnum(v, enums)
				},
			},
			"lifetime": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "",
			},
			"source_attribute": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "",
			},
			"salt": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "",
			},
			"name_id_type": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "",
			},
			"origin": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "",
			},
			"edit_type": {
				Type:        schema.TypeList,
				Required:    true,
				Description: "GluuUserRole",
				Elem: &schema.Schema{
					Type: schema.TypeString,
					ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

						enums := []string{"admin", "owner", "manager", "user", "whitepages"}
						return validateEnum(v, enums)
					},
				},
			},
			"view_type": {
				Type:        schema.TypeList,
				Required:    true,
				Description: "GluuUserRole",
				Elem: &schema.Schema{
					Type: schema.TypeString,
					ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

						enums := []string{"admin", "owner", "manager", "user", "whitepages"}
						return validateEnum(v, enums)
					},
				},
			},
			"usage_type": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "GluuAttributeUsageType",
				Elem: &schema.Schema{
					Type: schema.TypeString,
					ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

						enums := []string{"openid"}
						return validateEnum(v, enums)
					},
				},
			},
			"claim_name": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "",
			},
			"see_also": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "",
			},
			"saml1_uri": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "",
			},
			"saml2_uri": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "",
			},
			"urn": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "",
			},
			"scim_custom_attr": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value indicating if the attribute is a SCIM custom attribute",
			},
			"ox_multi_valued_attribute": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value indicating if the attribute can hold multiple value.",
			},
			"attribute_validation": {
				Type:        schema.TypeList,
				Optional:    true,
				MaxItems:    1,
				Description: "Details of validations to be applied on the attribute",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"regexp": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "Reguar expression to be used to validate the dataType",
						},
						"min_length": {
							Type:        schema.TypeInt,
							Optional:    true,
							Description: "Minimum length of the attribute value",
						},
						"max_length": {
							Type:        schema.TypeInt,
							Optional:    true,
							Description: "Maximum length of the attribute value",
						},
					},
				},
			},
			"tooltip": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "",
			},
			"jans_hide_on_discovery": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value indicating if the attribute should be shown on that discovery page.",
			},
			"selected": {
				Type:     schema.TypeBool,
				Optional: true,
			},
			"custom": {
				Type:     schema.TypeBool,
				Optional: true,
			},
			"required": {
				Type:     schema.TypeBool,
				Optional: true,
			},
			"admin_can_access": {
				Type:     schema.TypeBool,
				Optional: true,
			},
			"admin_can_view": {
				Type:     schema.TypeBool,
				Optional: true,
			},
			"admin_can_edit": {
				Type:     schema.TypeBool,
				Optional: true,
			},
			"user_can_access": {
				Type:     schema.TypeBool,
				Optional: true,
			},
			"user_can_view": {
				Type:     schema.TypeBool,
				Optional: true,
			},
			"user_can_edit": {
				Type:     schema.TypeBool,
				Optional: true,
			},
			"white_pages_can_view": {
				Type:     schema.TypeBool,
				Optional: true,
			},
			"base_dn": {
				Type:     schema.TypeString,
				Computed: true,
			},
		},
	}

}

func resourceAttributeCreate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var attr jans.Attribute
	if err := fromSchemaResource(d, &attr); err != nil {
		return diag.FromErr(err)
	}

	newAttr, err := c.CreateAttribute(ctx, &attr)
	if err != nil {
		return diag.FromErr(err)
	}

	d.SetId(newAttr.Inum)

	return resourceAttributeRead(ctx, d, meta)
}

func resourceAttributeRead(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var diags diag.Diagnostics

	inum := d.Id()
	attr, err := c.GetAttribute(ctx, inum)
	if err != nil {
		return handleNotFoundError(ctx, err, d)
	}

	if err := toSchemaResource(d, attr); err != nil {
		return diag.FromErr(err)
	}

	d.SetId(attr.Inum)

	return diags
}

func resourceAttributeUpdate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var attr jans.Attribute
	if err := fromSchemaResource(d, &attr); err != nil {
		return diag.FromErr(err)
	}

	if _, err := c.UpdateAttribute(ctx, &attr); err != nil {
		return diag.FromErr(err)
	}

	return resourceAttributeRead(ctx, d, meta)
}

func resourceAttributeDelete(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	inum := d.Id()
	if err := c.DeleteAttribute(ctx, inum); err != nil {
		return diag.FromErr(err)
	}

	return resourceAttributeRead(ctx, d, meta)
}
