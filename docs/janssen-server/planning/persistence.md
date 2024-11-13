---
tags:
  - administration
  - planning
  - persistence
  - MySQL
  - Aurora
  - Postgres
  - database
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

1. **MySQL** You know it... you love it. That's the biggest advantage.
Performance is great out of the box. But if you have high concurrency,
you'll have to figure out a plan for replication, and horizontal scaling. 
**MySQL is our default persistence for Production deployments using Kubernetes**.

1. **Postgres** Same as MySQL above, but there are some great commercial
distributions of Postgres like [EnterpriseDB](https://www.enterprisedb.com/). **Postgres is our default 
persistence for VM based non-production deployments**.

1. **Aurora** So you want MySQL, but you want Amazon to handle some of the care
and feeding? Aurora enables you to consume database as a cloud service.
Scalability is excellent and multi-region deployments are [possible](https://aws.amazon.com/blogs/database/deploy-multi-region-amazon-aurora-applications-with-a-failover-blueprint/).
The main catch is that write operations are limited to one region, with the
ability to failover to another region. But to accomplish this, you need a cloud
engineer to implement it.

