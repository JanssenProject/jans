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

	cacheCfg.CacheProviderType = "REDIS"
	if err := client.UpdateCacheConfiguration(ctx, cacheCfg); err != nil {
		t.Fatal(err)
	}

	cacheCfg, err = client.GetCacheConfiguration(ctx)
	if err != nil {
		t.Fatal(err)
	}

	if cacheCfg.CacheProviderType != "REDIS" {
		t.Error("expected cache provider type")
	}

	cacheCfg.CacheProviderType = "NATIVE_PERSISTENCE"
	if err := client.UpdateCacheConfiguration(ctx, cacheCfg); err != nil {
		t.Fatal(err)
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

	cacheCfg.MemcachedConfiguration.Servers = "localhost:11222"
	if err := client.UpdateCacheConfiguration(ctx, cacheCfg); err != nil {
		t.Fatal(err)
	}

	cacheCfg, err = client.GetCacheConfiguration(ctx)
	if err != nil {
		t.Error(err)
	}

	if cacheCfg.MemcachedConfiguration.Servers != "localhost:11222" {
		t.Fatal("expected updated memcached servers")
	}

	cacheCfg.MemcachedConfiguration.Servers = "localhost:11211"
	if err := client.UpdateCacheConfiguration(ctx, cacheCfg); err != nil {
		t.Fatal(err)
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

	cacheCfg.RedisConfiguration.Servers = "localhost:6389"
	if err := client.UpdateCacheConfiguration(ctx, cacheCfg); err != nil {
		t.Fatal(err)
	}

	cacheCfg, err = client.GetCacheConfiguration(ctx)
	if err != nil {
		t.Error(err)
	}

	if cacheCfg.RedisConfiguration.Servers != "localhost:6389" {
		t.Fatal("expected updated redis servers")
	}

	cacheCfg.RedisConfiguration.Servers = "localhost:6379"
	if err := client.UpdateCacheConfiguration(ctx, cacheCfg); err != nil {
		t.Fatal(err)
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

	cacheCfg.InMemoryConfiguration.DefaultPutExpiration = 120
	if err := client.UpdateCacheConfiguration(ctx, cacheCfg); err != nil {
		t.Fatal(err)
	}

	cacheCfg, err = client.GetCacheConfiguration(ctx)
	if err != nil {
		t.Error(err)
	}

	if cacheCfg.InMemoryConfiguration.DefaultPutExpiration != 120 {
		t.Fatal("expected updated in-memory cache config servers")
	}

	cacheCfg.InMemoryConfiguration.DefaultPutExpiration = 60
	if err := client.UpdateCacheConfiguration(ctx, cacheCfg); err != nil {
		t.Fatal(err)
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

	cacheCfg.NativePersistenceConfiguration.DefaultPutExpiration = 120
	if err := client.UpdateCacheConfiguration(ctx, cacheCfg); err != nil {
		t.Fatal(err)
	}

	cacheCfg, err = client.GetCacheConfiguration(ctx)
	if err != nil {
		t.Error(err)
	}

	if cacheCfg.NativePersistenceConfiguration.DefaultPutExpiration != 120 {
		t.Fatal("expected updated in-memory cache config servers")
	}

	cacheCfg.NativePersistenceConfiguration.DefaultPutExpiration = 60
	if err := client.UpdateCacheConfiguration(ctx, cacheCfg); err != nil {
		t.Fatal(err)
	}

}
