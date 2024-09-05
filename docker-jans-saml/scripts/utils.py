import os


def get_kc_db_password():
    db_password = os.environ.get("KC_DB_PASSWORD", "")

    if not db_password:
        passwd_file = os.environ.get("CN_SAML_KC_DB_PASSWORD_FILE", "/etc/jans/conf/kc_db_password")

        try:
            with open(passwd_file) as f:
                db_password = f.read().strip()
        except FileNotFoundError as exc:
            raise ValueError(f"Unable to get password from {passwd_file}; reason={exc}")

    # resolved password
    return db_password
