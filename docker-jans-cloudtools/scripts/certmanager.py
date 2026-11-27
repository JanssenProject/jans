import hashlib
import json
import logging.config
import os
import shlex
import subprocess
import sys
import time
from collections import Counter
from collections import deque
from contextlib import suppress
from datetime import datetime
from datetime import timedelta
from datetime import UTC
from typing import Any

import click

from jans.pycloudlib import get_manager
from jans.pycloudlib.meta import DockerMeta
from jans.pycloudlib.meta import KubernetesMeta
from jans.pycloudlib.persistence.sql import SqlClient
from jans.pycloudlib.persistence.utils import PersistenceMapper
from jans.pycloudlib.utils import as_boolean
from jans.pycloudlib.utils import encode_text
from jans.pycloudlib.utils import generate_base64_contents
from jans.pycloudlib.utils import generate_ssl_ca_certkey
from jans.pycloudlib.utils import generate_signed_ssl_certkey

from settings import LOGGING_CONFIG

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("cloudtools")

SIG_KEYS = "RS256 RS384 RS512 ES256 ES384 ES512 PS256 PS384 PS512"
ENC_KEYS = "RSA1_5 RSA-OAEP ECDH-ES"
KEY_STRATEGIES = ("OLDER", "NEWER", "FIRST")

# default archived JWK lifetime is 31536000 seconds (1 year)
DEFAULT_ARCHIVED_JWK_LIFETIME = 60 * 60 * 24 * 365

# defaults of the `privkey-push-retries` (extra push rounds) and `privkey-push-retry-interval` (seconds) options
DEFAULT_PRIVKEY_PUSH_RETRIES = 3
DEFAULT_PRIVKEY_PUSH_RETRY_INTERVAL = 10
# upper bounds of `privkey-push-retries` and `privkey-push-retry-interval` (seconds); the key rotation CronJob
# forbids concurrent runs, so together they keep the wait between push rounds at 5 minutes or less, far below the
# shortest rotation interval (`interval` is in whole hours)
MAX_PRIVKEY_PUSH_RETRIES = 5
MAX_PRIVKEY_PUSH_RETRY_INTERVAL = 60

# length of a SHA-256 digest in hex characters
SHA256_HEX_LENGTH = 64


def to_non_negative_int(value: object) -> int | None:
    """Return ``value`` as an integer, a negative one as 0; return None if it is not an integer."""
    try:
        return max(int(value), 0)
    except (TypeError, ValueError):
        return None


# TODO: replace with jans.pycloudlib.utils.exec_cmd once the image ships a jans-pycloudlib that has the env parameter
def exec_cmd(cmd, env=None):
    args = shlex.split(cmd)
    popen = subprocess.Popen(  # nosec: B603
        args, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
        env={**os.environ, **env} if env else None,
    )
    stdout, stderr = popen.communicate()
    return stdout.strip(), stderr.strip(), popen.returncode


def keytool_import_key(src_jks_fn, dest_jks_fn, alias, password):
    # keytool reads ``:env`` passwords from the environment, keeping them out of process arguments
    cmd = shlex.join([
        "keytool", "-importkeystore", "-srckeystore", src_jks_fn, "-srcstorepass:env", "KEYTOOL_STOREPASS",
        "-srcalias", alias, "-destkeystore", dest_jks_fn, "-deststorepass:env", "KEYTOOL_STOREPASS",
        "-destalias", alias,
    ])
    return exec_cmd(cmd, env={"KEYTOOL_STOREPASS": password})


def keytool_delete_key(jks_fn, alias, password):
    cmd = shlex.join(["keytool", "-delete", "-alias", alias, "-keystore", jks_fn, "-storepass:env", "KEYTOOL_STOREPASS"])
    return exec_cmd(cmd, env={"KEYTOOL_STOREPASS": password})


def encode_jks(manager, jks="/etc/certs/auth-keys.jks"):
    with open(jks, "rb") as fd:
        return encode_text(fd.read(), manager.secret.get("encoded_salt"))


def generate_openid_keys(passwd, jks_path, dn, exp=48, sig_keys=SIG_KEYS, enc_keys=ENC_KEYS, key_ops_type="connect"):
    if os.path.isfile(jks_path):
        os.unlink(jks_path)

    cmd = (
        "java -Dlog4j.defaultInitOverride=true "
        "-cp /app/javalibs/* "
        "io.jans.as.client.util.KeyGenerator "
        f"-enc_keys {enc_keys} -sig_keys {sig_keys} "
        f"-dnname '{dn}' -expiration_hours {exp} "
        f"-keystore {jks_path} -keypasswd {passwd} "
        f"-key_ops_type {key_ops_type}"
    )
    return exec_cmd(cmd)


def resolve_sig_keys(keys: str) -> str:
    """
    Resolves signing keys.

    :param keys: Space-separated signing keys.
    :returns: Space-separated allowed signing keys.
    """

    default_sig_keys = SIG_KEYS.split()
    sig_keys = []

    for k in keys.split():
        k = k.strip()

        if k not in default_sig_keys:
            continue
        sig_keys.append(k)

    if not sig_keys:
        sig_keys = default_sig_keys
        logger.warning("Signing keys are empty; fallback to default %s", SIG_KEYS)
    return " ".join(sig_keys)


def resolve_enc_keys(keys: str) -> str:
    """
    Resolves encryption keys.

    :param keys: Space-separated encryption keys.
    :returns: Space-separated allowed encryption keys.
    """

    default_enc_keys = ENC_KEYS.split()
    enc_keys = []

    for k in keys.split():
        k = k.strip()

        if k not in default_enc_keys:
            continue
        enc_keys.append(k)

    if not enc_keys:
        enc_keys = default_enc_keys
        logger.warning("Encryption keys are empty; fallback to default %s", ENC_KEYS)
    return " ".join(enc_keys)


