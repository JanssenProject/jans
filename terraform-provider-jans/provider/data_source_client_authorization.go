package provider

import (
	"context"

	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"

	"github.com/jans/terraform-provider-jans/jans"
)

func dataSourceClientAuthorization() *schema.Resource {
	return &schema.Resource{
		Description: `Data source for retrieving the OAuth client authorizations granted by a user.

This maps to the config-api read-only endpoint that returns, for the given user, every
client the user has authorized together with the scopes granted to that client.

## OAuth Scopes Required

- ` + "`https://jans.io/oauth/client/authorizations.readonly`" + `
`,
		ReadContext: dataSourceClientAuthorizationRead,
		Schema: map[string]*schema.Schema{
			"user_id": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "Identifier of the user whose client authorizations are retrieved.",
			},
			"authorizations": {
				Type:        schema.TypeList,
				Computed:    true,
				Description: "List of clients authorized by the user and the scopes granted to each.",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"client": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "Descriptor of the authorized client.",
						},
						"scopes": {
							Type:        schema.TypeList,
							Computed:    true,
							Description: "Scope identifiers granted to the client.",
							Elem: &schema.Schema{
								Type: schema.TypeString,
							},
						},
					},
				},
			},
		},
	}
}

func dataSourceClientAuthorizationRead(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
	c := meta.(*jans.Client)

	userId := d.Get("user_id").(string)

	clientAuth, err := c.GetClientAuthorization(ctx, userId)
	if err != nil {
		return diag.FromErr(err)
	}

	authorizations := make([]map[string]interface{}, 0, len(clientAuth.ClientAuths))
	for client, scopes := range clientAuth.ClientAuths {
		scopeIds := make([]string, len(scopes))
		for i, s := range scopes {
			scopeIds[i] = s.Id
		}
		authorizations = append(authorizations, map[string]interface{}{
			"client": client,
			"scopes": scopeIds,
		})
	}

	if err := d.Set("authorizations", authorizations); err != nil {
		return diag.FromErr(err)
	}

	d.SetId(userId)

	return nil
}
