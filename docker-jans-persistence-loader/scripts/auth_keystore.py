import typing as _t


def merge_auth_keystore_ctx(manager, ctx: dict[str, _t.Any]) -> dict[str, _t.Any]:
    # maintain compatibility with upstream template
    ctx["oxauth_openid_jks_fn"] = manager.config.get("auth_openid_jks_fn")
    return ctx
