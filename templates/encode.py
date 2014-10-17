#!/usr/bin/python

import sys
import base64
from pyDes import *

saltFn = "%(tomcatHome)s/conf/salt"
f = open(saltFn)
salt_property = f.read()
f.close()

key = salt_property.split("=")[1].strip()

def obscure(data=""):
    engine = triple_des(key, ECB, pad=None, padmode=PAD_PKCS5)
    data = data.encode('ascii')
    en_data = engine.encrypt(data)
    return base64.b64encode(en_data)

def unobscure(s=""):
    engine = triple_des(key, ECB, pad=None, padmode=PAD_PKCS5)
    cipher = triple_des(key)
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