package jans

import (
	"context"
	"testing"
)

func TestClientAuthorization(t *testing.T) {
	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	// The endpoint is read-only per user; a user with no authorizations returns
	// an empty map rather than an error.
	auth, err := client.GetClientAuthorization(ctx, "admin")
	if err != nil {
		t.Fatal(err)
	}

	t.Logf("Found %d client authorizations for user", len(auth.ClientAuths))
}
