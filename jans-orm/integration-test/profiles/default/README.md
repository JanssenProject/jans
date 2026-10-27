# Persistence test profiles

Profile is a folder with `conf` sub-folder which contains the same configuration files
which real Jans applications load from `/etc/jans/conf`:

```
profiles/<profile name>/conf/
    jans.properties        # persistence.type and base configuration
    jans-sql.properties    # (or jans-ldap.properties / jans-couchbase.properties / jans-spanner.properties)
    salt                   # encodeSalt used to decrypt passwords in properties files
```

Tests bootstrap `PersistenceEntryManager` exactly like real applications:
`jans.base` system property points to the profile folder, `StandalonePersistanceFactoryService`
reads `conf/jans.properties`, resolves `persistence.type` and loads the backend properties file.
Encrypted passwords (`auth.userPassword`, `bindPassword`) are decrypted with `encodeSalt` from `conf/salt`.

## Profile generation

`jans-linux-setup` generates a ready to use profile during test data load (`setup.py -t`):

```
/opt/jans/jans-setup/output/test/jans-orm/conf/
```

Copy this folder into the local checkout:

```
cp -r root@<server>:/opt/jans/jans-setup/output/test/jans-orm/* jans-orm/integration-test/profiles/<server>/
```

## Running

```
cd jans-orm/integration-test
mvn -Dcfg=<profile name> test
```

The `default` profile intentionally has no `conf` folder, so without `-Dcfg` all DB tests
are skipped and the build stays green.
