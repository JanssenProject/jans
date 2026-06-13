---
tags:
  - administration
  - auth-server
  - cryptography
  - key-generation
  - key-rotation
---

# Regenerate Jans Auth cryptographic keys

This procedure regenerates the Jans Auth signing and encryption keys on a package-based Janssen Server installation and keeps the following three locations synchronized:

1. The PKCS#12 keystore containing the private keys.
2. The public JWKS file on the filesystem.
3. The `jansConfWebKeys` value in persistence.

!!! danger
    Replacing only the PKCS#12 file, or only `jansConfWebKeys`, breaks key resolution. Jans Auth may then fail to sign tokens and can report errors such as:

    ```text
    Failed to find private key by kid
    check whether web keys JSON in persistence corresponds to keystore file
    ```

    Generate the keystore and JWKS as one pair, validate them, and update the filesystem and persistence in the same maintenance window.

## Scope

The commands below apply to a Linux package installation with:

- Jans Auth installed under `/opt/jans`
- the active keystore at `/etc/certs/jans-auth-keys.pkcs12`
- PostgreSQL persistence
- the `jansAppConf` table containing `jansConfDyn` and `jansConfWebKeys`

For LDAP or another persistence type, export and update the same `jansConfWebKeys` attribute using the appropriate persistence tooling instead of the PostgreSQL commands shown here.

## Prerequisites

Run the procedure as `root`.

Confirm that the required files and utilities exist:

```bash
test -x /opt/jre/bin/java
test -x /opt/jre/bin/keytool
test -f /opt/dist/jans/jans-auth-client-jar-with-dependencies.jar
command -v jq
command -v psql
```

Set reusable paths:

```bash
export JANS_AUTH_KEYSTORE=/etc/certs/jans-auth-keys.pkcs12
export JANS_AUTH_JWKS=/etc/certs/jans-auth-keys.json
export KEYGEN_JAR=/opt/dist/jans/jans-auth-client-jar-with-dependencies.jar
export RECOVERY_DIR=/root/jans-auth-key-recovery-$(date +%F-%H%M%S)

mkdir -p "$RECOVERY_DIR"
chmod 700 "$RECOVERY_DIR"
```

## 1. Read the configured keystore location and secret

Read the Jans Auth dynamic configuration from PostgreSQL:

```bash
sudo -u postgres psql -d jansdb -Atc \
'SELECT "jansConfDyn" FROM "jansAppConf" WHERE "jansConfDyn" IS NOT NULL;' \
| jq -r '
  paths(scalars) as $p
  | select(
      ($p | map(tostring) | join("."))
      | test("keyStoreFile|keyStoreSecret"; "i")
    )
  | "\($p | map(tostring) | join("."))=\(getpath($p))"
'
```

Example:

```text
keyStoreFile=/etc/certs/jans-auth-keys.pkcs12
keyStoreSecret=<configured-secret>
```

Use the configured secret. Do not arbitrarily assign a new password unless `keyStoreSecret` is also intentionally changed in persistence.

Store it without echoing it to the terminal:

```bash
read -rsp "Jans Auth keystore password: " JANS_AUTH_KEYPASS
echo
export JANS_AUTH_KEYPASS
```

## 2. Back up the current key material and persistence value

```bash
export BACKUP_DIR=/root/jans-auth-key-backup-$(date +%F-%H%M%S)

mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"

cp -a "$JANS_AUTH_KEYSTORE" "$BACKUP_DIR/"
cp -a "$JANS_AUTH_JWKS" "$BACKUP_DIR/" 2>/dev/null || true

sudo -u postgres psql -d jansdb -Atc \
'SELECT "jansConfWebKeys"
   FROM "jansAppConf"
  WHERE "jansConfWebKeys" IS NOT NULL;' \
> "$BACKUP_DIR/jansConfWebKeys.json"

chmod 600 "$BACKUP_DIR"/*
ls -lh "$BACKUP_DIR"
```

Confirm that the backup files are non-zero:

```bash
find "$BACKUP_DIR" -maxdepth 1 -type f -size 0 -print
```

The command must return no output.

## 3. Generate a new keystore and JWKS pair

!!! warning
    Generate into a protected temporary directory. Do not write directly over the active files in `/etc/certs`.

The following algorithm set creates Connect and SSA keys for common Jans Auth signing and encryption operations:

