package jans

import (
	"context"
	"testing"
)

func TestCacheConfig(t *testing.T) {

	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	cacheCfg, err := client.GetCacheConfiguration(ctx)
	if err != nil {
		t.Error(err)
	}

	if cacheCfg.CacheProviderType != "NATIVE_PERSISTENCE" {
		t.Fatal("expected cache provider type")
	}

	patches := []PatchRequest{
		{
			Op:    "replace",
			Path:  "/cacheProviderType",
			Value: "REDIS",
		},
	}

	cacheCfg, err = client.PatchCacheConfiguration(ctx, patches)
	if err != nil {
		t.Fatal(err)
	}

	t.Cleanup(func() {
		patches[0].Value = "NATIVE_PERSISTENCE"
		client.PatchCacheConfiguration(ctx, patches)
	})

	if cacheCfg.CacheProviderType != "REDIS" {
		t.Error("expected cache provider type")
	}

}

func TestMemcachedConfig(t *testing.T) {

	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	cacheCfg, err := client.GetCacheConfiguration(ctx)
	if err != nil {
		t.Error(err)
	}

	if cacheCfg.MemcachedConfiguration.Servers == "" {
		t.Fatal("expected memcached servers")
	}

	patches := []PatchRequest{
		{
			Op:    "replace",
			Path:  "/memcachedConfiguration/servers",
			Value: "localhost:11222",
		},
	}

	cacheCfg, err = client.PatchCacheConfiguration(ctx, patches)
	if err != nil {
		t.Fatal(err)
	}

	t.Cleanup(func() {
		patches[0].Value = "localhost:11211"
		client.PatchCacheConfiguration(ctx, patches)
	})

	if cacheCfg.MemcachedConfiguration.Servers != "localhost:11222" {
		t.Fatal("expected updated memcached servers")
	}

}

func TestRedisConfig(t *testing.T) {

	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	cacheCfg, err := client.GetCacheConfiguration(ctx)
	if err != nil {
		t.Error(err)
	}

	if cacheCfg.RedisConfiguration.Servers == "" {
		t.Fatal("expected redis servers")
	}

	patches := []PatchRequest{
		{
			Op:    "replace",
			Path:  "/redisConfiguration/servers",
			Value: "localhost:11222",
		},
	}

	cacheCfg, err = client.PatchCacheConfiguration(ctx, patches)
	if err != nil {
		t.Fatal(err)
	}

	t.Cleanup(func() {
		patches[0].Value = "localhost:6379"
		client.PatchCacheConfiguration(ctx, patches)
	})

	if cacheCfg.RedisConfiguration.Servers != "localhost:11222" {
		t.Fatalf("expected updated redis servers, got %v", cacheCfg.RedisConfiguration.Servers)
	}

}

func TestInMemoryCacheConfig(t *testing.T) {

	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	cacheCfg, err := client.GetCacheConfiguration(ctx)
	if err != nil {
		t.Error(err)
	}

	if cacheCfg.InMemoryConfiguration.DefaultPutExpiration != 60 {
		t.Fatal("expected different value for put expiration")
	}

	patches := []PatchRequest{
		{
			Op:    "replace",
			Path:  "/inMemoryConfiguration/defaultPutExpiration",
			Value: 120,
		},
	}

	cacheCfg, err = client.PatchCacheConfiguration(ctx, patches)
	if err != nil {
		t.Fatal(err)
	}

	t.Cleanup(func() {
		patches[0].Value = 60
		client.PatchCacheConfiguration(ctx, patches)
	})

	if cacheCfg.InMemoryConfiguration.DefaultPutExpiration != 120 {
		t.Fatal("expected updated in-memory cache config servers")
	}

}

func TestNativePersistenceCacheConfig(t *testing.T) {

	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	cacheCfg, err := client.GetCacheConfiguration(ctx)
	if err != nil {
		t.Error(err)
	}

	if cacheCfg.NativePersistenceConfiguration.DefaultPutExpiration != 60 {
		t.Fatal("expected different value for put expiration")
	}

	patches := []PatchRequest{
		{
			Op:    "replace",
			Path:  "/nativePersistenceConfiguration/defaultPutExpiration",
			Value: 120,
		},
	}

	cacheCfg, err = client.PatchCacheConfiguration(ctx, patches)
	if err != nil {
		t.Fatal(err)
	}

	t.Cleanup(func() {
		patches[0].Value = 60
		client.PatchCacheConfiguration(ctx, patches)
	})

	if cacheCfg.NativePersistenceConfiguration.DefaultPutExpiration != 120 {
		t.Fatal("expected updated in-memory cache config servers")
	}

}
