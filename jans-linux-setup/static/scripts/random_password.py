#!/usr/bin/python
"""Script to generate a random password"""

import os

random_password = os.urandom(32).encode('hex')
print random_password
