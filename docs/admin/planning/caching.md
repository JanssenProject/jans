---
tags:
  - planning
  - Redis
  - Couchbase
  - Memcached
---

There are two reasons to use caching. First, to improve performance by
reducing the number of writes to the disk. Second, to share session data
in a clustered deployment. Janssen supports a few different options for
caching, as controlled by the `cacheProviderType`. Also keep in mind that unless
the `sessionIdPersistInCache` is set to `True`, Auth Server will store sessions
in the database.

1. **In-Memory** If you only have one server, you can use RAM as the cache.
Watch the memory usage of Auth Server--if it gets too high you may want to
switch to another cache mechanism.

1. **Database** A "database cache" is an oxymoron. But in cases where you don't
want another component or service, but you need the session replication for a
cluster, it may be convenient to persist the "cache" data in the database.

1. **Couchbase**  If you are already using Couchbase for persistence, then
Janssen Auth Server can use "ephemeral buckets", which exist only in memory,
for caching.

1. **Redis** The best choice if you need a cache service for LDAP, RDBMS,
or Spanner. Great performance and low cache miss rate. Commercial Redis
supports TLS, which is a good option if you need secure communication.

1. **Memcached** Still a good choice, especially if that's what you already
run for other applications. We have observed a slightly higher cache miss
rate under high load, which is fairly atypical for most login applications.

1. **Cloud Cache** Amazon ElastiCache or Google Memorycache offer both Redis
and Memcached services. They work fine.
