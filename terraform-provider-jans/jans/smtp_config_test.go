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

	cfgUpdate := &SMTPConfiguration{
		Valid: true,
		// ConnectProtectionList:             []string{"None"},
		Host:                              "smtp.janssen.io",
		Port:                              587,
		ConnectProtection:                 "None",
		TrustHost:                         true,
		FromName:                          "Janssen",
		FromEmailAddress:                  "jans@janssen.io",
		RequiresAuthentication:            true,
		SmtpAuthenticationAccountUsername: "janssen",
		SmtpAuthenticationAccountPassword: "p4ssw0rd",
		KeyStore:                          "key.jks",
		KeyStorePassword:                  "changeit",
		KeyStoreAlias:                     "jans",
		SigningAlgorithm:                  "RS256",
	}

	_, err = client.UpdateSMTPConfiguration(ctx, cfgUpdate)
	if err != nil {
		t.Fatal(err)
	}

	updatedConfig, err := client.GetSMTPConfiguration(ctx)
	if err != nil {
		t.Fatal(err)
	}

	filter := cmp.FilterPath(func(p cmp.Path) bool {
		return p.String() == "SmtpAuthenticationAccountPassword" ||
			p.String() == "KeyStorePassword"
	}, cmp.Ignore())

	if diff := cmp.Diff(cfgUpdate, updatedConfig, filter); diff != "" {
		t.Errorf("Got different configuration after update: %s", diff)
	}

	if _, err := client.UpdateSMTPConfiguration(ctx, cfg); err != nil {
		t.Errorf("unexpected error: %v", err)
	}
}
