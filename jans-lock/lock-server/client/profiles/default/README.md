# Lock client test profiles

A profile is a folder holding `config-jans-lock-test-data.properties`, the same key=value
shape `jans-auth-server/client/profiles/<name>/` uses. Unlike `jans-orm/integration-test`'s
`jans.base`/persistence-config profiles, this is a **Maven resource-filter** profile: the
properties file is registered as a build `<filter>` in `pom.xml`

```xml
<filters>
    <filter>${basedir}/profiles/${cfg}/config-jans-lock-test-data.properties</filter>
</filters>
```

and its keys are substituted (`${key}`) into the filtered test resources under
`src/test/resources/` (currently `lock-client-test.properties`) at build time. Tests read the
*filtered* resource from `target/test-classes/` at runtime — nothing reads `profiles/` directly.

## Selecting a profile

```bash
cd jans-lock/lock-server
mvn -pl client -Dcfg=<profile name> test
```

`cfg` defaults to `default` when omitted (`pom.xml`'s `set-configuration-name` profile). The
`default` profile's `test.server.name` is the sentinel `CHANGE_ME`: any test that needs a real
server checks for it (`BaseLockClientTest.hasServer()`) and skips itself via
`Assumptions.assumeTrue(...)`, so the build stays green without `-Dcfg`.

## Adding a real profile

```
profiles/<name>/config-jans-lock-test-data.properties
```

with a real `test.server.name` (the bare hostname, e.g. `jans-dev.jans.info`). Only `default/`
is committed (see `../.gitignore`); every other profile folder is local-only, the same convention
`jans-orm/integration-test/profiles` and `jans-auth-server/client/profiles` use.

## Generation during install

`jans-linux-setup` renders `templates/test/jans-lock/client/config-jans-lock-test-data.properties`
into `/opt/jans/jans-setup/output/test/jans-lock/client/` — but **only** when the installer runs
with `-t`/`--test` (the `loadTestData` flag), not on a plain install. Copy it into a local profile
folder the same way `jans-orm/integration-test/profiles/default/README.md` documents:

```bash
cp -r root@<server>:/opt/jans/jans-setup/output/test/jans-lock/client/* profiles/<server>/
```

The generated file currently carries only `test.server.name`; the `trace.client.id`/
`trace.client.secret` keys are reserved for a future TRACE-scoped test client (see the comment in
`default/config-jans-lock-test-data.properties`) and are not populated by the installer today.