def key_ops_from_jwk(jwk):
    """Resolve key_ops_type first value."""
    types = jwk.get("key_ops_type") or []
    try:
        type_ = types[0]
    except IndexError:
        if jwk["kid"].startswith("connect_"):
            type_ = "connect"
        elif jwk["kid"].startswith("ssa_"):
            type_ = "ssa"
        else:
            type_ = ""
    return type_.lower()


def has_ext_jwks_uri(conf_dynamic, manager) -> bool:
    # avoid conflict with external JWKS URI
    jwks_uri = conf_dynamic["jwksUri"]
    default_jwks_uri = f"https://{manager.config.get('hostname')}/jans-auth/restv1/jwks"

    # compare JWKS URI in persistence and the default one
    # previously, the external JWKS URI is read from CN_OB_EXT_SIGNING_JWKS_URI env
    # if they're different, we assume it's an external JWKS URI
    if jwks_uri != default_jwks_uri:
        logger.warning(
            f"Found external JWKS URI at {jwks_uri}; "
            "skipping proccess to avoid conflict with "
            "builtin key rotation feature in jans-auth"
        )
        return True
    return False


# ===================
# Persistence helpers
# ===================


class BasePersistence:
    def get_auth_config(self):
        raise NotImplementedError

    def modify_auth_config(self, id_, rev, conf_dynamic, conf_webkeys):
        raise NotImplementedError

    def create_archived_jwk(self, jwk, lifetime):
        raise NotImplementedError


class SqlPersistence(BasePersistence):
    def __init__(self, manager):
        self.client = SqlClient(manager)

    def get_auth_config(self):
        config = self.client.get(
            "jansAppConf",
            "jans-auth",
            ["jansRevision", "jansConfDyn", "jansConfWebKeys"],
        )
        if not config:
            return {}

        config["id"] = "jans-auth"
        return config

    def modify_auth_config(self, id_, rev, conf_dynamic, conf_webkeys):
        return self.client.update(
            "jansAppConf",
            id_,
            {"jansRevision": rev, "jansConfDyn": json.dumps(conf_dynamic), "jansConfWebKeys": json.dumps(conf_webkeys)}
        )

    def create_archived_jwk(self, jwk, lifetime):
        table_name = "jansArchJwk"
        utc_now = datetime.now(UTC)
        column_mapping = {
            "doc_id": jwk["kid"],
            "objectClass": table_name,
            "dn": f"jansId={jwk['kid']},ou=archived_jwks,o=jans",
            "jansId": jwk["kid"],
            "creationDate": utc_now,
            "exp": utc_now + timedelta(seconds=lifetime),
            "del": True,
            "jansData": json.dumps(jwk),
            "attr": json.dumps({"attributes": {}}),
        }
        self.client.insert_into(table_name, column_mapping)


_backend_classes = {
    "sql": SqlPersistence,
}


# ================
# Service handlers
# ================


