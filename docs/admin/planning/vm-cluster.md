---
tags:
  - administration
  - planning
---

What if you need four or five nines availability, but cloud native is not
an option? You could cluster Janssen using VM's, but you'll have to do
some legwork, because we don't publish tools for this task. This planning
guide will cover some of the things you'll need to consider.

1. **Hostname** Remember when you run the linux setup to use the hostname of the
cluster,not the hostname of the individual server.  This is important because
the OpenID Provider metadata needs to use the public hostname (i.e. the URL's
you'll find at `/.well-known/openid-configuration`)

1. **Load Balancing / SSL** As Auth Server and FIDO (the two Internet facing
components) are stateless,  you can use any load balancer routing algorithm,
even round robin. You can also use the load balancer to terminate SSL.

1. **Database** The web services Janssen Components share the database, so
you'll have to use database replication (e.g. OpenDJ replication). You could
also use a cloud database that takes care of replication for you.

1. **Cache** You can't use `IN-MEMORY` cache which would have no way to
replicate to the other nodes in the network. You could use the Database
for caching, although this will impact performance, although it's only an
issue for high concurrency use cases. If you are using Couchbase, it has
caching built-in. But if you are another database and need high concurrency,
our recommendation is to use Redis for caching, which has less cache hit
misses then Memcached.

1. **Key management** Where to you store the private keys? If on the file
system, you'll have to make sure that they get securely copied. This is
trickier then it sounds because of key rotation. OpenID Connect working group
suggests key rotation every two day, to make sure developers who write OpenID
applications handle key rotation properly in their code, and don't hardcode
the metadata, but retrieve it from the OpenID configuration endpoint.

1. **Customizations** You must copy any libraries, xhtml pages, CSS or
javascript must be available on all servers.
