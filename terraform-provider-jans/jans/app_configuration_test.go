package jans

import (
	"context"
	"testing"
)

func TestAuthConfigMapping(t *testing.T) {

	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	_, err = client.GetAppConfiguration(ctx)
	if err != nil {
		t.Fatal(err)
	}

}

func TestPatchAuthConfig(t *testing.T) {

	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	cfg, err := client.GetAppConfiguration(ctx)
	if err != nil {
		t.Fatal(err)
	}

	if len(cfg.ClientBlackList) != 1 {
		t.Fatal("expected 1 client in blacklist")
	}

	cfg.ClientBlackList = []string{"*.attacker.com/*"}

	if _, err := client.UpdateAppConfiguration(ctx, cfg); err != nil {
		t.Fatal(err)
	}

	cfg, err = client.GetAppConfiguration(ctx)
	if err != nil {
		t.Fatal(err)
	}

	if len(cfg.ClientBlackList) != 1 {
		t.Fatal("expected 1 client in blacklist")
	}

	if (cfg.ClientBlackList[0]) != "*.attacker.com/*" {
		t.Fatal("expected *.attacker.com/* in blacklist")
	}
}
