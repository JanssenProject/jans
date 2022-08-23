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

try:
    import jwt
except ModuleNotFoundError:
    from pylib import jwt


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

        if 'access_token' in config['DEFAULT']:
            access_token = config['DEFAULT']['access_token']
        elif 'access_token_enc' in config['DEFAULT']:
            access_token = encode_decode(config['DEFAULT']['access_token_enc'], decode=True)

        debug = config['DEFAULT'].get('debug')
        log_dir = config['DEFAULT'].get('log_dir', log_dir)


def get_bool(val):
    if str(val).lower() in ('yes', 'true', '1', 'on'):
        return True
    return False

def write_config():
    with open(config_ini_fn, 'w') as w:
        config.write(w)

debug = get_bool(debug)


class JCA_CLI:

    def __init__(self, host, client_id, client_secret, access_token, test_client=False):
        self.host = self.idp_host = host
        self.client_id = client_id
        self.client_secret = client_secret
        self.use_test_client = test_client
        self.getCredentials()
        self.wrapped = __name__ != "__main__"
        self.access_token = access_token or config['DEFAULT'].get('access_token')
        self.jwt_validation_url = 'https://{}/jans-config-api/api/v1/acrs'.format(self.idp_host)
        self.set_user()
        self.plugins()

        if my_op_mode == 'scim':
            self.host += '/jans-scim/restv1/v2'

        self.set_logging()
        self.ssl_settings()

    def getCredentials(self): 
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
        try:

            response = requests.post(
                    url=url,
                    auth=(self.client_id, self.client_secret),
                    data={"grant_type": "client_credentials"},
                    verify=self.verify_ssl,
                    cert=self.mtls_client_cert
                )
        except Exception as e:
            if self.wrapped:
                return str(e)

            raise ValueError(
                self.colored_text("Unable to connect jans-auth server:\n {}".format(str(e)), error_color))


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


    def get_device_verification_code(self):
        response = requests.post(
            url='https://{}/jans-auth/restv1/device_authorization'.format(self.host),
            auth=(self.client_id, self.client_secret),
            data={'client_id': self.client_id, 'scope': 'openid+profile+email+offline_access'},
            verify=self.verify_ssl,
            cert=self.mtls_client_cert
        )

        self.log_response(response)

        return response


    def raise_error(self, msg):
        if not self.wrapped:
            msg = self.colored_text(msg, error_color)
        raise ValueError(msg)


    def get_jwt_access_token(self, device_verified=None):


        """
        STEP 1: Get device verification code
        This fucntion requests user code from jans-auth, print result and
        waits untill verification done.
        """
        if not device_verified:
            response = self.get_device_verification_code()
            if response.status_code != 200:
                msg = "Unable to get device authorization user code: {}".format(response.reason)
                self.raise_error(msg)

            result = response.json()

            if 'verification_uri' in result and 'user_code' in result:

                msg = "Please visit verification url {} and enter user code {} in {} secods".format(
                        self.colored_text(result['verification_uri'], success_color),
                        self.colored_text(result['user_code'], bold_color),
                        result['expires_in']
                        )
                print(msg)
                input(self.colored_text("Please press «Enter» when ready", warning_color))

            else:
                msg = "Unable to get device authorization user code"
                self.raise_error(msg)

        else:
            result = device_verified

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
            self.raise_error("Unable to get access token")

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
            self.raise_error("Unable to get access token")

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
            self.raise_error("Unable to get access token")

        result = response.json()

        self.access_token = result['access_token']
        access_token_enc = encode_decode(self.access_token)
        config['DEFAULT']['access_token_enc'] = access_token_enc
        write_config()

        return True, ''

    def get_access_token(self, scope):
        if self.use_test_client:
            self.get_scoped_access_token(scope)
        elif not self.access_token:
            self.check_access_token()
            self.get_jwt_access_token()
        return True, ''

    def print_exception(self, e):
        error_printed = False
        if hasattr(e, 'body'):
            try:
                jsdata = json.loads(e.body.decode())
                self.raise_error(e.body.decode())
                error_printed = True
            except:
                pass
        if not error_printed:
            msg = "Error retreiving data: "
            err = 'None'
            if isinstance(e, str):
                err = e
            if hasattr(e, 'reason'):
                err = e.reason
            if hasattr(e, 'body'):
                err = e.body
            if hasattr(e, 'args'):
                err = ', '.join(e.args)

            self.raise_error(msg + str(err))

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
        
        if self.wrapped:
            return response
        
        if response.status_code in (404, 401):
            print(self.colored_text("Server returned {}".format(response.status_code), error_color))
            print(self.colored_text(response.text, error_color))
            return None

        try:
            return response.json()
            print(response.status_code)
        except Exception as e:
            print("An error ocurred while retreiving data")
            self.print_exception(e)

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

    def get_mime_for_endpoint(self, endpoint, req='requestBody'):
        for key in endpoint.info[req]['content']:
            return key

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

        if self.wrapped:
            return response

        try:
            return response.json()
        except:
            print(response.text)


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

        if self.wrapped:
            return response

        try:
            result = response.json()
        except Exception:
            self.exit_with_error(response.text)

        return result


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

        if self.wrapped:
            return response

        self.print_response(response)

    def process_command_put(self, path, suffix_param, endpoint_params, data_fn, data=None):
        return self.process_command_post(path, suffix_param, endpoint_params, data_fn, data)

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



def main():

    
    error_log_file = os.path.join(log_dir, 'cli_eorror.log')
    cli_object = JCA_CLI(host, client_id, client_secret, access_token, test_client)

    if not os.path.exists(log_dir):
        os.makedirs(log_dir)

    if 1:
    #try:
        if not access_token:
            cli_object.check_connection()
        else:
            if args.info:
                cli_object.help_for(args.info)
            elif args.schema:
                cli_object.get_sample_schema(args.schema)
            elif args.operation_id:
                cli_object.process_command_by_id(args.operation_id, args.url_suffix, args.endpoint_args, args.data)
            print()

    #except Exception as e:
    #    print(u"\u001b[38;5;{}mAn Unhandled error raised: {}\u001b[0m".format(error_color, e))
    #    with open(error_log_file, 'a') as w:
    #        traceback.print_exc(file=w)
    #    print("Error is logged to {}".format(error_log_file))


if __name__ == "__main__":
    main()
