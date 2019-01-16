import sys
import os
import json
import argparse


from ldap3 import Server, Connection, SUBTREE, BASE, LEVEL, \
    MODIFY_REPLACE, MODIFY_ADD, MODIFY_DELETE


def modify_etc_hosts(host_ip, old_hosts, old_host):

    hosts = {
            'ipv4':{},
            'ipv6':{},
            }

    for l in old_hosts:
        ls=l.strip()
        if ls:
            if not ls[0]=='#':
                if ls[0]==':':
                    h_type='ipv6'
                else:
                    h_type='ipv4'

                lss = ls.split()
                ip_addr = lss[0]

                if not ip_addr in hosts[h_type]:
                    hosts[h_type][ip_addr]=[]
                for h in lss[1:]:

                    if (not h in hosts[h_type][ip_addr]) and (h!=old_host):
                        hosts[h_type][ip_addr].append(h)

    for h,i in host_ip:
        if h in hosts['ipv4']['127.0.0.1']:
            hosts['ipv4']['127.0.0.1'].remove(h)

    for h,i in host_ip:
        if h in hosts['ipv6']['::1']:
            hosts['ipv6']['::1'].remove(h)

    for h,i in host_ip:
        if i in hosts['ipv4']:
            if not h in hosts['ipv4'][i]:
                hosts['ipv4'][i].append(h)
        else:
            hosts['ipv4'][i] = [h]

    hostse = ''

    for iptype in hosts:
        for ipaddr in hosts[iptype]:
            host_list = [ipaddr] + hosts[iptype][ipaddr]
            hl =  "\t".join(host_list)
            hostse += hl +'\n'

    return hostse


class Installer:
    def __init__(self, c, gluu_version, server_os):
        self.c = c
        self.gluu_version = gluu_version
        self.server_os = server_os
        if not hasattr(self.c, 'fake_remote'):

            self.container = '/opt/gluu-server-{}'.format(gluu_version)

            if ('Ubuntu' in self.server_os) or ('Debian' in self.server_os):
                self.run_command = 'chroot {} /bin/bash -c "{}"'.format(self.container,'{}')
                self.install_command = 'chroot {} /bin/bash -c "apt-get install -y {}"'.format(self.container,'{}')
            elif 'CentOS' in self.server_os:
                self.run_command = ('ssh -o IdentityFile=/etc/gluu/keys/gluu-console '
                                '-o Port=60022 -o LogLevel=QUIET -o '
                                'StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null '
                                '-o PubkeyAuthentication=yes root@localhost \'{}\''
                                )

            self.install_command = self.run_command.format('yum install -y {}')
        else:
            self.run_command = '{}'

    def run(self, cmd):
        print "Executing:", cmd
        run_cmd = self.run_command.format(cmd)
        return self.c.run(run_cmd)

    def install(self, package):
        run_cmd = self.install_command.format(package)
        print "Executing:", run_cmd
        return self.c.run(run_cmd)

#Fake RemoteClient
class FakeRemote:

    """Provides fake remote class with the same run() function.
    """

    def run(self, cmd):

        """This method executes cmd as a sub-process.

        Args:
            cmd (string): commands to run locally

        Returns:
            Standard input, output and error of command

        """
        print cmd
        cin, cout, cerr = os.popen3(cmd)

        return '', cout.read(), cerr.read()


    def put_file(self, filename, filecontent):
        with open(filename, 'w') as f:
            f.write(filecontent)
            
    def exists(self, path):
        return os.path.exists(path)

    def rename(self, oldname, newname):
        os.rename(oldname, newname)

    def get_file(self, filename):
        return True, open(filename)

