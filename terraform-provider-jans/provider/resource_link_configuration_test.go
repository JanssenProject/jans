package provider

import (
	"testing"

	"github.com/google/go-cmp/cmp"
	"github.com/jans/terraform-provider-jans/jans"
)

func TestResourceLinkConfiguration_Mapping(t *testing.T) {

	schema := resourceLinkConfiguration()

	data := schema.Data(nil)

	ldap := jans.GluuLdapConfiguration{
		ConfigId:         "source",
		BindDN:           "cn=directory manager",
		BindPassword:     "ldap-secret",
		Servers:          []string{"localhost:1636"},
		MaxConnections:   10,
		UseSSL:           true,
		BaseDNs:          []string{"o=gluu"},
		PrimaryKey:       "uid",
		LocalPrimaryKey:  "uid",
		UseAnonymousBind: true,
		Enabled:          true,
		Version:          1,
		Level:            2,
	}

	cfg := jans.LinkConfiguration{
		SourceConfigs:           []jans.GluuLdapConfiguration{ldap},
		InumConfig:              ldap,
		TargetConfig:            ldap,
		LdapSearchSizeLimit:     1000,
		KeyAttributes:           []string{"uid"},
		KeyObjectClasses:        []string{"gluuPerson"},
		SourceAttributes:        []string{"mail"},
		CustomLdapFilter:        "(objectClass=*)",
		UpdateMethod:            "VM",
		DefaultInumServer:       true,
		KeepExternalPerson:      true,
		UseSearchLimit:          true,
		AttributeMapping:        []jans.LinkAttributeMapping{{Source: "mail", Destination: "email"}},
		SnapshotFolder:          "/var/jans/link/snapshot",
		SnapshotMaxCount:        10,
		BaseDN:                  "o=jans",
		PersonObjectClassTypes:  []string{"jansPerson"},
		PersonCustomObjectClass: "jansCustomPerson",
		ContactObjectClassTypes: []string{"jansContact"},
		AllowPersonModification: true,
		SupportedUserStatus:     []string{"active", "inactive"},
		LoggingLevel:            "INFO",
		LoggingLayout:           "text",

		ExternalLoggerConfiguration:        "/etc/jans/conf/link/log4j2.xml",
		MetricReporterInterval:             300,
		MetricReporterKeepDataDays:         15,
		MetricReporterEnabled:              true,
		DisableJdkLogger:                   true,
		DisableExternalLoggerConfiguration: true,
		CleanServiceInterval:               60,
		LinkEnabled:                        true,
		ServerIpAddress:                    "127.0.0.1",
		PollingInterval:                    "PT5M",
		LastUpdate:                         "2026-01-01T00:00:00Z",
		LastUpdateCount:                    "42",
		ProblemCount:                       "0",
		UseLocalCache:                      true,
	}

	if err := toSchemaResource(data, cfg); err != nil {
		t.Fatal(err)
	}

	newCfg := jans.LinkConfiguration{}

	if err := fromSchemaResource(data, &newCfg); err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(cfg, newCfg); diff != "" {
		t.Errorf("Got different configuration after mapping: %s", diff)
	}
}
