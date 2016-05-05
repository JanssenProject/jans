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
ENROLL_PREFIX = 'ENROLL'
ENROLL_REQUEST_PREFIX = 'ENROLL_REQUEST'

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
    b64 = base64.b64encode(val.encode('utf-8')).decode('utf-8')
    cookie = '%s|%s' % (prefix, b64)

    sig = _hmac_sha1(key.encode('utf-8'), cookie.encode('utf-8'))
    return '%s|%s' % (cookie, sig)

def _parse_vals(key, val, prefix, ikey):
    ts = int(time.time())
    u_prefix, u_b64, u_sig = val.split('|')
    cookie = '%s|%s' % (u_prefix, u_b64)
    e_key = key.encode('utf-8')
    e_cookie = cookie.encode('utf-8')

    sig = _hmac_sha1(e_key, e_cookie)
    if _hmac_sha1(e_key, sig.encode('utf-8')) != _hmac_sha1(e_key, u_sig.encode('utf-8')):
        return None

    if u_prefix != prefix:
        return None

    decoded = base64.b64decode(u_b64).decode('utf-8')
    user, u_ikey, exp = decoded.split('|')

    if u_ikey != ikey:
        return None

    if ts >= int(exp):
        return None

    return user

def _sign_request(ikey, skey, akey, username, prefix):
    """Generate a signed request for Duo authentication.
    The returned value should be passed into the Duo.init() call
    in the rendered web page used for Duo authentication.
    Arguments:
    ikey      -- Duo integration key
    skey      -- Duo secret key
    akey      -- Application secret key
    username  -- Primary-authenticated username
    prefix    -- DUO_PREFIX or ENROLL_REQUEST_PREFIX
    """
    if not username:
        return ERR_USER
    if '|' in username:
        return ERR_USER
    if not ikey or len(ikey) != IKEY_LEN:
        return ERR_IKEY
    if not skey or len(skey) != SKEY_LEN:
        return ERR_SKEY
    if not akey or len(akey) < AKEY_LEN:
        return ERR_AKEY

    vals = [ username, ikey ]

    try:
        duo_sig = _sign_vals(skey, vals, prefix, DUO_EXPIRE)
        app_sig = _sign_vals(akey, vals, APP_PREFIX, APP_EXPIRE)
    except Exception:
        return ERR_UNKNOWN

    return '%s:%s' % (duo_sig, app_sig)


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
    return _sign_request(ikey, skey, akey, username, DUO_PREFIX)


def sign_enroll_request(ikey, skey, akey, username):
    """Generate a signed request for Duo authentication.
    The returned value should be passed into the Duo.init() call
    in the rendered web page used for Duo authentication.
    Arguments:
    ikey      -- Duo integration key
    skey      -- Duo secret key
    akey      -- Application secret key
    username  -- Primary-authenticated username
    """
    return _sign_request(ikey, skey, akey, username, ENROLL_REQUEST_PREFIX)


def _verify_response(ikey, skey, akey, prefix, sig_response):
    """Validate the signed response returned from Duo.
    Returns the username of the authenticated user, or None.
    Arguments:
    ikey          -- Duo integration key
    skey          -- Duo secret key
    akey          -- Application secret key
    prefix        -- AUTH_PREFIX or ENROLL_PREFIX that sig_response
                     must match
    sig_response  -- The signed response POST'ed to the server
    """
    try:
        auth_sig, app_sig = sig_response.split(':')
        auth_user = _parse_vals(skey, auth_sig, AUTH_PREFIX, ikey)
        app_user = _parse_vals(akey, app_sig, APP_PREFIX, ikey)
    except Exception:
        return None

    if auth_user != app_user:
        return None

    return auth_user


def verify_response(ikey, skey, akey, sig_response):
    """Validate the signed response returned from Duo.
    Returns the username of the authenticated user, or None.
    Arguments:
    ikey          -- Duo integration key
    skey          -- Duo secret key
    akey          -- Application secret key
    sig_response  -- The signed response POST'ed to the server
    """
    return _verify_response(ikey, skey, akey, AUTH_PREFIX, sig_response)


def verify_enroll_response(ikey, skey, akey, sig_response):
    """Validate the signed response returned from Duo.
    Returns the username of the enrolled user, or None.
    Arguments:
    ikey          -- Duo integration key
    skey          -- Duo secret key
    akey          -- Application secret key
    sig_response  -- The signed response POST'ed to the server
    """
    return _verify_response(ikey, skey, akey, ENROLL_PREFIX, sig_response)
