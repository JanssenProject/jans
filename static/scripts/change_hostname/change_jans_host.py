import os
import json
import argparse
import subprocess

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

            self.container = '/opt/gluu-server'

            if 'nochroot' in self.gluu_version:
                self.run_command = '{}'
                self.container = '/'

            elif ('Ubuntu' in self.server_os) or ('Debian' in self.server_os):
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
        print("Executing:", cmd)
        run_cmd = self.run_command.format(cmd)
        return self.c.run(run_cmd)

    def install(self, package):
        run_cmd = self.install_command.format(package)
        print("Executing:", run_cmd)
        return self.c.run(run_cmd)

    def delete_key(self, suffix, hostname):

        defaultTrustStorePW = 'changeit'
        defaultTrustStoreFN = '/opt/jre/jre/lib/security/cacerts'
        cert = 'etc/certs/{0}.crt'.format(suffix)

        if os.path.exists(os.path.join(self.container, cert)):
            cmd=' '.join([
                            '/opt/jre/bin/keytool', "-delete", "-alias",
                            "%s_%s" % (hostname, suffix),
                            "-keystore", defaultTrustStoreFN,
                            "-storepass", defaultTrustStorePW
                            ])
            self.run(cmd)


    def import_key(self, suffix, hostname):
        """Imports key for identity server

        Args:
            suffix (string): suffix of the key to be imported
        """
        defaultTrustStorePW = 'changeit'
        defaultTrustStoreFN = '/opt/jre/jre/lib/security/cacerts'
        certFolder = '/etc/certs'
        public_certificate = '%s/%s.crt' % (certFolder, suffix)
        cmd =' '.join([
                        '/opt/jre/bin/keytool', "-import", "-trustcacerts",
                        "-alias", "%s_%s" % (hostname, suffix),
                        "-file", public_certificate, "-keystore",
                        defaultTrustStoreFN,
                        "-storepass", defaultTrustStorePW, "-noprompt"
                        ])

        self.run(cmd)

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
        print(cmd)
        p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True)

        return '', p.stdout.read().decode(), p.stderr.read().decode()


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
                    cert_country, ldap_password, os_type, ip_address,
                    gluu_version, server='localhost', local=False,
                    ssh_port=22
                    ):

        self.old_host = old_host
        self.new_host = new_host
        self.ip_address = ip_address
        self.cert_city = cert_city
        self.cert_mail = cert_mail
        self.cert_state = cert_state
        self.cert_country = cert_country
        self.server = server
        self.ssh_port = ssh_port
        self.ldap_password = ldap_password
        self.os_type = os_type
        self.gluu_version = gluu_version
        self.local = local
        self.logger_tid = None


    def startup(self):
        ldap_server = Server("ldaps://{}:1636".format(self.server), use_ssl=True)
        self.conn = Connection(ldap_server, user="cn=directory manager", password=self.ldap_password)
        r = self.conn.bind()
        if not r:
            print("Can't conect to LDAP Server")
            return False

        
        self.container =  '/' if 'nochroot' in self.gluu_version else '/opt/gluu-server'

        self.c = FakeRemote()


        self.installer = Installer(self.c, self.gluu_version, self.os_type)

        self.installer.hostname = self.server
        
        return True

    def change_ldap_entries(self):
        print("Changing LDAP Entries")

        self.conn.modify(
            'ou=configuration,o=gluu', 
             {'gluuHostname': [MODIFY_REPLACE, self.new_host]}
            )

        sdns = [
                'ou=configuration,o=gluu',
                'ou=clients,o=gluu',
                'ou=scripts,o=gluu',
                'ou=uma,o=gluu',
                'ou=scopes,o=gluu',
                ]

        for sdn in sdns:
            self.conn.search(search_base=sdn, search_scope=SUBTREE, search_filter='(objectclass=*)', attributes=['*'])

            for entry in self.conn.response:
                
                for field in entry['attributes']:
                    changeAttr = False
                    for i, e in enumerate(entry['attributes'][field]):
                        if isinstance(e, str) and self.old_host in e:
                            entry['attributes'][field][i] = e.replace(self.old_host, self.new_host)
                            changeAttr = True

                    if changeAttr:
                        self.conn.modify(
                                entry['dn'], 
                                {field: [MODIFY_REPLACE, entry['attributes'][field]]}
                                )

    def change_httpd_conf(self):
        print("Changing httpd configurations")
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
        print("Creating certificates")
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


        cert_list = ['httpd', 'asimba', 'idp-encryption', 'idp-signing', 'shibIDP', 'saml.pem']

        for crt in cert_list:

            for cmd in cmd_list:
                cmd = cmd.format(crt)
                print(self.installer.run(cmd))


            if not crt == 'saml.pem':

                self.installer.delete_key(crt, self.old_host)
                self.installer.import_key(crt, self.new_host)
            
        saml_crt_old_path = os.path.join(self.container, 'etc/certs/saml.pem.crt')
        saml_crt_new_path = os.path.join(self.container, 'etc/certs/saml.pem')
        self.c.rename(saml_crt_old_path, saml_crt_new_path)

        self.installer.run('chown jetty:jetty /etc/certs/oxauth-keys.*')

    def change_host_name(self):
        print("Changing hostname")
        hostname_file = os.path.join(self.container, 'etc/hostname')
        print(self.c.put_file(hostname_file, self.new_host))

    def modify_etc_hosts(self):
        print("Modifying /etc/hosts")
        hosts_file = os.path.join(self.container, 'etc/hosts')
        r = self.c.get_file(hosts_file)
        if r[0]:
            old_hosts = r[1]
            news_hosts = modify_etc_hosts([(self.new_host, self.ip_address)], old_hosts, self.old_host)
            print(self.c.put_file(hosts_file, news_hosts)) 

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
        )

    name_changer.startup()
    name_changer.change_ldap_entries()
    name_changer.change_httpd_conf()
    name_changer.create_new_certs()
    name_changer.change_host_name()
    name_changer.modify_etc_hosts()
