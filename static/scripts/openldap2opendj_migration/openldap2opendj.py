#!/usr/bin/python

# The MIT License (MIT)
#
# Copyright (c) 2014 Gluu
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.


import os.path
import Properties
import random
import shutil
import socket
import string
import time
import uuid
import json
import traceback
import subprocess
import sys
import getopt
import hashlib
import re
import glob
import base64


import ldap
from ldap.controls import SimplePagedResultsControl
import datetime
ldap.set_option(ldap.OPT_X_TLS_REQUIRE_CERT, ldap.OPT_X_TLS_ALLOW)
ldap.set_option(ldap.OPT_REFERRALS, 0)

from pyDes import *


migration_time = time.ctime().replace(' ','_')


ox_ldap_prop_fn = '/etc/gluu/conf/ox-ldap.properties'


def get_ldap_bind_pw():
    
    prop = open(ox_ldap_prop_fn)
    for l in prop:
        ls = l.strip()
        if ls.startswith('bindPassword'):
            
            n = ls.find(':')
            encpw = ls[n+1:].strip()
            clrpw = os.popen('python /opt/gluu/bin/encode.py -D ' + encpw).read()
            return clrpw.strip()

def update_ox_ldap_prop(bindDN, trustStoreFile, trustStorePin):
    prop = open(ox_ldap_prop_fn).readlines()
    
    for i, l in enumerate(prop):
        ls = l.strip()
        if ls.startswith('bindDN'):
            prop[i] = 'bindDN: {0}\n'.format(bindDN)
        elif ls.startswith('ssl.trustStoreFile'):
            prop[i] = 'ssl.trustStoreFile: {0}\n'.format(trustStoreFile)
        elif ls.startswith('ssl.trustStorePin'):
            prop[i] = 'ssl.trustStorePin: {0}\n'.format(trustStorePin)
    cmd = 'cp {0} {0}.back_{1}'.format(ox_ldap_prop_fn, migration_time)
        
    os.system(cmd)
    
    with open(ox_ldap_prop_fn, 'w') as w:
        w.write(''.join(prop))
    
def update_ox_idp(ldap_bind_dn, ldap_bind_pw):
    conn = ldap.initialize('ldaps://localhost:1636')
    conn.protocol_version = 3 
    conn.simple_bind_s(ldap_bind_dn, ldap_bind_pw)

    result = conn.search_s('ou=appliances,o=gluu',ldap.SCOPE_SUBTREE,'(oxIDPAuthentication=*)',['oxIDPAuthentication'])

    dn = result[0][0]
    oxIDPAuthentication = json.loads(result[0][1]['oxIDPAuthentication'][0])    
    config = json.loads(oxIDPAuthentication['config'])
    config['bindDN'] = 'cn=Directory Manager'
    oxIDPAuthentication['config'] = json.dumps(config)
    oxIDPAuthentication = json.dumps(oxIDPAuthentication)
    
    conn.modify_s(dn, [( ldap.MOD_REPLACE, 'oxIDPAuthentication',  oxIDPAuthentication)])

