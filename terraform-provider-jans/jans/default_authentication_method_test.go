package jans

import (
	"context"
	"testing"
)

func TestAuthenticationMethod(t *testing.T) {

	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	am, err := client.GetDefaultAuthenticationMethod(ctx)
	if err != nil {
		t.Fatal(err)
	}

	am.DefaultAcr = "simple_password_auth"

	updatedAM, err := client.UpdateDefaultAuthenticationMethod(ctx, am)
	if err != nil {
		t.Fatal(err)
	}

	if updatedAM.DefaultAcr != "simple_password_auth" {
		t.Fatal("DefaultAcr is not updated")
	}
}
