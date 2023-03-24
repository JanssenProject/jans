package jans

import (
	"context"
	"testing"

	"github.com/google/go-cmp/cmp"
)

func TestSMTPConfig(t *testing.T) {

	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	cfg, err := client.GetSMTPConfiguration(ctx)
	if err != nil {
		t.Fatal(err)
	}

	if cfg.Host != "" {
		t.Error("expected empty host")
	}

	cfg = &SMTPConfiguration{
		Host:                   "smtp.janssen.io",
		Port:                   587,
		RequiresSSL:            true,
		TrustHost:              true,
		FromName:               "Janssen",
		FromEmailAddress:       "jans@janssen.io",
		RequiresAuthentication: true,
		UserName:               "janssen",
		Password:               "p4ssw0rd",
	}

	_, err = client.UpdateSMTPConfiguration(ctx, cfg)
	if err != nil {
		t.Fatal(err)
	}

	updatedConfig, err := client.GetSMTPConfiguration(ctx)
	if err != nil {
		t.Fatal(err)
	}

	filter := cmp.FilterPath(func(p cmp.Path) bool {
		return p.String() == "Password"
	}, cmp.Ignore())

	if diff := cmp.Diff(cfg, updatedConfig, filter); diff != "" {
		t.Errorf("Got different configuration after update: %s", diff)
	}

	cfg = &SMTPConfiguration{
		Host:                   "",
		Port:                   0,
		RequiresSSL:            false,
		TrustHost:              false,
		FromName:               "",
		FromEmailAddress:       "",
		RequiresAuthentication: false,
		UserName:               "",
		Password:               "",
	}

	if _, err := client.UpdateSMTPConfiguration(ctx, cfg); err != nil {
		t.Errorf("unexpected error: %v", err)
	}
}
