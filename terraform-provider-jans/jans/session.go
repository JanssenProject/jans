package jans

import (
        "context"
)

// SessionId represents a user session in Janssen
type SessionId struct {
        Dn                       string                 `json:"dn,omitempty"`
        Id                       string                 `json:"id,omitempty"`
        Sid                      string                 `json:"sid,omitempty"`
        CreationDate             string                 `json:"creationDate,omitempty"`
        State                    string                 `json:"state,omitempty"`
        SessionState             string                 `json:"sessionState,omitempty"`
        UserDn                   string                 `json:"userDn,omitempty"`
        AuthenticationTime       string                 `json:"authenticationTime,omitempty"`
        LastUsedAt               string                 `json:"lastUsedAt,omitempty"`
        PermissionGranted        bool                   `json:"permissionGranted,omitempty"`
        PermissionGrantedMap     map[string]bool        `json:"permissionGrantedMap,omitempty"`
        InvolvedClientsIds       []string               `json:"involvedClientsIds,omitempty"`
        SessionAttributes        map[string]string      `json:"sessionAttributes,omitempty"`
        DeviceSecrets            []string               `json:"deviceSecrets,omitempty"`
        JansId                   string                 `json:"jansId,omitempty"`
}

// SessionPagedResult represents a paged result of sessions
type SessionPagedResult struct {
        Start             int         `json:"start,omitempty"`
        TotalEntriesCount int         `json:"totalEntriesCount,omitempty"`
        EntriesCount      int         `json:"entriesCount,omitempty"`
        Entries           []SessionId `json:"entries,omitempty"`
}

// GetSessions returns all active sessions from the Janssen server
func (c *Client) GetSessions(ctx context.Context) ([]SessionId, error) {
        token, err := c.getToken(ctx, "https://jans.io/oauth/jans-auth-server/session.readonly")
        if err != nil {
                return nil, err
        }

        var result SessionPagedResult
        err = c.get(ctx, "/jans-config-api/api/v1/jans-auth-server/session", token, &result)
        if err != nil {
                return nil, err
        }

        return result.Entries, nil
}

// GetSession returns a specific session by session ID
func (c *Client) GetSession(ctx context.Context, sid string) (*SessionId, error) {
        token, err := c.getToken(ctx, "https://jans.io/oauth/jans-auth-server/session.readonly")
        if err != nil {
                return nil, err
        }

        var session SessionId
        err = c.get(ctx, "/jans-config-api/api/v1/jans-auth-server/session/sid/"+sid, token, &session)
        if err != nil {
                return nil, err
        }

        return &session, nil
}

// RevokeUserSessions revokes all sessions for a specific user by userDn
func (c *Client) RevokeUserSessions(ctx context.Context, userDn string) error {
        // Session revocation requires both scopes
        token, err := c.getToken(ctx, "revoke_session https://jans.io/oauth/jans-auth-server/session.delete")
        if err != nil {
                return err
        }

        return c.delete(ctx, "/jans-config-api/api/v1/jans-auth-server/session/user/"+userDn, token)
}