class Setup(object):
    def __init__(self, install_dir=None):
        self.install_dir = install_dir

        self.oxVersion = '3.1.2.Final'
        self.githubBranchName = 'version_3.1.2'

        # Used only if -w (get wars) options is given to setup.py
        self.oxauth_war = 'https://ox.gluu.org/maven/org/xdi/oxauth-server/%s/oxauth-server-%s.war' % (self.oxVersion, self.oxVersion)
        self.oxauth_rp_war = 'https://ox.gluu.org/maven/org/xdi/oxauth-rp/%s/oxauth-rp-%s.war' % (self.oxVersion, self.oxVersion)
        self.oxtrust_war = 'https://ox.gluu.org/maven/org/xdi/oxtrust-server/%s/oxtrust-server-%s.war' % (self.oxVersion, self.oxVersion)
        self.idp3_war = 'http://ox.gluu.org/maven/org/xdi/oxshibbolethIdp/%s/oxshibbolethIdp-%s.war' % (self.oxVersion, self.oxVersion)
        self.idp3_dist_jar = 'http://ox.gluu.org/maven/org/xdi/oxShibbolethStatic/%s/oxShibbolethStatic-%s.jar' % (self.oxVersion, self.oxVersion)
        self.idp3_cml_keygenerator = 'http://ox.gluu.org/maven/org/xdi/oxShibbolethKeyGenerator/%s/oxShibbolethKeyGenerator-%s.jar' % (self.oxVersion, self.oxVersion)
        self.asimba_war = 'http://ox.gluu.org/maven/org/asimba/asimba-wa/%s/asimba-wa-%s.war' % (self.oxVersion, self.oxVersion)
        self.cred_manager_war = 'http://ox.gluu.org/maven/org/xdi/cred-manager/%s/cred-manager-%s.war' % (self.oxVersion, self.oxVersion)
        self.ce_setup_zip = 'https://github.com/GluuFederation/community-edition-setup/archive/%s.zip' % self.githubBranchName
        self.java_1_8_jce_zip = 'http://download.oracle.com/otn-pub/java/jce/8/jce_policy-8.zip'

        self.downloadWars = None
        self.templateRenderingDict = {}

        # OS commands
        self.cmd_ln = '/bin/ln'
        self.cmd_chmod = '/bin/chmod'
        self.cmd_chown = '/bin/chown'
        self.cmd_chgrp = '/bin/chgrp'
        self.cmd_mkdir = '/bin/mkdir'
        self.cmd_rpm = '/bin/rpm'
        self.cmd_dpkg = '/usr/bin/dpkg'
        self.opensslCommand = '/usr/bin/openssl'

        self.sysemProfile = "/etc/profile"

        # java commands
        self.jre_home = '/opt/jre'
        self.cmd_java = '%s/bin/java' % self.jre_home
        self.cmd_keytool = '%s/bin/keytool' % self.jre_home
        self.cmd_jar = '%s/bin/jar' % self.jre_home

        # Component versions
        self.jre_version = '112'
        self.jetty_version = '9.3.15.v20161220'
        self.jython_version = '2.7.0'
        self.node_version = '6.9.1'
        self.apache_version = None
        self.opendj_version = None

        # Gluu components installation status
        self.installOxAuth = True
        self.installOxTrust = True
        self.installLdap = True
        self.installHttpd = True
        self.installSaml = False
        self.installAsimba = False
        self.installOxAuthRP = False
        self.installPassport = False

        self.allowPreReleasedApplications = False
        self.allowDeprecatedApplications = False

        self.jreDestinationPath = '/opt/jdk1.8.0_%s' % self.jre_version

        self.os_types = ['centos', 'redhat', 'fedora', 'ubuntu', 'debian']
        self.os_type = None
        self.os_initdaemon = None

        self.shibboleth_version = 'v3'

        self.distFolder = '/opt/dist'
        self.distAppFolder = '%s/app' % self.distFolder
        self.distGluuFolder = '%s/gluu' % self.distFolder
        self.distTmpFolder = '%s/tmp' % self.distFolder

        self.setup_properties_fn = '%s/setup.properties' % self.install_dir
        self.log = '%s/setup.log' % self.install_dir
        self.logError = '%s/setup_error.log' % self.install_dir
        self.savedProperties = '%s/setup.properties.last' % self.install_dir

        self.gluuOptFolder = '/opt/gluu'
        self.gluuOptBinFolder = '%s/bin' % self.gluuOptFolder
        self.gluuOptSystemFolder = '%s/system' % self.gluuOptFolder
        self.gluuOptPythonFolder = '%s/python' % self.gluuOptFolder
        self.gluuBaseFolder = '/etc/gluu'
        self.configFolder = '%s/conf' % self.gluuBaseFolder
        self.certFolder = '/etc/certs'

        self.oxBaseDataFolder = "/var/ox"
        self.oxPhotosFolder = "/var/ox/photos"
        self.oxTrustRemovedFolder = "/var/ox/identity/removed"
        self.oxTrustCacheRefreshFolder = "/var/ox/identity/cr-snapshots"

        self.etc_hosts = '/etc/hosts'
        self.etc_hostname = '/etc/hostname'
        # OS /etc/default folder
        self.osDefault = '/etc/default'

        self.jython_home = '/opt/jython'

        self.node_home = '/opt/node'
        self.node_initd_script = '%s/static/system/initd/node' % self.install_dir
        self.node_base = '%s/node' % self.gluuOptFolder
        self.node_user_home = '/home/node'

        self.jetty_dist = '/opt/jetty-9.3'
        self.jetty_home = '/opt/jetty'
        self.jetty_base = '%s/jetty' % self.gluuOptFolder
        self.jetty_user_home = '/home/jetty'
        self.jetty_user_home_lib = '%s/lib' % self.jetty_user_home
        self.jetty_app_configuration = {
            'oxauth' : {'name' : 'oxauth',
                        'jetty' : {'modules' : 'deploy,http,logging,jsp,servlets,ext,http-forwarded,websocket'},
                        'memory' : {'ratio' : 0.3, "jvm_heap_ration" : 0.7, "max_allowed_mb" : 4096},
                        'installed' : False
                        },
            'identity' : {'name' : 'identity',
                          'jetty' : {'modules' : 'deploy,http,logging,jsp,ext,http-forwarded,websocket'},
                          'memory' : {'ratio' : 0.2, "jvm_heap_ration" : 0.7, "max_allowed_mb" : 2048},
                          'installed' : False
                          },
            'idp' : {'name' : 'idp',
                     'jetty' : {'modules' : 'deploy,http,logging,jsp,http-forwarded'},
                     'memory' : {'ratio' : 0.2, "jvm_heap_ration" : 0.7, "max_allowed_mb" : 1024},
                     'installed' : False
                     },
            'asimba' : {'name' : 'asimba',
                        'jetty' : {'modules' : 'deploy,http,logging,jsp,http-forwarded'},
                        'memory' : {'ratio' : 0.1, "jvm_heap_ration" : 0.7, "max_allowed_mb" : 1024},
                        'installed' : False
                        },
            'oxauth-rp' : {'name' : 'oxauth-rp',
                           'jetty' : {'modules' : 'deploy,http,logging,jsp,http-forwarded,websocket'},
                           'memory' : {'ratio' : 0.1, "jvm_heap_ration" : 0.7, "max_allowed_mb" : 512},
                           'installed' : False
                           },
            'passport' : {'name' : 'passport',
                          'node' : {},
                          'memory' : {'ratio' : 0.1, "max_allowed_mb" : 1024},
                          'installed' : False
                           }
        }

        self.app_custom_changes = {
            'jetty' : {
                'name' : 'jetty',
                'files' : [{
                    'path' : '%s/etc/webdefault.xml' % self.jetty_home,
                    'replace' : [
                        {
                            'pattern' : r'(\<param-name\>dirAllowed<\/param-name\>)(\s*)(\<param-value\>)true(\<\/param-value\>)',
                            'update' : r'\1\2\3false\4'
                        }
                    ]
                },
                    {
                        'path' : '%s/etc/jetty.xml' % self.jetty_home,
                        'replace' : [
                            {
                                'pattern' : '<New id="DefaultHandler" class="org.eclipse.jetty.server.handler.DefaultHandler"/>',
                                'update' : '<New id="DefaultHandler" class="org.eclipse.jetty.server.handler.DefaultHandler">\n\t\t\t\t <Set name="showContexts">false</Set>\n\t\t\t </New>'
                            }
                        ]
                    }]
            }
        }

        self.idp3Folder = "/opt/shibboleth-idp"
        self.idp3MetadataFolder = "%s/metadata" % self.idp3Folder
        self.idp3MetadataCredentialsFolder = "%s/credentials" % self.idp3MetadataFolder
        self.idp3LogsFolder = "%s/logs" % self.idp3Folder
        self.idp3LibFolder = "%s/lib" % self.idp3Folder
        self.idp3ConfFolder = "%s/conf" % self.idp3Folder
        self.idp3ConfAuthnFolder = "%s/conf/authn" % self.idp3Folder
        self.idp3CredentialsFolder = "%s/credentials" % self.idp3Folder
        self.idp3WebappFolder = "%s/webapp" % self.idp3Folder
        # self.idp3WarFolder = "%s/war"

        self.hostname = None
        self.ip = None
        self.orgName = None
        self.orgShortName = None
        self.countryCode = None
        self.city = None
        self.state = None
        self.admin_email = None
        self.encoded_ox_ldap_pw = None
        self.encoded_ldap_pw = None
        self.encoded_shib_jks_pw = None
        self.application_max_ram = None    # in MB
        self.encode_salt = None

        self.baseInum = None
        self.inumOrg = None
        self.inumAppliance = None
        self.inumOrgFN = None
        self.inumApplianceFN = None
        self.ldapBaseFolderldapPass = None
        self.oxauth_client_id = None
        self.oxauthClient_pw = None
        self.oxauthClient_encoded_pw = None
        self.oxTrustConfigGeneration = None

        self.oxd_hostname = '%(oxd_hostname)s'
        self.oxd_port = '%(oxd_port)s'

        self.outputFolder = '%s/output' % self.install_dir
        self.templateFolder = '%s/templates' % self.install_dir
        self.staticFolder = '%s/static' % self.install_dir

        self.extensionFolder = '%s/extension' % self.staticFolder

        self.oxauth_error_json = '%s/oxauth/oxauth-errors.json' % self.staticFolder

        self.oxauth_openid_jwks_fn = "%s/oxauth-keys.json" % self.certFolder
        self.oxauth_openid_jks_fn = "%s/oxauth-keys.jks" % self.certFolder
        self.oxauth_openid_jks_pass = None

        self.httpdKeyPass = None
        self.httpdKeyFn = '%s/httpd.key' % self.certFolder
        self.httpdCertFn = '%s/httpd.crt' % self.certFolder
        self.shibJksPass = None
        self.shibJksFn = '%s/shibIDP.jks' % self.certFolder
        self.asimbaJksPass = None
        self.asimbaJksFn = '%s/asimbaIDP.jks' % self.certFolder

        self.ldapTrustStoreFn = None
        self.encoded_ldapTrustStorePass = None

        self.opendj_cert_fn = '%s/opendj.crt' % self.certFolder
        self.opendj_p12_fn = '%s/opendj.pkcs12' % self.certFolder
        self.opendj_p12_pass = None

        self.ldap_type = 'opendj'
        self.opendj_ldap_binddn = 'cn=directory manager'
        self.ldap_hostname = "localhost"
        self.ldap_port = '1389'
        self.ldaps_port = '1636'
        self.ldap_jmx_port = '1689'
        self.ldap_admin_port = '4444'
        self.ldapBaseFolder = '/opt/opendj'

        self.ldapSetupCommand = '%s/setup' % self.ldapBaseFolder
        self.ldapDsconfigCommand = "%s/bin/dsconfig" % self.ldapBaseFolder
        self.ldapDsCreateRcCommand = "%s/bin/create-rc-script" % self.ldapBaseFolder
        self.ldapDsJavaPropCommand = "%s/bin/dsjavaproperties" % self.ldapBaseFolder
        
        self.ldap_user_home = '/home/ldap'
        self.ldapPassFn = '%s/.pw' % self.ldap_user_home
        self.ldap_backend_type = 'je'
        self.importLdifCommand = '%s/bin/import-ldif' % self.ldapBaseFolder
        self.ldapModifyCommand = '%s/bin/ldapmodify' % self.ldapBaseFolder
        self.loadLdifCommand = self.ldapModifyCommand
        self.gluuScriptFiles = ['%s/static/scripts/logmanager.sh' % self.install_dir,
                                '%s/static/scripts/testBind.py' % self.install_dir]

        self.openDjIndexJson = '%s/static/opendj/index.json' % self.install_dir
        self.openDjSchemaFolder = "%s/config/schema" % self.ldapBaseFolder
        
        
        self.openDjschemaFiles = ["%s/static/opendj/96-eduperson.ldif" % self.install_dir,
                            "%s/static/opendj/101-ox.ldif" % self.install_dir,
                            "%s/static/opendj/77-customAttributes.ldif" % self.install_dir]

        if os.path.exists(os.path.join(self.install_dir, 'static/opendj/deprecated')):
            self.openDjschemaFiles = glob.glob(os.path.join(self.install_dir, 'static/opendj/deprecated/*.ldif'))
        
        self.opendj_init_file = '%s/static/opendj/opendj' % self.install_dir
        self.opendj_service_centos7 = '%s/static/opendj/systemd/opendj.service' % self.install_dir

        self.redhat_services = ['httpd', 'rsyslog']
        self.debian_services = ['apache2', 'rsyslog']

        self.apache_start_script = '/etc/init.d/httpd'

        self.defaultTrustStoreFN = '%s/jre/lib/security/cacerts' % self.jre_home
        self.defaultTrustStorePW = 'changeit'

        self.openldapBaseFolder = '/opt/symas'
        self.openldapBinFolder = '/opt/symas/bin'
        self.openldapConfFolder = '/opt/symas/etc/openldap'
        self.openldapRootUser = "cn=directory manager,o=gluu"
        self.openldapSiteUser = "cn=directory manager,o=site"
        self.openldapKeyPass = None
        self.openldapTLSCACert = '%s/openldap.pem' % self.certFolder
        self.openldapTLSCert = '%s/openldap.crt' % self.certFolder
        self.openldapTLSKey = '%s/openldap.key' % self.certFolder
        self.openldapJksPass = None
        self.openldapJksFn = '%s/openldap.jks' % self.certFolder
        self.openldapP12Fn = '%s/openldap.pkcs12' % self.certFolder

        self.passportSpKeyPass = None
        self.passportSpTLSCACert = '%s/passport-sp.pem' % self.certFolder
        self.passportSpTLSCert = '%s/passport-sp.crt' % self.certFolder
        self.passportSpTLSKey = '%s/passport-sp.key' % self.certFolder
        self.passportSpJksPass = None
        self.passportSpJksFn = '%s/passport-sp.jks' % self.certFolder

        self.openldapSlapdConf = '%s/slapd.conf' % self.outputFolder
        self.openldapSymasConf = '%s/symas-openldap.conf' % self.outputFolder
        self.openldapRootSchemaFolder = "%s/schema" % self.gluuOptFolder
        self.openldapSchemaFolder = "%s/openldap" % self.openldapRootSchemaFolder
        self.openldapLogDir = "/var/log/openldap/"
        self.openldapSyslogConf = "%s/static/openldap/openldap-syslog.conf" % self.install_dir
        self.openldapLogrotate = "%s/static/openldap/openldap_logrotate" % self.install_dir
        self.openldapSetupAccessLog = False
        self.accessLogConfFile = "%s/static/openldap/accesslog.conf" % self.install_dir
        self.gluuAccessLogConf = "%s/static/openldap/o_gluu_accesslog.conf" % self.install_dir
        self.opendlapIndexDef = "%s/static/openldap/index.json" % self.install_dir

        # Stuff that gets rendered; filename is necessary. Full path should
        # reflect final path if the file must be copied after its rendered.
        self.passport_saml_config = '%s/passport-saml-config.json' % self.configFolder
        self.oxauth_config_json = '%s/oxauth-config.json' % self.outputFolder
        self.oxtrust_config_json = '%s/oxtrust-config.json' % self.outputFolder
        self.oxtrust_cache_refresh_json = '%s/oxtrust-cache-refresh.json' % self.outputFolder
        self.oxtrust_import_person_json = '%s/oxtrust-import-person.json' % self.outputFolder
        self.oxidp_config_json = '%s/oxidp-config.json' % self.outputFolder
        self.oxasimba_config_json = '%s/oxasimba-config.json' % self.outputFolder
        self.gluu_python_base = '%s/python' % self.gluuOptFolder
        self.gluu_python_readme = '%s/libs/python.txt' % self.gluuOptPythonFolder
        self.ox_ldap_properties = '%s/ox-ldap.properties' % self.configFolder
        self.oxauth_static_conf_json = '%s/oxauth-static-conf.json' % self.outputFolder
        self.oxTrust_log_rotation_configuration = "%s/conf/oxTrustLogRotationConfiguration.xml" % self.gluuBaseFolder
        self.apache2_conf = '%s/httpd.conf' % self.outputFolder
        self.apache2_ssl_conf = '%s/https_gluu.conf' % self.outputFolder
        self.apache2_24_conf = '%s/httpd_2.4.conf' % self.outputFolder
        self.apache2_ssl_24_conf = '%s/https_gluu.conf' % self.outputFolder
        self.ldif_base = '%s/base.ldif' % self.outputFolder
        self.ldif_appliance = '%s/appliance.ldif' % self.outputFolder
        self.ldif_attributes = '%s/attributes.ldif' % self.outputFolder
        self.ldif_scopes = '%s/scopes.ldif' % self.outputFolder
        self.ldif_clients = '%s/clients.ldif' % self.outputFolder
        self.ldif_people = '%s/people.ldif' % self.outputFolder
        self.ldif_groups = '%s/groups.ldif' % self.outputFolder
        self.ldif_site = '%s/static/cache-refresh/o_site.ldif' % self.install_dir
        self.ldif_scripts = '%s/scripts.ldif' % self.outputFolder
        self.ldif_configuration = '%s/configuration.ldif' % self.outputFolder
        self.ldif_scim = '%s/scim.ldif' % self.outputFolder
        self.ldif_asimba = '%s/asimba.ldif' % self.outputFolder
        self.ldif_passport = '%s/passport.ldif' % self.outputFolder
        self.ldif_idp = '%s/oxidp.ldif' % self.outputFolder
        self.ldif_scripts_cred_manager = '%s/scripts_cred_manager.ldif' % self.outputFolder
        self.passport_config = '%s/passport-config.json' % self.configFolder
        self.encode_script = '%s/bin/encode.py' % self.gluuOptFolder
        self.network = "/etc/sysconfig/network"
        self.system_profile_update = '%s/system_profile' % self.outputFolder

        self.asimba_conf_folder = '%s/asimba' % self.configFolder
        self.asimba_configuration_xml = '%s/asimba.xml' % self.asimba_conf_folder
        self.asimba_configuration = '%s/asimba.xml' % self.outputFolder
        self.asimba_selector_configuration = '%s/asimba-selector.xml' % self.outputFolder
        self.asimba_properties = '%s/asimba.properties' % self.outputFolder
        self.asimba_selector_configuration_xml = '%s/asimba-selector.xml' % self.asimba_conf_folder

        self.staticIDP3FolderConf = '%s/static/idp3/conf' % self.install_dir
        self.staticIDP3FolderMetadata = '%s/static/idp3/metadata' % self.install_dir
        self.idp3_configuration_properties = 'idp.properties'
        self.idp3_configuration_ldap_properties = 'ldap.properties'
        self.idp3_configuration_saml_nameid = 'saml-nameid.properties'
        self.idp3_configuration_services = 'services.properties'
        self.idp3_configuration_password_authn = 'authn/password-authn-config.xml'
        self.idp3_metadata = 'idp-metadata.xml'

        self.cred_manager_config = '%s/cred-manager.json' % self.outputFolder

        ### rsyslog file customised for init.d
        self.rsyslogUbuntuInitFile = "%s/static/system/ubuntu/rsyslog" % self.install_dir

        self.ldap_setup_properties = '%s/opendj-setup.properties' % self.templateFolder

        # oxAuth/oxTrust Base64 configuration files
        self.pairwiseCalculationKey = None
        self.pairwiseCalculationSalt = None

        # OpenID key generation default setting
        self.default_openid_jks_dn_name = 'CN=oxAuth CA Certificates'
        self.default_key_algs = 'RS256 RS384 RS512 ES256 ES384 ES512'
        self.default_key_expiration = 365

        # oxTrust SCIM configuration
        self.scim_rs_client_id = None
        self.scim_rs_client_jwks = None
        self.scim_rs_client_jks_fn = "%s/scim-rs.jks" % self.certFolder
        self.scim_rs_client_jks_pass = None
        self.scim_rs_client_jks_pass_encoded = None

        self.scim_rp_client_id = None
        self.scim_rp_client_jwks = None
        self.scim_rp_client_jks_fn = "%s/scim-rp.jks" % self.outputFolder
        self.scim_rp_client_jks_pass = 'secret'

        # oxPassport Configuration
        self.gluu_passport_base = '%s/passport' % self.node_base
        self.ldif_passport_config = '%s/oxpassport-config.ldif' % self.outputFolder

        self.passport_rs_client_id = None
        self.passport_rs_client_jwks = None
        self.passport_rs_client_jks_fn = "%s/passport-rs.jks" % self.certFolder
        self.passport_rs_client_jks_pass = None
        self.passport_rs_client_jks_pass_encoded = None

        self.passport_rp_client_id = None
        self.passport_rp_client_jwks = None
        self.passport_rp_client_jks_fn = "%s/passport-rp.jks" % self.certFolder
        self.passport_rp_client_cert_alg = "RS512"
        self.passport_rp_client_cert_alias = None
        self.passport_rp_client_cert_fn = "%s/passport-rp.pem" % self.certFolder
        self.passport_rp_client_jks_pass = 'secret'

        self.ldif_files = [self.ldif_base,
                           self.ldif_appliance,
                           self.ldif_attributes,
                           self.ldif_scopes,
                           self.ldif_clients,
                           self.ldif_people,
                           self.ldif_groups,
                           self.ldif_site,
                           self.ldif_scripts,
                           self.ldif_configuration,
                           self.ldif_scim,
                           self.ldif_asimba,
                           self.ldif_passport,
                           self.ldif_passport_config,
                           self.ldif_idp
                           ]

        self.ce_templates = {self.oxauth_config_json: False,
                             self.passport_saml_config:True,
                             self.gluu_python_readme: True,
                             self.oxtrust_config_json: False,
                             self.oxtrust_cache_refresh_json: False,
                             self.oxtrust_import_person_json: False,
                             self.oxidp_config_json: False,
                             self.oxasimba_config_json: False,
                             self.ox_ldap_properties: True,
                             self.oxauth_static_conf_json: False,
                             self.oxTrust_log_rotation_configuration: True,
                             self.ldap_setup_properties: False,
                             self.apache2_conf: False,
                             self.apache2_ssl_conf: False,
                             self.apache2_24_conf: False,
                             self.apache2_ssl_24_conf: False,
                             self.etc_hosts: False,
                             self.etc_hostname: False,
                             self.ldif_base: False,
                             self.ldif_appliance: False,
                             self.ldif_attributes: False,
                             self.ldif_scopes: False,
                             self.ldif_clients: False,
                             self.ldif_people: False,
                             self.ldif_groups: False,
                             self.ldif_scripts: False,
                             self.ldif_scim: False,
                             self.ldif_asimba: False,
                             self.ldif_passport: False,
                             self.ldif_passport_config: False,
                             self.ldif_idp: False,
                             self.asimba_configuration: False,
                             self.asimba_properties: False,
                             self.asimba_selector_configuration: False,
                             self.network: False,
                             self.cred_manager_config:False,
                             self.ldif_scripts_cred_manager: False,
                             }

        self.oxauth_keys_utils_libs = [ 'bcprov-jdk15on-*.jar', 'bcpkix-jdk15on-*.jar', 'commons-lang-*.jar',
                                        'log4j-*.jar', 'commons-codec-*.jar', 'commons-cli-*.jar', 'commons-io-*.jar',
                                        'jackson-core-*.jar', 'jackson-core-asl-*.jar', 'jackson-mapper-asl-*.jar', 'jackson-xc-*.jar',
                                        'jettison-*.jar', 'oxauth-model-*.jar', 'oxauth-client-*.jar' ]




    def get_ip(self):
        testIP = None
        detectedIP = None
        try:
            testSocket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            detectedIP = [(testSocket.connect(('8.8.8.8', 80)),
                           testSocket.getsockname()[0],
                           testSocket.close()) for s in [socket.socket(socket.AF_INET, socket.SOCK_DGRAM)]][0][1]
        except:
            self.logIt("No detected IP address", True)
            self.logIt(traceback.format_exc(), True)

        while not testIP:
            if detectedIP:
                testIP = self.getPrompt("Enter IP Address", detectedIP)
            else:
                testIP = self.getPrompt("Enter IP Address")
            if not self.isIP(testIP):
                testIP = None
                print 'ERROR: The IP Address is invalid. Try again\n'

        return testIP

    def check_properties(self):
        self.logIt('Checking properties')
        while not self.hostname:
            testhost = raw_input('Hostname of this server: ').strip()
            if len(testhost.split('.')) >= 3:
                self.hostname = testhost
            else:
                print 'The hostname has to be at least three domain components. Try again\n'
        while not self.ip:
            self.ip = self.get_ip()
        while not self.orgName:
            self.orgName = raw_input('Organization Name: ').strip()
        while not self.countryCode:
            testCode = raw_input('2 Character Country Code: ').strip()
            if len(testCode) == 2:
                self.countryCode = testCode
            else:
                print 'Country code should only be two characters. Try again\n'
        while not self.city:
            self.city = raw_input('City: ').strip()
        while not self.state:
            self.state = raw_input('State or Province: ').strip()
        if not self.admin_email:
            tld = None
            try:
                tld = ".".join(self.hostname.split(".")[-2:])
            except:
                tld = self.hostname
            self.admin_email = "support@%s" % tld
        if not self.httpdKeyPass:
            self.httpdKeyPass = self.getPW()
        if not self.ldapPass:
            self.ldapPass = self.getPW()
        if not self.shibJksPass:
            self.shibJksPass = self.getPW()
        if not self.oxauth_openid_jks_pass:
            self.oxauth_openid_jks_pass = self.getPW()
        if not self.asimbaJksPass:
            self.asimbaJksPass = self.getPW()
        if not self.openldapKeyPass:
            self.openldapKeyPass = self.getPW()
        if not self.openldapJksPass:
            self.openldapJksPass = self.getPW()
        if not self.opendj_p12_pass:
            self.opendj_p12_pass = self.getPW()
        if not self.passportSpKeyPass:
            self.passportSpKeyPass = self.getPW()
            self.passportSpJksPass = self.getPW()
        if not self.encode_salt:
            self.encode_salt= self.getPW() + self.getPW()
        if not self.baseInum:
            self.baseInum = '@!%s.%s.%s.%s' % tuple([self.getQuad() for i in xrange(4)])
        if not self.inumOrg:
            orgTwoQuads = '%s.%s' % tuple([self.getQuad() for i in xrange(2)])
            self.inumOrg = '%s!0001!%s' % (self.baseInum, orgTwoQuads)
        if not self.inumAppliance:
            applianceTwoQuads = '%s.%s' % tuple([self.getQuad() for i in xrange(2)])
            self.inumAppliance = '%s!0002!%s' % (self.baseInum, applianceTwoQuads)
        if not self.oxauth_client_id:
            clientTwoQuads = '%s.%s' % tuple([self.getQuad() for i in xrange(2)])
            self.oxauth_client_id = '%s!0008!%s' % (self.inumOrg, clientTwoQuads)
        if not self.scim_rs_client_id:
            scimClientTwoQuads = '%s.%s' % tuple([self.getQuad() for i in xrange(2)])
            self.scim_rs_client_id = '%s!0008!%s' % (self.inumOrg, scimClientTwoQuads)
        if not self.scim_rp_client_id:
            scimClientTwoQuads = '%s.%s' % tuple([self.getQuad() for i in xrange(2)])
            self.scim_rp_client_id = '%s!0008!%s' % (self.inumOrg, scimClientTwoQuads)
        if not self.passport_rs_client_id:
            passportClientTwoQuads = '%s.%s' % tuple([self.getQuad() for i in xrange(2)])
            self.passport_rs_client_id = '%s!0008!%s' % (self.inumOrg, passportClientTwoQuads)
        if not self.passport_rp_client_id:
            passportClientTwoQuads = '%s.%s' % tuple([self.getQuad() for i in xrange(2)])
            self.passport_rp_client_id = '%s!0008!%s' % (self.inumOrg, passportClientTwoQuads)
        if not self.inumApplianceFN:
            self.inumApplianceFN = self.inumAppliance.replace('@', '').replace('!', '').replace('.', '')
        if not self.inumOrgFN:
            self.inumOrgFN = self.inumOrg.replace('@', '').replace('!', '').replace('.', '')
        if not self.application_max_ram:
            self.application_max_ram = 3072

    def choose_from_list(self, list_of_choices, choice_name="item", default_choice_index=0):
        return_value = None
        choice_map = {}
        chosen_index = 0
        print "\nSelect the number for the %s from the following list:" % choice_name
        for choice in list_of_choices:
            choice_map[chosen_index] = choice
            chosen_index += 1
            print "  [%i]   %s" % (chosen_index, choice)
        while not return_value:
            choice_number = self.getPrompt("Please select a number listed above", str(default_choice_index + 1))
            try:
                choice_number = int(choice_number) - 1
                if (choice_number >= 0) & (choice_number < len(list_of_choices)):
                    return_value = choice_map[choice_number]
                else:
                    print '"%i" is not a valid choice' % (choice_number + 1)
            except:
                print 'Cannot convert "%s" to a number' % choice_number
                self.logIt(traceback.format_exc(), True)
        return return_value

    # = File system  =================================================================
    def findFiles(self, filePatterns, filesFolder):
        foundFiles = []
        try:
            for filePattern in filePatterns:
                fileFullPathPattern = "%s/%s" % (filesFolder, filePattern)
                for fileFullPath in glob.iglob(fileFullPathPattern):
                    foundFiles.append(fileFullPath)
        except:
            self.logIt("Error finding files %s in folder %s" % (":".join(filePatterns), filesFolder), True)
            self.logIt(traceback.format_exc(), True)

        return foundFiles

    def readFile(self, inFilePath):
        inFilePathText = None

        try:
            f = open(inFilePath)
            inFilePathText = f.read()
            f.close
        except:
            self.logIt("Error reading %s" % inFilePathText, True)
            self.logIt(traceback.format_exc(), True)

        return inFilePathText

    def writeFile(self, outFilePath, text):
        inFilePathText = None

        try:
            f = open(outFilePath, 'w')
            f.write(text)
            f.close()
        except:
            self.logIt("Error writing %s" % inFilePathText, True)
            self.logIt(traceback.format_exc(), True)

        return inFilePathText

    def commentOutText(self, text):
        textLines = text.split('\n')

        lines = []
        for textLine in textLines:
            lines.append('#%s' % textLine)

        return "\n".join(lines)

    def replaceInText(self, text, pattern, update):
        rePattern = re.compile(pattern,  flags=re.DOTALL | re.M)
        return rePattern.sub(update, text)

    def applyChangesInFiles(self, changes):
        self.logIt("Applying changes to %s files..." % changes['name'])
        for change in changes['files']:
            file = change['path']

            text = self.readFile(file)
            file_backup = '%s.bak' % file
            self.writeFile(file_backup, text)
            self.logIt("Created backup of %s file %s..." % (changes['name'], file_backup))

            for replace in change['replace']:
                text = self.replaceInText(text, replace['pattern'], replace['update'])

            self.writeFile(file, text)
            self.logIt("Wrote updated %s file %s..." % (changes['name'], file))

    def copyFile(self, inFile, destFolder):
        try:
            shutil.copy(inFile, destFolder)
            self.logIt("Copied %s to %s" % (inFile, destFolder))
        except:
            self.logIt("Error copying %s to %s" % (inFile, destFolder), True)
            self.logIt(traceback.format_exc(), True)

    def copyTree(self, src, dst, overwrite=False):
        try:
            if not os.path.exists(dst):
                os.makedirs(dst)

            for item in os.listdir(src):
                s = os.path.join(src, item)
                d = os.path.join(dst, item)
                if os.path.isdir(s):
                    self.copyTree(s, d, overwrite)
                else:
                    if overwrite and os.path.exists(d):
                        self.removeFile(d)

                    if not os.path.exists(d) or os.stat(s).st_mtime - os.stat(d).st_mtime > 1:
                        shutil.copy2(s, d)

            self.logIt("Copied tree %s to %s" % (src, dst))
        except:
            self.logIt("Error copying tree %s to %s" % (src, dst), True)
            self.logIt(traceback.format_exc(), True)

    def createDirs(self, name):
        try:
            if not os.path.exists(name):
                os.makedirs(name, 0700)
                self.logIt('Created dir: %s' % name)
        except:
            self.logIt("Error making directory %s" % name, True)
            self.logIt(traceback.format_exc(), True)

    def removeDirs(self, name):
        try:
            if os.path.exists(name):
                shutil.rmtree(name)
                self.logIt('Removed dir: %s' % name)
        except:
            self.logIt("Error removing directory %s" % name, True)
            self.logIt(traceback.format_exc(), True)

    def removeFile(self, fileName):
        try:
            if os.path.exists(fileName):
                os.remove(fileName)
                self.logIt('Removed file: %s' % fileName)
        except:
            self.logIt("Error removing file %s" % fileName, True)
            self.logIt(traceback.format_exc(), True)

    # = Utilities ====================================================================

    def logIt(self, msg, errorLog=False):
        if errorLog:
            f = open(self.logError, 'a')
            f.write('%s %s\n' % (time.strftime('%X %x'), msg))
            f.close()
        f = open(self.log, 'a')
        f.write('%s %s\n' % (time.strftime('%X %x'), msg))
        f.close()

    def appendLine(self, line, fileName=False):
        try:
            f = open(fileName, 'a')
            f.write('%s\n' % line)
            f.close()
        except:
            self.logIt("Error loading file %s" % fileName)

    def set_ulimits(self):
        try:
            if self.os_type in ['centos', 'redhat', 'fedora']:
                apache_user = 'apache'
            else:
                apache_user = 'www-data'

            self.appendLine("ldap       soft nofile     131072", "/etc/security/limits.conf")
            self.appendLine("ldap       hard nofile     262144", "/etc/security/limits.conf")
            self.appendLine("%s     soft nofile     131072" % apache_user, "/etc/security/limits.conf")
            self.appendLine("%s     hard nofile     262144" % apache_user, "/etc/security/limits.conf")
            self.appendLine("jetty      soft nofile     131072", "/etc/security/limits.conf")
            self.appendLine("jetty      hard nofile     262144", "/etc/security/limits.conf")
        except:
            self.logIt("Could not set limits.")
            self.logIt(traceback.format_exc(), True)

    def load_properties(self, fn):
        self.logIt('Loading Properties %s' % fn)
        p = Properties.Properties()
        try:
            p.load(open(fn))
            properties_list = p.keys()
            for prop in properties_list:
                try:
                    self.__dict__[prop] = p[prop]
                    if p[prop] == 'True':
                        self.__dict__[prop] = True
                    elif p[prop] == 'False':
                        self.__dict__[prop] = False
                except:
                    self.logIt("Error loading property %s" % prop)
                    self.logIt(traceback.format_exc(), True)
        except:
            self.logIt("Error loading properties", True)
            self.logIt(traceback.format_exc(), True)

    def load_json(self, fn):
        self.logIt('Loading JSON from %s' % fn)
        try:
            json_file = open(fn)
            json_text = json_file.read()
            json_file.close()
            return json.loads(json_text)
        except:
            self.logIt("Unable to read or parse json file from %s" % fn, True)
            self.logIt(traceback.format_exc(), True)
        return None

    def obscure(self, data=""):
        engine = triple_des(self.encode_salt, ECB, pad=None, padmode=PAD_PKCS5)
        data = data.encode('ascii')
        en_data = engine.encrypt(data)
        return base64.b64encode(en_data)


    def copy_output(self):
        self.logIt("Copying rendered templates to final destination")

        for dest_fn in self.ce_templates.keys():
            if self.ce_templates[dest_fn]:
                fn = os.path.split(dest_fn)[-1]
                output_fn = os.path.join(self.outputFolder, fn)
                try:
                    self.logIt("Copying %s to %s" % (output_fn, dest_fn))
                    dest_dir = os.path.dirname(dest_fn)
                    if not os.path.exists(dest_dir):
                        self.logIt("Created destination folder %s" % dest_dir)
                        os.makedirs(dest_dir)
                    shutil.copyfile(output_fn, dest_fn)
                except:
                    self.logIt("Error writing %s to %s" % (output_fn, dest_fn), True)
                    self.logIt(traceback.format_exc(), True)


    def detect_os_type(self):
        # TODO: Change this to support more distros. For example according to
        # http://unix.stackexchange.com/questions/6345/how-can-i-get-distribution-name-and-version-number-in-a-simple-shell-script
        distro_info = self.readFile('/etc/redhat-release')
        if distro_info == None:
            distro_info = self.readFile('/etc/os-release')

        if 'CentOS' in distro_info:
            return self.os_types[0]
        elif 'Red Hat' in distro_info:
            return self.os_types[1]
        elif 'Ubuntu' in distro_info:
            return self.os_types[3]
        elif 'Debian' in distro_info:
            return self.os_types[4]

        else:
            return self.choose_from_list(self.os_types, "Operating System")

    def detect_initd(self):
        return open(os.path.join('/proc/1/status'), 'r').read().split()[1]


    def determineOpenDJVersion(self):
        f = open('/opt/opendj/template/config/buildinfo', 'r')
        encode_script = f.read().split()[0]
        f.close()

        if re.match(r'2\.6\.0\..*', encode_script):
            return "2.6"

        return "3.0"

    def getPW(self, size=12, chars=string.ascii_uppercase + string.digits + string.lowercase):
        return ''.join(random.choice(chars) for _ in range(size))

    def ldap_encode(self, password):
        salt = os.urandom(4)
        sha = hashlib.sha1(password)
        sha.update(salt)
        b64encoded = '{0}{1}'.format(sha.digest(), salt).encode('base64').strip()
        encrypted_password = '{{SSHA}}{0}'.format(b64encoded)
        return encrypted_password

    def encode_passwords(self):
        self.logIt("Encoding passwords")
        try:
            self.encoded_ldap_pw = self.ldap_encode(self.ldapPass)
            self.encoded_shib_jks_pw = self.obscure(self.shibJksPass)
            self.encoded_ox_ldap_pw = self.obscure(self.ldapPass)
            self.encoded_openldapJksPass = self.obscure(self.openldapJksPass)
            self.encoded_opendj_p12_pass = self.obscure(self.opendj_p12_pass)
            self.oxauthClient_pw = self.getPW()
            self.oxauthClient_encoded_pw = self.obscure(self.oxauthClient_pw)
        except:
            self.logIt("Error encoding passwords", True)
            self.logIt(traceback.format_exc(), True)

    def encode_test_passwords(self):
        self.logIt("Encoding test passwords")
        try:
            self.templateRenderingDict['oxauthClient_2_pw'] = self.getPW()
            self.templateRenderingDict['oxauthClient_2_encoded_pw'] = self.obscure(self.templateRenderingDict['oxauthClient_2_pw'])

            self.templateRenderingDict['oxauthClient_3_pw'] = self.getPW()
            self.templateRenderingDict['oxauthClient_3_encoded_pw'] = self.obscure(self.templateRenderingDict['oxauthClient_3_pw'])

            self.templateRenderingDict['oxauthClient_4_pw'] = self.getPW()
            self.templateRenderingDict['oxauthClient_4_encoded_pw'] = self.obscure(self.templateRenderingDict['oxauthClient_4_pw'])
        except:
            self.logIt("Error encoding test passwords", True)
            self.logIt(traceback.format_exc(), True)


    def fomatWithDict(self, text, dictionary):
        text = re.sub(r"%([^\(])", r"%%\1", text)
        text = re.sub(r"%$", r"%%", text)  # There was a % at the end?

        return text % dictionary

    def renderTemplateInOut(self, filePath, templateFolder, outputFolder):
        self.logIt("Rendering template %s" % filePath)
        fn = os.path.split(filePath)[-1]
        f = open(os.path.join(templateFolder, fn))
        template_text = f.read()
        f.close()

        # Create output folder if needed
        if not os.path.exists(outputFolder):
            os.makedirs(outputFolder)

        newFn = open(os.path.join(outputFolder, fn), 'w+')
        newFn.write(self.fomatWithDict(template_text, self.merge_dicts(self.__dict__, self.templateRenderingDict)))
        newFn.close()

    def renderTemplate(self, filePath):
        self.renderTemplateInOut(filePath, self.templateFolder, self.outputFolder)

    def render_templates(self):
        self.logIt("Rendering templates")
        for fullPath in self.ce_templates.keys():
            try:
                self.renderTemplate(fullPath)
            except:
                self.logIt("Error writing template %s" % fullPath, True)
                self.logIt(traceback.format_exc(), True)

    def render_custom_templates(self, fullPath):
        output_dir = fullPath + '.output'

        self.logIt("Rendering custom templates")
        self.logIt("Rendering custom templates from %s to %s" % (fullPath, output_dir))

        try:
            self.run([self.cmd_mkdir, '-p', output_dir])
        except:
            self.logIt("Error creating output directory %s" % output_dir, True)
            self.logIt(traceback.format_exc(), True)

        try:
            for filename in self.get_filepaths(fullPath):
                self.renderTemplateInOut(filename, fullPath, output_dir)
        except:
            self.logIt("Error writing template %s" % fullPath, True)
            self.logIt(traceback.format_exc(), True)

    def render_configuration_template(self):
        self.logIt("Rendering configuration templates")

        fullPath = self.ldif_configuration
        try:
            self.renderTemplate(fullPath)
        except:
            self.logIt("Error writing template %s" % fullPath, True)
            self.logIt(traceback.format_exc(), True)

    def render_templates_folder(self, templatesFolder):
        self.logIt("Rendering templates folder: %s" % templatesFolder)

        for templateBase, templateDirectories, templateFiles in os.walk(templatesFolder):
            for templateFile in templateFiles:
                fullPath = '%s/%s' % (templateBase, templateFile)
                try:
                    self.logIt("Rendering test template %s" % fullPath)
                    # Remove ./template/ and everything left of it from fullPath
                    fn = re.match(r'(^.+/templates/)(.*$)', fullPath).groups()[1]
                    f = open(os.path.join(self.templateFolder, fn))
                    template_text = f.read()
                    f.close()

                    fullOutputFile = os.path.join(self.outputFolder, fn)
                    # Create full path to the output file
                    fullOutputDir = os.path.dirname(fullOutputFile)
                    if not os.path.exists(fullOutputDir):
                        os.makedirs(fullOutputDir)

                    newFn = open(fullOutputFile, 'w+')
                    newFn.write(template_text % self.merge_dicts(self.__dict__, self.templateRenderingDict))
                    newFn.close()
                except:
                    self.logIt("Error writing template %s" % fullPath, True)
                    self.logIt(traceback.format_exc(), True)

    def render_test_templates(self):
        self.logIt("Rendering test templates")

        testTepmplatesFolder = '%s/test/' % self.templateFolder
        self.render_templates_folder(testTepmplatesFolder)


    # args = command + args, i.e. ['ls', '-ltr']
    def run(self, args, cwd=None, env=None, useWait=False, shell=False):
        print "Run:", args
        self.logIt('Running: %s' % ' '.join(args))
        try:
            p = subprocess.Popen(args, stdout=subprocess.PIPE, stderr=subprocess.PIPE, cwd=cwd, env=env, shell=shell)
            if useWait:
                code = p.wait()
                self.logIt('Run: %s with result code: %d' % (' '.join(args), code) )
            else:
                output, err = p.communicate()
                if output:
                    self.logIt(output)
                if err:
                    self.logIt(err, True)
        except:
            self.logIt("Error running command : %s" % " ".join(args), True)
            self.logIt(traceback.format_exc(), True)


    def createLdapPw(self):

        f = open(self.ldapPassFn, 'w')
        f.write(self.ldapPass)
        f.close()
        self.run([self.cmd_chown, 'ldap:ldap', self.ldapPassFn])


    def deleteLdapPw(self):

        if os.path.exists(self.ldapPassFn):
            os.remove(self.ldapPassFn)
        if os.path.exists(os.path.join(self.ldapBaseFolder, 'opendj-setup.properties')):
            os.remove(os.path.join(self.ldapBaseFolder, 'opendj-setup.properties'))

    def configure_opendj(self):
        self.logIt("Configuring OpenDJ")

        opendj_prop_name = 'global-aci:\'(targetattr!="userPassword||authPassword||debugsearchindex||changes||changeNumber||changeType||changeTime||targetDN||newRDN||newSuperior||deleteOldRDN")(version 3.0; acl "Anonymous read access"; allow (read,search,compare) userdn="ldap:///anyone";)\''
        config_changes = [['set-global-configuration-prop', '--set', 'single-structural-objectclass-behavior:accept'],
                          ['set-attribute-syntax-prop', '--syntax-name', '"Directory String"',   '--set', 'allow-zero-length-values:true'],
                          ['set-password-policy-prop', '--policy-name', '"Default Password Policy"', '--set', 'allow-pre-encoded-passwords:true'],
                          ['set-log-publisher-prop', '--publisher-name', '"File-Based Audit Logger"', '--set', 'enabled:true'],
                          ['create-backend', '--backend-name', 'site', '--set', 'base-dn:o=site', '--type %s' % self.ldap_backend_type, '--set', 'enabled:true'],
                          ['set-connection-handler-prop', '--handler-name', '"LDAP Connection Handler"', '--set', 'enabled:false'],
                          ['set-access-control-handler-prop', '--remove', '%s' % opendj_prop_name],
                          ['set-global-configuration-prop', '--set', 'reject-unauthenticated-requests:true'],
                          ['set-password-policy-prop', '--policy-name', '"Default Password Policy"', '--set', 'default-password-storage-scheme:"Salted SHA-512"'],
                          ['set-global-configuration-prop', '--set', 'reject-unauthenticated-requests:true']
                          ]
        for changes in config_changes:
            dsconfigCmd = " ".join(['cd %s/bin ; ' % self.ldapBaseFolder,
                                    self.ldapDsconfigCommand,
                                    '--trustAll',
                                    '--no-prompt',
                                    '--hostname',
                                    self.ldap_hostname,
                                    '--port',
                                    self.ldap_admin_port,
                                    '--bindDN',
                                    '"%s"' % self.ldap_binddn,
                                    '--bindPasswordFile',
                                    self.ldapPassFn] + changes)
            self.run(['/bin/su',
                      'ldap',
                      '-c',
                      dsconfigCmd])

    def export_opendj_public_cert(self):
        # Load password to acces OpenDJ truststore
        self.logIt("Reading OpenDJ truststore")

        openDjPinFn = '%s/config/keystore.pin' % self.ldapBaseFolder
        openDjTruststoreFn = '%s/config/truststore' % self.ldapBaseFolder

        openDjPin = None
        try:
            f = open(openDjPinFn)
            openDjPin = f.read().splitlines()[0]
            f.close()
        except:
            self.logIt("Error reding OpenDJ truststore", True)
            self.logIt(traceback.format_exc(), True)

        # Export public OpenDJ certificate
        self.logIt("Exporting OpenDJ certificate")
        self.run([self.cmd_keytool,
                  '-exportcert',
                  '-keystore',
                  openDjTruststoreFn,
                  '-storepass',
                  openDjPin,
                  '-file',
                  self.opendj_cert_fn,
                  '-alias',
                  'server-cert',
                  '-rfc'])

        # Convert OpenDJ certificate to PKCS12
        self.logIt("Converting OpenDJ truststore")
        self.run([self.cmd_keytool,
                  '-importkeystore',
                  '-srckeystore',
                  openDjTruststoreFn,
                  '-srcstoretype',
                  'jks',
                  '-srcstorepass',
                  openDjPin,
                  '-destkeystore',
                  self.opendj_p12_fn,
                  '-deststoretype',
                  'pkcs12',
                  '-deststorepass',
                  self.opendj_p12_pass,
                  '-srcalias',
                  'server-cert'])

        # Import OpenDJ certificate into java truststore
        self.logIt("Import OpenDJ certificate")

        self.run([self.cmd_keytool, "-import", "-trustcacerts", "-alias", "%s_opendj" % self.hostname, \
                  "-file", self.opendj_cert_fn, "-keystore", self.defaultTrustStoreFN, \
                  "-storepass", "changeit", "-noprompt"])

    def index_opendj_backend(self, backend):
        index_command = 'create-backend-index'
            
        try:
            self.logIt("Running LDAP index creation commands for " + backend + " backend")
            # This json file contains a mapping of the required indexes.
            # [ { "attribute": "inum", "type": "string", "index": ["equality"] }, ...}
            index_json = self.load_json(self.openDjIndexJson)
            if index_json:
                for attrDict in index_json:
                    attr_name = attrDict['attribute']
                    index_types = attrDict['index']
                    for index_type in index_types:
                        backend_names = attrDict['backend']
                        for backend_name in backend_names:
                            if (backend_name == backend):
                                self.logIt("Creating %s index for attribute %s" % (index_type, attr_name))
                                indexCmd = " ".join(['cd %s/bin ; ' % self.ldapBaseFolder,
                                                     self.ldapDsconfigCommand,
                                                     index_command,
                                                     '--backend-name',
                                                     backend,
                                                     '--type',
                                                     'generic',
                                                     '--index-name',
                                                     attr_name,
                                                     '--set',
                                                     'index-type:%s' % index_type,
                                                     '--set',
                                                     'index-entry-limit:4000',
                                                     '--hostName',
                                                     self.ldap_hostname,
                                                     '--port',
                                                     self.ldap_admin_port,
                                                     '--bindDN',
                                                     '"%s"' % self.ldap_binddn,
                                                     '-j', self.ldapPassFn,
                                                     '--trustAll',
                                                     '--noPropertiesFile',
                                                     '--no-prompt'])
                                self.run(['/bin/su',
                                          'ldap',
                                          '-c',
                                          indexCmd])
            else:
                self.logIt('NO indexes found %s' % self.indexJson, True)
        except:
            self.logIt("Error occured during backend " + backend + " LDAP indexing", True)
            self.logIt(traceback.format_exc(), True)

    def index_opendj(self):
        self.index_opendj_backend('userRoot')
        self.index_opendj_backend('site')

    def prepare_opendj_schema(self):
        self.logIt("Copying OpenDJ schema")
        for schemaFile in self.openDjschemaFiles:
            self.copyFile(schemaFile, self.openDjSchemaFolder)

        self.run([self.cmd_chmod, '-R', 'a+rX', self.ldapBaseFolder])
        self.run([self.cmd_chown, '-R', 'ldap:ldap', self.ldapBaseFolder])

    def setup_opendj_service(self):
        service_path = self.detect_service_path()

        if self.os_type in ['centos', 'redhat', 'fedora'] and self.os_initdaemon == 'systemd':
            opendj_script_name = os.path.split(self.opendj_service_centos7)[-1]
            opendj_dest_folder = "/etc/systemd/system"
            try:
                self.copyFile(self.opendj_service_centos7, opendj_dest_folder)
                self.run([service_path, 'daemon-reload'])
                self.run([service_path, 'enable', 'opendj.service'])
                self.run([service_path, 'start', 'opendj.service'])
            except:
                self.logIt("Error copying script file %s to %s" % (opendj_script_name, opendj_dest_folder))
                self.logIt(traceback.format_exc(), True)
        else:
            self.run([self.ldapDsCreateRcCommand, "--outputFile", "/etc/init.d/opendj", "--userName",  "ldap"])
        
        if self.os_type in ['centos', 'fedora', 'redhat']:
            self.run(["/sbin/chkconfig", 'opendj', "on"])
            self.run([service_path, 'opendj', 'start'])
        elif self.os_type in ['ubuntu', 'debian']:
            self.run(["/usr/sbin/update-rc.d", 'opendj', 'start', '40', '3', "."])
            self.run(["/usr/sbin/update-rc.d", 'opendj', 'enable'])
            self.run([service_path, 'opendj', 'start'])


    def downloadAndExtractOpenDJ(self):

        openDJArchive = os.path.join(self.distFolder, 'app/opendj-server-3.0.1.gluu.zip')

        self.run(['mv', '/opt/opendj', '/opt/opendj.back_'+migration_time])

        self.logIt("Downloading opendj Server")
        self.run(['wget', 'https://ox.gluu.org/maven/org/forgerock/opendj/opendj-server-legacy/3.0.1.gluu/opendj-server-legacy-3.0.1.gluu.zip', '-O', openDJArchive])
        self.logIt("Unzipping %s in /opt/" % openDJArchive)
        self.run(['unzip', '-n', '-q', openDJArchive, '-d', '/opt/' ])
        realLdapBaseFolder = os.path.realpath(self.ldapBaseFolder)
        self.run([self.cmd_chown, '-R', 'ldap:ldap', realLdapBaseFolder])

    def install_opendj(self):
        self.logIt("Running OpenDJ Setup")
        # Copy opendj-setup.properties so user ldap can find it in /opt/opendj
        setupPropsFN = os.path.join(self.ldapBaseFolder, 'opendj-setup.properties')
        shutil.copy("%s/opendj-setup.properties" % self.outputFolder, setupPropsFN)
        ldapSetupCommand = '%s/setup' % self.ldapBaseFolder
        setupCmd = "cd /opt/opendj ; export OPENDJ_JAVA_HOME=" + self.jre_home + " ; " + " ".join([ldapSetupCommand,
                                                                                                   '--no-prompt',
                                                                                                   '--cli',
                                                                                                   '--propertiesFilePath',
                                                                                                   setupPropsFN,
                                                                                                   '--acceptLicense'
                                                                                                   ])
        self.run(['/bin/su',
                  'ldap',
                  '-c',
                  setupCmd])


        dsjavaCmd = "cd /opt/opendj/bin ; %s" % self.ldapDsJavaPropCommand
        self.run(['/bin/su',
                  'ldap',
                  '-c',
                  dsjavaCmd
                  ])

        stopDsJavaPropCommand = "%s/bin/stop-ds" % self.ldapBaseFolder
        dsjavaCmd = "cd /opt/opendj/bin ; %s" % stopDsJavaPropCommand
        self.run(['/bin/su',
                  'ldap',
                  '-c',
                  dsjavaCmd
                  ])



    def detect_service_path(self):
        service_path = '/sbin/service'
        if self.os_type in ['centos', 'redhat', 'fedora'] and self.os_initdaemon == 'systemd':
            service_path = '/usr/bin/systemctl'
        elif self.os_type in ['debian', 'ubuntu']:
            service_path = '/usr/sbin/service'

        return service_path

    def run_service_command(self, service, operation):
        service_path = self.detect_service_path()

        try:
            if self.os_type in ['centos', 'redhat', 'fedora'] and self.os_initdaemon == 'systemd':
                self.run([service_path, operation, service], None, None, True)
            else:
                self.run([service_path, service, operation], None, None, True)
        except:
            self.logIt("Error starting Jetty service '%s'" % operation)
            self.logIt(traceback.format_exc(), True)


    def install_ldap_server(self):
        self.logIt("Running OpenDJ Setup")

        self.opendj_version = self.determineOpenDJVersion()
        self.createLdapPw()

        if self.ldap_type == 'opendj':
            self.setup_opendj_service()
            self.prepare_opendj_schema()
            self.run_service_command('opendj', 'stop')
            self.run_service_command('opendj', 'start')
            self.configure_opendj()
            self.export_opendj_public_cert()
            self.index_opendj()

        self.deleteLdapPw()
    def merge_dicts(self, *dict_args):
        result = {}
        for dictionary in dict_args:
            result.update(dictionary)

        return result

    def load_properties(self, fn):
        self.logIt('Loading Properties %s' % fn)
        p = Properties.Properties()
        try:
            p.load(open(fn))
            properties_list = p.keys()
            for prop in properties_list:
                try:
                    self.__dict__[prop] = p[prop]
                    if p[prop] == 'True':
                        self.__dict__[prop] = True
                    elif p[prop] == 'False':
                        self.__dict__[prop] = False
                except:
                    self.logIt("Error loading property %s" % prop)
                    self.logIt(traceback.format_exc(), True)
        except:
            self.logIt("Error loading properties", True)
            self.logIt(traceback.format_exc(), True)


    def get_missing_files(self):
        if os.path.exists(os.path.join(self.install_dir, 'static/opendj/deprecated')):
            self.run(['wget', 'https://raw.githubusercontent.com/GluuFederation/community-edition-setup/master/schema/generator.py', '-O', './schema/generator.py'])
            cmd_l = ['python ./schema/manager.py generate --type opendj --filename ./schema/gluu_schema.json > ./static/opendj/deprecated/101-ox.ldif',
                     'python ./schema/manager.py generate --type opendj --filename ./schema/custom_schema.json > ./static/opendj/deprecated/77-customAttributes.ldif']

            for cmd in cmd_l:
                self.logIt('Running: ' + cmd)
                os.system(cmd)

            self.openDjschemaFiles = glob.glob(os.path.join(self.install_dir, 'static/opendj/deprecated/*.ldif'))
        
        if not os.path.exists(self.openDjIndexJson):
            self.run(['wget', 'https://raw.githubusercontent.com/GluuFederation/community-edition-setup/version_3.1.2/static/opendj/index.json', '-O', self.openDjIndexJson])
                    
        if not os.path.exists(self.opendj_service_centos7):
            os.system('mkdir %s/static/opendj/systemd' % self.install_dir)
            self.run(['wget', 'https://raw.githubusercontent.com/GluuFederation/community-edition-setup/version_3.1.2/static/opendj/systemd/opendj.service', '-O', self.opendj_service_centos7])


