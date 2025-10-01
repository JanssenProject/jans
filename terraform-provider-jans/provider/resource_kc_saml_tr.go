package provider

import (
	"context"
	"fmt"
	"io"
	"os"

	"github.com/hashicorp/terraform-plugin-log/tflog"
	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func resourceKCSamlTR() *schema.Resource {
	return &schema.Resource{
		Description:   "Resource for managing Keycloak SAML Trust Relationship.",
		CreateContext: resourceKCSamlTRCreate,
		ReadContext:   resourceKCSamlTRRead,
		UpdateContext: resourceKCSamlTRUpdate,
		DeleteContext: resourceKCSamlTRDelete,
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
			"owner": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Owner of the trust relationship.",
			},
			"name": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Name of the trust relationship.",
			},
			"display_name": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "Display name of the trust relationship.",
			},
			"description": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "Description of the trust relationship.",
			},
			"root_url": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Root URL of the trust relationship.",
			},
			"enabled": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Status of the trust relationship.",
			},
			"always_display_in_console": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Always display in console of the trust relationship.",
			},
			"client_authenticator_type": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Client authenticator type of the trust relationship.",
			},
			"secret": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Secret of the trust relationship.",
			},
			"registration_access_token": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Registration access token of the trust relationship.",
			},
			"consent_required": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Consent required of the trust relationship.",
			},
			"metadata_file": {
				Type:         schema.TypeString,
				Optional:     true,
				Description:  "Metadata file location for the trust relationship.",
				AtLeastOneOf: []string{"saml_metadata"},
			},
			"saml_metadata": {
				Type:         schema.TypeList,
				Optional:     true,
				AtLeastOneOf: []string{"metadata_file"},
				Description:  "SAML metadata of the trust relationship.",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"name_id_policy_format": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "Name ID policy format of the trust relationship.",
						},
						"entity_id": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "Entity ID of the trust relationship.",
						},
						"single_logout_service_url": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "Single logout service URL of the trust relationship.",
						},
						"jans_assertion_consumer_service_get_url": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "Jans assertion consumer service GET URL of the trust relationship.",
						},
						"jans_assertion_consumer_service_post_url": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "Jans assertion consumer service POST URL of the trust relationship.",
						},
					},
				},
				MaxItems: 1,
			},
			"redirect_uris": {
				Type:        schema.TypeList,
				Optional:    true,
				Elem:        &schema.Schema{Type: schema.TypeString},
				Description: "Redirect URIs of the trust relationship.",
			},
			"sp_meta_data_url": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "SP metadata URL of the trust relationship.",
			},
			"meta_location": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Meta location of the trust relationship.",
			},
			"released_attributes": {
				Type:        schema.TypeList,
				Optional:    true,
				Elem:        &schema.Schema{Type: schema.TypeString},
				Description: "Released attributes of the trust relationship.",
			},
			"sp_logout_url": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "SP logout URL of the trust relationship.",
			},
			"status": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Status of the trust relationship.",
			},
			"validation_status": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Validation status of the trust relationship.",
			},
			"validation_log": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "Validation log of the trust relationship.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"profile_configurations": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "Profile configurations of the trust relationship.",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"additional_prop1": additionalPropSchema(),
						"additional_prop2": additionalPropSchema(),
						"additional_prop3": additionalPropSchema(),
					},
				},
				MaxItems: 1,
			},
			"base_dn": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Base DN of the trust relationship.",
			},
		},
	}
}

func additionalPropSchema() *schema.Schema {
	return &schema.Schema{
		Type:        schema.TypeList,
		Optional:    true,
		Description: "Additional prop of the trust relationship.",
		Elem:        &schema.Schema{Type: schema.TypeString},
		MaxItems:    1,
	}
}

func resourceKCSamlTRRead(ctx context.Context, d *schema.ResourceData, m any) diag.Diagnostics {

	c := m.(*jans.Client)

	tr, err := c.GetTR(ctx, d.Get("inum").(string))
	if err != nil {
		return diag.FromErr(err)
	}

	if err := toSchemaResource(d, tr); err != nil {
		return diag.FromErr(err)
	}

	tflog.Debug(ctx, "ResourceKCSamlTRRead: Read trust relationship", map[string]any{"Inum": tr.Inum})

	return nil
}

func resourceKCSamlTRCreate(ctx context.Context, d *schema.ResourceData, m any) diag.Diagnostics {

	c := m.(*jans.Client)

	tr, f, err := handleMetadataFile[jans.TrustRelationship](d)
	if err != nil {
		return diag.FromErr(err)
	}

	tr, err = c.CreateTR(ctx, tr, f)
	if err != nil {
		return diag.FromErr(err)
	}

	return resourceKCSamlTRRead(ctx, d, m)
}

func resourceKCSamlTRUpdate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	tr, f, err := handleMetadataFile[jans.TrustRelationship](d)
	if err != nil {
		return diag.FromErr(err)
	}

	tr, err = c.UpdateTR(ctx, tr, f)
	if err != nil {
		return diag.FromErr(err)
	}

	return resourceKCSamlTRRead(ctx, d, meta)
}

func resourceKCSamlTRDelete(ctx context.Context, d *schema.ResourceData, m any) diag.Diagnostics {
	c := m.(*jans.Client)

	inum := d.Get("inum").(string)
	tflog.Debug(ctx, "Deleting trust relationship: inum=%s", map[string]any{"Inum": inum})
	if err := c.DeleteTR(ctx, inum); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "Deleted trust relationship: inum=%s", map[string]any{"Inum": inum})

	return resourceKCSamlTRRead(ctx, d, m)
}

func handleMetadataFile[T any](d *schema.ResourceData) (*T, io.Reader, error) {
	req := new(T)

	err := fromSchemaResource(d, req)
	if err != nil {
		return nil, nil, err
	}

	v, ok := d.GetOk("metadata_file")
	if !ok {
		return req, nil, nil
	}

	loc, ok := v.(string)
	if !ok {
		return nil, nil, fmt.Errorf("expected type string, got %T", v)
	}

	f, err := os.Open(loc)
	if err != nil {
		return nil, nil, fmt.Errorf("failed to open file: %w", err)
	}

	return req, f, nil
}
