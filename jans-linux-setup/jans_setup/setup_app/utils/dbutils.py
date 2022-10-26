import os
from ldap3 import Server, Connection

from jans_utils import read_properties_file
from cbm import CBM


def decode_password(encpw):
    cmd = '/opt/jans/bin/encode.py -D ' + encpw
    pw = os.popen(cmd).read().strip()
    return pw

def get_ldap_conn():
    ox_ldap_prop = read_properties_file('/etc/jans/conf/jans-ldap.properties')

    bindDN = ox_ldap_prop['bindDN']
    bindPassword = decode_password(ox_ldap_prop['bindPassword'])
    ldap_host, ldap_port = ox_ldap_prop['servers'].split(',')[0].strip().split(':')

    ldap_server = Server(ldap_host, port=int(ldap_port), use_ssl=True)
    ldap_conn = Connection(ldap_server, user=bindDN, password=bindPassword)
    ldap_conn.bind()

    return ldap_conn


def get_cbm_conn():
    jans_cb_prop = read_properties_file('/etc/jans/conf/jans-couchbase.properties')
    cb_serevr = jans_cb_prop['servers'].split(',')[0].strip()
    cb_admin = jans_cb_prop['auth.userName']
    cb_passwd = decode_password(jans_cb_prop['auth.userPassword'])

    cbm = CBM(cb_serevr, cb_admin, cb_passwd)

    return cbm
