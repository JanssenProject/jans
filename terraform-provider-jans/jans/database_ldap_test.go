package jans

import (
	"context"
	"testing"

	"github.com/google/go-cmp/cmp"
)

func TestLdapDBConfiguration(t *testing.T) {

	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	configs, err := client.GetLDAPDBConfigurations(ctx)
	if err != nil {
		t.Error(err)
	}

	for _, config := range configs {
		client.DeleteLDAPDBConfiguration(ctx, config.ConfigId)
		t.Logf("config: %+v", config)
	}

	newCfg := &LDAPDBConfiguration{
		LocalPrimaryKey: "uid",
		ConfigId:        "auth_ldap_server_3",
		MaxConnections:  100,
		Servers:         []string{"ldap.janssen.io:1636"},
		UseSSL:          true,
		BindDN:          "cn=directory manager",
		BindPassword:    "CLcHdW8FPW40PByaxmcXaQ==",
		PrimaryKey:      "uid",
		BaseDNs:         []string{"ou=people,o=jans"},
	}

	createdCfg, err := client.CreateLDAPDBConfiguration(ctx, newCfg)
	if err != nil {
		t.Fatal(err)
	}

	t.Cleanup(func() {
		_ = client.DeleteLDAPDBConfiguration(ctx, createdCfg.ConfigId)
	})

	// password is hashed, so we can't compare it
	filter := cmp.FilterPath(func(p cmp.Path) bool {
		return p.String() == "BindPassword"
	}, cmp.Ignore())

	if diff := cmp.Diff(newCfg, createdCfg, filter); diff != "" {
		t.Errorf("Got different configuration after creation: %s", diff)
	}

	loadedCfg, err := client.GetLDAPDBConfiguration(ctx, createdCfg.ConfigId)
	if err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(createdCfg, loadedCfg, filter); diff != "" {
		t.Errorf("Got different configuration after loading: %s", diff)
	}

	loadedCfg.MaxConnections = 200
	_, err = client.UpdateLDAPDBConfiguration(ctx, loadedCfg)
	if err != nil {
		t.Fatal(err)
	}

	updatedCfg, err := client.GetLDAPDBConfiguration(ctx, createdCfg.ConfigId)
	if err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(loadedCfg, updatedCfg, filter); diff != "" {
		t.Errorf("Got different configuration after update: %s", diff)
	}

	err = client.DeleteLDAPDBConfiguration(ctx, updatedCfg.ConfigId)
	if err != nil {
		t.Fatal(err)
	}

}
