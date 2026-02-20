package provider

import (
        "context"

        "github.com/hashicorp/terraform-plugin-sdk/v2/diag"
        "github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"

        "github.com/jans/terraform-provider-jans/jans"
)

func dataSourceTokens() *schema.Resource {
        return &schema.Resource{
                Description: `Data source for searching and retrieving tokens from Janssen server.

This data source allows you to search for OAuth access tokens and refresh tokens in the Janssen
authorization server. You can retrieve token metadata including client ID, grant type, creation
and expiration dates, and token scopes. Supports pagination for handling large token sets.

## Example Usage

` + "```hcl" + `
# Search tokens with pagination
data "jans_tokens" "recent_tokens" {
  limit = 10
}

# Search tokens for specific client
data "jans_tokens" "client_tokens" {
  client_id = "client123"
  limit     = 50
}

# Output access tokens only
output "access_tokens" {
  value = [
    for token in data.jans_tokens.recent_tokens.tokens :
    token if token.token_type == "access_token"
  ]
  sensitive = true
}
` + "```" + `

## OAuth Scopes Required

- ` + "`https://jans.io/oauth/config/token.readonly`" + `
`,
                ReadContext: dataSourceTokensRead,
                Schema: map[string]*schema.Schema{
                        "pattern": {
                                Type:        schema.TypeString,
                                Optional:    true,
                                Description: "Search pattern for filtering tokens",
                        },
                        "client_id": {
                                Type:        schema.TypeString,
                                Optional:    true,
                                Description: "Client ID to filter tokens by specific client",
                        },
                        "limit": {
                                Type:        schema.TypeInt,
                                Optional:    true,
                                Default:     50,
                                Description: "Maximum number of results to return",
                        },
                        "start_index": {
                                Type:        schema.TypeInt,
                                Optional:    true,
                                Default:     0,
                                Description: "The 1-based index of the first query result",
                        },
                        "start": {
                                Type:        schema.TypeInt,
                                Computed:    true,
                                Description: "Start index of the returned results",
                        },
                        "total_entries": {
                                Type:        schema.TypeInt,
                                Computed:    true,
                                Description: "Total number of entries available",
                        },
                        "entries_count": {
                                Type:        schema.TypeInt,
                                Computed:    true,
                                Description: "Number of entries returned in this result",
                        },
                        "tokens": {
                                Type:        schema.TypeList,
                                Computed:    true,
                                Description: "List of tokens",
                                Elem: &schema.Resource{
                                        Schema: map[string]*schema.Schema{
                                                "authorization_code": {
                                                        Type:        schema.TypeString,
                                                        Computed:    true,
                                                        Description: "Authorization code",
                                                },
                                                "client_id": {
                                                        Type:        schema.TypeString,
                                                        Computed:    true,
                                                        Description: "Client ID",
                                                },
                                                "creation_date": {
                                                        Type:        schema.TypeString,
                                                        Computed:    true,
                                                        Description: "Token creation date",
                                                },
                                                "expiration_date": {
                                                        Type:        schema.TypeString,
                                                        Computed:    true,
                                                        Description: "Token expiration date",
                                                },
                                                "grant_type": {
                                                        Type:        schema.TypeString,
                                                        Computed:    true,
                                                        Description: "Grant type",
                                                },
                                                "scope": {
                                                        Type:        schema.TypeString,
                                                        Computed:    true,
                                                        Description: "Token scope",
                                                },
                                                "token_code": {
                                                        Type:        schema.TypeString,
                                                        Computed:    true,
                                                        Description: "Token code",
                                                },
                                                "token_type": {
                                                        Type:        schema.TypeString,
                                                        Computed:    true,
                                                        Description: "Token type",
                                                },
                                                "user_id": {
                                                        Type:        schema.TypeString,
                                                        Computed:    true,
                                                        Description: "User ID",
                                                },
                                                "dn": {
                                                        Type:        schema.TypeString,
                                                        Computed:    true,
                                                        Description: "Distinguished name",
                                                },
                                        },
                                },
                        },
                },
        }
}

func dataSourceTokensRead(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
        c := meta.(*jans.Client)

        clientId := d.Get("client_id").(string)
        pattern := d.Get("pattern").(string)
        limit := d.Get("limit").(int)
        startIndex := d.Get("start_index").(int)

        var result *jans.TokenEntityPagedResult
        var err error

        // Use client_id filter if provided, otherwise use pattern search
        if clientId != "" {
                result, err = c.GetTokensByClient(ctx, clientId)
        } else {
                result, err = c.SearchTokens(ctx, pattern, limit, startIndex)
        }

        if err != nil {
                return diag.FromErr(err)
        }

        tokensList := make([]map[string]interface{}, len(result.Entries))
        for i, t := range result.Entries {
                tokensList[i] = map[string]interface{}{
                        "authorization_code": t.AuthorizationCode,
                        "client_id":          t.ClientId,
                        "creation_date":      t.CreationDate,
                        "expiration_date":    t.ExpirationDate,
                        "grant_type":         t.GrantType,
                        "scope":              t.Scope,
                        "token_code":         t.TokenCode,
                        "token_type":         t.TokenType,
                        "user_id":            t.UserId,
                        "dn":                 t.Dn,
                }
        }

        d.SetId("tokens")
        d.Set("start", result.Start)
        d.Set("total_entries", result.TotalEntries)
        d.Set("entries_count", result.EntriesCount)
        d.Set("tokens", tokensList)

        return nil
}