```bash
/opt/jre/bin/java \
  -Dlog4j.defaultInitOverride=true \
  -cp "$KEYGEN_JAR" \
  io.jans.as.client.util.KeyGenerator \
  -key_ops_type ALL \
  -keystore "$RECOVERY_DIR/jans-auth-keys.pkcs12" \
  -keypasswd "$JANS_AUTH_KEYPASS" \
  -sig_keys RS256 RS384 RS512 ES256 ES256K ES384 ES512 PS256 PS384 PS512 \
  -enc_keys RSA1_5 RSA-OAEP ECDH-ES \
  -dnname "CN=Jans Auth CA Certificates" \
  -expiration 3650 \
  > "$RECOVERY_DIR/jans-auth-keys.json"
```

`-expiration` is expressed in days for Connect keys. SSA key validity is handled separately by the generator and is normally much longer.

!!! note
    Supported algorithms can vary by Janssen version and Java security provider. Check the generator options on the installed server before changing the algorithm list:

    ```bash
    /opt/jre/bin/java \
      -cp "$KEYGEN_JAR" \
      io.jans.as.client.util.KeyGenerator -h
    ```

    Preserve every algorithm required by the deployment's clients and dynamic configuration. Removing an algorithm that is actively configured can break token signing, encryption, decryption, or client authentication.

## 4. Validate the generated pair

Validate the JSON:

```bash
jq empty "$RECOVERY_DIR/jans-auth-keys.json"
jq '.keys | length' "$RECOVERY_DIR/jans-auth-keys.json"
```

Validate the keystore password and inspect its entries:

```bash
/opt/jre/bin/keytool -list -v \
  -storetype PKCS12 \
  -keystore "$RECOVERY_DIR/jans-auth-keys.pkcs12" \
  -storepass "$JANS_AUTH_KEYPASS" \
| egrep 'Keystore type:|Your keystore contains|Alias name:|Entry type:|Valid from:'
```

Confirm that every generated JWKS `kid` has a corresponding private-key alias in the PKCS#12 keystore:

```bash
comm -3 \
  <(
    /opt/jre/bin/keytool -list \
      -storetype PKCS12 \
      -keystore "$RECOVERY_DIR/jans-auth-keys.pkcs12" \
      -storepass "$JANS_AUTH_KEYPASS" 2>/dev/null \
    | sed -n 's/^\([^,]*\),.*/\1/p' \
    | sort
  ) \
  <(
    jq -r '.keys[].kid' "$RECOVERY_DIR/jans-auth-keys.json" \
    | sort
  )
```

Expected result: no output.

Do not continue if the command reports any difference.

## 5. Stop Jans Auth

```bash
systemctl stop jans-auth
```

Some package installations report the service as `failed` after a clean stop because the Java process exits with status `143`. Confirm that the process and listener are actually stopped:

```bash
systemctl status jans-auth --no-pager -l
ss -ltnp | grep ':8081' || true
```

There must be no Jans Auth listener on port `8081`.

## 6. Install the new filesystem files

```bash
install -o jetty -g root -m 660 \
  "$RECOVERY_DIR/jans-auth-keys.pkcs12" \
  "$JANS_AUTH_KEYSTORE"

install -o jetty -g root -m 660 \
  "$RECOVERY_DIR/jans-auth-keys.json" \
  "$JANS_AUTH_JWKS"

ls -lh "$JANS_AUTH_KEYSTORE" "$JANS_AUTH_JWKS"
```

## 7. Update `jansConfWebKeys` in PostgreSQL

Compact the generated JWKS and update the configuration in a transaction:

```bash
NEW_JWKS="$(jq -c . "$RECOVERY_DIR/jans-auth-keys.json")"

sudo -u postgres psql -v ON_ERROR_STOP=1 -d jansdb <<SQL
BEGIN;

UPDATE "jansAppConf"
SET "jansConfWebKeys" = \$jwks\$$NEW_JWKS\$jwks\$
WHERE "jansConfWebKeys" IS NOT NULL;

SELECT
  COUNT(*) AS rows_with_webkeys,
  jsonb_array_length("jansConfWebKeys"::jsonb -> 'keys') AS key_count
FROM "jansAppConf"
WHERE "jansConfWebKeys" IS NOT NULL
GROUP BY "jansConfWebKeys";

COMMIT;
SQL
```

Expected result includes:

```text
UPDATE 1
COMMIT
```

The reported `key_count` must equal:

