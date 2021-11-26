# 
# duo_web.py
#
# Copyright (c) 2011 Duo Security
# All rights reserved, all wrongs reversed.
#

import base64
import hashlib
import hmac
import time

DUO_PREFIX  = 'TX'
APP_PREFIX  = 'APP'
AUTH_PREFIX = 'AUTH'

DUO_EXPIRE = 300
APP_EXPIRE = 3600

IKEY_LEN = 20
SKEY_LEN = 40
AKEY_LEN = 40

ERR_USER = 'ERR|The username passed to sign_request() is invalid.'
ERR_IKEY = 'ERR|The Duo integration key passed to sign_request() is invalid.'
ERR_SKEY = 'ERR|The Duo secret key passed to sign_request() is invalid.'
ERR_AKEY = 'ERR|The application secret key passed to sign_request() must be at least %s characters.' % AKEY_LEN
ERR_UNKNOWN = 'ERR|An unknown error has occurred.'

def _hmac_sha1(key, msg):
    ctx = hmac.new(key, msg, hashlib.sha1)
    return ctx.hexdigest()

def _sign_vals(key, vals, prefix, expire):
    exp = str(int(time.time()) + expire)

    val = '|'.join(vals + [ exp ])
    b64 = base64.b64encode(val)
    cookie = '%s|%s' % (prefix, b64)

    sig = _hmac_sha1(key, cookie)
    return '%s|%s' % (cookie, sig)

def _parse_vals(key, val, prefix):
    ts = int(time.time())
    u_prefix, u_b64, u_sig = val.split('|')

    sig = _hmac_sha1(key, '%s|%s' % (u_prefix, u_b64))
    if _hmac_sha1(key, sig) != _hmac_sha1(key, u_sig):
        return None

    if u_prefix != prefix:
        return None

    user, ikey, exp = base64.b64decode(u_b64).split('|')

    if ts >= int(exp):
        return None

    return user

def sign_request(ikey, skey, akey, username):
    """Generate a signed request for Duo authentication.
    The returned value should be passed into the Duo.init() call 
    in the rendered web page used for Duo authentication.
    
    Arguments:
    
    ikey      -- Duo integration key
    skey      -- Duo secret key
    akey      -- Application secret key
    username  -- Primary-authenticated username
    """
    if not username:
        return ERR_USER
    if not ikey or len(ikey) != IKEY_LEN:
        return ERR_IKEY
    if not skey or len(skey) != SKEY_LEN:
        return ERR_SKEY
    if not akey or len(akey) < AKEY_LEN:
        return ERR_AKEY

    vals = [ username, ikey ]

    try:
        duo_sig = _sign_vals(skey, vals, DUO_PREFIX, DUO_EXPIRE)
        app_sig = _sign_vals(akey, vals, APP_PREFIX, APP_EXPIRE)
    except:
        return ERR_UNKNOWN

    return '%s:%s' % (duo_sig, app_sig)

def verify_response(ikey, skey, akey, sig_response):
    """Validate the signed response returned from Duo.
    Returns the username of the authenticated user, or None.
    
    Arguments:
    
    ikey          -- Duo integration key
    skey          -- Duo secret key
    akey          -- Application secret key
    sig_response  -- The signed response POST'ed to the server
    """
    try:
        auth_sig, app_sig = sig_response.split(':')
        auth_user = _parse_vals(skey, auth_sig, AUTH_PREFIX)
        app_user = _parse_vals(akey, app_sig, APP_PREFIX)
    except:
        return None

    if auth_user != app_user:
        return None

    return auth_user
