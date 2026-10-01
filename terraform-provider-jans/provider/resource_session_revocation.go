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

This resource revokes sessions either for a specific user identified by their DN (Distinguished Name),
or a single session identified by its session id (sid). Exactly one of user_dn or sid must be set.
Use this for security operations such as forcing user logout, responding to security incidents, or
implementing session management policies. The resource supports triggers to re-execute revocation
when specified values change.

## Example Usage

` + "```hcl" + `
# Revoke all sessions for a specific user
resource "jans_session_revocation" "revoke_user" {
  user_dn = "inum=user123,ou=people,o=jans"
}

# Revoke a single session by its sid
resource "jans_session_revocation" "revoke_sid" {
  sid = "7cbad817-0b96-40ca-8667-1073bfa726c3"
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
				Type:         schema.TypeString,
				Optional:     true,
				ForceNew:     true,
				ExactlyOneOf: []string{"user_dn", "sid"},
				Description:  "User distinguished name whose sessions should be revoked. Exactly one of user_dn or sid must be set.",
			},
			"sid": {
				Type:         schema.TypeString,
				Optional:     true,
				ForceNew:     true,
				ExactlyOneOf: []string{"user_dn", "sid"},
				Description:  "Session identifier (sid) of a single session to revoke. Exactly one of user_dn or sid must be set.",
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

	id, err := revokeSession(ctx, c, d)
	if err != nil {
		return diag.FromErr(err)
	}

	d.SetId(id)

	return nil
}

// revokeSession performs the revocation for a jans_session_revocation resource,
// dispatching on whether a single session id (sid) or a user_dn was provided,
// and returns the resource ID to use. Exactly one of the two must be set.
func revokeSession(ctx context.Context, c *jans.Client, d *schema.ResourceData) (string, error) {
	userDn := d.Get("user_dn").(string)
	sid := d.Get("sid").(string)

	switch {
	case sid != "" && userDn != "":
		return "", fmt.Errorf("only one of 'user_dn' or 'sid' may be set")
	case sid != "":
		if err := c.DeleteSessionBySid(ctx, sid); err != nil {
			return "", err
		}
		return fmt.Sprintf("session_revocation_sid_%s", sid), nil
	case userDn != "":
		if err := c.RevokeUserSessions(ctx, userDn); err != nil {
			return "", err
		}
		return fmt.Sprintf("session_revocation_%s", userDn), nil
	default:
		return "", fmt.Errorf("either 'user_dn' or 'sid' must be set")
	}
}

func resourceSessionRevocationRead(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
	// This is a command-style resource, so there's nothing to read
	// The resource exists as long as it's in the state
	return nil
}

func resourceSessionRevocationUpdate(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
	// If triggers changed, re-run the revocation
	if d.HasChange("triggers") {
		c := meta.(*jans.Client)

		if _, err := revokeSession(ctx, c, d); err != nil {
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
