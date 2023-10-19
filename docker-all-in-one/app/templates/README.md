The directory contains dynamic templates, mostly synchronized from `jans-linux-setup` and `flex-linux-setup`.

```
/app/templates
├── README.md
├── agama.ldif
├── attributes.ldif
├── base.ldif
├── casa_client.ldif
├── casa_config.ldif
├── casa_person_authentication_script.ldif
├── casa_scopes.ldif
├── configuration.ldif
├── etc
│   └── jans
│       └── conf
│           ├── jans-couchbase.properties
│           ├── jans-ldap.properties
│           ├── jans-mysql.properties
│           ├── jans-pgsql.properties
│           ├── jans-spanner.properties
│           ├── jans.properties
│           └── salt
├── jans-auth
│   ├── configuration.ldif
│   ├── groups.ldif
│   ├── jans-auth-config.json
│   ├── jans-auth-errors.json
│   ├── jans-auth-static-conf.json
│   ├── people.ldif
│   ├── role-scope-mappings.json
│   └── role-scope-mappings.ldif
├── jans-cli
│   └── client.ldif
├── jans-config-api
│   ├── clients.ldif
│   ├── config.ldif
│   ├── dynamic-conf.json
│   ├── scim-scopes.ldif
│   └── scopes.ldif
├── jans-fido2
│   ├── dynamic-conf.json
│   ├── fido2.ldif
│   └── static-conf.json
├── jans-scim
│   ├── clients.ldif
│   ├── configuration.ldif
│   ├── dynamic-conf.json
│   ├── scopes.ldif
│   └── static-conf.json
├── log4j2.xml
├── o_metric.ldif
├── o_site.ldif
├── opt
│   └── jans
│       └── jetty
│           ├── casa
│           │   ├── resources
│           │   │   └── log4j2.xml
│           │   └── webapps
│           │       └── casa.xml
│           ├── jans-auth
│           │   ├── resources
│           │   │   └── log4j2.xml
│           │   └── webapps
│           │       └── jans-auth.xml
│           ├── jans-config-api
│           │   ├── resources
│           │   │   └── log4j2.xml
│           │   └── webapps
│           │       └── jans-config-api.xml
│           ├── jans-fido2
│           │   ├── resources
│           │   │   └── log4j2.xml
│           │   └── webapps
│           │       └── jans-fido2.xml
│           └── jans-scim
│               ├── resources
│               │   └── log4j2.xml
│               └── webapps
│                   └── jans-scim.xml
├── scopes.ldif
└── scripts.ldif
```
