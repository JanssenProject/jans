package io.jans.kc.oidc.impl;

import java.util.Map;

import io.jans.kc.oidc.OIDCMetaCache;

import java.util.HashMap;

public class HashBasedOIDCMetaCache implements OIDCMetaCache{
    
    private static final long DEFAULT_CACHE_TTL = 20*60l; // 20 seconds 

    private long cacheEntryTtl;

    private Map<String,Map<String,CacheEntry>> cacheEntries;
    
    public HashBasedOIDCMetaCache() {
        this(DEFAULT_CACHE_TTL);
    }

    public HashBasedOIDCMetaCache(long cacheEntryTtl) {

        this.cacheEntryTtl = cacheEntryTtl;
        if(this.cacheEntryTtl == 0) {
            this.cacheEntryTtl = DEFAULT_CACHE_TTL;
        }
        this.cacheEntryTtl = this.cacheEntryTtl * 1000; // convert to milliseconds
        this.cacheEntries = new HashMap<>();
    }

    @Override
    public void put(String issuer, String key, Object value) {

        synchronized(cacheEntries) {
            createIfNotExistIssuerCacheEntry(issuer);
            addIssuerCacheEntry(issuer,key,value);
            performHouseCleaning();
        }
     }

    @Override
    public Object get(String issuer, String key) {

        synchronized(cacheEntries) {
            if(issuerCacheEntryIsMissing(issuer)) {
                performHouseCleaning();
                return null;
            }

            Object ret = getIssuerCacheEntryValue(issuer, key);
            performHouseCleaning();
            return ret;
        }
    }

    private boolean issuerCacheEntryIsMissing(String issuer) {

        return cacheEntries.get(issuer) == null;
    }

    private Object getIssuerCacheEntryValue(String issuer, String key) {
        
        Map<String,CacheEntry> issuerCache = cacheEntries.get(issuer);
        return issuerCache.get(key).getValue();
    }

    private void createIfNotExistIssuerCacheEntry(String issuer) {

        cacheEntries.computeIfAbsent(issuer, k-> new HashMap<>());
    }

    private void addIssuerCacheEntry(String issuer,String key, Object value) {

        Map<String,CacheEntry> issuerCache = cacheEntries.get(issuer);
        if(issuerCache == null) {
            return;
        }

        for(Map.Entry<String,CacheEntry> entry : issuerCache.entrySet()) {
            if(entry.getKey().equalsIgnoreCase(key)) {
                //update cache entry
                entry.getValue().updateValue(value);
                return;
            }
        }
        issuerCache.put(key,new CacheEntry(cacheEntryTtl, value));
    }

    private void performHouseCleaning() {

        for(Map.Entry<String,Map<String,CacheEntry>> cacheEntry: cacheEntries.entrySet()) {
            Map<String,CacheEntry> issuerCache = cacheEntries.get(cacheEntry.getKey());
           for(Map.Entry<String,CacheEntry> issuerEntry : issuerCache.entrySet()) {
             if(issuerEntry.getValue().isExpired()) {
                issuerCache.remove(issuerEntry.getKey());
             }
           }
        }
    }

    private class CacheEntry {

        private long updateTime;
        private long ttl;
        private Object value;

        public CacheEntry(long ttl, Object value) {

            this.updateTime = System.currentTimeMillis();
            this.ttl = ttl;
            this.value = value;
        }

        public Object getValue() {

            return this.value;
        }

        public boolean isExpired()  {

            return (System.currentTimeMillis() - this.updateTime) > (this.ttl * 1000) ;
        }

        public void updateValue(Object value) {

            this.value = value;
            this.updateTime = System.currentTimeMillis();
        }

        public void refresh() {

            this.updateTime = System.currentTimeMillis();
        }
    }
}
