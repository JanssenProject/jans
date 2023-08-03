import os

prop_file = '/install/community-edition-setup/setup.properties.last'

prop = {}

for l in open(prop_file):
    ls=l.strip()
    n = ls.find('=')
    if not ls.startswith('#'):
        key = ls[:n]
        val = ls[n+1:].strip()
        val = val.replace('\\=','=').replace('\\:',':')
        prop[key] = val



def delete_key(suffix):
    default_trust_store_pw = 'changeit'
    default_trust_store_fn = '/opt/jre/jre/lib/security/cacerts'
    cert = '/etc/certs/{0}.crt'.format(suffix)

    if os.path.exists(cert):
        cmd=' '.join([
                        '/opt/jre/bin/keytool', "-delete", "-alias",
                        "%s_%s" % (prop['hostname'], suffix),
                        "-keystore", default_trust_store_fn,
                        "-storepass", default_trust_store_pw
                        ])
        os.system(cmd)


def import_key(suffix):
    default_trust_store_pw = 'changeit'
    default_trust_store_fn = '/opt/jre/jre/lib/security/cacerts'
    certFolder = '/etc/certs'
    public_certificate = '%s/%s.crt' % (certFolder, suffix)
    cmd =' '.join([
                    '/opt/jre/bin/keytool', "-import", "-trustcacerts",
                    "-alias", "%s_%s" % (prop['hostname'], suffix),
                    "-file", public_certificate, "-keystore",
                    default_trust_store_fn,
                    "-storepass", default_trust_store_pw, "-noprompt"
                    ])

    os.system(cmd)


def create_new_certs():
    print "Creating certificates"
    cmd_list = [
        '/usr/bin/openssl genrsa -des3 -out /etc/certs/{0}.key.orig -passout pass:secret 2048',
        '/usr/bin/openssl rsa -in /etc/certs/{0}.key.orig -passin pass:secret -out /etc/certs/{0}.key',
        '/usr/bin/openssl req -new -key /etc/certs/{0}.key -out /etc/certs/{0}.csr -subj '
        '"/C={4}/ST={5}/L={1}/O=Gluu/CN={2}/emailAddress={3}"'.format('{0}', prop['city'], prop['hostname'], prop['admin_email'] , prop['countryCode'] , prop['state']),
        '/usr/bin/openssl x509 -req -days 365 -in /etc/certs/{0}.csr -signkey /etc/certs/{0}.key -out /etc/certs/{0}.crt',
        'chown root:gluu /etc/certs/{0}.key.orig',
        'chmod 700 /etc/certs/{0}.key.orig',
        'chown root:gluu /etc/certs/{0}.key',
        'chmod 700 /etc/certs/{0}.key',
        ]


    cert_list = ['httpd', 'asimba', 'idp-encryption', 'idp-signing', 'shibIDP', 'saml.pem']

    for crt in cert_list:

        for cmd in cmd_list:
            cmd = cmd.format(crt)
            os.system(cmd)

        if not crt == 'saml.pem':
            delete_key(crt)
            import_key(crt)

    os.rename('/etc/certs/saml.pem.crt', '/etc/certs/saml.pem')

    os.system('chown jetty:jetty /etc/certs/oxauth-keys.*')


create_new_certs()
