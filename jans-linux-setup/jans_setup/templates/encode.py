#!/usr/bin/python3
import os
import sys
import base64

sys.path.append(os.path.join("%(install_dir)s", 'jans_setup/pylib'))

from pyDes import *

saltFn = "%(configFolder)s/salt"
f = open(saltFn)
salt_property = f.read()
f.close()

key = salt_property.split("=")[1].strip()

def obscure(data=""):
    engine = triple_des(key, ECB, pad=None, padmode=PAD_PKCS5)
    data = data.encode('utf-8')
    en_data = engine.encrypt(data)
    return base64.b64encode(en_data).decode('utf-8')

def unobscure(s=""):
    engine = triple_des(key, ECB, pad=None, padmode=PAD_PKCS5)
    cipher = triple_des(key)
    decrypted = cipher.decrypt(base64.b64decode(s), padmode=PAD_PKCS5)
    return decrypted.decode('utf-8')

def Usage():
    print("To encode:   encode <string>")
    print("To decode:   encode -D <string>")
    print()
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
    print(unobscure(arg))
else:
    print(obscure(arg))
