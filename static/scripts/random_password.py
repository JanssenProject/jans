#!/usr/bin/python

import os

random_password = os.urandom(32).encode('hex')
print random_password
