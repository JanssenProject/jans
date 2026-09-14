db.schema.name=${rdbm_schema}

% if context.get('rdbm_type') == 'pgsql':
######### POSTGRESQL PROPERTIES BEGINS ##############

connection.uri=jdbc:postgresql://${rdbm_host}:${rdbm_port}/${rdbm_db}
connection.driver-property.ssl=${rdbm_enable_ssl}
connection.driver-property.sslmode=${rdbm_sslmode}
connection.driver-property.sslfactory=org.postgresql.ssl.SingleCertValidatingFactory
connection.driver-property.sslfactoryarg=file:${postgresql_ca_crt_fn}

# Prefix connection.driver-property.key=value will be coverterd to key=value JDBC driver properties
#connection.driver-property.driverProperty=driverPropertyValu



auth.userName=${rdbm_user}
auth.userPassword=${rdbm_password_enc}

# Password hash method
password.encryption.method=SSHA-256

# Argon 2 parameters
# 0 - ARGON2_d, 1 - ARGON2_i, 2 - ARGON2_id
#password.method.argon2.type=2
# 1.0 - 16, 1.3 - 19
#password.method.argon2.version=19
#password.method.argon2.salt-length=16
#password.method.argon2.memory=7168
#password.method.argon2.iterations=5
#password.method.argon2.parallelism=1
#password.method.argon2.hash-length=32

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

# Sets whether objects created for the pool will be validated before being returned from it
#connection.pool.test-on-create=true

# Sets whether objects borrowed from the pool will be validated when they are returned to the pool
#connection.pool.test-on-return=true

# Enable check after entry merge
# `true` value will enable entry load after update to check if all attributes equal to attributes which server requested to persist
#orm.validate-after-update=false
binaryAttributes=objectGUID
certificateAttributes=userCertificate

# disable time zone
db.disable.time-zone=true


% elif context.get('rdbm_type') == 'mysql':
######### MYSQL PROPERTIES ##############

connection.uri=jdbc:mysql://${rdbm_host}:${rdbm_port}/${rdbm_db}?enabledTLSProtocols=TLSv1.2
connection.driver-property.sslMode=${rdbm_sslmode}

connection.driver-property.serverTimezone=${server_time_zone}
# Prefix connection.driver-property.key=value will be coverterd to key=value JDBC driver properties
#connection.driver-property.driverProperty=driverPropertyValue

#connection.driver-property.useServerPrepStmts=false
connection.driver-property.cachePrepStmts=false
connection.driver-property.cacheResultSetMetadata=true
connection.driver-property.metadataCacheSize=500
#connection.driver-property.prepStmtCacheSize=500
#connection.driver-property.prepStmtCacheSqlLimit=1024

auth.userName=${rdbm_user}
auth.userPassword=${rdbm_password_enc}

# Password hash method
password.encryption.method=SSHA-256

# Argon 2 parameters
# 0 - ARGON2_d, 1 - ARGON2_i, 2 - ARGON2_id
#password.method.argon2.type=2
# 1.0 - 16, 1.3 - 19
#password.method.argon2.version=19
#password.method.argon2.salt-length=16
#password.method.argon2.memory=7168
#password.method.argon2.iterations=5
#password.method.argon2.parallelism=1
#password.method.argon2.hash-length=32

# Connection pool size
connection.pool.max-total=40
connection.pool.max-idle=15
connection.pool.min-idle=5

# Max time needed to create connection pool in milliseconds
connection.pool.create-max-wait-time-millis=20000

# Max wait 20 seconds
connection.pool.max-wait-time-millis=20000

# Evict idle connections after 30 minutes (must be less than MySQL wait_timeout)
connection.pool.min-evictable-idle-time-millis=1800000
 
# Enable validation query (SELECT 1) to detect stale connections before use.
# Prevents "last packet received X milliseconds ago" errors after MySQL closes idle connections.
# Set to false only if you want to handle reconnection manually.
connection.pool.validation-enabled=true

# Sets whether objects created for the pool will be validated before being returned from it
#connection.pool.test-on-create=true

# Validate connection before borrowing from pool.
# Ensures a stale connection is never handed to the application.
connection.pool.test-on-borrow=true
 
# Validate idle connections in background eviction thread.
# Allows proactive removal of connections killed by MySQL on the server side.
connection.pool.test-while-idle=true
 
# How often the background eviction thread runs (5 minutes).
# Must be set for test-while-idle to have any effect.
# Should be well below MySQL wait_timeout (default 28800s, commonly overridden to 3600s).
connection.pool.time-between-eviction-runs-millis=300000

# Sets whether objects borrowed from the pool will be validated when they are returned to the pool
#connection.pool.test-on-return=true

# Enable check after entry merge
# `true` value will enable entry load after update to check if all attributes equal to attributes which server requested to persist
#orm.validate-after-update=false

binaryAttributes=objectGUID
certificateAttributes=userCertificate

mysql.simple-json=true

% endif
