---
tags:
  - planning
  - LDAP
  - MySQL
  - Couchbase
  - Aurora
  - Spanner
  - Postgres
---

The performance of the web tier is derived from the performance of the
persistence tier. While memory-caching short lived entities to reduce
persistence is an option, there are still long lived objects that need to be
written to the disk.

Picking the right database has a huge impact on the operational burden of a
digital identity infrastructure. All database administration requires some
black art--every platform has some secret knobs and levers that are critical
to tuning, availability and rapid diagnosis. So beyond the performance
requirements that determine the right choice for a database, you also need to
consider operational concerns.

Janssen's strategy is to provide optionality for persistence. There is no one
size fits all solution for databases. The following section will detail some of
the pros and cons of the various databases we currently support.

1. **OpenDJ (LDAP)** Janssen supports Gluu's distribution of OpenDJ, and probably any
other similar distributions like ForgeRock OpenDJ, Ping Directory Server, or
Oracle Unified Directory. LDAP in general and OpenDJ in particular have been  
successfully backing authentication service for more then 20 years. People tend
to think of the LDAP tree structure as fast for reads, and slow for writes.
That's just not true anymore--OpenDJ is able to perform quite well for write
operations as well. OpenDJ has mature replication support, excellent command
line tools for administration, and excellent stability. The main disadvantage
of OpenDJ is scaling large datasets for high concurrency. While you can get
around this shortcoming with a global LDAP proxy, such a topology gets
complicated and costly to operate, as you have to break up the data and
configure multiple replicated topologies. As a rule of thumb, if concurrency of
more then 120 OpenID code flow authentications per second are needed, you should
consider another database. But for concurrency less then this, OpenDJ is an
excellent choice.

1. **MySQL** You know it... you love it. That's the biggest advantage.
Performance is great out of the box. But if you have high concurrency,
you'll have to figure out a plan for replication, and horizontal scaling.

1. **Postgres** Same as MySQL above, but there are some great commercial
distributions of Postgres like [EnterpriseDB](https://www.enterprisedb.com/).

1. **Couchbase** A JSON NoSQL database that supports automatic distribution
of data for auto-scaling multi-region sharded cloud native deployments. Janssen
only supports the commercial distribution--but the previously mentioned database
properties are enterprise class. If you need to host your own database, and you
need infinite horizontal scalability, Couchbase should be your goto choice.

1. **Aurora** So you want MySQL, but you want Amazon to handle some of the care
and feeding? Aurora enables you to consume database as a cloud service.
Scalability is excellent and multi-region deployments are [possible](https://aws.amazon.com/blogs/database/deploy-multi-region-amazon-aurora-applications-with-a-failover-blueprint/).
The main catch is that write operations are limited to one region, with the
ability failover to another region. But to accomplish this, you need a cloud
engineer to implement it.

1. **Spanner** Google's multi-region cloud database as a service, Spanner
was purpose built for auto-scaling, multi-region persistence. It has it's own
API, although recently Google added support for MySQL and Postgres drivers.

1. **MariaDB** Coming soon! Similar to MySQL but more cutting edge.