if __name__ == '__main__':

    setupOptions = {
        'install_dir': '.',
        'setup_properties': None,
        'noPrompt': False,
        'downloadWars': False,
        'installOxAuth': True,
        'installOxTrust': True,
        'installLDAP': True,
        'installHTTPD': True,
        'installSaml': False,
        'installAsimba': False,
        'installOxAuthRP': False,
        'installPassport': False,
        'allowPreReleasedApplications': False,
        'allowDeprecatedApplications': False,
        'installJce': False
    }


    installObject = Setup(setupOptions['install_dir'])
    installObject.load_properties(installObject.setup_properties_fn)
    installObject.check_properties()
    installObject.ldapPass = get_ldap_bind_pw()
    installObject.ldap_binddn='cn=Directory Manager'
    installObject.ldap_type = 'opendj'
    installObject.encode_passwords()
    
    
    # Get the OS type
    installObject.os_type = installObject.detect_os_type()
    # Get the init type
    installObject.os_initdaemon = installObject.detect_initd()

    
    if len(sys.argv) > 1:
        if sys.argv[1] == '-p':
            update_ox_idp(installObject.ldap_binddn, installObject.ldapPass)
            sys.exit("Completed")
        else:
            sys.exit("Unrecognized argument")

    installObject.get_missing_files()
    installObject.createLdapPw()
    installObject.downloadAndExtractOpenDJ()
    installObject.install_opendj()        
    installObject.install_ldap_server()
    update_ox_ldap_prop(installObject.ldap_binddn, installObject.opendj_p12_fn, installObject.encoded_opendj_p12_pass)
    
