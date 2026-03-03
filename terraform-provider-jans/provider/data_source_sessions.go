package provider

import (
        "context"

        "github.com/hashicorp/terraform-plugin-sdk/v2/diag"
        "github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"

        "github.com/jans/terraform-provider-jans/jans"
)

func dataSourceSessions() *schema.Resource {
        return &schema.Resource{
                Description: `Data source for retrieving active user sessions from Janssen server.

This data source allows you to list all active user sessions in the Janssen authorization server.
Sessions contain information about authenticated users, their authentication time, involved clients,
and session state.

## Example Usage

` + "```hcl" + `
# List all active sessions
data "jans_sessions" "active_sessions" {}

# Output session count
output "session_count" {
  value = length(data.jans_sessions.active_sessions.sessions)
}

# Filter sessions for specific user
output "user_sessions" {
  value = [
    for session in data.jans_sessions.active_sessions.sessions :
    session if can(regex("inum=admin", session.user_dn))
  ]
}
` + "```" + `

## OAuth Scopes Required

- ` + "`https://jans.io/oauth/jans-auth-server/session.readonly`" + `
`,
                ReadContext: dataSourceSessionsRead,
                Schema: map[string]*schema.Schema{
                        "sessions": {
                                Type:        schema.TypeList,
                                Computed:    true,
                                Description: "List of active sessions",
                                Elem: &schema.Resource{
                                        Schema: map[string]*schema.Schema{
                                                "dn": {
                                                        Type:        schema.TypeString,
                                                        Computed:    true,
                                                        Description: "Distinguished name of the session",
                                                },
                                                "id": {
                                                        Type:        schema.TypeString,
                                                        Computed:    true,
                                                        Description: "Session ID",
                                                },
                                                "sid": {
                                                        Type:        schema.TypeString,
                                                        Computed:    true,
                                                        Description: "Session identifier",
                                                },
                                                "creation_date": {
                                                        Type:        schema.TypeString,
                                                        Computed:    true,
                                                        Description: "Session creation date",
                                                },
                                                "state": {
                                                        Type:        schema.TypeString,
                                                        Computed:    true,
                                                        Description: "Session state",
                                                },
                                                "session_state": {
                                                        Type:        schema.TypeString,
                                                        Computed:    true,
                                                        Description: "Session state value",
                                                },
                                                "user_dn": {
                                                        Type:        schema.TypeString,
                                                        Computed:    true,
                                                        Description: "User distinguished name",
                                                },
                                                "authentication_time": {
                                                        Type:        schema.TypeString,
                                                        Computed:    true,
                                                        Description: "Authentication time",
                                                },
                                                "last_used_at": {
                                                        Type:        schema.TypeString,
                                                        Computed:    true,
                                                        Description: "Last time session was used",
                                                },
                                                "permission_granted": {
                                                        Type:        schema.TypeBool,
                                                        Computed:    true,
                                                        Description: "Whether permission is granted",
                                                },
                                                "involved_clients_ids": {
                                                        Type:        schema.TypeList,
                                                        Computed:    true,
                                                        Description: "List of involved client IDs",
                                                        Elem: &schema.Schema{
                                                                Type: schema.TypeString,
                                                        },
                                                },
                                                "device_secrets": {
                                                        Type:        schema.TypeList,
                                                        Computed:    true,
                                                        Description: "List of device secrets",
                                                        Elem: &schema.Schema{
                                                                Type: schema.TypeString,
                                                        },
                                                },
                                                "jans_id": {
                                                        Type:        schema.TypeString,
                                                        Computed:    true,
                                                        Description: "Janssen ID",
                                                },
                                        },
                                },
                        },
                },
        }
}

func dataSourceSessionsRead(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
        c := meta.(*jans.Client)

        sessions, err := c.GetSessions(ctx)
        if err != nil {
                return diag.FromErr(err)
        }

        sessionsList := make([]map[string]interface{}, len(sessions))
        for i, s := range sessions {
                sessionsList[i] = map[string]interface{}{
                        "dn":                   s.Dn,
                        "id":                   s.Id,
                        "sid":                  s.Sid,
                        "creation_date":        s.CreationDate,
                        "state":                s.State,
                        "session_state":        s.SessionState,
                        "user_dn":              s.UserDn,
                        "authentication_time":  s.AuthenticationTime,
                        "last_used_at":         s.LastUsedAt,
                        "permission_granted":   s.PermissionGranted,
                        "involved_clients_ids": s.InvolvedClientsIds,
                        "device_secrets":       s.DeviceSecrets,
                        "jans_id":              s.JansId,
                }
        }

        d.SetId("sessions")
        d.Set("sessions", sessionsList)

        return nil
}