```bash
jq '.keys | length' "$RECOVERY_DIR/jans-auth-keys.json"
```

## 8. Start Jans Auth

```bash
systemctl reset-failed jans-auth
systemctl start jans-auth
sleep 20
systemctl status jans-auth --no-pager -l
```

The service must be `active (running)`.

## 9. Verify the published JWKS

Determine the public issuer hostname, then query the Jans Auth JWKS endpoint:

```bash
export JANS_FQDN=your-jans-host.example.org

curl -fsSk "https://${JANS_FQDN}/jans-auth/restv1/jwks" \
| jq -r '"published_key_count=\(.keys | length)", (.keys[].kid)'
```

The number of published keys can be lower than the number generated. Jans Auth publishes only algorithms enabled and supported by the running configuration and provider.

Confirm that every published `kid` exists in the generated JWKS:

```bash
comm -23 \
  <(
    curl -fsSk "https://${JANS_FQDN}/jans-auth/restv1/jwks" \
    | jq -r '.keys[].kid' \
    | sort
  ) \
  <(
    jq -r '.keys[].kid' "$RECOVERY_DIR/jans-auth-keys.json" \
    | sort
  )
```

Expected result: no output.

Also verify discovery:

```bash
curl -fsSk "https://${JANS_FQDN}/.well-known/openid-configuration" \
| jq '{issuer, jwks_uri, token_endpoint, authorization_endpoint}'
```

## 10. Restart dependent Jans services

Applications can cache discovery metadata or JWKS values. After Jans Auth has started successfully, restart the remaining Jans services and Apache:

```bash
systemctl restart 'jans-*' apache2
```

Verify their status:

```bash
systemctl list-units --no-pager \
  --type=service \
  'jans-*' \
  apache2.service
```

## 11. Functional validation

Use a new private/incognito browser session and test:

1. Open the Admin UI.
2. Authenticate.
3. Complete the authorization-code redirect.
4. Confirm that the Admin UI loads without an HTTP 500 response.
5. Test any critical OIDC, OAuth, UMA, SSA, or encrypted-token workflows used by the deployment.

Monitor Jans Auth while testing:

```bash
tail -F \
  /opt/jans/jetty/jans-auth/logs/jans-auth.log \
  /opt/jans/jetty/jans-auth/logs/"$(date +%F | tr - _).jetty.log" \
| egrep -i \
  'private key|kid|null|sign|keystore|exception|error|failed'
```

## Rollback

Use rollback when Jans Auth does not start or functional validation fails.

Stop Jans Auth:

```bash
systemctl stop jans-auth
```

Restore the backed-up files:

```bash
install -o jetty -g root -m 660 \
  "$BACKUP_DIR/jans-auth-keys.pkcs12" \
  "$JANS_AUTH_KEYSTORE"

if test -f "$BACKUP_DIR/jans-auth-keys.json"; then
  install -o jetty -g root -m 660 \
    "$BACKUP_DIR/jans-auth-keys.json" \
    "$JANS_AUTH_JWKS"
fi
```

Restore `jansConfWebKeys`:

```bash
OLD_JWKS="$(jq -c . "$BACKUP_DIR/jansConfWebKeys.json")"

sudo -u postgres psql -v ON_ERROR_STOP=1 -d jansdb <<SQL
BEGIN;

UPDATE "jansAppConf"
SET "jansConfWebKeys" = \$jwks\$$OLD_JWKS\$jwks\$
WHERE "jansConfWebKeys" IS NOT NULL;

COMMIT;
SQL
```

Restart and verify:

```bash
systemctl reset-failed jans-auth
systemctl start jans-auth
systemctl restart 'jans-*' apache2
systemctl status jans-auth --no-pager -l
```

## Operational recommendations

- Treat the PKCS#12 keystore and `jansConfWebKeys` as one atomic configuration unit.
- Never overwrite the active keystore before creating and validating a backup.
- Never use a short test validity such as two or ten days in production.
- Keep the keystore password aligned with `keyStoreSecret` in `jansConfDyn`.
- Restrict the keystore and JWKS files to the service account.
- Schedule key rotation before expiration and test it in a non-production environment first.
- Retain previous public keys for an overlap period when existing signed tokens must remain verifiable. A destructive replacement can invalidate tokens that were signed with the previous keys.
- For clustered deployments, distribute the same keystore to every Jans Auth node and update shared persistence only once. Restart nodes in a controlled sequence.
