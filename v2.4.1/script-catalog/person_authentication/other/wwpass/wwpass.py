# -*- coding: utf-8 -*-
__author__="Rostislav Kondratenko <r.kondratenko@wwpass.com>"
__date__ ="$27.11.2014 18:05:15$"

# Copyright 2009-2019 WWPASS Corporation
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import pickle
from threading import Lock
import ssl
try:
    # python3
    from urllib.request import urlopen
    from urllib.parse import urlencode
    from urllib.error import URLError
except ImportError:
    # python2
    from urllib2 import urlopen, URLError
    from urllib import urlencode

DEFAULT_CADATA = u'''-----BEGIN CERTIFICATE-----
MIIGATCCA+mgAwIBAgIJAN7JZUlglGn4MA0GCSqGSIb3DQEBCwUAMFcxCzAJBgNV
BAYTAlVTMRswGQYDVQQKExJXV1Bhc3MgQ29ycG9yYXRpb24xKzApBgNVBAMTIldX
UGFzcyBDb3Jwb3JhdGlvbiBQcmltYXJ5IFJvb3QgQ0EwIhgPMjAxMjExMjgwOTAw
MDBaGA8yMDUyMTEyODA4NTk1OVowVzELMAkGA1UEBhMCVVMxGzAZBgNVBAoTEldX
UGFzcyBDb3Jwb3JhdGlvbjErMCkGA1UEAxMiV1dQYXNzIENvcnBvcmF0aW9uIFBy
aW1hcnkgUm9vdCBDQTCCAiIwDQYJKoZIhvcNAQEBBQADggIPADCCAgoCggIBAMmF
pl1WX80osygWx4ZX8xGyYfHx8cpz29l5s/7mgQIYCrmUSLK9KtSryA0pmzrOFkyN
BuT0OU5ucCuv2WNgUriJZ78b8sekW1oXy2QXndZSs+CA+UoHFw0YqTEDO659/Tjk
NqlE5HMXdYvIb7jhcOAxC8gwAJFgAkQboaMIkuWsAnpOtKzrnkWHGz45qoyICjqz
feDcN0dh3ITMHXrYiwkVq5fGXHPbuJPbuBN+unnakbL3Ogk3yPnEcm6YV+HrxQ7S
Ky83q60Abdy8ft0RpSJeUkBjJVwiHu4y4j5iKC1tNgtV8qE9Zf2g5vAHzL3obqnu
IMr8JpmWp0MrrUa9jYOtKXk2LnZnfxurJ74NVk2RmuN5I/H0a/tUrHWtCE5pcVNk
b3vmoqeFsbTs2KDCMq/gzUhHU31l4Zrlz+9DfBUxlb5fNYB5lF4FnR+5/hKgo75+
OaNjiSfp9gTH6YfFCpS0OlHmKhsRJlR2aIKpTUEG9hjSg3Oh7XlpJHhWolQQ2BeL
++3UOyRMTDSTZ1bGa92oz5nS+UUsE5noUZSjLM+KbaJjZGCxzO9y2wiFBbRSbhL2
zXpUD2dMB1G30jZwytjn15VAMEOYizBoHEp2Nf9PNhsDGa32AcpJ2a0n89pbSOlu
yr/vEzYjJ2DZ/TWQQb7upi0G2kRX17UIZ5ZfhjmBAgMBAAGjgcswgcgwHQYDVR0O
BBYEFGu/H4b/gn8RzL7XKHBT6K4BQcl7MIGIBgNVHSMEgYAwfoAUa78fhv+CfxHM
vtcocFPorgFByXuhW6RZMFcxCzAJBgNVBAYTAlVTMRswGQYDVQQKExJXV1Bhc3Mg
Q29ycG9yYXRpb24xKzApBgNVBAMTIldXUGFzcyBDb3Jwb3JhdGlvbiBQcmltYXJ5
IFJvb3QgQ0GCCQDeyWVJYJRp+DAPBgNVHRMBAf8EBTADAQH/MAsGA1UdDwQEAwIB
BjANBgkqhkiG9w0BAQsFAAOCAgEAE46CMikI7378mkC3qZyKcVxkNfLRe3eD4h04
OO27rmfZj/cMrDDCt0Bn2t9LBUGBdXfZEn13gqn598F6lmLoObtN4QYqlyXrFcPz
FiwQarba+xq8togxjMkZ2y70MlV3/PbkKkwv4bBjOcLZQ1DsYehPdsr57C6Id4Ee
kEQs/aMtKcMzZaSipkTuXFxfxW4uBifkH++tUASD44OD2r7m1UlSQ5viiv3l0qvA
B89dPifVnIeAvPcd7+GY2RXTZCw36ZipnFiOWT9TkyTDpB/wjWQNFrgmmQvxQLeW
BWIUSaXJwlVzMztdtThnt/bNZNGPMRfaZ76OljYB9BKC7WUmss2f8toHiys+ERHz
0xfCTVhowlz8XtwWfb3A17jzJBm+KAlQsHPgeBEqtocxvBJcqhOiKDOpsKHHz+ng
exIO3elr1TCVutPTE+UczYTBRsL+jIdoIxm6aA9rrN3qDVwMnuHThSrsiwyqOXCz
zjCaCf4l5+KG5VNiYPytiGicv8PCBjwFkzIr+LRSyUiYzAZuiyRchpdT+yRAfL7q
qHBuIHYhG3E47a3GguwUwUGcXR+NjrSmteHRDONOUYUCH41hw6240Mo1lL4F+rpr
LEBB84k3+v+AtbXePEwvp+o1nu/+1sRkhqlNFHN67vakqC4xTxiuPxu6Pb/uDeNI
ip0+E9I=
-----END CERTIFICATE----- '''

