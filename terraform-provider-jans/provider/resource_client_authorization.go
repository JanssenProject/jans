package provider

import (
	"context"

	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func resourceClientAuthorization() *schema.Resource {
	return &schema.Resource{
		Description:   "Resource for managing client authorizations in Janssen server",
		CreateContext: resourceClientAuthorizationCreate,
		ReadContext:   resourceClientAuthorizationRead,
		UpdateContext: resourceClientAuthorizationUpdate,
		DeleteContext: resourceClientAuthorizationDelete,
		Importer: &schema.ResourceImporter{
			StateContext: schema.ImportStatePassthroughContext,
		},
		Schema: map[string]*schema.Schema{
			"id": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "ID of the client authorization",
			},
			"inum": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "Unique identifier",
			},
			"dn": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "Distinguished name",
			},
			"client_id": {
				Type:        schema.TypeString,
				Required:    true,
				ForceNew:    true,
				Description: "Client identifier",
			},
			"user_id": {
				Type:        schema.TypeString,
				Required:    true,
				ForceNew:    true,
				Description: "User identifier",
			},
			"scopes": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "Authorized scopes",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"redirect_uris": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "Redirect URIs for the client authorization",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"grant_types": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "Grant types for the client authorization",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"creation_date": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "Creation date in RFC3339 format",
			},
			"expiration_date": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Expiration date in RFC3339 format",
			},
			"deletable": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Whether this authorization can be deleted",
			},
		},
	}
}

func resourceClientAuthorizationCreate(ctx context.Context, data *schema.ResourceData, meta interface{}) diag.Diagnostics {
	c := meta.(*jans.Client)

	var clientAuth jans.ClientAuthorization
	if err := fromSchemaResource(data, &clientAuth); err != nil {
		return diag.FromErr(err)
	}

	createdAuth, err := c.CreateClientAuthorization(ctx, &clientAuth)
	if err != nil {
		return diag.FromErr(err)
	}

	data.SetId(createdAuth.UserId)

	if err := toSchemaResource(data, createdAuth); err != nil {
		return diag.FromErr(err)
	}

	return nil
}

func resourceClientAuthorizationRead(ctx context.Context, data *schema.ResourceData, meta interface{}) diag.Diagnostics {
	c := meta.(*jans.Client)

	userId := data.Id()

	clientAuth, err := c.GetClientAuthorization(ctx, userId)
	if err != nil {
		return diag.FromErr(err)
	}

	if err := toSchemaResource(data, clientAuth); err != nil {
		return diag.FromErr(err)
	}

	return nil
}

func resourceClientAuthorizationUpdate(ctx context.Context, data *schema.ResourceData, meta interface{}) diag.Diagnostics {
	c := meta.(*jans.Client)

	var clientAuth jans.ClientAuthorization
	if err := fromSchemaResource(data, &clientAuth); err != nil {
		return diag.FromErr(err)
	}

	updatedAuth, err := c.UpdateClientAuthorization(ctx, &clientAuth)
	if err != nil {
		return diag.FromErr(err)
	}

	if err := toSchemaResource(data, updatedAuth); err != nil {
		return diag.FromErr(err)
	}

	return nil
}

func resourceClientAuthorizationDelete(ctx context.Context, data *schema.ResourceData, meta interface{}) diag.Diagnostics {
	c := meta.(*jans.Client)

	userId := data.Get("user_id").(string)
	clientId := data.Get("client_id").(string)
	// For username, we'll use the user_id if no separate username is provided
	username := userId

	if err := c.DeleteClientAuthorization(ctx, userId, clientId, username); err != nil {
		return diag.FromErr(err)
	}

	return nil
}
