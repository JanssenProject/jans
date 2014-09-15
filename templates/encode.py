#!/usr/bin/python

import sys
import base64
from pyDes import *

randstring = "%(blowfish_passphrase)s"

def obscure(s=""):
    cipher = triple_des(randstring)
    crypted = cipher.encrypt(s, padmode=PAD_PKCS5)
    return base64.b64encode(crypted)

def unobscure(s=""):
    cipher = triple_des(randstring)
    decrypted = cipher.decrypt(base64.b64decode(s), padmode=PAD_PKCS5)
    return decrypted

def Usage():
    print "To encode:   encode <string>"
    print "To decode:   encode -D <string>"
    print
    sys.exit(0)

arg = ""
decode = False
if len(sys.argv) == 1:
    Usage()
if len(sys.argv) == 3:
    decode = True
    arg = sys.argv[2]
if len(sys.argv) == 2:
    arg = sys.argv[1]

if decode:
    print unobscure(arg)
else:
    print obscure(arg)
