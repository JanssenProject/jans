#!/usr/bin/python
"""Script to encrypt a plaintext password to LDAP SSHA format.

Usage: python ldap_encrypt.py password
"""

import os, hashlib, sys


def ldap_encode(password):
        salt = os.urandom(4)
        sha = hashlib.sha1(password)
        sha.update(salt)
        b64encoded = '{0}{1}'.format(sha.digest(), salt).encode('base64').strip()
        encrypted_password = '{{SSHA}}{0}'.format(b64encoded)
        return encrypted_password

print ldap_encode(sys.argv[1])

