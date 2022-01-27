#!/usr/bin/python3
import os
import sys
import base64
import configparser
import re

sys.path.append(os.path.join("%(install_dir)s", 'setup_app/pylib'))

from Crypto.Cipher import AES
from pyAes import AESCipher, AESKeyLength, AES_CBC_P, AES_ECB_P, AES_GCM_NP

saltFn = "%(configFolder)s/salt"
f = open(saltFn)
salt_property = f.read()
f.close()

res_properties = '[dummy_section]\n' + salt_property;

config = configparser.ConfigParser()
config.read_string(res_properties)

details_dict = dict(config.items('dummy_section'))

salt = None
passw = None
alg = None

if 'encodesalt' in details_dict:
    salt = details_dict['encodesalt']

if 'encodepassw' in details_dict:
    passw = details_dict['encodepassw']

if 'encodealg' in details_dict:
    alg = details_dict['encodealg']

def obscure(data=""):
    engine = get_engine(passw, salt, alg)
    en_data = engine.encrypt(data)
    return base64.b64encode(en_data).decode()

def unobscure(s=""):
    engine = get_engine(passw, salt, alg)
    decrypted = engine.decrypt(base64.b64decode(s))
    return decrypted.decode()

def get_engine(passw='', salt='', alg=''):
    if alg is None or len(alg) == 0:
        return get_aes_engine(AES_GCM_NP, '256', passw, salt)
    alg_sep_array = re.split(":", alg)
    if len(alg_sep_array) == 0 or len(alg_sep_array) == 1 and alg_sep_array[0] == 'AES':
        return get_aes_engine(AES_GCM_NP, '256', passw, salt)
    elif len(alg_sep_array) == 3 and alg_sep_array[0] == 'AES':
        return get_aes_engine(alg_sep_array[1], alg_sep_array[2], passw, salt)
    else:
        raise AttributeError("wrong alg value: alg = " + alg)

def get_aes_engine(mode='', key_length='', passw='', salt=''):
    eff_mode = None
    eff_key_length = None
    if key_length == '128':
        eff_key_length = AESKeyLength.KL128
    elif key_length == '192':
        eff_key_length = AESKeyLength.KL192
    elif key_length == '256':
        eff_key_length = AESKeyLength.KL256
    else:
        raise AttributeError("wrong key_length value: key_length = " + key_length)
    if mode == AES_CBC_P:
        eff_mode = AES.MODE_CBC
    elif mode == AES_GCM_NP:
        eff_mode = AES.MODE_GCM
    elif mode == AES_ECB_P:
        eff_mode = AES.MODE_ECB
    else:
        raise AttributeError("this mode isn't supported: mode = " + mode)
    return AESCipher(eff_mode, eff_key_length, passw, salt)

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
