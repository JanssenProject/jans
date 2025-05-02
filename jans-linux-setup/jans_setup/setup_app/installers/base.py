import os
import uuid
import inspect
import json

from setup_app import paths
from setup_app.utils import base
from setup_app.config import Config
from setup_app.pylib.ldif4.ldif import LDIFWriter

from setup_app.utils.db_utils import dbUtils
from setup_app.utils.progress import jansProgress
from setup_app.utils.printVersion import get_war_info

class BaseInstaller:
    needdb = True
    dbUtils = dbUtils
    service_scopes_created = False

    def register_progess(self):
        if not hasattr(self, 'output_folder'):
            self.output_folder = os.path.join(Config.output_dir, self.service_name)

        if not hasattr(self, 'templates_dir'):
            self.templates_dir = os.path.join(Config.templateFolder, self.service_name)

        jansProgress.register(self)

    def start_installation(self):

        if not hasattr(self, 'pbar_text'):
            pbar_text = "Installing " + self.service_name.title()
        else:
            pbar_text = self.pbar_text
        self.logIt(pbar_text, pbar=self.service_name)

        self.pre_install()

        if self.needdb:
            self.dbUtils.bind()

        self.check_for_download()

        if not base.snap:
            self.create_user()

        self.create_folders()

        self.install()
        self.copy_static()
        self.generate_configuration()

        # before rendering templates, let's push variables of this class to Config.templateRenderingDict
        self.update_rendering_dict()
        self.render_unit_file()

        self.render_import_templates()
        if not self.service_scopes_created:
            self.create_scopes()

        self.update_backend()
        self.service_post_setup()

    def render_unit_file(self, unit=None):
        # render unit file
        if getattr(self, 'no_unit_file', False):
            return
        units = self.get_systemd_service_list(unit)

        for unit in units:
            unit_files_dir = os.path.join(Config.staticFolder, 'system/systemd')
            unit_file = os.path.join(unit_files_dir, unit + '.service')
            if os.path.exists(unit_file):
                self.renderTemplateInOut(unit_file, unit_files_dir, Config.unit_files_path)

    def update_rendering_dict(self):
        mydict = {}
        for obj_name, obj in inspect.getmembers(self):
            if obj_name in ('dbUtils',):
                continue
            if not obj_name.startswith('__') and (not callable(obj)):
                mydict[obj_name] = obj

        Config.templateRenderingDict.update(mydict)


    def check_clients(self, client_var_id_list, resource=False, create=True):
        field_name, ou = ('jansId', 'resources') if resource else ('inum', 'clients')

        ret_val = {}

        for cids in client_var_id_list:
            client_var_name = cids[0] 
            client_id_prefix = cids[1]
            if len(cids) > 2:
                client_pw = cids[2]['pw']
                client_encoded_pw = cids[2]['encoded']
            else:
                tmp_ = client_var_name.split('_')
                client_pw =  '_'.join(tmp_[:-1]) + '_pw'
                client_encoded_pw = '_'.join(tmp_[:-1]) + '_encoded_pw'

            self.logIt("Checking ID for client {}".format(client_var_name))
            rv = 1

            if Config.get(client_var_name):
                result = self.dbUtils.search('ou={},o=jans'.format(ou), '(&({}={}*)(objectClass=jansClnt))'.format(field_name, client_id_prefix))
                if result:
                    setattr(Config, client_var_name, result[field_name])
                    self.logIt("{} was found in backend as {}".format(client_var_name, result[field_name]))
                    if 'jansClntSecret' in result:
                        setattr(Config, client_encoded_pw, result['jansClntSecret'])
                        setattr(Config, client_pw, self.unobscure(result['jansClntSecret']))
                else:
                    rv = -1
            else:
                rv = -1


            ret_val[client_id_prefix] = rv

            if create:

                if not Config.get(client_var_name):
                    setattr(Config, client_var_name, client_id_prefix + str(uuid.uuid4()))
                    self.logIt("Client ID for {} was created as {}".format(client_var_name, Config.get(client_var_name)))
                else:
                    self.logIt("Client {} exists in current configuration as {}".format(client_var_name, getattr(Config, client_var_name)))

                if not Config.get(client_pw):
                    self.logIt("Generating password for {}".format(client_pw))
                    client_pw_s = self.getPW()
                    client_encoder_pw_s = self.obscure(client_pw_s)
                    setattr(Config, client_pw, client_pw_s)
                    setattr(Config, client_encoded_pw, client_encoder_pw_s)

        return ret_val

    def check_scope(self, scope_id):
        search_filter = '(&(objectClass=jansScope)(jansId={}))'.format(scope_id)
        result = self.dbUtils.search('ou=scopes,o=jans', search_filter=search_filter)
        if result:
            return result.get('dn')


    def get_systemd_service_list(self, service):
        if service:
            return [service]

        if hasattr(self, 'systemd_units'):
            return self.systemd_units

        return [self.service_name]

    def run_service_command(self, operation, service):

        services = self.get_systemd_service_list(service)

        for service in services:

            if base.snap:
                service = os.environ['SNAP_NAME'] + '.' + service

            try:
                if base.snap:
                    cmd_list = [base.snapctl, operation, service]
                    if operation == 'start':
                        cmd_list.insert(-1, '--enable')
                    self.run(cmd_list, None, None, True)
                elif base.systemctl:
                    local_script = os.path.join(Config.jansOptFolder, 'scripts', service)
                    if os.path.exists(local_script):
                        self.run([local_script, operation], useWait=True)
                    else:
                        self.run([base.service_cmd, service, operation], useWait=True)
                elif (base.clone_type == 'rpm' and base.os_initdaemon == 'systemd') or base.deb_sysd_clone:
                    self.run([base.service_path, operation, service], None, None, True)
                else:
                    self.run([base.service_path, service, operation], None, None, True)
            except:
                self.logIt("Error running operation {} for service {}".format(operation, service), True)

    def enable(self, service=None):
        if not base.snap:
            self.run_service_command('enable', service)

    def stop(self, service=None):
        self.run_service_command('stop', service)

    def start(self, service=None):
        self.run_service_command('start', service)

    def restart(self, service=None):
        self.stop(service)
        self.start(service)

    def reload_daemon(self, service=None):
        if not base.snap:
            if not service:
                service = self.service_name
            if (base.clone_type == 'rpm' and base.os_initdaemon == 'systemd') or base.deb_sysd_clone:
                self.run([base.service_path, 'daemon-reload'])
            elif base.os_name == 'ubuntu16':
                self.run([paths.cmd_update_rc, service, 'defaults'])

    def pre_install(self):
        """Installer may require some settings before installation"""
        pass

    def generate_configuration(self):
        pass

    def render_import_templates(self):
        pass

    def update_backend(self):
        pass


    def check_for_download(self):
        # execute for each installer
        if base.argsp.force_download:
            self.download_files(force=True)
        else:
            self.download_files()

    def download_file(self, url, src):
        Config.pbar.progress(self.service_name, "Downloading {}".format(os.path.basename(src)))
        base.download(url, src)

    def download_files(self, force=False, downloads=[]):
        if hasattr(self, 'source_files'):
            for i, item in enumerate(self.source_files[:]):
                src = item[0]
                url = item[1]
                src_name = os.path.basename(src)

                if downloads and not src_name in downloads:
                    continue

                if force or not os.path.exists(src):
                    self.download_file(url, src)


    def create_user(self):
        pass

    def create_folders(self):
        pass

    def copy_static(self):
        pass

    def installed(self):
        return None

    def check_need_for_download(self):
        pass

    def service_post_setup(self):
        pass

    def service_post_install_tasks(self):
        pass

    def create_scopes(self):
        scopes_json_fn = os.path.join(self.templates_dir, 'scopes.json')

        if not os.path.exists(scopes_json_fn):
            return

        self.logIt(f"Creating {self.service_name} scopes from {scopes_json_fn}")
        scopes = base.readJsonFile(scopes_json_fn)
        scopes_ldif_fn = os.path.join(self.output_folder, 'scopes.ldif')
        self.createDirs(self.output_folder)

        scopes_list = []

        with open(scopes_ldif_fn, 'wb') as scope_ldif_fd:
            ldif_scopes_writer = LDIFWriter(scope_ldif_fd, cols=1000)
            for scope in scopes:
                scope_dn = 'inum={},ou=scopes,o=jans'.format(scope['inum'])
                scopes_list.append(scope_dn)
                ldif_dict = {
                            'objectClass': ['top', 'jansScope'],
                            'description': [scope['description']],
                            'displayName': [scope['displayName']],
                            'inum': [scope['inum']],
                            'jansDefScope': [str(scope['jansDefScope'])],
                            'jansId': [scope['jansId']],
                            'jansScopeTyp': [scope['jansScopeTyp']],
                            'jansAttrs': [json.dumps({
                                            "spontaneousClientId":None,
                                            "spontaneousClientScopes":[],
                                            "showInConfigurationEndpoint": False
                                        })],
                        }
                ldif_scopes_writer.unparse(scope_dn, ldif_dict)

        self.dbUtils.import_ldif([scopes_ldif_fn])
        self.service_scopes_created = True
        return scopes_list
