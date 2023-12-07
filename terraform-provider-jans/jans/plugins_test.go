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

	if len(plugins) != 4 {
		t.Fatal("expected 4 plugins, got ", len(plugins))
	}
}