class AuthHandler:
    def __init__(self, manager, dry_run, **opts):
        self.manager = manager
        self.dry_run = dry_run
        self.opts = opts

        # resolve backend
        mapper = PersistenceMapper()
        backend_type = mapper.mapping["default"]
        self.backend = _backend_classes[backend_type](manager)

        self.rotation_interval = opts.get("interval", 48)
        self.push_keys = as_boolean(opts.get("push-to-container", True))
        self.key_strategy = opts.get("key-strategy", "NEWER")
        self.privkey_push_delay = int(opts.get("privkey-push-delay", 0))
        # how many extra rounds are made for containers whose JKS push failed, and the wait between rounds (seconds);
        # None if the value is not an integer, which patch() reports only when privkey-push-delay is used
        self.privkey_push_retries = to_non_negative_int(opts.get("privkey-push-retries", DEFAULT_PRIVKEY_PUSH_RETRIES))
        self.privkey_push_retry_interval = to_non_negative_int(
            opts.get("privkey-push-retry-interval", DEFAULT_PRIVKEY_PUSH_RETRY_INTERVAL),
        )
        # container name -> SHA-256 of the JKS the container had before this run, recorded when its backup was first
        # verified; kept for the whole run, so that a later round reuses that backup instead of making a new one
        self._original_backups = {}
        self.privkey_push_strategy = opts.get("privkey-push-strategy", "NEWER")
        self.sig_keys = resolve_sig_keys(opts.get("sig-keys", SIG_KEYS))
        self.enc_keys = resolve_enc_keys(opts.get("enc-keys", ENC_KEYS))

        metadata = os.environ.get("CN_CONTAINER_METADATA", "docker")
        if metadata == "kubernetes":
            self.meta_client = KubernetesMeta()
        else:
            self.meta_client = DockerMeta()

    @property
    def allowed_key_algs(self):
        return self.sig_keys.split() + self.enc_keys.split()

    # `container` below is a pod (Kubernetes) or a Docker container object returned by the meta client
    def _remote_sha256(self, container: Any, file_path: str) -> str:
        """Return SHA-256 of a file inside container, or empty string if it cannot be determined.

        Handles the different shapes returned by the meta clients: plain string (Kubernetes) and
        ``ExecResult(exit_code, output)`` with bytes output (Docker).
        """
        result = self.meta_client.exec_cmd(container, f"sha256sum {file_path}")

        if hasattr(result, "exit_code") and hasattr(result, "output"):
            if result.exit_code not in (0, None):
                return ""
            output = result.output
        else:
            output = result
        output_text = output.decode(errors="replace") if isinstance(output, bytes) else (output or "")

        parts = output_text.split()
        token = parts[0] if parts else ""
        # a failed sha256sum returns its error text (e.g. "sha256sum: ...: No such file"); two such errors
        # must never compare equal, so only a real SHA-256 hex digest is accepted
        is_digest = len(token) == SHA256_HEX_LENGTH and set(token.lower()) <= set("0123456789abcdef")
        return token if is_digest else ""

    def push_jks_verified(self, container: Any, jks_fn: str) -> bool:
        """Push JKS to a container and verify it arrived intact.

        ``copy_to_container`` does not report the exit status of the remote ``tar``, so a failed or partial
        extraction is not raised. The checksum of the remote file is compared with the local one instead.
        On a failed push after the backup was verified, the previous JKS (``<jks_fn>.backup``) is restored so the
        container keeps a loadable keystore.

        A container whose JKS already equals the local one (for example one restarted after the secret was updated)
        is counted as verified without a backup or a copy.

        Returns True only if the remote file matches the local one.
        """
        name = self.meta_client.get_container_name(container)
        backup_ok = False

        try:
            with open(jks_fn, "rb") as jks_file:
                local_sum = hashlib.sha256(jks_file.read()).hexdigest()

            # a container (re)started after the secret was updated loaded the new JKS already; its backup from an
            # earlier round is gone with its old file system, so the backup check below would fail on every round
            if self._remote_sha256(container, jks_fn) == local_sum:
                logger.info("%s:%s already holds the new JKS", name, jks_fn)
                return True

            self._ensure_verified_backup(container, jks_fn)
            backup_ok = True

            logger.info("creating new %s:%s", name, jks_fn)
            self.meta_client.copy_to_container(container, jks_fn)

            remote_sum = self._remote_sha256(container, jks_fn)

            if remote_sum == local_sum:
                return True
            reason = f"checksum mismatch (local={local_sum}, remote={remote_sum or 'n/a'})"
        except Exception as exc:  # noqa: BLE001
            reason = str(exc)

        # a single bad container must not abort the push to the remaining ones; a container that is restarted later
        # loads the JKS from the updated secret
        logger.warning("unable to push %s to %s; reason=%s", jks_fn, name, reason)

        if backup_ok:
            self._restore_jks_backup(container, jks_fn)
        return False

    def _ensure_verified_backup(self, container: Any, jks_fn: str) -> None:
        """Make sure ``<jks_fn>.backup`` on the container holds the JKS the container had before this push.

        The first attempt for a container creates the backup and records its checksum; a retry only checks that the
        recorded backup is still intact. Raises RuntimeError when the backup cannot be verified.
        """
        name = self.meta_client.get_container_name(container)
        original_sum = self._original_backups.get(name)

        if original_sum:
            # retry of a container whose earlier attempt failed: its JKS may now be partial, so it must not be
            # backed up again (that would replace the original with the partial file); keep using the
            # original backup, but only while it is still intact
            if self._remote_sha256(container, f"{jks_fn}.backup") != original_sum:
                raise RuntimeError("original backup of JKS is missing or changed; leaving JKS unchanged")
            return

        logger.info("creating backup of %s:%s", name, jks_fn)
        self.meta_client.exec_cmd(container, f"cp -p {jks_fn} {jks_fn}.backup")

        # only a backup created and verified by this attempt may be used for restore; an old
        # `.backup` left by an earlier rotation must never overwrite the current JKS
        current_sum = self._remote_sha256(container, jks_fn)
        backup_sum = self._remote_sha256(container, f"{jks_fn}.backup")
        if not current_sum or current_sum != backup_sum:
            raise RuntimeError("unable to verify backup of current JKS; leaving it unchanged")
        self._original_backups[name] = current_sum

    def _restore_jks_backup(self, container: Any, jks_fn: str) -> None:
        """Restore the verified backup of the previous JKS after a failed push and log whether it worked."""
        name = self.meta_client.get_container_name(container)
        try:
            # -p keeps the pre-rotation modification time, so jks_sync.py (CN_SYNC_JKS_ENABLED) pulls the new JKS later
            self.meta_client.exec_cmd(container, f"cp -p {jks_fn}.backup {jks_fn}")
            # make sure the restore really happened, otherwise a partial JKS may remain on the container
            restored_sum = self._remote_sha256(container, jks_fn)
            backup_sum = self._remote_sha256(container, f"{jks_fn}.backup")
            # an empty checksum means "could not be read" and must never count as a match
            if restored_sum and restored_sum == backup_sum:
                logger.warning("restored previous %s on %s", jks_fn, name)
            else:
                logger.error(
                    "unable to verify restore of previous %s on %s; the container may have an incomplete keystore "
                    "and should be restarted",
                    jks_fn, name,
                )
        except Exception as exc:  # noqa: BLE001
            logger.error(
                "unable to restore previous %s on %s; reason=%s; the container may have an incomplete keystore "
                "and should be restarted",
                jks_fn, name, exc,
            )

    def _list_auth_containers(self) -> dict[str, Any] | None:
        """Return the auth-server containers the meta client lists now, keyed by container name (in list order).

        Called only after the delay, when the new keys are already in persistence and secrets, so a failed listing
        must not stop the job: it is logged and None is returned.
        """
        try:
            containers = self.meta_client.get_containers("APP_NAME=auth-server")
        except Exception as exc:  # noqa: BLE001 - the meta client may be Kubernetes or Docker
            logger.warning("unable to list auth-server containers; reason=%s", exc)
            return None
        return {self.meta_client.get_container_name(container): container for container in containers}

    def _push_jks_to_pending(self, jks_fn: str, containers: dict[str, Any], updated_names: set[str]) -> list[str]:
        """Push the JKS to each of ``containers`` (name -> container) whose name is not in ``updated_names``.

        Adds the names of the containers that verified the new JKS to ``updated_names``; returns the names of the
        containers that failed, in order.
        """
        failed_names = []
        for name, container in containers.items():
            if name in updated_names:
                continue
            if self.push_jks_verified(container, jks_fn):
                updated_names.add(name)
            else:
                failed_names.append(name)
        return failed_names

    def _push_jks_to_unlisted(
        self, jks_fn: str, known_containers: list[Any], listed_containers: dict[str, Any], updated_names: set[str],
        listing_failed: bool,
    ) -> list[str]:
        """Make one push attempt to each of ``known_containers`` whose name is not in ``listed_containers``.

        Adds the names that verified the new JKS to ``updated_names``; returns the names that failed. Called in
        round 1 only: a failure here does not start another round, and a container that is not listed in any round
        is not tried again.

        On Kubernetes only Running, Ready, not terminating pods are listed, but exec and copy also reach a pod that
        is not Ready; an attempt on a container that is gone fails and is logged.
        """
        unlisted_containers = {}
        for container in known_containers:
            name = self.meta_client.get_container_name(container)
            if name not in listed_containers:
                unlisted_containers[name] = container

        if not unlisted_containers:
            return []

        reason = "listed before the delay" if listing_failed else "listed before the delay but not now"
        logger.info("pushing %s once to containers %s: %s", jks_fn, reason, ", ".join(unlisted_containers))
        return self._push_jks_to_pending(jks_fn, unlisted_containers, updated_names)

    def _retry_options_are_valid(self) -> bool:
        """Log every error and return False when a retry option is not an integer or is above its upper bound."""
        if None in (self.privkey_push_retries, self.privkey_push_retry_interval):
            logger.error(
                "privkey-push-retries and privkey-push-retry-interval must be integers; got %r and %r",
                self.opts.get("privkey-push-retries", DEFAULT_PRIVKEY_PUSH_RETRIES),
                self.opts.get("privkey-push-retry-interval", DEFAULT_PRIVKEY_PUSH_RETRY_INTERVAL),
            )
            return False
        is_valid = True
        if self.privkey_push_retries > MAX_PRIVKEY_PUSH_RETRIES:
            logger.error(
                "privkey-push-retries must be at most %s; got %s",
                MAX_PRIVKEY_PUSH_RETRIES, self.privkey_push_retries,
            )
            is_valid = False
        if self.privkey_push_retry_interval > MAX_PRIVKEY_PUSH_RETRY_INTERVAL:
            logger.error(
                "privkey-push-retry-interval must be at most %s seconds; got %s",
                MAX_PRIVKEY_PUSH_RETRY_INTERVAL, self.privkey_push_retry_interval,
            )
            is_valid = False
        return is_valid

    def _log_failed_round(
        self, round_number: int, total_rounds: int, jks_fn: str, listing_failed: bool, failed_names: list[str],
    ) -> None:
        """Warn that a round failed and that the next one starts after the retry interval.

        A failed listing was already logged with its reason by ``_list_auth_containers``, so it is not repeated.
        """
        if listing_failed:
            logger.warning(
                "round %s of %s: listing failed; next round in %s seconds",
                round_number, total_rounds, self.privkey_push_retry_interval,
            )
        else:
            logger.warning(
                "round %s of %s: unable to push %s to %s; next round in %s seconds",
                round_number, total_rounds, jks_fn, ", ".join(failed_names), self.privkey_push_retry_interval,
            )

    def _log_not_verified(self, jks_fn: str, failed_names: set[str], updated_names: set[str]) -> None:
        """Log one warning naming every container that had a failed attempt and never verified the new JKS.

        Such a container, if it still runs, keeps its previous JKS until it is restarted (it then loads the new one
        from the secret): it keeps signing with the previous keys, which stay published in the JWKS, but it cannot
        sign with an algorithm added in this rotation, nor decrypt requests encrypted to the new keys. A container
        whose restore failed or could not be verified may hold an incomplete JKS; that is logged as an error by
        ``_restore_jks_backup``. A container that was never listed (not before the delay, not in any round) was
        never pushed to and is not named.
        """
        not_updated_names = sorted(failed_names - updated_names)
        if not_updated_names:
            logger.warning(
                "new %s not verified on %s; until restarted, a running container among them keeps its previous keys "
                "(unless its restore failed, see the errors above) and cannot sign with an algorithm added in this "
                "rotation or decrypt with the new keys",
                jks_fn, ", ".join(not_updated_names),
            )

    def push_jks_with_retry(self, jks_fn: str, known_containers: list[Any]) -> int:
        """Push the new JKS to the auth-server containers after the delay; return how many verified it.

        Round 1 pushes to every container listed now, then once to each of ``known_containers`` (the list taken
        before the delay) that is not listed now. After a round in which a push or the listing failed, up to
        ``privkey_push_retries`` more rounds follow, each ``privkey_push_retry_interval`` seconds after the previous
        one. A round lists again and pushes to every listed container that has not verified the new JKS yet,
        including ones listed for the first time; a round whose listing fails pushes to no listed container.

        Returns the number of containers that verified the new JKS, listed or not: a container that is gone needs
        no reload, and one that kept its previous JKS keeps signing with the previous keys. Never raises because of
        a single container or a failed listing.
        """
        updated_names = set()
        failed_names = set()
        total_rounds = self.privkey_push_retries + 1

        for round_number in range(1, total_rounds + 1):
            listed_containers = self._list_auth_containers()
            listing_failed = listed_containers is None
            if listing_failed:
                listed_containers = {}

            round_failed_names = self._push_jks_to_pending(jks_fn, listed_containers, updated_names)
            failed_names.update(round_failed_names)

            if round_number == 1:
                failed_names.update(
                    self._push_jks_to_unlisted(
                        jks_fn, known_containers, listed_containers, updated_names, listing_failed,
                    )
                )

            if round_number == total_rounds or not (listing_failed or round_failed_names):
                break

            self._log_failed_round(round_number, total_rounds, jks_fn, listing_failed, round_failed_names)
            time.sleep(self.privkey_push_retry_interval)

        self._log_not_verified(jks_fn, failed_names, updated_names)
        return len(updated_names)

    def get_merged_keys(self, exp_hours):
        # get previous JWKS
        with open("/etc/certs/auth-keys.old.json") as f:
            old_jwks = json.loads(f.read()).get("keys", [])

        # get previous JKS
        old_jks_fn = "/etc/certs/auth-keys.old.jks"
        self.manager.secret.to_file("auth_jks_base64", old_jks_fn, decode=True, binary_mode=True)

        # generate new JWKS and JKS
        jks_pass = self.manager.secret.get("auth_openid_jks_pass")
        jks_dn = r"{}".format(self.manager.config.get("default_openid_jks_dn_name"))
        jks_fn = "/etc/certs/auth-keys.jks"
        jwks_fn = "/etc/certs/auth-keys.json"
        logger.info("Generating new %s and %s", jwks_fn, jks_fn)

        # a counter to determine value of `key_ops_type` to be passed to `KeyGenerator`
        ops_type_cnt = Counter(key_ops_from_jwk(jwk) for jwk in old_jwks)
        logger.info("Detected key_ops_type=%s", dict(ops_type_cnt))

        # if we have ssa key, use `connect` keys
        if ops_type_cnt["ssa"]:
            logger.info("Found existing SSA keys ... generating keys with key_ops_type=connect")
            key_ops_type = "connect"
        else:
            # likely upgrading from previous version where SSA keys are not generated initially
            logger.info("No existing SSA keys ... generating keys with key_ops_type=all")
            key_ops_type = "all"

        # if ssa keys exist, use `key_ops_type=connect` otherwise use `key_ops_type=all`
        out, err, retcode = generate_openid_keys(
            jks_pass, jks_fn, jks_dn, exp=exp_hours, sig_keys=self.sig_keys, enc_keys=self.enc_keys, key_ops_type=key_ops_type,
        )

        if retcode != 0:
            logger.error("Unable to generate keys; reason=%s", err.decode())
            return "", ""

        new_jwks = deque(json.loads(out).get("keys", []))

        logger.info("Merging keys from previous rotation (if any)")
        # make sure keys sorted by newer ``exp`` first, so the older one
        # won't be added to new JWKS
        old_jwks = sorted(old_jwks, key=lambda k: k["exp"], reverse=True)

        # counter for `connect` keys
        cnt = Counter(
            jwk["alg"].upper() for jwk in new_jwks
            if key_ops_from_jwk(jwk) == "connect"
        )

        # counter for `ssa` keys
        cnt_ssa = Counter(
            jwk["alg"].upper() for jwk in new_jwks
            if key_ops_from_jwk(jwk) == "ssa"
        )

        for jwk in old_jwks:
            alg = (jwk.get("alg") or "").upper()

            # exclude alg if it's not allowed
            if alg not in self.allowed_key_algs:
                continue

            ops_type = key_ops_from_jwk(jwk)

            # exclude unsupported key_ops_type
            if ops_type not in ["ssa", "connect"]:
                continue

            # cannot have more than 2 connect keys for same algorithm in new JWKS
            if ops_type == "connect" and cnt[alg] >= 2:
                continue

            # cannot have more than 1 ssa keys for same algorithm in new JWKS
            if ops_type == "ssa" and cnt_ssa[alg] >= 1:
                continue

            # insert old key to new keys
            new_jwks.appendleft(jwk)

            if ops_type == "connect":
                cnt[alg] += 1

            if ops_type == "ssa":
                cnt_ssa[alg] += 1

            # import key to new JKS
            keytool_import_key(old_jks_fn, jks_fn, jwk["kid"], jks_pass)

        # update new JWKS file
        with open(jwks_fn, "w") as f:
            data = {"keys": list(new_jwks)}
            f.write(json.dumps(data, indent=2))

        # finalizing
        return jwks_fn, jks_fn

    def patch(self):
        strategies = ", ".join(KEY_STRATEGIES)

        if self.key_strategy not in KEY_STRATEGIES:
            logger.error("Key strategy must be one of %s", strategies)
            sys.exit(1)

        if self.privkey_push_strategy not in KEY_STRATEGIES:
            logger.error("Private key push strategy must be one of %s", strategies)
            sys.exit(1)

        push_delay_invalid = False
        try:
            if self.privkey_push_delay < 0:
                push_delay_invalid = True
        except ValueError:
            push_delay_invalid = True

        if push_delay_invalid:
            logger.error("Invalid integer value for private key push delay")
            sys.exit(1)

        if self.privkey_push_delay > 0 and not self._retry_options_are_valid():
            sys.exit(1)

        config = self.backend.get_auth_config()

        if not config:
            # search failed due to missing entry
            logger.warning("Unable to find jans-auth config")
            return

        try:
            conf_dynamic = json.loads(config["jansConfDyn"])
        except TypeError:  # not string/buffer
            conf_dynamic = config["jansConfDyn"]

        if conf_dynamic["keyRegenerationEnabled"]:
            logger.warning("keyRegenerationEnabled config was set to true; "
                           "skipping proccess to avoid conflict with "
                           "builtin key rotation feature in jans-auth")
            return

        # avoid conflict with external JWKS URI
        if has_ext_jwks_uri(conf_dynamic, self.manager):
            return

        jks_pass = self.manager.secret.get("auth_openid_jks_pass")

        conf_dynamic.update({
            "keyRegenerationEnabled": False,  # always set to False
            "keyRegenerationInterval": int(self.rotation_interval),
            "webKeysStorage": "keystore",
            "keyStoreSecret": jks_pass,
            "keySelectionStrategy": self.key_strategy,
            "keyAlgsAllowedForGeneration": self.allowed_key_algs,
            "keyStoreFile": self.manager.config.get("auth_openid_jks_fn"),
            "jwksUri": f"https://{self.manager.config.get('hostname')}/jans-auth/restv1/jwks",
        })

        # get old JWKS from persistence
        try:
            web_keys = json.loads(config["jansConfWebKeys"])
        except TypeError:
            web_keys = config["jansConfWebKeys"]
        except json.decoder.JSONDecodeError as exc:
            # probably corrupted JSON
            logger.warning("Unable to load existing JWKS; reason=%s. New JWKS will be created.", exc)
            web_keys = {"keys": []}

        with open("/etc/certs/auth-keys.old.json", "w") as f:
            f.write(json.dumps(web_keys, indent=2))

        exp_hours = int(self.rotation_interval) + int(conf_dynamic["idTokenLifetime"] / 3600)

        jwks_fn, jks_fn = self.get_merged_keys(exp_hours)

        if self.dry_run:
            return

        auth_containers = []

        if self.push_keys:
            auth_containers = self.meta_client.get_containers("APP_NAME=auth-server")
            if not auth_containers:
                logger.warning(
                    "Unable to find any jans-auth container; make sure "
                    "to deploy jans-auth and set APP_NAME=auth-server "
                    "label on container level; Note that pushing keys to "
                    "containers will be skipped!"
                )

        for container in auth_containers:
            name = self.meta_client.get_container_name(container)

            logger.info("creating backup of %s:%s", name, jwks_fn)
            self.meta_client.exec_cmd(container, f"cp {jwks_fn} {jwks_fn}.backup")
            logger.info("creating new %s:%s", name, jwks_fn)
            self.meta_client.copy_to_container(container, jwks_fn)

            if self.privkey_push_delay > 0:
                # delayed jks push
                continue

            logger.info("creating backup of %s:%s", name, jks_fn)
            self.meta_client.exec_cmd(container, f"cp {jks_fn} {jks_fn}.backup")
            logger.info("creating new %s:%s", name, jks_fn)
            self.meta_client.copy_to_container(container, jks_fn)

        try:
            with open(jwks_fn) as f:
                keys = json.loads(f.read())

            logger.info("modifying jans-auth configuration")
            logger.info("using keySelectionStrategy %s", self.key_strategy)
            rev = int(config["jansRevision"]) + 1
            modified = self.backend.modify_auth_config(
                config["id"],
                rev,
                conf_dynamic,
                keys,
            )

            if not modified:
                # restore jks and jwks
                logger.warning("failed to modify jans-auth configuration")
                for container in auth_containers:
                    name = self.meta_client.get_container_name(container)
                    logger.info("restoring backup of %s:%s", name, jwks_fn)
                    self.meta_client.exec_cmd(container, f"cp {jwks_fn}.backup {jwks_fn}")

                    if self.privkey_push_delay > 0:
                        # delayed jks revert
                        continue

                    logger.info("restoring backup of %s:%s", name, jks_fn)
                    self.meta_client.exec_cmd(container, f"cp {jks_fn}.backup {jks_fn}")

            else:
                self.manager.secret.set("auth_jks_base64", encode_jks(self.manager))
                self.manager.config.set("auth_key_rotated_at", int(time.time()))
                self.manager.secret.set("auth_openid_jks_pass", jks_pass)
                self.manager.config.set("auth_sig_keys", self.sig_keys)
                self.manager.config.set("auth_enc_keys", self.enc_keys)
                # jwks
                self.manager.secret.set(
                    "auth_openid_key_base64",
                    generate_base64_contents(json.dumps(keys)),
                )

                # create archived jwks (if applicable)
                archived_jwk_lifetime = conf_dynamic.get("archivedJwkLifetimeInSeconds") or DEFAULT_ARCHIVED_JWK_LIFETIME
                self.create_archived_jwks("/etc/certs/auth-keys.old.json", jwks_fn, archived_jwk_lifetime)

                # publish delayed jks
                if self.push_keys and self.privkey_push_delay > 0 and auth_containers:
                    logger.info("Waiting for private key push delay (%s seconds) ...", self.privkey_push_delay)
                    time.sleep(self.privkey_push_delay)

                    # containers may have been evicted, replaced or become not Ready during the delay
                    verified_count = self.push_jks_with_retry(jks_fn, auth_containers)

                    if not verified_count:
                        logger.warning("no auth-server container verified the new %s; skipping reload", jks_fn)
                        return

                    # as new JKS pushed to container, we need to tell auth-server to reload the private keys
                    # by increasing jansRevision again; note that as jansRevision may have been modified externally
                    # we need to ensure we have fresh jansRevision value to increase to
                    config = self.backend.get_auth_config()
                    rev = int(config["jansRevision"]) + 1
                    conf_dynamic.update({
                        "keySelectionStrategy": self.privkey_push_strategy,
                    })

                    logger.info("using keySelectionStrategy %s", self.privkey_push_strategy)

                    self.backend.modify_auth_config(
                        config["id"],
                        rev,
                        conf_dynamic,
                        keys,
                    )
        except (TypeError, ValueError,) as exc:
            logger.warning("Unable to get public keys; reason=%s", exc)

    def prune(self):
        config = self.backend.get_auth_config()

        if not config:
            # search failed due to missing entry
            logger.warning("Unable to find jans-auth config")
            return

        try:
            conf_dynamic = json.loads(config["jansConfDyn"])
        except TypeError:  # not string/buffer
            conf_dynamic = config["jansConfDyn"]

        if conf_dynamic["keyRegenerationEnabled"]:
            logger.warning("keyRegenerationEnabled config was set to true; "
                           "skipping proccess to avoid conflict with "
                           "builtin key rotation feature in jans-auth")
            return

        # avoid conflict with external JWKS URI
        if has_ext_jwks_uri(conf_dynamic, self.manager):
            return

        jks_pass = self.manager.secret.get("auth_openid_jks_pass")

        conf_dynamic.update({
            "keyRegenerationEnabled": False,  # always set to False
            "webKeysStorage": "keystore",
            "keyStoreSecret": jks_pass,
            "keyAlgsAllowedForGeneration": self.allowed_key_algs,
            "keyStoreFile": self.manager.config.get("auth_openid_jks_fn"),
            "jwksUri": f"https://{self.manager.config.get('hostname')}/jans-auth/restv1/jwks",
        })

        # get old JWKS from persistence
        try:
            web_keys = json.loads(config["jansConfWebKeys"])
        except TypeError:
            web_keys = config["jansConfWebKeys"]
        except json.decoder.JSONDecodeError as exc:
            # probably corrupted JSON
            logger.warning("Unable to load existing JWKS; reason=%s. Existing JWKS will be deleted.", exc)
            web_keys = {"keys": []}

        logger.info("Cleaning up keys (if any)")

        jks_fn = "/etc/certs/auth-keys.jks"
        self.manager.secret.to_file("auth_jks_base64", jks_fn, decode=True, binary_mode=True)

        # non-pruned keys
        new_jwks = []

        # make sure keys sorted by newer ``exp`` first, so the older one
        # won't be added to new JWKS
        old_jwks = web_keys.get("keys", [])
        old_jwks = sorted(old_jwks, key=lambda k: k["exp"], reverse=True)

        cnt = Counter(
            jwk["alg"].upper() for jwk in new_jwks
            if key_ops_from_jwk(jwk) == "connect"
        )

        cnt_ssa = Counter(
            jwk["alg"].upper() for jwk in new_jwks
            if key_ops_from_jwk(jwk) == "ssa"
        )

        for jwk in old_jwks:
            alg = (jwk.get("alg") or "").upper()

            # exclude alg if it's not allowed
            if alg not in self.allowed_key_algs:
                keytool_delete_key(jks_fn, jwk["kid"], jks_pass)
                continue

            ops_type = key_ops_from_jwk(jwk)

            # cannot have more than 1 connect key for same algorithm in new JWKS
            if ops_type == "connect" and cnt[alg] >= 1:
                keytool_delete_key(jks_fn, jwk["kid"], jks_pass)
                continue

            # cannot have more than 1 ssa key for same algorithm in new JWKS
            if ops_type == "ssa" and cnt_ssa[alg] >= 1:
                keytool_delete_key(jks_fn, jwk["kid"], jks_pass)
                continue

            # preserve the key
            new_jwks.append(jwk)

            if ops_type == "connect":
                cnt[alg] += 1

            if ops_type == "ssa":
                cnt_ssa[alg] += 1

        web_keys["keys"] = new_jwks

        jwks_fn = "/etc/certs/auth-keys.json"
        with open(jwks_fn, "w") as f:
            f.write(json.dumps(web_keys, indent=2))

        if self.dry_run:
            return

        auth_containers = []

        if self.push_keys:
            auth_containers = self.meta_client.get_containers("APP_NAME=auth-server")
            if not auth_containers:
                logger.warning(
                    "Unable to find any jans-auth container; make sure "
                    "to deploy jans-auth and set APP_NAME=auth-server "
                    "label on container level. Note that pushing keys to "
                    "containers will be skipped!"
                )
                # exit immediately to avoid persistence/secrets being modified
                return

        for container in auth_containers:
            name = self.meta_client.get_container_name(container)

            logger.info("creating backup of %s:%s", name, jks_fn)
            self.meta_client.exec_cmd(container, f"cp {jks_fn} {jks_fn}.backup")
            logger.info("creating new %s:%s", name, jks_fn)
            self.meta_client.copy_to_container(container, jks_fn)

            logger.info("creating backup of %s:%s", name, jwks_fn)
            self.meta_client.exec_cmd(container, f"cp {jwks_fn} {jwks_fn}.backup")
            logger.info("creating new %s:%s", name, jwks_fn)
            self.meta_client.copy_to_container(container, jwks_fn)

        try:
            with open(jwks_fn) as f:
                keys = json.loads(f.read())

            logger.info("modifying jans-auth configuration")
            rev = int(config["jansRevision"])
            modified = self.backend.modify_auth_config(
                config["id"],
                rev + 1,
                conf_dynamic,
                keys,
            )

            if not modified:
                # restore jks and jwks
                logger.warning("failed to modify jans-auth configuration")
                for container in auth_containers:
                    name = self.meta_client.get_container_name(container)
                    logger.info("restoring backup of %s:%s", name, jks_fn)
                    self.meta_client.exec_cmd(container, f"cp {jks_fn}.backup {jks_fn}")
                    logger.info("restoring backup of %s:%s", name, jwks_fn)
                    self.meta_client.exec_cmd(container, f"cp {jwks_fn}.backup {jwks_fn}")
            else:
                self.manager.secret.set("auth_jks_base64", encode_jks(self.manager))
                self.manager.config.set("auth_key_rotated_at", int(time.time()))
                self.manager.secret.set("auth_openid_jks_pass", jks_pass)
                self.manager.config.set("auth_sig_keys", self.sig_keys)
                self.manager.config.set("auth_enc_keys", self.enc_keys)
                # jwks
                self.manager.secret.set(
                    "auth_openid_key_base64",
                    generate_base64_contents(json.dumps(keys)),
                )
        except (TypeError, ValueError,) as exc:
            logger.warning("Unable to get public keys; reason=%s", exc)

    def create_archived_jwks(self, old_jwks_fn, new_jwks_fn, lifetime):
        with open(old_jwks_fn) as f:
            old_jwks = json.loads(f.read()).get("keys", [])

        with open(new_jwks_fn) as f:
            new_jwks = json.loads(f.read()).get("keys", [])

        # old JWK that is not listed in new JWKS will be saved as an archived JWK
        new_jwks_kids = {new_jwk["kid"] for new_jwk in new_jwks}
        for old_jwk in old_jwks:
            if old_jwk["kid"] in new_jwks_kids:
                continue

            try:
                self.backend.create_archived_jwk(old_jwk, lifetime)
                logger.info("JWK %s has been archived", old_jwk["kid"])
            except Exception as exc:  # noqa: BLE001 - best-effort; rotation already committed
                logger.warning("Unable to archive JWK %s; reason=%s", old_jwk["kid"], exc)


