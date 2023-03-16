package jans

import (
	"context"
	"testing"
)

func TestPlugins(t *testing.T) {

	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	plugins, err := client.GetPlugins(ctx)
	if err != nil {
		t.Fatal(err)
	}

	if len(plugins) != 3 {
		t.Fatal("expected 3 plugins, got ", len(plugins))
	}
}
