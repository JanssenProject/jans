package jans

import (
	"context"
	"testing"
)

func TestServiceProviderConfig(t *testing.T) {

	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	_, err = client.GetServiceProviderConfig(ctx)
	if err != nil {
		t.Fatal(err)
	}

}
