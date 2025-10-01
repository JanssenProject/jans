!!! note
    This page is a work-in-progress.

## Config and Secret

Janssen Server app container at its core relies on config and secret layers to self-configure the container.

```py
import os

from jans.pycloudlib import get_manager


def create_manager():
    # use Kubernetes ConfigMap as config layer
    os.environ["CN_CONFIG_ADAPTER"] = "kubernetes"

    # use Kubernetes Secret as secret layer
    os.environ["CN_SECRET_ADAPTER"] = "kubernetes"

    manager = get_manager()
    return manager


if __name__ == "__main__":
    manager = create_manager()

    # get hostname of Janssen Server
    manager.config.get("hostname")

    # get SSL cert of web-facing interface (i.e. nginx or ingress)
    manager.secret.get("ssl_cert")
```

## Startup Orders

```py
import os

from jans.pycloudlib import get_manager
from jans.pycloudlib.wait import wait_for


def create_manager():
    # use Kubernetes ConfigMap as config layer
    os.environ["CN_CONFIG_ADAPTER"] = "kubernetes"

    # use Kubernetes Secret as secret layer
    os.environ["CN_SECRET_ADAPTER"] = "kubernetes"

    manager = get_manager()
    return manager


def wait(manager):
    # ensure startup orders is guarded by waiting for readiness of
    # the following dependencies
    deps = ["config", "secret", "sql"]
    wait_for(manager, deps)


if __name__ == "__main__":
    manager = create_manager()
    wait(manager)
```

## Persistence

Create ``/app/templates/salt.tmpl``:

    encodeSalt = %(encode_salt)s

Create ``/app/templates/jans.properties.tmpl``:

```
persistence.type=%(persistence_type)s

jansAuth_ConfigurationEntryDN=ou=jans-auth,ou=configuration,o=jans
fido2_ConfigurationEntryDN=ou=jans-fido2,ou=configuration,o=jans
scim_ConfigurationEntryDN=ou=jans-scim,ou=configuration,o=jans
configApi_ConfigurationEntryDN=ou=jans-config-api,ou=configuration,o=jans

certsDir=/etc/certs
confDir=
pythonModulesDir=/opt/jans/python/libs:/opt/jython/Lib/site-packages
```

Create ``/app/templates/jans-mysql.properties``:

```
db.schema.name=%(rdbm_schema)s

connection.uri=jdbc:mysql://%(rdbm_host)s:%(rdbm_port)s/%(rdbm_db)s?enabledTLSProtocols=TLSv1.2

connection.driver-property.serverTimezone=%(server_time_zone)s
# Prefix connection.driver-property.key=value will be coverterd to key=value JDBC driver properties
#connection.driver-property.driverProperty=driverPropertyValue

#connection.driver-property.useServerPrepStmts=false
connection.driver-property.cachePrepStmts=false
connection.driver-property.cacheResultSetMetadata=true
connection.driver-property.metadataCacheSize=500
#connection.driver-property.prepStmtCacheSize=500
#connection.driver-property.prepStmtCacheSqlLimit=1024

auth.userName=%(rdbm_user)s
auth.userPassword=%(rdbm_password_enc)s

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

Create a Python script to configure persistence:

```py
import os

from jans.pycloudlib import get_manager
from jans.pycloudlib.persistence.utils import render_salt
from jans.pycloudlib.persistence.utils import render_base_properties
from jans.pycloudlib.persistence.sql import render_sql_properties
from jans.pycloudlib.persistence.sql import sync_sql_password


def create_manager():
    # use Kubernetes ConfigMap as config layer
    os.environ["CN_CONFIG_ADAPTER"] = "kubernetes"

    # use Kubernetes Secret as secret layer
    os.environ["CN_SECRET_ADAPTER"] = "kubernetes"

    manager = get_manager()
    return manager


def configure_persistence(manager):
    persistence_type = os.environ.get("CN_PERSISTENCE_TYPE", "sql")

    render_salt(manager, "/app/templates/salt", "/etc/jans/conf/salt")
    render_base_properties("/app/templates/jans.properties", "/etc/jans/conf/jans.properties")

    render_sql_properties(
        manager,
        "/app/templates/jans-mysql.properties",
        "/etc/jans/conf/jans-mysql.properties",
    )
    sync_sql_password(manager)


if __name__ == "__main__":
    manager = create_manager()
    configure_persistence(manager)
```
