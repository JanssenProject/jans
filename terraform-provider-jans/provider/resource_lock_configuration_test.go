package provider

import (
	"testing"

	"github.com/google/go-cmp/cmp"
	"github.com/jans/terraform-provider-jans/jans"
)

func TestResourceLockConfiguration_Mapping(t *testing.T) {

	schema := resourceLockConfiguration()

	data := schema.Data(nil)

	cfg := jans.LockConfiguration{
		BaseDN:               "o=jans",
		BaseEndpoint:         "https://example.jans.io/jans-lock/v1",
		OpenIdIssuer:         "https://example.jans.io",
		ProtectionMode:       "oauth",
		AuditPersistenceMode: "internal",
		CedarlingConfiguration: jans.CedarlingConfiguration{
			Enabled: true,
			PolicySources: []jans.PolicySource{
				{
					Enabled:            true,
					AuthorizationToken: "secret-token",
					PolicyStoreUri:     "https://example.jans.io/policy-store",
				},
			},
			LogType:                "memory",
			LogLevel:               "DEBUG",
			ExternalPolicyStoreUri: "https://example.jans.io/external-policy-store",
			MaxEntries:             20,
		},
		GrpcConfiguration: jans.GrpcConfiguration{
			ServerMode:            "tls_server",
			GrpcPort:              9090,
			UseTls:                true,
			TlsCertChainFilePath:  "/etc/jans/conf/lock/cert-chain.pem",
			TlsPrivateKeyFilePath: "/etc/jans/conf/lock/key.pem",
		},
		StatEnabled:                        true,
		StatTimerIntervalInSeconds:         60,
		TokenChannels:                      []string{"jans_token"},
		ClientId:                           "client-1",
		ClientPassword:                     "client-secret",
		DisableJdkLogger:                   true,
		DisableExternalLoggerConfiguration: true,
		LoggingLevel:                       "INFO",
		LoggingLayout:                      "text",
		ExternalLoggerConfiguration:        "/etc/jans/conf/lock/log4j2.xml",
		MetricReporterInterval:             300,
		MetricReporterKeepDataDays:         15,
		MetricReporterEnabled:              true,
		CleanServiceInterval:               60,
		MessageConsumerType:                "DISABLED",
		ErrorReasonEnabled:                 true,
		CleanServiceBatchChunkSize:         100,
	}

	if err := toSchemaResource(data, cfg); err != nil {
		t.Fatal(err)
	}

	newCfg := jans.LockConfiguration{}

	if err := fromSchemaResource(data, &newCfg); err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(cfg, newCfg); diff != "" {
		t.Errorf("Got different configuration after mapping: %s", diff)
	}
}