class ChangeGluuHostname:
    def __init__(self, old_host, new_host, cert_city, cert_mail, cert_state,
                    cert_country, ldap_password, os_type, ip_address, server='localhost',
                    gluu_version='',
                    local=True, ldap_type='opendj'):

        self.old_host = old_host
        self.new_host = new_host
        self.ip_address = ip_address
        self.cert_city = cert_city
        self.cert_mail = cert_mail
        self.cert_state = cert_state
        self.cert_country = cert_country
        self.server = server
        self.ldap_password = ldap_password
        self.os_type = os_type
        self.gluu_version = gluu_version
        self.local = local
        self.base_inum = None
        self.appliance_inum = None
        self.ldap_type = ldap_type


    def startup(self):
        if self.local:
            ldap_server = 'localhost'
        else:
            ldap_server = self.server

        ldap_bind_dn = "cn=directory manager"
        if self.ldap_type == 'openldap':
            ldap_bind_dn += ' ,o=gluu'

        ldap_server = Server("ldaps://{}:1636".format(self.server), use_ssl=True)
        self.conn = Connection(ldap_server, user=ldap_bind_dn, password=self.ldap_password)
        r = self.conn.bind()
        if not r:
            print "Can't conect to LDAP Server"
            return False

        self.container = '/opt/gluu-server-{}'.format(self.gluu_version)
        if not self.local:
            print "NOT LOCAL?"

            sys.path.append("..")
            from clustermgr.core.remote import RemoteClient
            self.c = RemoteClient(self.server)
            self.c.startup()
        else:
            self.c = FakeRemote()
            if os.path.exists('/etc/gluu/conf/ox-ldap.properties'):
                self.container = '/'
                self.c.fake_remote = True


        self.installer = Installer(self.c, self.gluu_version, self.os_type)

        self.appliance_inum = self.get_appliance_inum()
        self.base_inum = self.get_base_inum()

        return True
    def get_appliance_inum(self):
        self.conn.search(search_base='ou=appliances,o=gluu',
                    search_filter='(objectclass=*)',
                    search_scope=SUBTREE, attributes=['inum'])

        for r in self.conn.response:
            if r['attributes']['inum']:
                return r['attributes']['inum'][0]


    def get_base_inum(self):
        self.conn.search(search_base='o=gluu',
                search_filter='(objectclass=gluuOrganization)',
                search_scope=SUBTREE, attributes=['o'])

        for r in self.conn.response:
            if r['attributes']['o']:
                return r['attributes']['o'][0]


    def change_appliance_config(self):
        print "Changing LDAP Applience configurations"
        config_dn = 'ou=configuration,inum={},ou=appliances,o=gluu'.format(
                    self.appliance_inum)


        for dns, cattr in (
                    ('', 'oxIDPAuthentication'),
                    ('oxauth', 'oxAuthConfDynamic'),
                    ('oxidp', 'oxConfApplication'),
                    ('oxtrust', 'oxTrustConfApplication'),
                    ):
            if dns:
                dn = 'ou={},{}'.format(dns, config_dn)
            else:
                dn = 'inum={},ou=appliances,o=gluu'.format(self.appliance_inum)

            self.conn.search(search_base=dn,
                        search_filter='(objectClass=*)',
                        search_scope=BASE, attributes=[cattr])

            config_data = json.loads(self.conn.response[0]['attributes'][cattr][0])

            for k in config_data:
                kVal = config_data[k]
                if type(kVal) == type(u''):
                    if self.old_host in kVal:
                        kVal=kVal.replace(self.old_host, self.new_host)
                        config_data[k]=kVal

            config_data = json.dumps(config_data)
            self.conn.modify(dn, {cattr: [MODIFY_REPLACE, config_data]})


    def change_clients(self):
        print "Changing LDAP Clients configurations"
        dn = "ou=clients,o={},o=gluu".format(self.base_inum)
        self.conn.search(search_base=dn,
                    search_filter='(objectClass=oxAuthClient)',
                    search_scope=SUBTREE, attributes=[
                                                'oxAuthPostLogoutRedirectURI',
                                                'oxAuthRedirectURI',
                                                'oxClaimRedirectURI',
                                                ])

        result = self.conn.response[0]['attributes']

        dn = self.conn.response[0]['dn']

        for atr in result:
            for i in range(len(result[atr])):
                changeAttr = False
                if self.old_host in result[atr][i]:
                    changeAttr = True
                    result[atr][i] = result[atr][i].replace(self.old_host, self.new_host)
                    self.conn.modify(dn, {atr: [MODIFY_REPLACE, result[atr]]})



    def change_uma(self):
        print "Changing LDAP UMA Configurations"

        for ou, cattr in (
                    ('resources','oxResource'),
                    ('scopes', 'oxId'),
                    ):

            dn = "ou={},ou=uma,o={},o=gluu".format(ou, self.base_inum)

            self.conn.search(search_base=dn, search_filter='(objectClass=*)', search_scope=SUBTREE, attributes=[cattr])
            result = self.conn.response

            for r in result:
                for i in range(len( r['attributes'][cattr])):
                    changeAttr = False
                    if self.old_host in r['attributes'][cattr][i]:
                        r['attributes'][cattr][i] = r['attributes'][cattr][i].replace(self.old_host, self.new_host)
                        self.conn.modify(r['dn'], {cattr: [MODIFY_REPLACE, r['attributes'][cattr]]})


    def change_httpd_conf(self):
        print "Changing httpd configurations"
        if 'CentOS' in self.os_type:

            httpd_conf = os.path.join(self.container, 'etc/httpd/conf/httpd.conf')
            https_gluu = os.path.join(self.container, 'etc/httpd/conf.d/https_gluu.conf')
            conf_files = [httpd_conf, https_gluu]

        elif 'Ubuntu' in self.os_type:
            https_gluu = os.path.join(self.container, 'etc/apache2/sites-available/https_gluu.conf')
            conf_files = [https_gluu]

        for conf_file in conf_files:
            result, fileObj = self.c.get_file(conf_file)
            if result:
                config_text = fileObj.read()
                config_text = config_text.replace(self.old_host, self.new_host)
                self.c.put_file(conf_file, config_text)

    def create_new_certs(self):
        print "Backing up certificates"
        cmd_list = [
            'mkdir /etc/certs/backup',
            'cp /etc/certs/* /etc/certs/backup'
            ]
        for cmd in cmd_list:
            print self.installer.run(cmd)

        print "Creating certificates"
        cmd_list = [
            '/usr/bin/openssl genrsa -des3 -out /etc/certs/{0}.key.orig -passout pass:secret 2048',
            '/usr/bin/openssl rsa -in /etc/certs/{0}.key.orig -passin pass:secret -out /etc/certs/{0}.key',
            '/usr/bin/openssl req -new -key /etc/certs/{0}.key -out /etc/certs/{0}.csr -subj '
            '"/C={4}/ST={5}/L={1}/O=Gluu/CN={2}/emailAddress={3}"'.format('{0}', self.cert_city, self.new_host, self.cert_mail, self.cert_country, self.cert_state),
            '/usr/bin/openssl x509 -req -days 365 -in /etc/certs/{0}.csr -signkey /etc/certs/{0}.key -out /etc/certs/{0}.crt',
            'chown root:gluu /etc/certs/{0}.key.orig',
            'chmod 700 /etc/certs/{0}.key.orig',
            'chown root:gluu /etc/certs/{0}.key',
            'chmod 700 /etc/certs/{0}.key',
            ]

        cert_list = ['httpd', 'idp-encryption', 'idp-signing', 'shibIDP', 'opendj', 'passport-sp', 'asimba', 'saml.pem']
        

        for crt in cert_list:

            for cmd in cmd_list:
                cmd = cmd.format(crt)
                print self.installer.run(cmd)


            if not crt == 'saml.pem':

                del_key = ( '/opt/jre/bin/keytool -delete -alias {}_{} -keystore '
                        '/opt/jre/jre/lib/security/cacerts -storepass changeit').format(self.old_host, crt)


                r = self.installer.run(del_key)
                #if r[1]:
                #    print "Info:", r[1]
                #if r[2]:
                #    print "** ERROR:", r[2]

                add_key = ('/opt/jre/bin/keytool -import -trustcacerts -alias '
                      '{0}_{1} -file /etc/certs/{2}.crt -keystore '
                      '/opt/jre/jre/lib/security/cacerts -storepass changeit -noprompt').format(self.new_host, crt, crt)

                r = self.installer.run(add_key)

        self.installer.run('chown jetty:jetty /etc/certs/oxauth-keys.*')

    def modify_saml_passport(self):
        print "Modifying SAML & Passport if installed"
        
        files = [
            '/opt/gluu-server-{0}/opt/shibboleth-idp/conf/idp.properties'.format(self.gluu_version),
            '/opt/gluu-server-{0}/opt/shibboleth-idp/metadata/idp-metadata.xml'.format(self.gluu_version),
            '/opt/gluu-server-{0}/etc/gluu/conf/passport-config.json'.format(self.gluu_version),
            ]

        for fn in files:
            if self.c.exists(fn):
                print "Modifying Shibboleth {0}".format(fn)
                r = self.c.get_file(fn)
                if r[0]:
                    f = r[1].read()
                    f = f.replace(self.old_host, self.new_host)
                    print self.c.put_file(fn, f)

    def change_host_name(self):
        print "Changing hostname"
        hostname_file = os.path.join(self.container, 'etc/hostname')
        print self.c.put_file(hostname_file, self.new_host)

    def modify_etc_hosts(self):
        print "Modifying /etc/hosts"
        hosts_file = os.path.join(self.container, 'etc/hosts')
        r = self.c.get_file(hosts_file)
        if r[0]:
            old_hosts = r[1]
            news_hosts = modify_etc_hosts([(self.new_host, self.ip_address)], old_hosts, self.old_host)
            print self.c.put_file(hosts_file, news_hosts)

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('-old', required=True,  help="Old hostanme")
    parser.add_argument('-new', required=True,  help="New hostname")
    parser.add_argument('-server', required=True, help="Hostname or IP for LDAP and ssh")
    parser.add_argument('-mail',  required=True, help="Email of admin")
    parser.add_argument('-city',  required=True, help="City for creating certificates")
    parser.add_argument('-state',  required=True, help="State for creating certificates")
    parser.add_argument('-country',  required=True, help="Country for creating certificates")
    parser.add_argument('-password',  required=True, help="Admin password")
    parser.add_argument('-os', required=True, help="OS type: CentOS, Ubuntu", choices=['CentOS','Ubuntu'])
    parser.add_argument('-ldaptype', required=True, help="Ldap Server: openldap, opendj", choices=['openldap','opendj'])

    args = parser.parse_args()

    name_changer = ChangeGluuHostname(
        old_host=args.old,
        new_host=args.new,
        cert_city=args.city,
        cert_mail=args.mail,
        cert_state=args.state,
        cert_country=args.country,
        server=args.server,
        ldap_password=args.password,
        os_type=args.os,
        ldap_type=ldaptype
        )

    name_changer.startup()
    name_changer.change_appliance_config()
    name_changer.change_clients()
    name_changer.change_uma()
    name_changer.change_httpd_conf()
    name_changer.create_new_certs()
    name_changer.change_host_name()
    name_changer.modify_saml_passport()
