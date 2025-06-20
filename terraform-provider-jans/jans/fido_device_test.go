package jans

import (
	"context"
	"testing"
)

func TestFidoDevices(t *testing.T) {

	if skipKnownFailures {
		t.SkipNow()
	}

	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	_, err = client.GetFidoDevices(ctx)
	if err != nil {
		t.Fatal(err)
	}

}
