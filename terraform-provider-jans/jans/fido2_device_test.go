package jans

import (
	"context"
	"testing"
)

func TestFido2Devices(t *testing.T) {

	if skipKnownFailures {
		t.SkipNow()
	}

	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	_, err = client.GetFido2Devices(ctx)
	if err != nil {
		t.Fatal(err)
	}

}