PIN = 'p'
SESSION_KEY = 's'
CLIENT_KEY = 'c'

class WWPassException(IOError):
    pass

class WWPassConnection(object):

    def __init__(self, key_file, cert_file, timeout=10, spfe_addr='https://spfe.wwpass.com', cafile=None):
        """Construct class.

        Args:
            key_file (str): ???
            cert_file (str): ??
            timeout (int): ??
            spfe_addr (str): ??
            cafile (str): ??
        """

        self.context = ssl.SSLContext(protocol=ssl.PROTOCOL_TLSv1)
        self.context.load_cert_chain(certfile=cert_file, keyfile=key_file)
        if cafile is None:
            self.context.load_verify_locations(cadata=DEFAULT_CADATA)
        else:
            self.context.load_verify_locations(cafile=cafile)
        self.spfe_addr = 'https://%s' % spfe_addr if spfe_addr.find('://') == -1 else spfe_addr
        self.timeout = timeout

    def makeRequest(self, method, command, attempts=3,**paramsDict):
        params = {k:v.encode('UTF-8') if hasattr(v,"encode") else v for k, v in paramsDict.items() if v is not None}
        try:
            if method == 'GET':
                res = urlopen(self.spfe_addr +'/'+command+'?'+urlencode(params), context=self.context, timeout=self.timeout)
            else:
                res = urlopen(self.spfe_addr +'/'+command, data=urlencode(params).encode('UTF-8'), context=self.context, timeout=self.timeout)
            res = pickle.loads(res.read())
            if not res['result']:
                if 'code'in res:
                    raise WWPassException('SPFE returned error: %s: %s' %(res['code'], res['data']))
                raise WWPassException('SPFE returned error: %s' % res['data'])
            return res
        except URLError:
            if attempts>0:
                attempts -= 1
            else:
                raise
        return self.makeRequest(method, command, attempts,**params)

    @classmethod
    def makeAuthTypeString(cls, auth_types):
        valid_auth_types = (PIN, SESSION_KEY, CLIENT_KEY)
        return ''.join(x for x in auth_types if x in valid_auth_types)

    def getName(self):
        ticket = self.getTicket(ttl=0)['ticket']
        pos = ticket.find(':')
        if pos == -1:
            raise WWPassException('Cannot extract service provider name from ticket.')
        return ticket[:pos]

    def getTicket(self, ttl=None, auth_types=()):
        result = self.makeRequest('GET','get', ttl=ttl or None, auth_type=self.makeAuthTypeString(auth_types) or None)
        return {'ticket' : result['data'], 'ttl' : result['ttl']}

    def getPUID(self, ticket, auth_types=(), finalize=None):
        result = self.makeRequest('GET','puid', ticket=ticket, auth_type=self.makeAuthTypeString(auth_types) or None, finalize=finalize)
        return {'puid' : result['data']}

    def putTicket(self, ticket, ttl=None, auth_types=(), finalize=None):
        result = self.makeRequest('GET','put', ticket=ticket, ttl=ttl or None, auth_type=self.makeAuthTypeString(auth_types) or None, finalize=finalize)
        return {'ticket' : result['data'], 'ttl' : result['ttl']}

    def readData(self, ticket, container=b'', finalize=None):
        result = self.makeRequest('GET','read', ticket=ticket, container=container or None, finalize=finalize)
        return {'data' : result['data']}

    def readDataAndLock(self, ticket, lockTimeout, container=b''):
        result = self.makeRequest('GET','read', ticket=ticket, container=container or None, lock='1', to=lockTimeout)
        return {'data' : result['data']}


    def writeData(self, ticket, data, container=b'', finalize=None):
        self.makeRequest('POST','write', ticket=ticket, data=data, container=container or None, finalize=finalize)
        return True

    def writeDataAndUnlock(self, ticket, data, container=b'', finalize=None):
        self.makeRequest('POST','write', ticket=ticket, data=data, container=container or None, unlock='1', finalize=finalize)
        return True

    def lock(self, ticket, lockTimeout, lockid):
        self.makeRequest('GET','lock',ticket=ticket, lockid=lockid, to=lockTimeout)
        return True

    def unlock(self, ticket, lockid, finalize=None):
        self.makeRequest('GET','unlock', ticket=ticket, lockid=lockid, finalize=finalize)
        return True

    def getSessionKey(self, ticket, finalize=None):
        result = self.makeRequest('GET','key', ticket=ticket, finalize=finalize)
        return {'sessionKey' : result['data']}

    def createPFID(self, data=''):
        if data:
            result =  self.makeRequest('POST','sp/create', data=data)
        else:
            result =  self.makeRequest('GET','sp/create')
        return {'pfid' : result['data']}

    def removePFID(self, pfid):
        self.makeRequest('POST','sp/remove', pfid=pfid)
        return True

    def readDataSP(self, pfid):
        result =  self.makeRequest('GET','sp/read', pfid=pfid)
        return {'data' : result['data']}

    def readDataSPandLock(self, pfid, lockTimeout):
        result =  self.makeRequest('GET','sp/read', pfid=pfid, to=lockTimeout, lock=1)
        return {'data' : result['data']}

    def writeDataSP(self, pfid, data):
        self.makeRequest('POST','sp/write', pfid=pfid, data=data)
        return True

    def writeDataSPandUnlock(self, pfid, data):
        self.makeRequest('POST','sp/write', pfid=pfid, data=data, unlock=1)
        return True

    def lockSP(self, lockid, lockTimeout):
        self.makeRequest('GET','sp/lock',lockid=lockid, to=lockTimeout)
        return True

    def unlockSP(self, lockid):
        self.makeRequest('GET','sp/unlock',lockid=lockid)
        return True

    def getClientKey(self, ticket):
        result =  self.makeRequest('GET','clientkey',ticket=ticket)
        res_dict = {'clientKey' : result['data'], 'ttl' : result['ttl']}
        if 'originalTicket' in result:
            res_dict['originalTicket'] = result['originalTicket']
        return res_dict

class WWPassConnectionMT(WWPassConnection):

    def __init__(self, key_file, cert_file, timeout=10, spfe_addr='https://spfe.wwpass.com', ca_file=None, initial_connections=2):
        self.Pool = []
        self.key_file = key_file
        self.cert_file = cert_file
        self.ca_file = ca_file
        self.timeout = timeout
        self.spfe_addr = spfe_addr
        for _ in range(initial_connections):
            self.addConnection()

    def addConnection(self, acquired = False):
        c = WWPassConnection(self.key_file, self.cert_file, self.timeout, self.spfe_addr, self.ca_file)
        c.lock = Lock()
        if acquired:
            c.lock.acquire()
        self.Pool.append(c)
        return c

    def getConnection(self):
        for conn in (c for c in self.Pool if c.lock.acquire(False)):
            return conn
        conn=self.addConnection(True)
        return conn

    def makeRequest(self, method, command, attempts=3,**paramsDict):
        conn = None
        try:
            conn = self.getConnection()
            return conn.makeRequest(method, command, attempts, **paramsDict)
        finally:
            if conn is not None:
                conn.lock.release()

WWPASSConnection = WWPassConnection
WWPASSConnectionMT = WWPassConnectionMT
