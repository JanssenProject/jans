---
tags:
  - administration
  - reference
  - database
  - remote database
---

### MySQL as choice for Persistence
While installing the Jans Server, the administrator will be presented a menu of the possible choices for the persistence layer (LDAP, MySQL, Couchbase, Postgres etc). 
MySQL database can be installed in 2 possible ways and the administrator will be prompted to make this choice during installation:
1. **Locally** : 
<br>The administrator can configure:
*  RDBM username
*  RDBM password
*  RDBM database name
2. **Remotely** :
<br>The administrator will be prompted to configure:
*  RDBM username
*  RDBM password
*  RDBM port
*  RDBM database
*  RDBM host

### Database configuration file name 
The database configurations are stored in `/etc/gluu/conf/jans-sql.properties` and contain the following information: 
 ```
 db.schema.name=jansdb

 connection.uri=jdbc:mysql://localhost:3306/jansdb?enabledTLSProtocols=TLSv1.2

 connection.driver-property.serverTimezone=UTC+0000
 # Prefix connection.driver-property.key=value will be coverterd to key=value JDBC driver properties
 #connection.driver-property.driverProperty=driverPropertyValue

 #connection.driver-property.useServerPrepStmts=false
 connection.driver-property.cachePrepStmts=false
 connection.driver-property.cacheResultSetMetadata=true
 connection.driver-property.metadataCacheSize=500
 #connection.driver-property.prepStmtCacheSize=500
 #connection.driver-property.prepStmtCacheSqlLimit=1024

 auth.userName=vi....bo
 auth.userPassword=qphWF1h....XmUAvn9g==

 # Password hash method
 password.encryption.method=SSHA-256

 # Connection pool size
 connection.pool.max-total=40
 connection.pool.max-idle=15
 connection.pool.min-idle=5

 # Max time needed to create connection pool in milliseconds
 connection.pool.create-max-wait-time-millis=20000

 # Max wait 20 seconds
 connection.pool.max-wait-time-millis=20000

 # Allow to evict connection in pool after 30 minutes
 connection.pool.min-evictable-idle-time-millis=1800000

 binaryAttributes=objectGUID
 certificateAttributes=userCertificate

 ```