class WebHandler:
    def __init__(self, manager, dry_run, **opts):
        self.manager = manager
        self.dry_run = dry_run
        self.opts = opts

    def patch(self):
        source = self.opts.get("source", "")

        try:
            valid_to = int(self.opts.get("valid-to", 365))
        except ValueError:
            valid_to = 365

        if valid_to < 1:
            valid_to = 365

        ssl_cert = "/etc/certs/web_https.crt"
        ssl_key = "/etc/certs/web_https.key"  # // # gitleaks:allow
        ssl_csr = "/etc/certs/web_https.csr"

        ssl_ca_cert = "/etc/certs/ca.crt"
        ssl_ca_key = "/etc/certs/ca.key"  # // # gitleaks:allow

        if source == "from-files":
            if not any([os.path.isfile(ssl_cert), os.path.isfile(ssl_key)]):
                logger.warning("Unable to find existing %s or %s", ssl_cert, ssl_key)
            else:
                logger.info("Using existing %s and %s", ssl_cert, ssl_key)
        else:
            email = self.manager.config.get("admin_email")
            hostname = self.manager.config.get("hostname")
            org_name = self.manager.config.get("orgName")
            country_code = self.manager.config.get("country_code")
            state = self.manager.config.get("state")
            city = self.manager.config.get("city")

            logger.info("Creating self-generated %s and %s", ssl_ca_cert, ssl_ca_key)

            ca_cert, ca_key = generate_ssl_ca_certkey(
                "ca",
                email,
                "Janssen CA",
                org_name,
                country_code,
                state,
                city,
                valid_to=valid_to,
            )

            logger.info("Creating self-generated %s, %s, and %s", ssl_csr, ssl_cert, ssl_key)
            generate_signed_ssl_certkey(
                "web_https",
                ca_key,
                ca_cert,
                email,
                hostname,
                org_name,
                country_code,
                state,
                city,
                valid_to=valid_to,
            )

        if not self.dry_run:
            if os.path.isfile(ssl_ca_key):
                self.manager.secret.from_file("ssl_ca_key", ssl_ca_key)
            if os.path.isfile(ssl_ca_cert):
                self.manager.secret.from_file("ssl_ca_cert", ssl_ca_cert)

            if os.path.isfile(ssl_cert):
                self.manager.secret.from_file("ssl_cert", ssl_cert)
            if os.path.isfile(ssl_key):
                self.manager.secret.from_file("ssl_key", ssl_key)
            if os.path.isfile(ssl_csr):
                self.manager.secret.from_file("ssl_csr", ssl_csr)


