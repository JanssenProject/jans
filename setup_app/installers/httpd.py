import os
import glob
import shutil

from setup_app import paths
from setup_app.utils import base
from setup_app.static import AppType, InstallOption
from setup_app.config import Config
from setup_app.utils.setup_utils import SetupUtils
from setup_app.installers.base import BaseInstaller

class HttpdInstaller(BaseInstaller, SetupUtils):

    def __init__(self):
        setattr(base.current_app, self.__class__.__name__, self)
        self.service_name = base.httpd_name
        self.pbar_text = "Configuring " + base.httpd_name
        self.app_type = AppType.SERVICE
        self.install_type = InstallOption.OPTONAL
        self.install_var = 'installHttpd'
        self.register_progess()

        self.needdb = False # we don't need backend connection in this class

        self.apache_version = base.determineApacheVersion()

        self.httpdKeyFn = os.path.join(Config.certFolder, 'httpd.key')
        self.httpdCertFn = os.path.join(Config.certFolder, 'httpd.crt')

        self.templates_folder = os.path.join(Config.templateFolder, 'apache')
        self.output_folder = os.path.join(Config.outputFolder, 'apache')

        self.apache2_conf = os.path.join(self.output_folder, 'httpd.conf')
        self.apache2_ssl_conf = os.path.join(self.output_folder, 'https_jans.conf')
        self.apache2_24_conf = os.path.join(self.output_folder, 'httpd_2.4.conf')
        self.apache2_ssl_24_conf = os.path.join(self.output_folder, 'https_jans.conf')
        self.https_jans_fn = '/etc/httpd/conf.d/https_jans.conf' if base.clone_type == 'rpm' else '/etc/apache2/sites-available/https_jans.conf'

    def configure(self):
        self.logIt(self.pbar_text, pbar=self.service_name)
        self.stop()

        self.write_httpd_config()

        self.writeFile('/var/www/html/index.html', 'OK')

        if base.snap:
            icons_conf_fn = '/etc/apache2/mods-available/alias.conf'
        elif base.clone_type == 'rpm':
            icons_conf_fn = '/etc/httpd/conf.d/autoindex.conf'
        else:
            icons_conf_fn = '/etc/apache2/mods-available/alias.conf'

        with open(icons_conf_fn[:]) as f:
            icons_conf = f.readlines()

        for i, l in enumerate(icons_conf[:]):
            if l.strip().startswith('Alias') and ('/icons/' in l.strip().split()):
                icons_conf[i] =  l.replace('Alias', '#Alias')

        self.writeFile(icons_conf_fn, ''.join(icons_conf))

        error_templates = glob.glob(os.path.join(self.templates_folder,'error_pages/*.html'))

        for tmp_fn in error_templates:
            self.copyFile(tmp_fn, '/var/www/html')

        # we only need these modules
        mods_enabled = ['env', 'proxy_http', 'access_compat', 'alias', 'authn_core', 'authz_core', 'authz_host', 'headers', 'mime', 'mpm_event', 'proxy', 'proxy_ajp', 'security2', 'reqtimeout', 'setenvif', 'socache_shmcb', 'ssl', 'unique_id']
        if Config.profile == 'openbanking':
            mods_enabled.append('rewrite')

        if base.snap:
            mods_enabled_dir = os.path.join(base.snap_common, 'etc/apache2/mods-enabled')
            mods_available_dir = os.path.join(base.snap_common, 'etc/apache2/mods-available')

            for em in os.listdir(mods_enabled_dir):
                em_n, em_e = os.path.splitext(em)
                if not em_n in mods_enabled:
                    os.unlink(os.path.join(mods_enabled_dir, em))

            for m in mods_enabled:
                load_fn = os.path.join(mods_available_dir, m + '.load')
                conf_fn = os.path.join(mods_available_dir, m + '.conf')
                if os.path.exists(load_fn):
                    target_fn = os.path.join(mods_enabled_dir, m + '.load')
                    if not os.path.exists(target_fn):
                        os.symlink(load_fn, target_fn)
                if os.path.exists(conf_fn):
                    target_fn = os.path.join(mods_enabled_dir, m + '.conf')
                    if not os.path.exists(target_fn):
                        os.symlink(conf_fn, target_fn)

        elif base.clone_type == 'rpm':

            for mod_load_fn in glob.glob('/etc/httpd/conf.modules.d/*'):

                with open(mod_load_fn) as f:
                    mod_load_content = f.readlines()

                modified = False

                for i, l in enumerate(mod_load_content[:]):
                    ls = l.strip()

                    if ls and not ls.startswith('#'):
                        lsl = ls.split('/')
                        module =  lsl[-1][4:-3]

                        if not module in mods_enabled:
                            mod_load_content[i] = l.replace('LoadModule', '#LoadModule')
                            modified = True

                if modified:
                    self.writeFile(mod_load_fn, ''.join(mod_load_content))
        else:

            cmd_a2enmod = shutil.which('a2enmod')
            cmd_a2dismod = shutil.which('a2dismod')

            for mod_load_fn in glob.glob('/etc/apache2/mods-enabled/*'):
                mod_load_base_name = os.path.basename(mod_load_fn)
                f_name, f_ext = os.path.splitext(mod_load_base_name)
                if not f_name in mods_enabled:
                    self.run([cmd_a2dismod, mod_load_fn])

            for amod in mods_enabled:
                self.run([cmd_a2enmod, amod])

        if not Config.get('httpdKeyPass'):
            Config.httpdKeyPass = self.getPW()


        # generate httpd self signed certificate
        self.gen_cert('httpd', Config.httpdKeyPass, 'jetty')

        self.enable()
        self.start()

    def write_httpd_config(self):
        
        self.update_rendering_dict()
        for tmp in (self.apache2_conf, self.apache2_ssl_conf, self.apache2_24_conf, self.apache2_ssl_24_conf):
            self.renderTemplateInOut(tmp, self.templates_folder, self.output_folder)

        # CentOS 7.* + systemd + apache 2.4
        if self.service_name == 'httpd' and self.apache_version == "2.4":
            self.copyFile(self.apache2_24_conf, '/etc/httpd/conf/httpd.conf')
            self.copyFile(self.apache2_ssl_24_conf, '/etc/httpd/conf.d/https_jans.conf')

        if base.clone_type == 'rpm' and base.os_initdaemon == 'init':
            self.copyFile(self.apache2_conf, '/etc/httpd/conf/httpd.conf')
            self.copyFile(self.apache2_ssl_conf, self.https_jans_fn)

        if base.clone_type == 'deb':
            self.copyFile(self.apache2_ssl_conf, self.https_jans_fn)
            self.run([paths.cmd_ln, '-s', self.https_jans_fn,
                      '/etc/apache2/sites-enabled/https_jans.conf'])

    def installed(self):
        return os.path.exists(self.https_jans_fn)
