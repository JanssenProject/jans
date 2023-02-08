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

	am.DefaultAcr = "pwd"

	updatedAM, err := client.UpdateDefaultAuthenticationMethod(ctx, am)
	if err != nil {
		t.Fatal(err)
	}

	if updatedAM.DefaultAcr != "pwd" {
		t.Fatal("DefaultAcr is not updated")
	}

	updatedAM, err = client.UpdateDefaultAuthenticationMethod(ctx, &DefaultAuthenticationMethod{
		DefaultAcr: "token",
	})
	if err != nil {
		t.Fatal(err)
	}

	if updatedAM.DefaultAcr != "token" {
		t.Fatal("DefaultAcr is not updated")
	}
}
