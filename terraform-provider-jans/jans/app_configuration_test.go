package jans

import (
	"context"
	"fmt"
	"math/rand"
	"testing"
	"time"
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

func TestAppConfigGet(t *testing.T) {
	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	cfg, err := client.GetAppConfiguration(ctx)
	if err != nil {
		t.Fatal(err)
	}

	fmt.Printf("cfg: %+v", cfg)
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

	rand.Seed(time.Now().UnixNano())

	newEntry := []string{fmt.Sprintf("*.attacker-%v.com/*", rand.Intn(100))}

	patches := []PatchRequest{
		{
			Op:    "replace",
			Path:  "/clientBlackList",
			Value: newEntry,
		},
	}

	cfg.ClientBlackList = newEntry

	if _, err := client.PatchAppConfiguration(ctx, patches); err != nil {
		t.Fatal(err)
	}

	cfg, err = client.GetAppConfiguration(ctx)
	if err != nil {
		t.Fatal(err)
	}

	if len(cfg.ClientBlackList) != 1 {
		t.Fatal("expected 1 client in blacklist")
	}

	if (cfg.ClientBlackList[0]) != newEntry[0] {
		t.Fatalf("expected '%s' in blacklist, got '%s'", newEntry, cfg.ClientBlackList[0])
	}

}