# ====
# CLIs
# ====


#: Map between service name and its handler class
PATCH_SERVICE_MAP = {
    "web": WebHandler,
    "auth": AuthHandler,
}

PRUNE_SERVICE_MAP = {
    "auth": AuthHandler,
}


def _parse_opts(opts):
    parsed_opts = {}
    for opt in opts:
        with suppress(ValueError):
            k, v = opt.split(":", 1)
            if k and v:
                parsed_opts[k] = v
    return parsed_opts


@click.group(context_settings={"help_option_names": ["-h", "--help"]})
def certmanager():
    """Manage certs and crypto keys."""


@certmanager.command()
@click.argument("service", type=click.Choice(PATCH_SERVICE_MAP.keys()))
@click.option("--dry-run", help="Enable dryrun mode.", is_flag=True)
@click.option(
    "--opts",
    help="Options for targeted service (can be set multiple times).",
    multiple=True,
    metavar="KEY:VALUE",
)
def patch(service, dry_run, opts):
    """Patch cert and/or crypto keys for the targeted service.
    """
    manager = get_manager()

    if dry_run:
        logger.warning("Dry-run mode is enabled!")

    logger.info("Processing updates for service %s", service)
    parsed_opts = _parse_opts(opts)
    callback_cls = PATCH_SERVICE_MAP[service]

    with manager.create_lock(f"certmanager-patch-{service}"):
        callback_cls(manager, dry_run, **parsed_opts).patch()


@certmanager.command()
@click.argument("service", type=click.Choice(PRUNE_SERVICE_MAP.keys()))
@click.option("--dry-run", help="Enable dryrun mode.", is_flag=True)
@click.option(
    "--opts",
    help="Options for targeted service (can be set multiple times).",
    multiple=True,
    metavar="KEY:VALUE",
)
def prune(service, dry_run, opts):
    """Cleanup expired crypto keys for the targeted service.
    """
    manager = get_manager()

    if dry_run:
        logger.warning("Dry-run mode is enabled!")

    logger.info("Processing updates for service %s", service)
    parsed_opts = _parse_opts(opts)
    callback_cls = PRUNE_SERVICE_MAP[service]

    with manager.create_lock(f"certmanager-prune-{service}"):
        callback_cls(manager, dry_run, **parsed_opts).prune()


if __name__ == "__main__":
    certmanager(prog_name="certmanager")
