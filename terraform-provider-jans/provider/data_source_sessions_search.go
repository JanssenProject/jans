package provider

import (
	"context"

	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"

	"github.com/jans/terraform-provider-jans/jans"
)

func dataSourceSessionsSearch() *schema.Resource {
	return &schema.Resource{
		Description: "Data source for searching active user sessions via the Janssen session search endpoint (GET /jans-auth-server/session/search).",
		ReadContext: dataSourceSessionsSearchRead,
		Schema: map[string]*schema.Schema{
			"sessions": {
				Type:        schema.TypeList,
				Computed:    true,
				Description: "List of matching sessions",
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
							Sensitive:   true,
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

func dataSourceSessionsSearchRead(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
	c := meta.(*jans.Client)

	sessions, err := c.SearchSessions(ctx)
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

	d.SetId("sessions_search")
	if err := d.Set("sessions", sessionsList); err != nil {
		return diag.FromErr(err)
	}

	return nil
}
