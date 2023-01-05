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
    deps = ["config", "secret", "ldap"]
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

Create ``/app/templates/jans-ldap.properties.tmpl``:

```
bindDN: %(ldap_binddn)s
bindPassword: %(encoded_ox_ldap_pw)s
servers: %(ldap_hostname)s:%(ldaps_port)s

useSSL: true
ssl.trustStoreFile: %(ldapTrustStoreFn)s
ssl.trustStorePin: %(encoded_ldapTrustStorePass)s
ssl.trustStoreFormat: pkcs12

maxconnections: 10

# Max wait 20 seconds
connection.max-wait-time-millis=20000

# Force to recreate polled connections after 30 minutes
connection.max-age-time-millis=1800000

# Invoke connection health check after checkout it from pool
connection-pool.health-check.on-checkout.enabled=false

# Interval to check connections in pool. Value is 3 minutes. Not used when onnection-pool.health-check.on-checkout.enabled=true
connection-pool.health-check.interval-millis=180000

# How long to wait during connection health check. Max wait 20 seconds
connection-pool.health-check.max-response-time-millis=20000

binaryAttributes=objectGUID
certificateAttributes=userCertificate
```

Create a Python script to configure persistence:

```py
import os

from jans.pycloudlib import get_manager
from jans.pycloudlib.persistence import render_salt
from jans.pycloudlib.persistence import render_base_properties
from jans.pycloudlib.persistence import render_ldap_properties
from jans.pycloudlib.persistence import sync_ldap_truststore


def create_manager():
    # use Kubernetes ConfigMap as config layer
    os.environ["CN_CONFIG_ADAPTER"] = "kubernetes"

    # use Kubernetes Secret as secret layer
    os.environ["CN_SECRET_ADAPTER"] = "kubernetes"

    manager = get_manager()
    return manager


def configure_persistence(manager):
    persistence_type = os.environ.get("CN_PERSISTENCE_TYPE", "ldap")

    render_salt(manager, "/app/templates/salt.tmpl", "/etc/jans/conf/salt")
    render_base_properties("/app/templates/jans.properties.tmpl", "/etc/jans/conf/jans.properties")

    render_ldap_properties(
        manager,
        "/app/templates/jans-ldap.properties.tmpl",
        "/etc/jans/conf/jans-ldap.properties",
    )
    sync_ldap_truststore(manager)


if __name__ == "__main__":
    manager = create_manager()
    configure_persistence(manager)
```
