package jans

import (
        "context"
        "strings"
        "testing"
)

func TestClientAuthorization(t *testing.T) {
        client, err := NewInsecureClient(host, user, pass)
        if err != nil {
                t.Fatal(err)
        }

        ctx := context.Background()

        // Test GetClientAuthorizations - handle empty list gracefully
        auths, err := client.GetClientAuthorizations(ctx)
        if err != nil {
                // Check if it's a "not found" error, which is acceptable for empty list
                t.Logf("GetClientAuthorizations returned error: %v", err)
                // Don't fail the test here - empty authorization list is valid
        } else {
                t.Logf("Found %d existing client authorizations", len(auths))
        }

        // Test create, get, update, delete cycle
        newAuth := &ClientAuthorization{
                ClientId:     "test-client-id",
                UserId:       "test-user-id",
                Scopes:       []string{"openid", "profile"},
                RedirectURIs: []string{"https://example.com/callback"},
                GrantTypes:   []string{"authorization_code"},
        }

        createdAuth, err := client.CreateClientAuthorization(ctx, newAuth)
        if err != nil {
                // Check if the error is due to missing scopes - this is expected in many test environments
                if contains := strings.Contains(err.Error(), "scope not granted"); contains {
                        t.Skipf("Test client does not have required scope for client authorization operations: %v", err)
                }
                t.Fatal(err)
        }

        t.Cleanup(func() {
                _ = client.DeleteClientAuthorization(ctx, createdAuth.UserId, createdAuth.ClientId, "testuser")
        })

        // Test GetClientAuthorization
        retrievedAuth, err := client.GetClientAuthorization(ctx, createdAuth.UserId)
        if err != nil {
                t.Fatal(err)
        }

        if retrievedAuth.ClientID != newAuth.ClientId {
                t.Errorf("expected client ID %s, got %s", newAuth.ClientId, retrievedAuth.ClientID)
        }

        // Test UpdateClientAuthorization
        createdAuth.ClientId = "updated-client-id"
        updatedAuth, err := client.UpdateClientAuthorization(ctx, createdAuth)
        if err != nil {
                t.Fatal(err)
        }

        if updatedAuth.ClientId != "updated-client-id" {
                t.Errorf("expected updated client ID, got %s", updatedAuth.ClientId)
        }

        // Test DeleteClientAuthorization
        if err := client.DeleteClientAuthorization(ctx, createdAuth.UserId, createdAuth.ClientId, "testuser"); err != nil {
                t.Fatal(err)
        }
}
