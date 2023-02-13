package provider

import (
	"context"
	"errors"
	"testing"

	"github.com/google/go-cmp/cmp"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/resource"
	"github.com/hashicorp/terraform-plugin-sdk/v2/terraform"
	"github.com/jans/terraform-provider-jans/jans"
)

func TestResourceCacheConfiguration_Mapping(t *testing.T) {

	schema := resourceCacheConfiguration()

	data := schema.Data(nil)

	cfg := jans.CacheConfiguration{
		InMemoryConfiguration: jans.InMemoryCacheConfiguration{
			DefaultPutExpiration: 120,
		},
		MemcachedConfiguration: jans.MemcachedConfiguration{
			Servers:                 "localhost:11211",
			MaxOperationQueueLength: 100,
			BufferSize:              100,
			DefaultPutExpiration:    120,
			ConnectionFactoryType:   "BINARY",
		},
		NativePersistenceConfiguration: jans.NativePersistenceConfiguration{
			DefaultPutExpiration:             120,
			DefaultCleanupBatchSize:          100,
			DeleteExpiredOnGetRequest:        true,
			DisableAttemptUpdateBeforeInsert: true,
		},
		RedisConfiguration: jans.RedisConfiguration{
			Servers:                 "localhost:6379",
			RedisProviderType:       "JEDIS",
			Password:                "password",
			DefaultPutExpiration:    120,
			SentinelMasterGroupName: "master",
			UseSSL:                  true,
			SslTrustStoreFilePath:   "/path/to/truststore",
			MaxIdleConnections:      100,
			MaxTotalConnections:     100,
			ConnectionTimeout:       100,
			SoTimeout:               100,
			MaxRetryAttempts:        100,
		},
		CacheProviderType: "REDIS",
	}

	if err := toSchemaResource(data, cfg); err != nil {
		t.Fatal(err)
	}

	newCfg := jans.CacheConfiguration{}

	if err := fromSchemaResource(data, &newCfg); err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(cfg, newCfg); diff != "" {
		t.Errorf("Got different configuration after mapping: %s", diff)
	}
}

func TestAccResourceCacheConfiguration_basic(t *testing.T) {

	resource.Test(t, resource.TestCase{
		PreCheck:     func() { testAccPreCheck(t) },
		Providers:    testAccProviders,
		CheckDestroy: testAccReourceCheckCacheConfigurationDestroy,
		Steps: []resource.TestStep{
			{
				Config:           testAccResourceCacheConfigurationConfig_basic(),
				ResourceName:     "jans_cache_configuration.global",
				ImportState:      true,
				ImportStateId:    "jans_cache_configuration.jans_cache_configuration",
				ImportStateCheck: testAccResourceCheckCacheConfigurationImport,
			},
		},
	})
}

func testAccResourceCacheConfigurationConfig_basic() string {
	return `
resource "jans_cache_configuration" "global" {
}
`
}

func testAccResourceCheckCacheConfigurationImport(states []*terraform.InstanceState) error {

	found := false
	for _, is := range states {

		if is.ID != "jans_cache_configuration" {
			continue
		}

		found = true

		if err := checkAttribute(is, "cache_provider_type", "NATIVE_PERSISTENCE"); err != nil {
			return err
		}

		if err := checkAttribute(is, "native_persistence_configuration.0.default_put_expiration", "60"); err != nil {
			return err
		}

		if err := checkAttribute(is, "redis_configuration.0.connection_timeout", "3000"); err != nil {
			return err
		}

		break
	}

	if !found {
		return errors.New("resource not found in states")
	}

	return nil
}

func testAccReourceCheckCacheConfigurationDestroy(s *terraform.State) error {

	// since this is a global resource, delete should not have any effect

	c := testAccProvider.Meta().(*jans.Client)

	ctx := context.Background()

	for _, rs := range s.RootModule().Resources {
		if rs.Type != "jans_cache_configuration" {
			continue
		}

		_, err := c.GetCacheConfiguration(ctx)
		if err != nil {
			return err
		}
	}

	return nil
}
