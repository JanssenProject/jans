"""
jans.pycloudlib.persistence.ldap
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This module contains various helpers related to LDAP persistence.
"""

import os


def render_ldap_properties(manager, src: str, dest: str) -> None:
    """Render file contains properties to connect to LDAP server.

    :params manager: An instance of :class:`~jans.pycloudlib.manager._Manager`.
    :params src: Absolute path to the template.
    :params dest: Absolute path where generated file is located.
    """
    ldap_url = os.environ.get("CN_LDAP_URL", "localhost:1636")
    ldap_hostname, ldaps_port = ldap_url.split(":")

    with open(src) as f:
        txt = f.read()

    with open(dest, "w") as f:
        rendered_txt = txt % {
            "ldap_binddn": manager.config.get("ldap_binddn"),
            "encoded_ox_ldap_pw": manager.secret.get("encoded_ox_ldap_pw"),
            "ldap_hostname": ldap_hostname,
            "ldaps_port": ldaps_port,
            "ldapTrustStoreFn": manager.config.get("ldapTrustStoreFn"),
            "encoded_ldapTrustStorePass": manager.secret.get(
                "encoded_ldapTrustStorePass"
            ),
        }
        f.write(rendered_txt)


def sync_ldap_truststore(manager, dest: str = "") -> None:
    """Pull secret contains base64-string contents of LDAP truststore,
    and save it as a JKS file, i.e. ``/etc/certs/opendj.pkcs12``.

    :params manager: An instance of :class:`~jans.pycloudlib.manager._Manager`.
    :params dest: Absolute path where generated file is located.
    """
    dest = dest or manager.config.get("ldapTrustStoreFn")
    manager.secret.to_file(
        "ldap_pkcs12_base64", dest, decode=True, binary_mode=True,
    )
