package provider

import (
        "context"
        "fmt"

        "github.com/hashicorp/terraform-plugin-sdk/v2/diag"
        "github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"

        "github.com/jans/terraform-provider-jans/jans"
)

func resourceSessionRevocation() *schema.Resource {
        return &schema.Resource{
                Description: `Resource for revoking all user sessions. This is a command-style resource that revokes sessions on create/update.

This resource revokes all active sessions for a specific user identified by their DN (Distinguished Name).
Use this for security operations such as forcing user logout, responding to security incidents, or
implementing session management policies. The resource supports triggers to re-execute revocation
when specified values change.

## Example Usage

` + "```hcl" + `
# Revoke all sessions for a specific user
resource "jans_session_revocation" "revoke_user" {
  user_dn = "inum=user123,ou=people,o=jans"
}

# Conditional revocation with triggers
resource "jans_session_revocation" "security_incident" {
  user_dn = var.compromised_user_dn

  triggers = {
    incident_id = var.security_incident_id
    timestamp   = timestamp()
  }
}

# Revoke sessions when user status changes
resource "jans_session_revocation" "on_deactivation" {
  user_dn = jans_user.admin.dn

  triggers = {
    user_status = jans_user.admin.status
  }
}
` + "```" + `

## OAuth Scopes Required

- ` + "`revoke_session`" + `
- ` + "`https://jans.io/oauth/jans-auth-server/session.delete`" + `

Note: Both scopes must be granted to the OAuth client for this resource to work.

## Known Issues

None currently known. The resource correctly handles both successful revocations and 404 responses
for non-existent users.
`,
                CreateContext: resourceSessionRevocationCreate,
                ReadContext:   resourceSessionRevocationRead,
                UpdateContext: resourceSessionRevocationUpdate,
                DeleteContext: resourceSessionRevocationDelete,
                Schema: map[string]*schema.Schema{
                        "user_dn": {
                                Type:        schema.TypeString,
                                Required:    true,
                                ForceNew:    true,
                                Description: "User distinguished name whose sessions should be revoked",
                        },
                        "triggers": {
                                Type:        schema.TypeMap,
                                Optional:    true,
                                Description: "Map of values which should trigger a session revocation when changed",
                                Elem: &schema.Schema{
                                        Type: schema.TypeString,
                                },
                        },
                },
        }
}

func resourceSessionRevocationCreate(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
        c := meta.(*jans.Client)
        
        userDn := d.Get("user_dn").(string)
        
        err := c.RevokeUserSessions(ctx, userDn)
        if err != nil {
                return diag.FromErr(err)
        }
        
        d.SetId(fmt.Sprintf("session_revocation_%s", userDn))
        
        return nil
}

func resourceSessionRevocationRead(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
        // This is a command-style resource, so there's nothing to read
        // The resource exists as long as it's in the state
        return nil
}

func resourceSessionRevocationUpdate(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
        // If triggers changed, revoke sessions again
        if d.HasChange("triggers") {
                c := meta.(*jans.Client)
                userDn := d.Get("user_dn").(string)
                
                err := c.RevokeUserSessions(ctx, userDn)
                if err != nil {
                        return diag.FromErr(err)
                }
        }
        
        return nil
}

func resourceSessionRevocationDelete(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
        // Nothing to do on delete for a command-style resource
        d.SetId("")
        return nil
}
