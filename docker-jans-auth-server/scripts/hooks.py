"""Hooks are special callables that can be used to change the behavior
of default callables, i.e. change keystore in specific Janssen image.

Currently, hooks are meant to be overriden manually. In the future,
we can use specialized hooks/plugins system.
"""
import base64


def get_auth_keys_hook(manager):
    manager.secret.to_file(
        "auth_jks_base64",
        "/etc/certs/auth-keys.jks",
        decode=True,
        binary_mode=True,
    )
    with open("/etc/certs/auth-keys.json", "w") as f:
        f.write(base64.b64decode(manager.secret.get("auth_openid_key_base64")).decode())
