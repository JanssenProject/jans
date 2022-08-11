#!/usr/bin/env python3

import sys
import os
import json
import re
import urllib3
import configparser
import readline
import argparse
import random
import datetime
import ruamel.yaml
import code
import traceback
import ast
import base64
import requests
import html
import glob
import logging
import http.client

from pathlib import Path
from types import SimpleNamespace
from urllib.parse import urlencode
from collections import OrderedDict
from urllib.parse import urljoin
from http.client import HTTPConnection
from pygments import highlight, lexers, formatters

home_dir = Path.home()
config_dir = home_dir.joinpath('.config')
config_dir.mkdir(parents=True, exist_ok=True)
config_ini_fn = config_dir.joinpath('jans-cli.ini')
cur_dir = os.path.dirname(os.path.realpath(__file__))
sys.path.append(cur_dir)

from pylib.tabulate.tabulate import tabulate
try:
    import jwt
except ModuleNotFoundError:
    from pylib import jwt

tabulate_endpoints = {
    'jca.get-config-scripts': ['scriptType', 'name', 'enabled', 'inum'],
    'jca.get-user': ['inum', 'userId', 'mail','sn', 'givenName', 'jansStatus'],
    'jca.get-attributes': ['inum', 'name', 'displayName', 'status', 'dataType', 'claimName'],
    'jca.get-oauth-openid-clients': ['inum', 'displayName', 'clientName', 'applicationType'],
    'jca.get-oauth-scopes': ['dn', 'id', 'scopeType'],
    'scim.get-users': ['id', 'userName', 'displayName', 'active']
}

tabular_dataset = {'scim.get-users': 'Resources'}

my_op_mode = 'scim' if 'scim' in os.path.basename(sys.argv[0]) else 'jca'
plugins = []

warning_color = 214
error_color = 196
success_color = 10
bold_color = 15
grey_color = 242


def clear():
    if not debug:
        os.system('clear')

urllib3.disable_warnings()
config = configparser.ConfigParser()

host = os.environ.get('jans_host')
client_id = os.environ.get(my_op_mode + 'jca_client_id')
client_secret = os.environ.get(my_op_mode + 'jca_client_secret')
access_token = None
debug = os.environ.get('jans_client_debug')
log_dir = os.environ.get('cli_log_dir', cur_dir)

def encode_decode(s, decode=False):
    cmd = '/opt/jans/bin/encode.py '
    if decode:
        cmd += '-D '
    result = os.popen(cmd + s + ' 2>/dev/null').read()
    return result.strip()


##################### arguments #####################

# load yaml file

my_op_mode

debug_json = 'swagger_yaml.json'
if os.path.exists(debug_json):
    with open(debug_json) as f:
        cfg_yml = json.load(f, object_pairs_hook=OrderedDict)
else:
    with open(os.path.join(cur_dir, my_op_mode+'.yaml')) as f:
        cfg_yml = ruamel.yaml.load(f.read().replace('\t', ''), ruamel.yaml.RoundTripLoader)
        if os.environ.get('dump_yaml'):
            with open(debug_json, 'w') as w:
                json.dump(cfg_yml, w, indent=2)

op_list = []

name_regex = re.compile('[^a-zA-Z0-9]')

def get_named_tag(tag):
    return name_regex.sub('', tag.title())

for path in cfg_yml['paths']:
    for method in cfg_yml['paths'][path]:
        if isinstance(cfg_yml['paths'][path][method], dict):
            for tag_ in cfg_yml['paths'][path][method].get('tags', []):
                tag = get_named_tag(tag_)
                if not tag in op_list:
                    op_list.append(tag)

for yml_fn in glob.glob(os.path.join(cur_dir, '*.yaml')):
    fname, fext = os.path.splitext(os.path.basename(yml_fn))
    op_list.append(fname)

op_list.sort()

parser = argparse.ArgumentParser()
parser.add_argument("--host", help="Hostname of server")
parser.add_argument("--client-id", help="Jans Config Api Client ID")
parser.add_argument("--client-secret", "--client_secret", help="Jans Config Api Client ID secret")
parser.add_argument("--access-token", help="JWT access token or path to file containing JWT access token")
parser.add_argument("--plugins", help="Available plugins separated by comma")
parser.add_argument("-debug", help="Run in debug mode", action='store_true')
parser.add_argument("--debug-log-file", default='swagger.log', help="Log file name when run in debug mode")
parser.add_argument("--operation-id", help="Operation ID to be done")
parser.add_argument("--url-suffix", help="Argument to be added api endpoint url. For example inum:2B29")
parser.add_argument("--info", choices=op_list, help="Help for operation")
parser.add_argument("--op-mode", choices=['get', 'post', 'put', 'patch', 'delete'], default='get',
                    help="Operation mode to be done")
parser.add_argument("--endpoint-args",
                    help="Arguments to pass endpoint separated by comma. For example limit:5,status:INACTIVE")
parser.add_argument("--schema", help="Get sample json schema")

parser.add_argument("-CC", "--config-api-mtls-client-cert", help="Path to SSL Certificate file")
parser.add_argument("-CK", "--config-api-mtls-client-key", help="Path to SSL Key file")
parser.add_argument("--key-password", help="Password for SSL Key file")
parser.add_argument("-noverify", help="Ignore verifying the SSL certificate", action='store_true', default=True)

parser.add_argument("-use-test-client", help="Use test client without device authorization", action='store_true')


parser.add_argument("--patch-add", help="Colon delimited key:value pair for add patch operation. For example loggingLevel:DEBUG")
parser.add_argument("--patch-replace", help="Colon delimited key:value pair for replace patch operation. For example loggingLevel:DEBUG")
parser.add_argument("--patch-remove", help="Key for remove patch operation. For example imgLocation")
parser.add_argument("-no-color", help="Do not colorize json dumps", action='store_true')
parser.add_argument("--log-dir", help="Log directory", default=log_dir)

parser.add_argument("--data", help="Path to json data file")
args = parser.parse_args()


################## end of arguments #################

test_client = args.use_test_client


if args.plugins:
    for plugin in args.plugins.split(','):
        plugins.append(plugin.strip())


if not(host and (client_id and client_secret or access_token)):
    host = args.host
    client_id = args.client_id
    client_secret = args.client_secret
    debug = args.debug
    log_dir = args.log_dir

    access_token = args.access_token
    if access_token and os.path.isfile(access_token):
        with open(access_token) as f:
            access_token = f.read()


if not(host and (client_id and client_secret or access_token)):

    if config_ini_fn.exists():
        config.read_string(config_ini_fn.read_text())
        host = config['DEFAULT']['jans_host']

        if 'jca_test_client_id' in config['DEFAULT'] and test_client:
            client_id = config['DEFAULT']['jca_test_client_id']
            secret_key_str = 'jca_test_client_secret'
        else:
            client_id = config['DEFAULT']['jca_client_id']
            secret_key_str = 'jca_client_secret'

        secret_enc_key_str = secret_key_str + '_enc'
        if config['DEFAULT'].get(secret_key_str):
            client_secret = config['DEFAULT'][secret_key_str]
        elif config['DEFAULT'].get(secret_enc_key_str):
            client_secret_enc = config['DEFAULT'][secret_enc_key_str]
            client_secret = encode_decode(client_secret_enc, decode=True)

        debug = config['DEFAULT'].get('debug')
        log_dir = config['DEFAULT'].get('log_dir', log_dir)

    else:
        config['DEFAULT'] = {'jans_host': 'jans server hostname,e.g, jans.foo.net',
                             'jca_client_id': 'your jans config api client id',
                             'jca_client_secret': 'client secret for your jans config api client',
                             'scim_client_id': 'your jans scim client id',
                             'scim_client_secret': 'client secret for your jans scim client'}


def get_bool(val):
    if str(val).lower() in ('yes', 'true', '1', 'on'):
        return True
    return False

def write_config():
    with open(config_ini_fn, 'w') as w:
        config.write(w)

debug = get_bool(debug)


