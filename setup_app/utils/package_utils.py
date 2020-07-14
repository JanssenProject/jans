
from setup_app import paths
from setup_app.config import Config
from setup_app.utils import base
from setup_app.utils.setup_utils import SetupUtils

class PackageUtils(SetupUtils):

    #TODO: get commands from paths
    def get_install_commands(self):
        if base.clone_type == 'deb':
            install_command = 'DEBIAN_FRONTEND=noninteractive apt-get install -y {0}'
            update_command = 'DEBIAN_FRONTEND=noninteractive apt-get update -y'
            query_command = 'dpkg-query -W -f=\'${{Status}}\' {} 2>/dev/null | grep -c "ok installed"'
            check_text = '0'

        elif self.os_type in ('centos', 'red', 'fedora'):
            install_command = 'yum install -y {0}'
            update_command = 'yum install -y epel-release'
            query_command = 'rpm -q {0}'
            check_text = 'is not installed'
            
        return install_command, update_command, query_command, check_text

    def check_and_install_packages(self):

        install_command, update_command, query_command, check_text = self.get_install_commands()

        install_list = {'mondatory': [], 'optional': []}

        package_list = {
                'debian 10': {'mondatory': 'apache2 curl wget tar xz-utils unzip facter python3 rsyslog python3-ldap3 python3-requests python3-ruamel.yaml bzip2', 'optional': 'memcached'},
                'debian 9': {'mondatory': 'apache2 curl wget tar xz-utils unzip facter python3 rsyslog python3-ldap3 python3-requests python3-ruamel.yaml bzip2', 'optional': 'memcached'},
                'debian 8': {'mondatory': 'apache2 curl wget tar xz-utils unzip facter python3 rsyslog python3-ldap3 python3-requests python3-ruamel.yaml bzip2', 'optional': 'memcached'},
                'ubuntu 16': {'mondatory': 'apache2 curl wget xz-utils unzip facter python3 rsyslog python3-ldap3 python3-requests python3-ruamel.yaml bzip2', 'optional': 'memcached'},
                'ubuntu 18': {'mondatory': 'apache2 curl wget xz-utils unzip facter python3 rsyslog python3-ldap3 net-tools python3-requests python3-ruamel.yaml bzip2', 'optional': 'memcached'},
                'centos 7': {'mondatory': 'httpd mod_ssl curl wget tar xz unzip facter python3 python3-ldap3 python3-ruamel-yaml rsyslog bzip2', 'optional': 'memcached'},
                'red 7': {'mondatory': 'httpd mod_ssl curl wget tar xz unzip facter python3 rsyslog python3-ldap3 python3-requests python3-ruamel-yaml bzip2', 'optional': 'memcached'},
                'fedora 22': {'mondatory': 'httpd mod_ssl curl wget tar xz unzip facter python3 rsyslog python3-ldap3 python3-requests python3-ruamel-yaml bzip2', 'optional': 'memcached'},
                }

        os_type_version = self.os_type+' '+self.os_version

        for install_type in install_list:
            for package in package_list[os_type_version][install_type].split():
                if os_type_version in ('centos 7', 'red 7') and package.startswith('python3-'):
                    package_query = package.replace('python3-', 'python36-')
                else:
                    package_query = package
                sout, serr = self.run(query_command.format(package_query), shell=True, get_stderr=True)
                if check_text in sout+serr:
                    self.logIt('Package {0} was not installed'.format(package_query))
                    install_list[install_type].append(package_query)
                else:
                    self.logIt('Package {0} was installed'.format(package_query))

        install = {'mondatory': True, 'optional': False}

        for install_type in install_list:
            if install_list[install_type]:
                packages = " ".join(install_list[install_type])

                if not setupOptions['noPrompt']:
                    if install_type == 'mondatory':
                        print("The following packages are required for Gluu Server")
                        print(packages)
                        r = input("Do you want to install these now? [Y/n] ")
                        if r and r.lower()=='n':
                            install[install_type] = False
                            if install_type == 'mondatory':
                                print("Can not proceed without installing required packages. Exiting ...")
                                sys.exit()

                    elif install_type == 'optional':
                        print("You may need the following packages")
                        print(packages)
                        r = input("Do you want to install these now? [y/N] ")
                        if r and r.lower()=='y':
                            install[install_type] = True

                if install[install_type]:
                    self.logIt("Installing packages " + packages)
                    print("Installing packages", packages)
                    if not self.os_type == 'fedora':
                        sout, serr = self.run(update_command, shell=True, get_stderr=True)
                    self.run(install_command.format(packages), shell=True)

        if self.os_type in ('ubuntu', 'debian'):
            self.run('a2enmod ssl headers proxy proxy_http proxy_ajp', shell=True)
            default_site = '/etc/apache2/sites-enabled/000-default.conf'
            if os.path.exists(default_site):
                os.remove(default_site)


    def installPackage(self, packageName):
        if base.clone_type == 'deb':
            output = self.run([paths.cmd_dpkg, '--install', packageName])
        else:
            output = self.run([paths.cmd_rpm, '--install', '--verbose', '--hash', packageName])

        return output
