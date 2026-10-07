package jans

import (
	"context"
	"testing"
)

func TestFido2Configuration(t *testing.T) {

	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	_, err = client.GetFido2Config(ctx)
	if err != nil {
		t.Fatal(err)
	}
}