class Menu(object):

    def __init__(self, name, method='', info={}, path=''):
        self.name = name
        self.display_name = name
        self.method = method
        self.info = info
        self.path = path
        self.children = []
        self.parent = None
        self.ignore = False

    def __iter__(self):
        self.current_index = 0
        return self

    def __repr__(self):
        return self.display_name
        self.__print_child(self)

    def tree(self):
        print(self.name)
        self.__print_child(self)

    def __get_parent_number(self, child):
        n = 0
        while True:
            if not child.parent:
                break
            n += 1
            child = child.parent

        return n

    def __print_child(self, menu):
        if menu.children:
            for child in menu.children:
                print(' ' * self.__get_parent_number(child) * 2, child)
                self.__print_child(child)

    def add_child(self, node):
        assert isinstance(node, Menu)
        node.parent = self
        self.children.append(node)

    def get_child(self, n):
        if len(self.children) > n:
            return self.children[n]

    def __next__(self):
        if self.current_index < len(self.children):
            retVal = self.children[self.current_index]
            self.current_index += 1
            return retVal

        else:
            raise StopIteration

    def __contains__(self, child):
        for child_ in self.children:
            if child_.name == child:
                return True
        return False


class JCA_CLI:

    def __init__(self, host, client_id, client_secret, access_token, test_client=False, wrapped=False):
        self.host = self.idp_host = host
        self.client_id = client_id
        self.client_secret = client_secret
        self.use_test_client = test_client
        self.getCredintials()
        self.wrapped = wrapped
        self.access_token = access_token or config['DEFAULT'].get('access_token')
        self.jwt_validation_url = 'https://{}/jans-config-api/api/v1/acrs'.format(self.idp_host)
        self.set_user()
        self.plugins()

        if not self.access_token and config['DEFAULT'].get('access_token_enc'):
            self.access_token = encode_decode(config['DEFAULT']['access_token_enc'], decode=True)

        if my_op_mode == 'scim':
            self.host += '/jans-scim/restv1/v2'

        self.set_logging()
        self.ssl_settings()
        self.make_menu()
        self.current_menu = self.menu
        self.enums()

    def getCredintials(self): 
        if self.host == '' or self.client_id == '' or self.client_secret == '' :
            if config_ini_fn.exists():
                config.read_string(config_ini_fn.read_text())
                host_data = config['DEFAULT']['jans_host']

                if 'jca_test_client_id' in config['DEFAULT'] and test_client:
                    client_id_data = config['DEFAULT']['jca_test_client_id']
                    secret_key_str = 'jca_test_client_secret'
                else:
                    client_id_data = config['DEFAULT']['jca_client_id']
                    secret_key_str = 'jca_client_secret'

                secret_enc_key_str = secret_key_str + '_enc'
                if config['DEFAULT'].get(secret_key_str):
                    client_secret_data = config['DEFAULT'][secret_key_str]
                elif config['DEFAULT'].get(secret_enc_key_str):
                    client_secret_enc = config['DEFAULT'][secret_enc_key_str]
                    client_secret_data = encode_decode(client_secret_enc, decode=True)

                self.host = self.idp_host=host_data.replace("'","")  
                self.client_id = client_id_data.replace("'","")
                self.client_secret = client_secret_data.replace("'","")

    def set_logging(self):
        if debug:
            self.cli_logger = logging.getLogger("urllib3")
            self.cli_logger.setLevel(logging.DEBUG)
            self.cli_logger.propagate = True
            HTTPConnection.debuglevel = 1
            file_handler = logging.FileHandler(os.path.join(log_dir, 'cli_debug.log'))
            file_handler.setLevel(logging.DEBUG)
            file_handler.setFormatter(logging.Formatter("%(asctime)s [%(levelname)-5.5s]  %(message)s"))
            self.cli_logger.addHandler(file_handler)
            def print_to_log(*args):
                self.cli_logger.debug(" ".join(args))
            http.client.print = print_to_log


    def log_response(self, response):
        if debug:
            self.cli_logger.debug('requests response status: %s', str(response.status_code))
            self.cli_logger.debug('requests response headers: %s', str(response.headers))
            self.cli_logger.debug('requests response text: %s', str(response.text))

    def enums(self):
        self.enum_dict = {
                            "CustomAttribute": {
                              "properties.name": {
                                "f": "get_attrib_list"
                              }
                            }
                          }

    def set_user(self):
        self.auth_username = None
        self.auth_password = None
        self.askuser = get_bool(config['DEFAULT'].get('askuser'))

        if self.askuser:
            if args.username:
                self.auth_username = args.username
            if args.password:
                self.auth_password = args.password
            elif args.j:
                if os.path.isfile(args.j):
                    with open(args.j) as reader:
                        self.auth_password = reader.read()
                else:
                    print(args.j, "does not exist. Exiting ...")
                    sys.exit()
            if not (self.auth_username and self.auth_password):
                print("I need username and password. Exiting ...")
                sys.exit()

    def plugins(self):
        for plugin_s in config['DEFAULT'].get(my_op_mode + '_plugins', '').split(','):
            plugin = plugin_s.strip()
            if plugin:
                plugins.append(plugin)

    def ssl_settings(self):
        if args.noverify:
            self.verify_ssl = False
        else:
            self.verify_ssl = True
        self.mtls_client_cert = None
        if args.config_api_mtls_client_cert and args.config_api_mtls_client_key:
            self.mtls_client_cert = (args.config_api_mtls_client_cert, args.config_api_mtls_client_key)

    def drop_to_shell(self, mylocals):
        locals_ = locals()
        locals_.update(mylocals)
        code.interact(local=locals_)
        sys.exit()

    def get_request_header(self, headers={}, access_token=None):
        if not access_token:
            access_token = self.access_token

        ret_val = {'Authorization': 'Bearer {}'.format(access_token)}
        ret_val.update(headers)
        return ret_val


    def check_connection(self):
        url = 'https://{}/jans-auth/restv1/token'.format(self.idp_host)
        response = requests.post(
                url=url,
                auth=(self.client_id, self.client_secret),
                data={"grant_type": "client_credentials"},
                verify=self.verify_ssl,
                cert=self.mtls_client_cert
            )

        self.log_response(response)
        if response.status_code != 200:
            if self.wrapped:
                return response.text

            raise ValueError(
                self.colored_text("Unable to connect jans-auth server:\n {}".format(response.text), error_color))

        if not self.use_test_client and self.access_token:
            response = requests.get(
                    url = self.jwt_validation_url,
                    headers=self.get_request_header({'Accept': 'application/json'}),
                    verify=self.verify_ssl,
                    cert=self.mtls_client_cert
                )

        if not response.status_code == 200:
            self.access_token = None
            return response.text

        return True


    def check_access_token(self):

        if not self.access_token :
            print(self.colored_text("Access token was not found.", warning_color))
            return

        try:
            jwt.decode(self.access_token,
                    options={
                            'verify_signature': False,
                            'verify_exp': True,
                            'verify_aud': False
                             }
                    )
        except Exception as e:
            print(self.colored_text("Unable to validate access token: {}".format(e), error_color))
            self.access_token = None


    def validate_date_time(self, date_str):
        try:
            datetime.datetime.fromisoformat(date_str)
            return True
        except Exception as e:
            self.log_response('invalid date-time format: %s'.format(str(e)))
            return False


    def make_menu(self):

        menu_groups = []

        def get_sep_pos(s):
            for i, c in enumerate(s):
                if c in ('-', '–'):
                    return i
            return -1

        def get_group_obj(mname):
            for grp in menu_groups:
                if grp.mname == mname:
                    return grp


        for tag in cfg_yml['tags']:
            tname = tag['name'].strip()
            if tname == 'developers':
                continue
            n = get_sep_pos(tname)
            mname = tname[:n].strip() if n > -1 else tname
            grp = get_group_obj(mname)
            if not grp:
                grp = SimpleNamespace()
                grp.tag = None if n > -1 else tname
                grp.mname = mname
                grp.submenu = []
                menu_groups.append(grp)

            if n > -1:
                sname = tname[n+1:].strip()
                sub = SimpleNamespace()
                sub.tag = tname
                sub.mname = sname
                grp.submenu.append(sub)


        def get_methods_of_tag(tag):
            methods = []
            if tag:
                for path_name in cfg_yml['paths']:
                    path = cfg_yml['paths'][path_name]
                    for method_name in path:
                        method = path[method_name]
                        if 'tags' in method and tag in method['tags'] and 'operationId' in method:
                            if method.get('x-cli-plugin') and  method['x-cli-plugin'] not in plugins:
                                continue
                            method['__method_name__'] = method_name
                            method['__path_name__'] = path_name
                            methods.append(method)

            return methods

        menu = Menu('Main Menu')

        
        for grp in menu_groups:
            methods = get_methods_of_tag(grp.tag)
            m = Menu(name=grp.mname)
            m.display_name = m.name + ' ˅'
            menu.add_child(m)

            for method in methods:
                for tag in method['tags']:
                    menu_name = method.get('summary') or method.get('description')
                    sm = Menu(
                        name=menu_name.strip('.'),
                        method=method['__method_name__'],
                        info=method,
                        path=method['__path_name__'],
                    )
                    m.add_child(sm)

            if grp.submenu:
                m.display_name = m.name + ' ˅'
                for sub in grp.submenu:
                    methods = get_methods_of_tag(sub.tag)
                    if not methods:
                        continue
                    smenu = Menu(name=sub.mname)
                    smenu.display_name = smenu.name + ' ˅'
                    m.add_child(smenu)
                    
                    for method in methods:
                        for tag in method['tags']:

                            sub_menu_name = method.get('summary') or method.get('description')
                            ssm = Menu(
                                    name=sub_menu_name.strip('.'),
                                    method=method['__method_name__'],
                                    info=method,
                                    path=method['__path_name__'],
                                )
                            smenu.add_child(ssm)

        self.menu = menu


    def get_scoped_access_token(self, scope):

        if not self.wrapped:
            scope_text = " for scope {}\n".format(scope) if scope else ''
            sys.stderr.write("Getting access token{}".format(scope_text))

        url = 'https://{}/jans-auth/restv1/token'.format(self.host)

        if self.askuser:
            post_params = {"grant_type": "password", "scope": scope, "username": self.auth_username,
                           "password": self.auth_password}
        else:
            post_params = {"grant_type": "client_credentials", "scope": scope}

        response = requests.post(
            url,
            auth=(self.use_test_client, self.client_secret),
            data=post_params,
            verify=self.verify_ssl,
            cert=self.mtls_client_cert
        )
        self.log_response(response)
        try:
            result = response.json()
            if 'access_token' in result:
                self.access_token = result['access_token']
            else:
                sys.stderr.write("Error while getting access token")
                sys.stderr.write(result)
                sys.stderr.write('\n')
        except Exception as e:
            print("Error while getting access token")
            sys.stderr.write(response.text)
            sys.stderr.write(str(e))
            sys.stderr.write('\n')

    def get_device_authorization (self):
        response = requests.post(
            url='https://{}/jans-auth/restv1/device_authorization'.format(self.host),
            auth=(self.client_id, self.client_secret),
            data={'client_id': self.client_id, 'scope': 'openid+profile+email+offline_access'},
            verify=self.verify_ssl,
            cert=self.mtls_client_cert
        )
        self.log_response(response)
        if response.status_code != 200:
            raise ValueError(
                self.colored_text("Unable to get device authorization user code: {}".format(response.reason), error_color))

        return response.json()


    def get_jwt_access_token(self):


        """
        STEP 1: Get device verification code
        This fucntion requests user code from jans-auth, print result and
        waits untill verification done.
        """

        response = requests.post(
            url='https://{}/jans-auth/restv1/device_authorization'.format(self.host),
            auth=(self.client_id, self.client_secret),
            data={'client_id': self.client_id, 'scope': 'openid+profile+email+offline_access'},
            verify=self.verify_ssl,
            cert=self.mtls_client_cert
        )
        self.log_response(response)
        if response.status_code != 200:
            raise ValueError(
                self.colored_text("Unable to get device authorization user code: {}".format(response.reason), error_color))

        result = response.json()

        if 'verification_uri' in result and 'user_code' in result:

            print("Please visit verification url {} and enter user code {} in {} secods".format(
                    self.colored_text(result['verification_uri'], success_color),
                    self.colored_text(result['user_code'], bold_color),
                    result['expires_in']
                    )
                )

            input(self.colored_text("Please press «Enter» when ready", warning_color))

        else:
            raise ValueError(self.colored_text("Unable to get device authorization user code"))


        """
        STEP 2: Get access token for retreiving user info
        After device code was verified, we use it to retreive refresh token
        """
        response = requests.post(
            url='https://{}/jans-auth/restv1/token'.format(self.host),
            auth=(self.client_id, self.client_secret),
            data=[
                ('client_id',self.client_id),
                ('scope','openid+profile+email+offline_access'),
                ('grant_type', 'urn:ietf:params:oauth:grant-type:device_code'),
                ('grant_type', 'refresh_token'),
                ('device_code',result['device_code'])
                ],
             verify=self.verify_ssl,
             cert=self.mtls_client_cert
            )
        self.log_response(response)
        if response.status_code != 200:
            raise ValueError(
                self.colored_text("Unable to get access token"))

        result = response.json()

        headers_basic_auth = self.get_request_header(access_token=result['access_token'])

        """
        STEP 3: Get user info
        refresh token is used for retreiving user information to identify user roles
        """
        response = requests.post(
            url='https://{}/jans-auth/restv1/userinfo'.format(self.host),
            headers=headers_basic_auth,
            data={'access_token': result['access_token']},
            verify=self.verify_ssl,
            cert=self.mtls_client_cert
            )
        self.log_response(response)
        if response.status_code != 200:
            raise ValueError(
                self.colored_text("Unable to get access token"))

        result = response.text


        """
        STEP 4: Get access token for config-api endpoints
        Use client creditentials to retreive access token for client endpoints.
        Since introception script will be executed, access token will have permissions with all scopes
        """
        response = requests.post(
            url='https://{}/jans-auth/restv1/token'.format(self.host),
            headers=headers_basic_auth,
            data={'grant_type': 'client_credentials', 'scope': 'openid', 'ujwt': result},
            verify=self.verify_ssl,
            cert=self.mtls_client_cert
            )
        self.log_response(response)
        if response.status_code != 200:
            raise ValueError(
                self.colored_text("Unable to get access token"))

        result = response.json()

        self.access_token = result['access_token']
        access_token_enc = encode_decode(self.access_token)
        config['DEFAULT']['access_token_enc'] = access_token_enc
        write_config()


    def get_access_token(self, scope):
        if self.use_test_client:
            self.get_scoped_access_token(scope)
        elif not self.access_token:
            self.check_access_token()
            self.get_jwt_access_token()

    def print_exception(self, e):
        error_printed = False
        if hasattr(e, 'body'):
            try:
                jsdata = json.loads(e.body.decode())
                print(self.colored_text(e.body.decode(), error_color))
                error_printed = True
            except:
                pass
        if not error_printed:
            print(self.colored_text("Error retreiving data", warning_color))
            print('\u001b[38;5;196m')
            if isinstance(e, str):
                print(e)
            if hasattr(e, 'reason'):
                print(e.reason)
            if hasattr(e, 'body'):
                print(e.body)
            if hasattr(e, 'args'):
                print(', '.join(e.args))
            print('\u001b[0m')

    def colored_text(self, text, color=255):
        return u"\u001b[38;5;{}m{}\u001b[0m".format(color, text)


    def guess_bool(self, val):
        if val == '_false':
            return False
        if val == '_true':
            return True


    def check_type(self, val, vtype):
        if vtype == 'string' and val:
            return str(val)
        elif vtype == 'integer':
            if isinstance(val, int):
                return val
            if val.isnumeric():
                return int(val)
        elif vtype == 'object':
            try:
                retVal = json.loads(val)
                if isinstance(retVal, dict):
                    return retVal
            except:
                pass
        elif vtype == 'boolean':
            guessed_val = self.guess_bool(val)
            if not guessed_val is None:
                return guessed_val

        error_text = "Please enter a(n) {} value".format(vtype)
        if vtype == 'boolean':
            error_text += ': _true, _false'

        raise TypeError(self.colored_text(error_text, warning_color))

    def get_input(self, values=[], text='Selection', default=None, itype=None,
                  help_text=None, sitype=None, enforce='__true__',
                  example=None, spacing=0, iformat=None
                  ):
        if isinstance(default, str):
            default = html.escape(default)

        if 'b' in values and 'q' in values and 'x' in values:
            greyed_help_list = [ ('b', 'back'), ('q', 'quit'),  ('x', 'logout and quit') ]
            for k,v in (('w', 'write result'), ('y', 'yes'), ('n', 'no')):
                if k in values:
                    greyed_help_list.insert(1, (k, v))
            grey_help_text = ', '.join(['{}: {}'.format(k,v) for k,v in greyed_help_list])
            print(self.colored_text(grey_help_text, grey_color))
        print()
        type_text = ''
        if itype:
            if itype == 'array':
                type_text = "Type: array of {} separated by _,".format(sitype)
                if values:
                    type_text += ' Valid values: {}'.format(', '.join(values))
            elif itype == 'boolean':
                type_text = "Type: " + itype
                if default is None:
                    default = False
            else:
                type_text = "Type: " + itype
                if values:
                    type_text += ', Valid values: {}'.format(self.colored_text(', '.join(values), bold_color))

        if help_text:
            help_text = help_text.strip('.') + '. ' + type_text
        else:
            help_text = type_text

        if help_text:
            print(' ' * spacing, self.colored_text('«{}»'.format(help_text), 244), sep='')

        if example:
            if isinstance(example, list):
                example_str = ', '.join(example)
            else:
                example_str = str(example)
            print(' ' * spacing, self.colored_text('Example: {}'.format(example_str), 244), sep='')

        if not default is None:
            default_text = str(default).lower() if itype == 'boolean' else str(default)
            text += '  [<b>' + default_text + '</b>]'
            if itype == 'integer':
                default = int(default)

        if not text.endswith('?'):
            text += ':'

        if itype == 'boolean' and not values:
            values = ['_true', '_false']

        while True:

            selection = input(' ' * spacing + self.colored_text(text, 20) + ' ')

            selection = selection.strip()
            if selection.startswith('_file '):
                fname = selection.split()[1]
                if os.path.isfile(fname):
                    with open(fname) as f:
                        selection = f.read().strip()
                else:
                    print(self.colored_text("File {} does not exist".format(fname), warning_color))
                    continue

            if itype == 'boolean' and not selection:
                return False

            if not selection and default:
                return default

            if enforce and not selection:
                continue

            if not enforce and not selection:
                if itype == 'array':
                    return []
                return None

            if selection and iformat:
                if iformat == 'date-time' and not self.validate_date_time(selection):
                    print(' ' * spacing,
                              self.colored_text('Please enter date-time string, i.e. 2001-07-04T12:08:56.235', warning_color),
                              sep='')
                    continue

            if 'q' in values and selection == 'q':
                print("Quiting...")
                sys.exit()

            if 'x' in values and selection == 'x':
                print("Logging out...")
                if 'access_token_enc' in config['DEFAULT']:
                    config['DEFAULT'].pop('access_token_enc')
                    write_config()
                print("Quiting...")
                sys.exit()
                break


            if itype == 'object' and sitype:
                try:
                    object_ = self.check_type(selection, itype)
                except Exception as e:
                    print(' ' * spacing, e, sep='')
                    continue

                data_ok = True
                for items in object_:
                    try:
                        self.check_type(object_[items], sitype)
                    except Exception as e:
                        print(' ' * spacing, e, sep='')
                        data_ok = False
                if data_ok:
                    return object_
                else:
                    continue

            if itype == 'array' and default and not selection:
                return default

            if itype == 'array' and sitype:
                if selection == '_null':
                    selection = []
                    data_ok = True
                else:
                    selection = selection.split('_,')
                    for i, item in enumerate(selection):
                        data_ok = True
                        try:
                            selection[i] = self.check_type(item.strip(), sitype)
                            if selection[i] == '_null':
                                selection[i] = None
                            if values:
                                if not selection[i] in values:
                                    data_ok = False
                                    print(' ' * spacing, self.colored_text(
                                        "Please enter array of {} separated by _,".format(', '.join(values)),
                                        warning_color), sep='')
                                    break
                        except TypeError as e:
                            print(' ' * spacing, e, sep='')
                            data_ok = False
                if data_ok:
                    break
            else:
                if not itype is None:
                    try:
                        selection = self.check_type(selection, itype)
                    except TypeError as e:
                        if enforce:
                            print(' ' * spacing, e, sep='')
                            continue

                if values:
                    if selection in values:
                        break
                    elif itype == 'boolean':
                        if isinstance(selection, bool):
                            break
                        else:
                            continue
                    else:
                        print(' ' * spacing,
                              self.colored_text('Please enter one of {}'.format(', '.join(values)), warning_color),
                              sep='')

                if not values and not selection and not enforce:
                    break

                if not values and selection:
                    break

        if selection == '_null':
            selection = None
        elif selection == '_q':
            selection = 'q'

        return selection


    def print_underlined(self, text):
        print()
        print(text)
        print('-' * len(text.splitlines()[-1]))


    def pretty_print(self, data):
        pp_string = json.dumps(data, indent=2)
        if args.no_color:
            print(pp_string)
        else:
            colorful_json = highlight(pp_string, lexers.JsonLexer(), formatters.TerminalFormatter())
            print(colorful_json)

    def get_url_param(self, url):
        if url.endswith('}'):
            pname = re.findall('/\{(.*?)\}$', url)[0]
            return pname

    def get_endpiont_url_param(self, endpoint):
        param = {}
        pname = self.get_url_param(endpoint.path)
        if pname:
            param = {'name': pname, 'description': pname, 'schema': {'type': 'string'}}

        return param


    def obtain_parameters(self, endpoint, single=False):
        parameters = {}

        endpoint_parameters = []
        if 'parameters' in endpoint.info:
            endpoint_parameters = endpoint.info['parameters']

        end_point_param = self.get_endpiont_url_param(endpoint)
        if end_point_param and not end_point_param in endpoint_parameters:
            endpoint_parameters.insert(0, end_point_param)

        n = 1 if single else len(endpoint_parameters)

        for param in endpoint_parameters[0:n]:
            param_name = param['name']
            if param_name not in parameters:
                text_ = param['name']
                help_text = param.get('description') or param.get('summary')
                enforce = True if param['schema']['type'] == 'integer' or (end_point_param and end_point_param['name'] == param['name']) else False

                parameters[param_name] = self.get_input(
                    text=text_.strip('.'),
                    itype=param['schema']['type'],
                    default=param['schema'].get('default'),
                    enforce=enforce,
                    help_text=help_text,
                    example=param.get('example'),
                    values=param['schema'].get('enum', [])
                )

        return parameters


    def get_path_by_id(self, operation_id):
        retVal = {}
        for path in cfg_yml['paths']:
            for method in cfg_yml['paths'][path]:
                if 'operationId' in cfg_yml['paths'][path][method] and cfg_yml['paths'][path][method][
                    'operationId'] == operation_id:
                    retVal = cfg_yml['paths'][path][method].copy()
                    retVal['__path__'] = path
                    retVal['__method__'] = method
                    retVal['__urlsuffix__'] = self.get_url_param(path)

        return retVal


    def get_scope_for_endpoint(self, endpoint):
        scope = []
        for security in endpoint.info.get('security', []):
            for stype in security:
                scope += security[stype]

        return ' '.join(scope)


    def tabular_data(self, data, ome):
        tab_data = []
        headers = tabulate_endpoints[ome]
        for i, entry in enumerate(data):
            row_ = [i + 1]
            for header in headers:
                row_.append(str(entry.get(header, '')))
            tab_data.append(row_)

        print(tabulate(tab_data, ['#']+headers, tablefmt="grid"))


    def get_requests(self, endpoint, params={}):
        if not self.wrapped:
            sys.stderr.write("Please wait while retreiving data ...\n")

        security = self.get_scope_for_endpoint(endpoint)
        self.get_access_token(security)

        url_param_name = self.get_url_param(endpoint.path)

        url = 'https://{}{}'.format(self.host, endpoint.path)
        if params and url_param_name in params:
            url = url.format(**{url_param_name: params.pop(url_param_name)})

        if params:
            url += '?' + urlencode(params)

        response = requests.get(
            url = url,
            headers=self.get_request_header({'Accept': 'application/json'}),
            params=params,
            verify=self.verify_ssl,
            cert=self.mtls_client_cert
        )
        self.log_response(response)
        if response.status_code == 404:
            print(self.colored_text("Server returned 404", error_color))
            print(self.colored_text(response.text, error_color))
            return None

        try:
            return response.json()
            print(response.status_code)
        except Exception as e:
            print("An error ocurred while retreiving data")
            self.print_exception(e)

    def process_get(self, endpoint, return_value=False, parameters=None, noprompt=False, update=False):
        clear()
        if not return_value:
            title = endpoint.name
            if endpoint.name != endpoint.info['description'].strip('.'):
                title += '\n' + endpoint.info['description']

            self.print_underlined(title)

        if not parameters and not noprompt:
            parameters = self.obtain_parameters(endpoint, single=return_value)

            for param in parameters.copy():
                if not parameters[param]:
                    del parameters[param]

        if parameters and not return_value:
            print("Calling Api with parameters:", parameters)

        data = self.get_requests(endpoint, parameters)

        if return_value:
            return data

        selections = ['q', 'x', 'b']
        item_counters = []
        tabulated = False

        if data:
            try:
                if 'response' in data:
                    data = data['response']
            except:
                pass

            op_mode_endpoint = my_op_mode + '.' + endpoint.info['operationId']
            import copy
            if op_mode_endpoint in tabulate_endpoints:
                try:
                    data_ext = copy.deepcopy(data)
                    if endpoint.info['operationId'] == 'get-user':
                        for entry in data_ext:
                            if entry.get('customAttributes'):
                                for attrib in entry['customAttributes']:
                                    if attrib['name'] == 'mail':
                                        entry['mail'] = ', '.join(attrib['values'])
                                    elif attrib['name'] in tabulate_endpoints[op_mode_endpoint]:
                                        entry[attrib['name']] = attrib['values'][0]

                    tab_data = data_ext
                    if op_mode_endpoint in tabular_dataset:
                        tab_data = data_ext[tabular_dataset[op_mode_endpoint]]
                    self.tabular_data(tab_data, op_mode_endpoint)
                    item_counters = [str(i + 1) for i in range(len(data))]
                    tabulated = True
                except:
                    self.pretty_print(data)
            else:
                self.pretty_print(data)

        if update:
            return item_counters, data

        selections += item_counters
        while True:
            selection = self.get_input(selections)
            if selection == 'b':
                self.display_menu(endpoint.parent)
                break
            elif selection == 'w':
                fn = input('File name: ')
                try:
                    with open(fn, 'w') as w:
                        json.dump(data, w, indent=2)
                        print("Output was written to", fn)
                except Exception as e:
                    print("An error ocurred while saving data")
                    self.print_exception(e)
            elif selection in item_counters:
                self.pretty_print(data[int(selection) - 1])

    def get_schema_from_reference(self, ref):
        schema_path_list = ref.strip('/#').split('/')
        schema = cfg_yml[schema_path_list[0]]

        schema_ = schema.copy()

        for p in schema_path_list[1:]:
            schema_ = schema_[p]

        if 'allOf' in schema_:
            all_schema = OrderedDict()
            all_schema['required'] = []

            all_schema['properties'] = OrderedDict()
            for sch in schema_['allOf']:
                if '$ref' in sch:
                    all_schema.update(self.get_schema_from_reference(sch['$ref']))
                elif 'properties' in sch:
                    for sprop in sch['properties']:
                        all_schema['properties'][sprop] = sch['properties'][sprop]
                all_schema['required'] += sch.get('required', [])

            schema_ = all_schema

        for key_ in schema_.get('properties', []):
            if '$ref' in schema_['properties'][key_]:
                schema_['properties'][key_] = self.get_schema_from_reference(schema_['properties'][key_]['$ref'])
            elif schema_['properties'][key_].get('type') == 'array' and '$ref' in schema_['properties'][key_]['items']:
                ref_path = schema_['properties'][key_]['items'].pop('$ref')
                ref_schema = self.get_schema_from_reference(ref_path)
                schema_['properties'][key_]['properties'] = ref_schema['properties']
                schema_['properties'][key_]['title'] = ref_schema['title']
                schema_['properties'][key_]['description'] = ref_schema.get('description', '')
                schema_['properties'][key_]['__schema_name__'] = ref_schema['__schema_name__']

        if not 'title' in schema_:
            schema_['title'] = p

        schema_['__schema_name__'] = p

        return schema_

    def get_schema_for_endpoint(self, endpoint):
        schema_ = {}
        for content_type in endpoint.info.get('requestBody', {}).get('content', {}):
            if 'schema' in endpoint.info['requestBody']['content'][content_type]:
                schema = endpoint.info['requestBody']['content'][content_type]['schema']
                break
        else:
            return schema_

        schema_ = schema.copy()

        if schema_.get('type') == 'array':
            schma_ref = schema_.get('items', {}).pop('$ref')
        else:
            schma_ref = schema_.pop('$ref')

        if schma_ref:
            schema_ref_ = self.get_schema_from_reference(schma_ref)
            schema_.update(schema_ref_)

        return schema_


    def get_attrib_list(self):
        for parent in self.menu:
            for children in parent:
                if children.info['operationId'] == 'get-attributes':
                    attributes = self.process_get(children, return_value=True, parameters={'limit': 1000} )
                    attrib_names = []
                    for a in attributes:
                        attrib_names.append(a['name'])
                    attrib_names.sort()
                    return attrib_names

    def get_enum(self, schema):
        if schema['__schema_name__'] in self.enum_dict:
            enum_obj = schema
            
            for path in self.enum_dict[schema['__schema_name__']].copy():
                for p in path.split('.'):
                    enum_obj = enum_obj[p]

                if not 'enum' in self.enum_dict[schema['__schema_name__']][path]:
                    self.enum_dict[schema['__schema_name__']][path]['enum'] = getattr(self, self.enum_dict[schema['__schema_name__']][path]['f'])()

                enum_obj['enum'] = self.enum_dict[schema['__schema_name__']][path]['enum']


    def get_input_for_schema_(self, schema, data={}, spacing=0, getitem=None, required_only=False):

        self.get_enum(schema)

        for prop in schema['properties']:
            item = schema['properties'][prop]
            if getitem and prop != getitem['__name__'] or prop in ('dn', 'inum'):
                continue

            if (required_only and schema.get('required')) and not prop in schema.get('required'):
                continue

            
            if item['type'] == 'object' and 'properties' in item:

                print()
                data_obj = {}
                print("Data for object {}. {}".format(prop, item.get('description', '')))
                result = self.get_input_for_schema_(item, data_obj)
                data[prop] = result
                #model_name_str = item.get('__schema_name__') or item.get('title') or item.get('description')
                #model_name = self.get_name_from_string(model_name_str)

                #if initialised and getattr(model, prop_):
                #    sub_model = getattr(model, prop_)
                #    self.get_input_for_schema_(item, sub_model, spacing=3, initialised=initialised)
                #elif isinstance(model, type) and hasattr(swagger_client.models, model_name):
                #    sub_model_class = getattr(swagger_client.models, model_name)
                #    result = self.get_input_for_schema_(item, sub_model_class, spacing=3, initialised=initialised)
                #    setattr(model, prop_, result)
                #elif hasattr(swagger_client.models, model.swagger_types[prop_]):
                #    sub_model = getattr(swagger_client.models, model.swagger_types[prop_])
                #    result = self.get_input_for_schema_(item, sub_model, spacing=3, initialised=initialised)
                #    setattr(model, prop_, result)
                #else:
                #    sub_model = getattr(model, prop_)
                #    self.get_input_for_schema_(item, sub_model, spacing=3, initialised=initialised)
                #    # print(self.colored_text("Fix me: can't find model", error_color))

            elif item['type'] == 'array' and '__schema_name__' in item:

                sub_data_list = []
                sub_data_list_title_text = item.get('title') or item.get('description') or prop
                sub_data_list_selection = 'y'
                print(sub_data_list_title_text)
                while sub_data_list_selection == 'y':
                    sub_data = {}
                    self.get_input_for_schema_(item, sub_data, spacing=spacing + 3)
                    sub_data_list.append(sub_data)
                    sub_data_list_selection = self.get_input(
                        text="Add another {}?".format(sub_data_list_title_text), values=['y', 'n'])
                data[prop] = sub_data_list

            else:

                default = data.get(prop) or item.get('default')
                values_ = item.get('enum',[])

                if isinstance(default, property):
                    default = None
                enforce = True if item['type'] == 'boolean' else False

                if prop in schema.get('required', []):
                    enforce = True
                if not values_ and item['type'] == 'array' and 'enum' in item['items']:
                    values_ = item['items']['enum']
                if item['type'] == 'object' and not default:
                    default = {}

                val = self.get_input(
                    values=values_,
                    text=prop,
                    default=default,
                    itype=item['type'],
                    help_text=item.get('description'),
                    sitype=item.get('items', {}).get('type'),
                    enforce=enforce,
                    example=item.get('example'),
                    spacing=spacing,
                    iformat=item.get('format')
                )

                data[prop] = val

        return data


    def post_requests(self, endpoint, data):
        url = 'https://{}{}'.format(self.host, endpoint.path)
        security = self.get_scope_for_endpoint(endpoint)
        self.get_access_token(security)
        mime_type = self.get_mime_for_endpoint(endpoint)

        headers = self.get_request_header({'Accept': 'application/json', 'Content-Type': mime_type})

        response = requests.post(url,
            headers=headers,
            json=data,
            verify=self.verify_ssl,
            cert=self.mtls_client_cert
            )
        self.log_response(response)

        try:
            return response.json()
        except:
            self.print_exception(response.text)

    def process_post(self, endpoint):
        schema = self.get_schema_for_endpoint(endpoint)

        if schema:

            title = schema.get('description') or schema['title']
            data_dict = {}

            if my_op_mode == 'scim':
                if endpoint.path == '/jans-scim/restv1/v2/Groups':
                    schema['properties']['schemas']['default'] = ['urn:ietf:params:scim:schemas:core:2.0:Group']
                elif endpoint.path == '/jans-scim/restv1/v2/Users':
                    schema['properties']['schemas']['default'] = ['urn:ietf:params:scim:schemas:core:2.0:User']
                if endpoint.info['operationId'] == 'create-user':
                    schema['required'] = ['userName', 'name', 'displayName', 'emails', 'password']

            data = self.get_input_for_schema_(schema, data_dict, required_only=True)

            optional_fields = []
            required_fields = schema.get('required', []) + ['dn', 'inum']
            for field in schema['properties']:
                if not field in required_fields:
                    optional_fields.append(field)

            if optional_fields:
                fill_optional = self.get_input(values=['y', 'n'], text='Populate optional fields?')
                fields_numbers = []
                if fill_optional == 'y':
                    print("Optional Fields:")
                    for i, field in enumerate(optional_fields):
                        print(i + 1, field)
                        fields_numbers.append(str(i + 1))

                    while True:
                        optional_selection = self.get_input(values=['q', 'x', 'c'] + fields_numbers,
                                                            help_text="c: continue, #: populate field")
                        if optional_selection == 'c':
                            break
                        if optional_selection in fields_numbers:
                            item_name = optional_fields[int(optional_selection) - 1]
                            schema_item = schema['properties'][item_name].copy()
                            schema_item['__name__'] = item_name
                            item_data = self.get_input_for_schema_(schema, getitem=schema_item)
                            data[item_name] = item_data[item_name]

            print("Obtained Data:")
            self.pretty_print(data)

            selection = self.get_input(values=['q', 'x', 'b', 'y', 'n'], text='Continue?')

        else:
            selection = 'y'
            model = None

        if selection == 'y':
            print("Please wait while posting data ...\n")
            response = self.post_requests(endpoint, data)
            if response:
                self.pretty_print(response)

        selection = self.get_input(values=['q', 'x', 'b'])
        if selection in ('b', 'n'):
            self.display_menu(endpoint.parent)


    def delete_requests(self, endpoint, url_param_dict):
        security = self.get_scope_for_endpoint(endpoint)
        self.get_access_token(security)

        response = requests.delete(
            url='https://{}{}'.format(self.host, endpoint.path.format(**url_param_dict)),
            headers=self.get_request_header({'Accept': 'application/json'}),
            verify=self.verify_ssl,
            cert=self.mtls_client_cert
            )
        self.log_response(response)
        if response.status_code in (200, 204):
            return None

        return response.text.strip()


    def process_delete(self, endpoint):
        url_param = self.get_endpiont_url_param(endpoint)
        if url_param:
            url_param_val = self.get_input(text=url_param['name'], help_text='Entry to be deleted')
        else:
            url_param_val = ''
        selection = self.get_input(text="Are you sure want to delete {} ?".format(url_param_val),
                                   values=['b', 'y', 'n', 'q', 'x'])
        if selection in ('b', 'n'):
            self.display_menu(endpoint.parent)
        elif selection == 'y':
            print("Please wait while deleting {} ...\n".format(url_param_val))
            
            url_param_dict = {url_param['name']: url_param_val}

            response = self.delete_requests(endpoint, url_param_dict)

            if response is None:
                print(self.colored_text("\nEntry {} was deleted successfully\n".format(url_param_val), success_color))
            else:
                print(self.colored_text("An error ocurred while deleting entry:", error_color))
                print(self.colored_text(response, error_color))

        selection = self.get_input(['b', 'q', 'x'])
        if selection == 'b':
            self.display_menu(endpoint.parent)

    def patch_requests(self, endpoint, url_param_dict, data):
        url = 'https://{}{}'.format(self.host, endpoint.path.format(**url_param_dict))
        security = self.get_scope_for_endpoint(endpoint)
        self.get_access_token(security)

        headers = self.get_request_header({'Accept': 'application/json', 'Content-Type': 'application/json-patch+json'})
        data = data
        response = requests.patch(
            url=url,
            headers=headers,
            json=data,
            verify=self.verify_ssl,
            cert=self.mtls_client_cert
            )
        self.log_response(response)
        try:
            return response.json()
        except:
            self.print_exception(response.text)


    def process_patch(self, endpoint):

        schema = self.get_schema_for_endpoint(endpoint)

        if 'PatchOperation' in cfg_yml['components']['schemas']:
            schema = cfg_yml['components']['schemas']['PatchOperation'].copy()
            schema['__schema_name__'] = 'PatchOperation'
        else:
            schema = cfg_yml['components']['schemas']['PatchRequest'].copy()
            schema['__schema_name__'] = 'PatchRequest'


        for item in schema['properties']:
            print("d-checking", item)
            if not 'type' in schema['properties'][item] or schema['properties'][item]['type']=='object':
                schema['properties'][item]['type'] = 'string'

        url_param_dict = {}
        url_param_val = None
        url_param = self.get_endpiont_url_param(endpoint)
        if 'name' in url_param:
            url_param_val = self.get_input(text=url_param['name'], help_text='Entry to be patched')
            url_param_dict = {url_param['name']: url_param_val}
    
        body = []

        while True:
            data = self.get_input_for_schema_(schema)
            #guessed_val = self.guess_bool(data.value)
            #if not guessed_val is None:
            #    data.value = guessed_val
            if my_op_mode != 'scim' and not data['path'].startswith('/'):
                data['path'] = '/' + data['path']

            if my_op_mode == 'scim':
                data['path'] = data['path'].replace('/', '.')

            body.append(data)
            selection = self.get_input(text='Another patch operation?', values=['y', 'n'])
            if selection == 'n':
                break

        if endpoint.info['operationId'] == 'patch-oauth-uma-resources-by-id':
            for patch_item in body:
                if patch_item['path'] == '/clients':
                    patch_item['value'] = patch_item['value'].split('_,')

        self.pretty_print(body)

        selection = self.get_input(values=['y', 'n'], text='Continue?')

        if selection == 'y':

            if my_op_mode == 'scim':
                body = {'schemas': ['urn:ietf:params:scim:api:messages:2.0:PatchOp'], 'Operations': body}

            self.patch_requests(endpoint, url_param_dict, body)

        selection = self.get_input(['b'])
        if selection == 'b':
            self.display_menu(endpoint.parent)

    def get_mime_for_endpoint(self, endpoint, req='requestBody'):
        for key in endpoint.info[req]['content']:
            return key


    def put_requests(self, endpoint, data):

        security = self.get_scope_for_endpoint(endpoint)
        self.get_access_token(security)

        mime_type = self.get_mime_for_endpoint(endpoint)

        response = requests.put(
                url='https://{}{}'.format(self.host, endpoint.path),
                headers=self.get_request_header({'Accept': mime_type}),
                json=data,
                verify=self.verify_ssl,
                cert=self.mtls_client_cert
            )
        self.log_response(response)
        try:
            result = response.json()
        except Exception:
            self.exit_with_error(response.text)

        return result


    def process_put(self, endpoint):

        schema = self.get_schema_for_endpoint(endpoint)

        cur_data = None
        go_back = False

        if endpoint.info.get('x-cli-getdata') != '_file':
            if 'x-cli-getdata' in endpoint.info and endpoint.info['x-cli-getdata'] != None:
                for m in endpoint.parent:
                    if m.info['operationId'] == endpoint.info['x-cli-getdata']:
                        while True:
                            try:
                                print("cur_data-1")
                                cur_data = self.process_get(m, return_value=True)
                                break
                            except ValueError as e:
                                retry = self.get_input(values=['y', 'n'], text='Retry?')
                                if retry == 'n':
                                    self.display_menu(endpoint.parent)
                                    break
                        get_endpoint = m
                        break

            else:
                for mi in endpoint.parent :
                    if mi.method == 'get' and not mi.path.endswith('}'):
                        cur_data = self.process_get(mi, noprompt=True, update=True)
                        values = ['b', 'q', 'x'] + cur_data[0]
                        item_number = self.get_input(text="Enter item # to update", values=values)

                        cur_data = cur_data[1][int(item_number) -1]
                """
                for m in endpoint.parent:
                    if m.method == 'get' and m.path.endswith('}'):
                        while True:
                            while True:
                                try:
                                    
                                    key_name_desc = self.get_endpiont_url_param(m)
                                    if key_name_desc and 'name' in key_name_desc:
                                        key_name = key_name_desc['name']
                                    print("P-X", m)
                                    cur_data = self.process_get(m, return_value=True)
                                    break
                                except ValueError as e:
                                    retry = self.get_input(values=['y', 'n'], text='Retry?')
                                    if retry == 'n':
                                        self.display_menu(endpoint.parent)
                                        break

                            if cur_data is not None:
                                break

                        get_endpoint = m
                        break
                """
            if not cur_data:
                for m in endpoint.parent:
                    if m.method == 'get' and not m.path.endswith('}'):
                        cur_data = self.process_get(m, return_value=True)
                        get_endpoint = m

        end_point_param = self.get_endpiont_url_param(endpoint)

        if endpoint.info.get('x-cli-getdata') == '_file':

            # TODO: To be implemented
            schema_desc = schema.get('description') or schema['__schema_name__']
            text = 'Enter filename to load data for «{}»: '.format(schema_desc)
            data_fn = input(self.colored_text(text, 244))
            if data_fn == 'b':
                go_back = True
            elif data_fn == 'q':
                sys.exit()
            else:
                data_org = self.get_json_from_file(data_fn)

            data = {}
            for k in data_org:
                if k in cur_model.attribute_map:
                    mapped_key = cur_model.attribute_map[k]
                    data[mapped_key] = data_org[k]
                else:
                    data[k] = data_org[k]

            print("Please wait while posting data ...\n")

            response = self.put_requests(endpoint, cur_data)

            if response:
                self.pretty_print(response)

            selection = self.get_input(values=['q', 'x', 'b'])
            if selection == 'b':
                self.display_menu(endpoint.parent)

        else:

            end_point_param_val = None
            if end_point_param and end_point_param['name'] in cur_data:
                end_point_param_val = cur_data[end_point_param['name']]

            schema = self.get_schema_for_endpoint(endpoint)
            if schema['properties'].get('keys', {}).get('properties'):
                schema = schema['properties']['keys']


            attr_name_list = list(schema['properties'].keys())
            if 'dn' in attr_name_list:
                attr_name_list.remove('dn')

            attr_name_list.sort()
            item_numbers = []

            def print_fields():
                print("Fields:")
                for i, attr_name in enumerate(attr_name_list):
                    print(str(i + 1).rjust(2), attr_name)
                    item_numbers.append(str(i + 1))

            print_fields()
            changed_items = []
            selection_list = ['q', 'x', 'b', 'v', 's', 'l'] + item_numbers
            help_text = 'q: quit, v: view, s: save, l: list fields #: update field'

            while True:
                selection = self.get_input(values=selection_list, help_text=help_text)
                if selection == 'v':
                    self.pretty_print(cur_data)
                elif selection == 'l':
                    print_fields()
                elif selection in item_numbers:
                    item = attr_name_list[int(selection) - 1]

                    schema_item = schema['properties'][item]
                    schema_item['__name__'] = item
                    self.get_input_for_schema_(schema, data=cur_data, getitem=schema_item)
                    changed_items.append(item)

                if selection == 'b':
                    self.display_menu(endpoint.parent)
                    break
                elif selection == 's':
                    print('Changes:')
                    for ci in changed_items:
                        str_val = str(cur_data[ci])
                        print(self.colored_text(ci, bold_color) + ':', self.colored_text(str_val, success_color))

                    selection = self.get_input(values=['y', 'n'], text='Continue?')

                    if selection == 'y':
                        print("Please wait while posting data ...\n")
                        put_pname = self.get_url_param(endpoint.path)

                        response = self.put_requests(endpoint, cur_data)

                        if response:
                            self.pretty_print(response)
                            go_back = True
                            break

            if go_back:
                selection = self.get_input(values=['q', 'x', 'b'])
                if selection == 'b':
                    self.display_menu(endpoint.parent)
            else:
                self.get_input_for_schema_(schema, data=cur_data)

    def display_menu(self, menu):
        clear()
        self.current_menu = menu

        name_list = [menu.name]
        par = menu
        while True:
            par = par.parent
            if not par:
                break
            name_list.insert(0, par.name)

        if len(name_list) > 1:
            del name_list[0]

        self.print_underlined(': '.join(name_list))

        selection_values = ['q', 'x', 'b']

        menu_numbering = {}

        c = 0
        for i, item in enumerate(menu):
            if item.info.get('x-cli-ignore') or (item.parent.name == 'Main Menu' and not item.children):
                continue

            print(c + 1, item)
            selection_values.append(str(c + 1))
            menu_numbering[c + 1] = i
            c += 1

        selection = self.get_input(selection_values)

        if selection == 'b' and not menu.parent:
            print("Quiting...")
            sys.exit()
        elif selection == 'b':
            self.display_menu(menu.parent)
        elif int(selection) in menu_numbering and menu.get_child(menu_numbering[int(selection)]).children:
            self.display_menu(menu.get_child(menu_numbering[int(selection)]))
        else:
            m = menu.get_child(menu_numbering[int(selection)])
            getattr(self, 'process_' + m.method)(m)

    def parse_command_args(self, args):
        args_dict = {}

        if args:
            for arg in args.split(','):
                neq = arg.find(':')
                if neq > 1:
                    arg_name = arg[:neq].strip()
                    arg_val = arg[neq + 1:].strip()
                    if arg_name and arg_val:
                        args_dict[arg_name] = arg_val

        return args_dict

    def parse_args(self, args, path):
        param_names = []
        if not 'parameters' in path:
            return {}
        for param in path['parameters']:
            param_names.append(param['name'])

        args_dict = self.parse_command_args(args)

        for arg_name in args_dict:
            if not arg_name in param_names:
                self.exit_with_error("valid endpoint args are: {}".format(', '.join(param_names)))

        return args_dict

    def help_for(self, op_name):

        schema_path = None

        for path_name in cfg_yml['paths']:
            for method in cfg_yml['paths'][path_name]:
                path = cfg_yml['paths'][path_name][method]
                if isinstance(path, dict):
                    for tag_ in path['tags']:
                        tag = get_named_tag(tag_)
                        if tag != op_name:
                            continue

                        print('Operation ID:', path['operationId'])
                        print('  Description:', path['description'])
                        if path.get('__urlsuffix__'):
                            print('  url-suffix:', path['__urlsuffix__'])
                        if 'parameters' in path:
                            param_names = []
                            for param in path['parameters']:
                                desc = param.get('description', 'No description is provided for this parameter')
                                param_type = param.get('schema', {}).get('type')
                                if param_type:
                                    desc += ' [{}]'.format(param_type)
                                param_names.append((param['name'], desc))
                            if param_names:
                                print('  Parameters:')
                                for param in param_names:
                                    print('  {}: {}'.format(param[0], param[1]))

                        if 'requestBody' in path:
                            for apptype in path['requestBody'].get('content', {}):
                                if 'schema' in path['requestBody']['content'][apptype]:
                                    if path['requestBody']['content'][apptype]['schema'].get('type') == 'array':
                                        schema_path = path['requestBody']['content'][apptype]['schema']['items']['$ref']
                                        print('  Schema: Array of {}'.format(schema_path[1:]))
                                    else:
                                        schema_path = path['requestBody']['content'][apptype]['schema']['$ref']
                                        print('  Schema: {}'.format(schema_path[1:]))

        if schema_path:
            print()
            print("To get sample schema type {0} --schema <schma>, for example {0} --schema {1}".format(sys.argv[0],
                                                                                                        schema_path[
                                                                                                        1:]))

    def render_json_entry(self, val):
        if isinstance(val, str) and val.startswith('_file '):
            file_path = val[6:].strip()
            if os.path.exists(file_path):
                with open(file_path) as f:
                    val = f.read()
            else:
                raise ValueError("File '{}' not found".format(file_path))
        return val

    def get_json_from_file(self, data_fn):

        if not os.path.exists(data_fn):
            self.exit_with_error("Can't find file {}".format(data_fn))

        try:
            with open(data_fn) as f:
                data = json.load(f)
        except:
            self.exit_with_error("Error parsing json file {}".format(data_fn))

        if isinstance(data, list):
            for entry in data:
                if isinstance(entry, dict):
                    for k in entry:
                        entry[k] = self.render_json_entry(entry[k])

        if isinstance(data, dict):
            for k in data:
                data[k] = self.render_json_entry(data[k])

        return data


    def process_command_get(self, path, suffix_param, endpoint_params, data_fn, data=None):
        endpoint = self.get_fake_endpoint(path)
        response = self.get_requests(endpoint, endpoint_params)
        if not self.wrapped:
            self.pretty_print(response)
        else:
            return response

    def exit_with_error(self, error_text):
        error_text += '\n'
        sys.stderr.write(self.colored_text(error_text, error_color))
        print()
        sys.exit()


    def get_fake_endpoint(self, path):
        endpoint = SimpleNamespace()
        endpoint.path = path['__path__']
        endpoint.info = path
        return endpoint


    def print_response(self, response):
        if response:
            sys.stderr.write("Server Response:\n")
            self.pretty_print(response)

    def process_command_post(self, path, suffix_param, endpoint_params, data_fn, data):

        # TODO: suffix_param, endpoint_params

        endpoint = self.get_fake_endpoint(path)

        if not data:

            if data_fn.endswith('jwt'):
                with open(data_fn) as reader:
                    data = jwt.decode(reader.read(),
                                          options={"verify_signature": False, "verify_exp": False, "verify_aud": False})
            else:
                try:
                    data = self.get_json_from_file(data_fn)
                except ValueError as ve:
                    self.exit_with_error(str(ve))

        if path['__method__'] == 'post':
            response = self.post_requests(endpoint, data)
        elif path['__method__'] == 'put':
            response = self.put_requests(endpoint, data)

        self.print_response(response)

    def process_command_put(self, path, suffix_param, endpoint_params, data_fn, data=None):
        self.process_command_post(path, suffix_param, endpoint_params, data_fn, data=None)

    def process_command_patch(self, path, suffix_param, endpoint_params, data_fn, data=None):
        # TODO: suffix_param, endpoint_params

        endpoint = self.get_fake_endpoint(path)
        
        if not data:
            try:
                data = self.get_json_from_file(data_fn)
            except ValueError as ve:
                self.exit_with_error(str(ve))

            if not isinstance(data, list):
                self.exit_with_error("{} must be array of /components/schemas/PatchRequest".format(data_fn))

        op_modes = ('add', 'remove', 'replace', 'move', 'copy', 'test')

        for item in data:
            if not item['op'] in op_modes:
                print("op must be one of {}".format(', '.join(op_modes)))
                sys.exit()
            if not item['path'].startswith('/'):
                item['path'] = '/' + item['path']

        response = self.patch_requests(endpoint, suffix_param, data)

        self.print_response(response)


    def process_command_delete(self, path, suffix_param, endpoint_params, data_fn, data=None):
        endpoint = self.get_fake_endpoint(path)
        response = self.delete_requests(endpoint, suffix_param)
        if response:
            self.print_response(response)
        else:
            print(self.colored_text("Object was successfully deleted.", success_color))

    def process_command_by_id(self, operation_id, url_suffix, endpoint_args, data_fn, data=None):
        path = self.get_path_by_id(operation_id)

        if not path:
            self.exit_with_error("No such Operation ID")

        suffix_param = self.parse_command_args(url_suffix)
        endpoint_params = self.parse_command_args(endpoint_args)

        if path.get('__urlsuffix__') and not path['__urlsuffix__'] in suffix_param:
            self.exit_with_error("This operation requires a value for url-suffix {}".format(path['__urlsuffix__']))

        endpoint = Menu('', info=path)
        schema = self.get_schema_for_endpoint(endpoint)

        if not data:
            op_path = self.get_path_by_id(operation_id)
            if op_path['__method__'] == 'patch' and not data_fn:
                pop, pdata = '', ''
                if args.patch_add:
                    pop = 'add'
                    pdata = args.patch_add 
                elif args.patch_remove:
                    pop = 'remove'
                    pdata = args.patch_remove
                elif args.patch_replace:
                    pop = 'replace'
                    pdata = args.patch_replace

                if pop:
                    if pop != 'remove' and pdata.count(':') != 1:
                        self.exit_with_error("Please provide --patch-data as colon delimited key:value pair")

                    if pop != 'remove':
                        ppath, pval = pdata.split(':')
                        data = [{'op': pop, 'path': '/'+ ppath.lstrip('/'), 'value': pval}]
                    else:
                        data = [{'op': pop, 'path': '/'+ pdata.lstrip('/')}]

        if (schema and not data_fn) and not data:
            self.exit_with_error("Please provide schema with --data argument")

        caller_function = getattr(self, 'process_command_' + path['__method__'])
        return caller_function(path, suffix_param, endpoint_params, data_fn, data=data)


    def get_sample_schema(self, ref):
        schema = self.get_schema_from_reference('#' + args.schema)
        sample_schema = OrderedDict()
        for prop_name in schema.get('properties', {}):
            prop = schema['properties'][prop_name]
            if 'default' in prop:
                sample_schema[prop_name] = prop['default']
            elif 'example' in prop:
                sample_schema[prop_name] = prop['example']
            elif 'enum' in prop:
                sample_schema[prop_name] = random.choice(prop['enum'])
            elif prop.get('type') == 'object':
                sample_schema[prop_name] = {}
            elif prop.get('type') == 'array':
                if 'items' in prop:
                    if 'enum' in prop['items']:
                        sample_schema[prop_name] = [random.choice(prop['items']['enum'])]
                    elif 'type' in prop['items']:
                        sample_schema[prop_name] = [prop['items']['type']]
                else:
                    sample_schema[prop_name] = []
            elif prop.get('type') == 'boolean':
                sample_schema[prop_name] = random.choice((True, False))
            elif prop.get('type') == 'integer':
                sample_schema[prop_name] = random.randint(1,200)
            else:
                sample_schema[prop_name]='string'

        print(json.dumps(sample_schema, indent=2))

    def runApp(self):
        clear()
        self.display_menu(self.menu)


def main():


    error_log_file = os.path.join(log_dir, 'cli_eorror.log')
    cli_object = JCA_CLI(host, client_id, client_secret, access_token, test_client)

    if not os.path.exists(log_dir):
        os.makedirs(log_dir)

    try:
        if not access_token:
            cli_object.check_connection()
        if not (args.operation_id or args.info or args.schema):
            # reset previous color
            print('\033[0m', end='')
            cli_object.runApp()
        else:
            print()
            if args.info:
                cli_object.help_for(args.info)
            elif args.schema:
                cli_object.get_sample_schema(args.schema)
            elif args.operation_id:
                cli_object.process_command_by_id(args.operation_id, args.url_suffix, args.endpoint_args, args.data)
            print()
    except Exception as e:
        if os.environ.get('errstdout'):
            print(traceback.print_exc())
        print(u"\u001b[38;5;{}mAn Unhandled error raised: {}\u001b[0m".format(error_color, e))
        with open(error_log_file, 'a') as w:
            traceback.print_exc(file=w)
        print("Error is logged to {}".format(error_log_file))


if __name__ == "__main__":
    main()
