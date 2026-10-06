package provider

import (
	"context"
	"fmt"

	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"

	"github.com/jans/terraform-provider-jans/jans"
)

func resourceClientAuthorizationRevocation() *schema.Resource {
	return &schema.Resource{
		Description: `Resource for revoking a client authorization. This is a command-style resource that revokes the authorization on create/update.

This resource revokes the authorization a user granted to a specific client (DELETE /clients/authorizations/{userId}/{clientId}/{username}).
Use it for security operations such as forcing re-consent or responding to a compromised client. The resource supports
triggers to re-execute the revocation when specified values change.

## Example Usage

` + "```hcl" + `
resource "jans_client_authorization_revocation" "revoke" {
  user_id   = "122ff2df-911d-424b-bbfe-891a43a70e95"
  client_id = "2000.cc8b29ae-cb4a-49ea-b176-8695e53919d9"
  username  = "admin"
}
` + "```" + `

## OAuth Scopes Required

- ` + "`https://jans.io/oauth/client/authorizations.delete`" + `
`,
		CreateContext: resourceClientAuthorizationRevocationCreate,
		ReadContext:   resourceClientAuthorizationRevocationRead,
		UpdateContext: resourceClientAuthorizationRevocationUpdate,
		DeleteContext: resourceClientAuthorizationRevocationDelete,
		Schema: map[string]*schema.Schema{
			"user_id": {
				Type:        schema.TypeString,
				Required:    true,
				ForceNew:    true,
				Description: "Identifier of the user whose authorization should be revoked.",
			},
			"client_id": {
				Type:        schema.TypeString,
				Required:    true,
				ForceNew:    true,
				Description: "Identifier of the client the authorization was granted to.",
			},
			"username": {
				Type:        schema.TypeString,
				Required:    true,
				ForceNew:    true,
				Description: "User name of the user whose authorization should be revoked.",
			},
			"triggers": {
				Type:        schema.TypeMap,
				Optional:    true,
				Description: "Map of values which should trigger a revocation when changed",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
		},
	}
}

func resourceClientAuthorizationRevocationCreate(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
	c := meta.(*jans.Client)

	userId := d.Get("user_id").(string)
	clientId := d.Get("client_id").(string)
	username := d.Get("username").(string)

	if err := c.DeleteClientAuthorization(ctx, userId, clientId, username); err != nil {
		return diag.FromErr(err)
	}

	d.SetId(fmt.Sprintf("client_authorization_revocation_%s_%s_%s", userId, clientId, username))

	return nil
}

func resourceClientAuthorizationRevocationRead(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
	// Command-style resource: nothing to read. It exists as long as it's in state.
	return nil
}

func resourceClientAuthorizationRevocationUpdate(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
	if d.HasChange("triggers") {
		c := meta.(*jans.Client)

		userId := d.Get("user_id").(string)
		clientId := d.Get("client_id").(string)
		username := d.Get("username").(string)

		if err := c.DeleteClientAuthorization(ctx, userId, clientId, username); err != nil {
			return diag.FromErr(err)
		}
	}

	return nil
}

func resourceClientAuthorizationRevocationDelete(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
	// Nothing to do on delete for a command-style resource
	d.SetId("")
	return nil
}
