package provider

import (
        "context"
        "fmt"

        "github.com/hashicorp/terraform-plugin-sdk/v2/diag"
        "github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"

        "github.com/jans/terraform-provider-jans/jans"
)

func resourceSSARevocation() *schema.Resource {
        return &schema.Resource{
                Description: `Resource for revoking Software Statement Assertions (SSA). This is a command-style resource that revokes SSA on create/update.

This resource revokes Software Statement Assertions used in OAuth 2.0 Dynamic Client Registration.
You can revoke SSAs by JWT ID (jti), organization ID, or both. Use this for security operations,
compliance requirements, or managing the lifecycle of dynamically registered clients. The resource
supports triggers to re-execute revocation when specified values change.

Note: Only revocation is supported via the Config API. SSA creation and management is done through
other mechanisms.

## Example Usage

` + "```hcl" + `
# Revoke SSA by JWT ID
resource "jans_ssa_revocation" "revoke_by_jti" {
  jti = "550e8400-e29b-41d4-a716-446655440000"
}

# Revoke all SSAs for an organization
resource "jans_ssa_revocation" "revoke_by_org" {
  org_id = "acme-corp"
}

# Revoke specific SSA with both JTI and Org ID
resource "jans_ssa_revocation" "revoke_specific" {
  jti    = var.ssa_jti
  org_id = var.org_id
}

# Conditional revocation with triggers
resource "jans_ssa_revocation" "on_compliance_violation" {
  org_id = var.org_id

  triggers = {
    compliance_violation = var.violation_id
    severity            = var.severity
    timestamp           = timestamp()
  }
}

# Revoke on certificate expiration
resource "jans_ssa_revocation" "cert_expiry" {
  org_id = var.org_id

  triggers = {
    cert_expiry_date = var.certificate_expiry_date
    cert_thumbprint  = var.certificate_thumbprint
  }
}
` + "```" + `

## OAuth Scopes Required

- ` + "`https://jans.io/oauth/config/ssa.delete`" + `

## Known Issues

The API may return HTTP 500 "Unprocessable Entity" for non-existent SSAs instead of 404.
This is a server-side issue and is handled correctly by the provider.
`,
                CreateContext: resourceSSARevocationCreate,
                ReadContext:   resourceSSARevocationRead,
                UpdateContext: resourceSSARevocationUpdate,
                DeleteContext: resourceSSARevocationDelete,
                Schema: map[string]*schema.Schema{
                        "jti": {
                                Type:        schema.TypeString,
                                Optional:    true,
                                ForceNew:    true,
                                Description: "JWT ID - unique identifier for the JWT to revoke",
                        },
                        "org_id": {
                                Type:        schema.TypeString,
                                Optional:    true,
                                ForceNew:    true,
                                Description: "Organization ID to revoke SSA for",
                        },
                        "triggers": {
                                Type:        schema.TypeMap,
                                Optional:    true,
                                Description: "Map of values which should trigger an SSA revocation when changed",
                                Elem: &schema.Schema{
                                        Type: schema.TypeString,
                                },
                        },
                },
        }
}

func resourceSSARevocationCreate(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
        c := meta.(*jans.Client)
        
        jti := d.Get("jti").(string)
        orgId := d.Get("org_id").(string)
        
        // At least one of jti or org_id must be provided
        if jti == "" && orgId == "" {
                return diag.Errorf("Either 'jti' or 'org_id' must be provided")
        }
        
        err := c.RevokeSSA(ctx, jti, orgId)
        if err != nil {
                return diag.FromErr(err)
        }
        
        id := "ssa_revocation"
        if jti != "" {
                id = fmt.Sprintf("%s_jti_%s", id, jti)
        }
        if orgId != "" {
                id = fmt.Sprintf("%s_org_%s", id, orgId)
        }
        
        d.SetId(id)
        
        return nil
}

func resourceSSARevocationRead(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
        // This is a command-style resource, so there's nothing to read
        // The resource exists as long as it's in the state
        return nil
}

func resourceSSARevocationUpdate(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
        // If triggers changed, revoke SSA again
        if d.HasChange("triggers") {
                c := meta.(*jans.Client)
                jti := d.Get("jti").(string)
                orgId := d.Get("org_id").(string)
                
                err := c.RevokeSSA(ctx, jti, orgId)
                if err != nil {
                        return diag.FromErr(err)
                }
        }
        
        return nil
}

func resourceSSARevocationDelete(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
        // Nothing to do on delete for a command-style resource
        d.SetId("")
        return nil
}
