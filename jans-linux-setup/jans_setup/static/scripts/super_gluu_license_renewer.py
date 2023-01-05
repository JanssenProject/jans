#!/usr/bin/python3

import urllib
import os
import json
import ldap3
import time
import shutil

def renew_license():

    ldap_properties_fn = '/etc/jans/conf/jans-ldap.properties'
    salt_fn = '/etc/jans/conf/salt'
    encode_fn = '/opt/jans/bin/encode.py'
    
    for fn in (ldap_properties_fn, salt_fn, encode_fn):
        if not os.path.exists(fn):
            print("Not found:", fn)
            return
    
    prop = {'bindDN':'', 'bindPassword':'', 'servers':''}
    content = open(ldap_properties_fn).readlines()
    for l in content:
        ls = l.strip()
        if ls and not ls[0] == '#':
            for p in prop:
                if ls.startswith(p):
                    v = ls[len(p):].strip()[1:].strip()
                    v=v.replace('\\=','=')
                    v=v.replace("\\'","'")
                    v=v.replace('\\"','"')
                    prop[p] = v

    cmd = encode_fn + ' -D ' + prop['bindPassword']
    encoded_password = os.popen(cmd).read().strip()
    if not encoded_password:
        print("Password can't be encoded")
        return

    ldap_server = prop['servers'].split(',')[0].strip()
    ldap_host, ldap_port = ldap_server.split(':')

    server = ldap3.Server(ldap_host, port=int(ldap_port), use_ssl=True)
    ldap_conn = ldap3.Connection(server, user=prop['bindDN'], password=encoded_password)
    try:
        ldap_conn.bind()
    except:
        print("Can't connect to ldap server")
        return

    scr_dn = 'inum=92F0-BF9E,ou=scripts,o=jans'

    ldap_conn.search(
                search_base=scr_dn, 
                search_scope=ldap3.BASE, 
                search_filter='(objectClass=*)', 
                attributes=['jansConfProperty']
                )

    result = ldap_conn.response

    if not result:
        print("Can't find Super Gluu script with dn {} in ldap".format(scr_dn))
        return
        
    for oxprop_s in result[0]['attributes']['jansConfProperty']:
         oxprop = json.loads(oxprop_s)
         if oxprop['value1'] == 'license_file':
             license_fn = oxprop['value2']
             break
    else:
        print("Super gluu license file not found in ldap")
        return


    if not os.path.exists(license_fn):
        print("Super gluu license file {} does not exist".format(license_fn))
        return
        
    with open(license_fn) as f:
        license_js = json.load(f)

    licenseId = license_js.get('licenseId')

    if not licenseId:
        print(license_fn, " does not include licenseId")
        return

    url_metadata = 'https://license.gluu.org/oxLicense/rest/metadata?licenseId=' + licenseId

    try:
        url_metadata_fd = urllib.urlopen(url_metadata)
        metadata_s = url_metadata_fd.read()

    except:
        print("Can't read from", url_metadata)
        return

    try:
        metadata = json.loads(metadata_s)
    except:
        print("Can't load json from", metadata_s)
        return
        
    if 'expiration_date' in metadata: 
        expiration_date =  metadata['expiration_date']
    else:
        print("Can't get expiration_date from json", metadata)
        return

    seconds_left = int(expiration_date/1000) - time.time()
    a_day = 24*60*60

    if seconds_left >= a_day:
        print(int(seconds_left/a_day), "days left to expire. No need to renew")
        return

    url = 'https://license.gluu.org/oxLicense/rest/generate?licenseId=' + licenseId
    try:
        url_fd = urllib.urlopen(url)
        data_s = url_fd.read()

    except:
        print("Can't read from", url)
        return

    try:
        data = json.loads(data_s)
    except:
        print("Can't load json from", data)
        return

    if data and data[0].get('license','').startswith('rO'):
        license_js['license'] = data[0]['license']
        shutil.copyfile(license_fn, license_fn + '.back_' + time.ctime().replace(' ','_'))
        with open(license_fn, 'w') as w:
            json.dump(license_js, w, indent=2)

    else:
        print("Data read from", url, "does not contain valid license")
        return

    print("Super Gluu license was renewed successfully")

renew_license()
