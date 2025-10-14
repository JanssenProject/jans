package provider

import (
        "context"
        "fmt"

        "github.com/hashicorp/terraform-plugin-sdk/v2/diag"
        "github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"

        "github.com/jans/terraform-provider-jans/jans"
)

func resourceTokenRevocation() *schema.Resource {
        return &schema.Resource{
                Description: `Resource for revoking OAuth tokens. This is a command-style resource that revokes tokens on create/update.

This resource revokes a specific OAuth access token or refresh token identified by its token code.
Use this for security operations such as emergency token revocation, token rotation, or implementing
token lifecycle management. The resource supports triggers to re-execute revocation when specified
values change.

## Example Usage

` + "```hcl" + `
# Revoke a specific token
resource "jans_token_revocation" "revoke_token" {
  token_code = var.token_to_revoke
}

# Emergency revocation with triggers
resource "jans_token_revocation" "emergency" {
  token_code = var.compromised_token

  triggers = {
    alert_id  = var.security_alert_id
    timestamp = timestamp()
  }
}

# Token rotation scenario
resource "jans_token_revocation" "rotate_old" {
  count      = length(var.old_tokens)
  token_code = var.old_tokens[count.index]

  triggers = {
    secret_version = var.client_secret_version
  }
}
` + "```" + `

## OAuth Scopes Required

- ` + "`https://jans.io/oauth/config/token.delete`" + `

## Known Issues

None currently known. The resource correctly handles both successful revocations and 404 responses
for non-existent tokens.
`,
                CreateContext: resourceTokenRevocationCreate,
                ReadContext:   resourceTokenRevocationRead,
                UpdateContext: resourceTokenRevocationUpdate,
                DeleteContext: resourceTokenRevocationDelete,
                Schema: map[string]*schema.Schema{
                        "token_code": {
                                Type:        schema.TypeString,
                                Required:    true,
                                ForceNew:    true,
                                Description: "Token code to revoke",
                        },
                        "triggers": {
                                Type:        schema.TypeMap,
                                Optional:    true,
                                Description: "Map of values which should trigger a token revocation when changed",
                                Elem: &schema.Schema{
                                        Type: schema.TypeString,
                                },
                        },
                },
        }
}

func resourceTokenRevocationCreate(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
        c := meta.(*jans.Client)
        
        tokenCode := d.Get("token_code").(string)
        
        err := c.RevokeToken(ctx, tokenCode)
        if err != nil {
                return diag.FromErr(err)
        }
        
        d.SetId(fmt.Sprintf("token_revocation_%s", tokenCode))
        
        return nil
}

func resourceTokenRevocationRead(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
        // This is a command-style resource, so there's nothing to read
        // The resource exists as long as it's in the state
        return nil
}

func resourceTokenRevocationUpdate(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
        // If triggers changed, revoke the token again
        if d.HasChange("triggers") {
                c := meta.(*jans.Client)
                tokenCode := d.Get("token_code").(string)
                
                err := c.RevokeToken(ctx, tokenCode)
                if err != nil {
                        return diag.FromErr(err)
                }
        }
        
        return nil
}

func resourceTokenRevocationDelete(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
        // Nothing to do on delete for a command-style resource
        d.SetId("")
        return nil
}
