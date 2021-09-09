#!/usr/bin/env python3

import sys
import os
import json
import re
import urllib3
import configparser
import argparse
import inspect
import random
import datetime
import ruamel.yaml
import importlib
import code
import traceback
import ast

import pprint
from pathlib import Path
from urllib.parse import urlencode
from collections import OrderedDict

home_dir = Path.home()
config_dir = home_dir.joinpath('.config')
config_dir.mkdir(parents=True, exist_ok=True)
config_ini_fn = config_dir.joinpath('jans-cli.ini')
cur_dir = os.path.dirname(os.path.realpath(__file__))
sys.path.append(cur_dir)

from pylib.tabulate.tabulate import tabulate
from pylib import jwt

tabulate_endpoints = {
    'jca.get-config-scripts': ['scriptType', 'name', 'enabled', 'inum'],
}

my_op_mode = 'scim' if 'scim' in os.path.basename(sys.argv[0]) else 'jca'
sys.path.append(os.path.join(cur_dir, my_op_mode))
swagger_client = importlib.import_module(my_op_mode + '.swagger_client')
swagger_client.models = importlib.import_module(my_op_mode + '.swagger_client.models')
swagger_client.api = importlib.import_module(my_op_mode + '.swagger_client.api')
swagger_client.rest = importlib.import_module(my_op_mode + '.swagger_client.rest')

warning_color = 214
error_color = 196
success_color = 10
bold_color = 15
grey_color = 242

clear = lambda: os.system('clear')
urllib3.disable_warnings()
config = configparser.ConfigParser()

host = os.environ.get('jans_host')
client_id = os.environ.get(my_op_mode + '_client_id')
client_secret = os.environ.get(my_op_mode + '_client_secret')
debug = os.environ.get('jans_client_debug')
debug_log_file = os.environ.get('jans_debug_log_file')
error_log_file = os.path.join(cur_dir, 'error.log')


# dummy api class to reach private ApiClient methods
class MyApiClient(swagger_client.ApiClient):
    pass


class DummyPool:
    def close(self):
        pass

    def join(self):
        pass


myapi = MyApiClient()
myapi.pool = DummyPool()

##################### arguments #####################
op_list = []

for api_name in dir(swagger_client.api):
    if api_name.endswith('Api') and inspect.isclass(getattr(swagger_client.api, api_name)):
        op_list.append(api_name[:-3])

parser = argparse.ArgumentParser()
parser.add_argument("--host", help="Hostname of server")
parser.add_argument("--client-id", help="Jans Config Api Client ID")
parser.add_argument("--client_secret", help="Jans Config Api Client ID secret")
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

parser.add_argument("--username", help="Auth username")
parser.add_argument("--password", help="Auth password")
parser.add_argument("-j", help="Auth password file")

parser.add_argument("-CC", "--config-api-mtls-client-cert", help="Path to SSL Certificate file")
parser.add_argument("-CK", "--config-api-mtls-client-key", help="Path to SSL Key file")
parser.add_argument("-noverify", help="Ignore verifying the SSL certificate", action='store_true', default=True)

parser.add_argument("--patch-add", help="Colon delimited key:value pair for add patch operation. For example loggingLevel:DEBUG")
parser.add_argument("--patch-replace", help="Colon delimited key:value pair for replace patch operation. For example loggingLevel:DEBUG")
parser.add_argument("--patch-remove", help="Key for remove patch operation. For example imgLocation")

# parser.add_argument("-show-data-type", help="Show data type in schema query", action='store_true')
parser.add_argument("--data", help="Path to json data file")
args = parser.parse_args()

################## end of arguments #################

if not (host and client_id and client_secret):
    host = args.host
    client_id = args.client_id
    client_secret = args.client_secret
    debug = args.debug
    debug_log_file = args.debug_log_file

if not (host and client_id and client_secret):
    if config_ini_fn.exists():
        config.read_string(config_ini_fn.read_text())
        host = config['DEFAULT']['jans_host']
        client_id = config['DEFAULT'][my_op_mode + '_client_id']
        if config['DEFAULT'].get(my_op_mode + '_client_secret'):
            client_secret = config['DEFAULT'][my_op_mode + '_client_secret']
        elif config['DEFAULT'].get(my_op_mode + '_client_secret_enc'):
            client_secret_enc = config['DEFAULT'][my_op_mode + '_client_secret_enc']
            client_secret = os.popen('/opt/jans/bin/encode.py -D ' + client_secret_enc).read().strip()
        debug = config['DEFAULT'].get('debug')
        debug_log_file = config['DEFAULT'].get('debug_log_file')
    else:
        config['DEFAULT'] = {'jans_host': 'jans server hostname,e.g, jans.foo.net',
                             'jca_client_id': 'your jans config api client id',
                             'jca_client_secret': 'client secret for your jans config api client',
                             'scim_client_id': 'your jans scim client id',
                             'scim_client_secret': 'client secret for your jans scim client'}

        config.write(config_ini_fn.open('w'))

        print(
            "Pelase fill {} or set environmental variables jans_host, jans_client_id ,and jans_client_secret and re-run".format(config_ini_fn)
            )
        sys.exit()


def get_bool(val):
    if str(val).lower() in ('yes', 'true', '1', 'on'):
        return True
    return False


debug = get_bool(debug)


class Menu(object):

    def __init__(self, name, method='', info={}, path=''):
        self.name = name
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
        return self.name
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

    def __init__(self, host, client_id, client_secret):
        self.host = host
        self.client_id = client_id
        self.client_secret = client_secret
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

        self.swagger_configuration = swagger_client.Configuration()
        self.swagger_configuration.host = 'https://{}'.format(self.host)
        if my_op_mode == 'scim':
            self.swagger_configuration.host += '/jans-scim/restv1/v2'

        if args.noverify:
            self.swagger_configuration.verify_ssl = False
        else:
            self.swagger_configuration.verify_ssl = True

        if args.config_api_mtls_client_cert:
            self.swagger_configuration.cert_file = args.config_api_mtls_client_cert

        if args.config_api_mtls_client_key:
            self.swagger_configuration.key_file = args.config_api_mtls_client_key

        self.swagger_configuration.debug = debug
        if self.swagger_configuration.debug:
            self.swagger_configuration.logger_file = debug_log_file

        self.swagger_yaml_fn = os.path.join(cur_dir, my_op_mode + '.yaml')

        self.cfg_yml = self.get_yaml()
        self.make_menu()
        self.current_menu = self.menu

    def drop_to_shell(self, mylocals):
        locals_ = locals()
        locals_.update(mylocals)
        code.interact(local=locals_)
        sys.exit()

    def get_yaml(self):
        debug_json = 'swagger_yaml.json'
        if os.path.exists(debug_json):
            with open(debug_json) as f:
                return json.load(f, object_pairs_hook=OrderedDict)

        with open(self.swagger_yaml_fn) as f:
            self.cfg_yml = ruamel.yaml.load(f.read().replace('\t', ''), ruamel.yaml.RoundTripLoader)
            if os.environ.get('dump_yaml'):
                with open(debug_json, 'w') as w:
                    json.dump(self.cfg_yml, w, indent=2)
        return self.cfg_yml

    def check_connection(self):
        rest = swagger_client.rest.RESTClientObject(self.swagger_configuration)
        headers = urllib3.make_headers(basic_auth='{}:{}'.format(self.client_id, self.client_secret))
        url = 'https://{}/jans-auth/restv1/token'.format(self.host)
        headers['Content-Type'] = 'application/x-www-form-urlencoded'

        response = rest.POST(
            url,
            headers=headers,
            post_params={"grant_type": "client_credentials"}
        )

        if response.status != 200:
            raise ValueError(
                self.colored_text("Unable to connect jans-auth server: {}".format(response.reason), error_color))

    def make_menu(self):

        menu = Menu('Main Menu')

        for tag in self.cfg_yml['tags']:
            if tag['name'] != 'developers':
                m = Menu(name=tag['name'])
                menu.add_child(m)
                for path in self.cfg_yml['paths']:
                    for method in self.cfg_yml['paths'][path]:
                        if 'tags' in self.cfg_yml['paths'][path][method] and m.name in \
                                self.cfg_yml['paths'][path][method]['tags'] and 'operationId' in \
                                self.cfg_yml['paths'][path][method]:
                            # if isinstance(self.cfg_yml['paths'][path][method], dict) and self.cfg_yml['paths'][path][method].get('x-cli-ignore'):
                            #    continue
                            menu_name = self.cfg_yml['paths'][path][method].get('summary') or \
                                        self.cfg_yml['paths'][path][method].get('description')

                            sm = Menu(
                                name=menu_name.strip('.'),
                                method=method,
                                info=self.cfg_yml['paths'][path][method],
                                path=path,
                            )

                            m.add_child(sm)

        self.menu = menu

    def get_access_token(self, scope):
        sys.stderr.write("Getting access token for scope {}\n".format(scope))
        rest = swagger_client.rest.RESTClientObject(self.swagger_configuration)
        headers = urllib3.make_headers(basic_auth='{}:{}'.format(self.client_id, self.client_secret))
        url = 'https://{}/jans-auth/restv1/token'.format(self.host)
        headers['Content-Type'] = 'application/x-www-form-urlencoded'
        if self.askuser:
            post_params = {"grant_type": "password", "scope": scope, "username": self.auth_username,
                           "password": self.auth_password}
        else:
            post_params = {"grant_type": "client_credentials", "scope": scope}

        response = rest.POST(
            url,
            headers=headers,
            post_params=post_params
        )

        try:
            data = json.loads(response.data)
            if 'access_token' in data:
                self.swagger_configuration.access_token = data['access_token']
            else:
                sys.stderr.write("Error while getting access token")
                sys.stderr.write(data)
                sys.stderr.write('\n')
        except:
            print("Error while getting access token")
            sys.stderr.write(response.data)
            sys.stderr.write('\n')

    def print_exception(self, e):
        print(self.colored_text("Error retreiving data", warning_color))
        print('\u001b[38;5;196m')
        if hasattr(e, 'reason'):
            print(e.reason)
        if hasattr(e, 'body'):
            print(e.body)
        if hasattr(e, 'args'):
            print(', '.join(e.args))
        print('\u001b[0m')

    def colored_text(self, text, color=255):
        return u"\u001b[38;5;{}m{}\u001b[0m".format(color, text)

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
            if val == '_false':
                return False
            if val == '_true':
                return True

        error_text = "Please enter a(n) {} value".format(vtype)
        if vtype == 'boolean':
            error_text += ': _true, _false'

        raise TypeError(self.colored_text(error_text, warning_color))

    def get_input(self, values=[], text='Selection', default=None, itype=None,
                  help_text=None, sitype=None, enforce='__true__',
                  example=None, spacing=0
                  ):
        print(self.colored_text("b: back, q: quit", grey_color))
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
            text += self.colored_text('  [' + default_text + ']', 11)
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

            if selection == '_x':
                if self.current_menu.parent:
                    self.display_menu(self.current_menu.parent)
                else:
                    print("Quiting...")
                    sys.exit()
                break

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

            if 'q' in values and selection == 'q':
                print("Quiting...")
                sys.exit()

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

    def print_colored_output(self, data):
        try:
            data_json = json.dumps(data, indent=2)
        except:
            data = ast.literal_eval(str(data))
            data_json = json.dumps(data, indent=2)

        print(self.colored_text(data_json, success_color))

    def pretty_print(self, data):
        pp = pprint.PrettyPrinter(indent=2)
        pp_string = pp.pformat(data)
        print(self.colored_text(pp_string, success_color))

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

    def make_swagger_var(self, varname):
        word_list = re.sub(r'([A-Z])', r' \1', varname).lower().split()
        return '_'.join(word_list)

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
            param_name = self.make_swagger_var(param['name'])
            if not param_name in parameters:
                text_ = param['name']
                help_text = param.get('description') or param.get('summary')
                enforce = True if end_point_param and end_point_param['name'] == param['name'] else False

                parameters[param_name] = self.get_input(
                    text=text_.strip('.'),
                    itype=param['schema']['type'],
                    default=param['schema'].get('default'),
                    enforce=enforce,
                    help_text=help_text
                )

        return parameters

    def get_name_from_string(self, txt):
        return re.sub(r'[^0-9a-zA-Z\s]+', '', txt)

    def get_api_class_name(self, name):
        namle_list = self.get_name_from_string(name).split()
        for i, w in enumerate(namle_list[:]):
            if len(w) > 1:
                w = w[0].upper() + w[1:]
            else:
                w = w.upper()

            namle_list[i] = w

        return ''.join(namle_list) + 'Api'

    def get_path_by_id(self, operation_id):
        retVal = {}
        for path in self.cfg_yml['paths']:
            for method in self.cfg_yml['paths'][path]:
                if 'operationId' in self.cfg_yml['paths'][path][method] and self.cfg_yml['paths'][path][method][
                    'operationId'] == operation_id:
                    retVal = self.cfg_yml['paths'][path][method].copy()
                    retVal['__path__'] = path
                    retVal['__method__'] = method
                    retVal['__urlsuffix__'] = self.get_url_param(path)

        return retVal

    def get_tag_from_api_name(self, api_name, qmethod=None):

        for tag in self.cfg_yml['tags']:
            api_class_name = self.get_api_class_name(tag['name'])
            if api_class_name == api_name:
                break

        paths = []

        for path in self.cfg_yml['paths']:

            for method in self.cfg_yml['paths'][path]:

                if 'tags' in self.cfg_yml['paths'][path][method] and tag['name'] in self.cfg_yml['paths'][path][method][
                    'tags'] and 'operationId' in self.cfg_yml['paths'][path][method]:
                    retVal = self.cfg_yml['paths'][path][method].copy()
                    retVal['__path__'] = path
                    retVal['__method__'] = method
                    retVal['__urlsuffix__'] = self.get_url_param(path)
                    if qmethod:
                        if method == qmethod:
                            paths.append(retVal)
                    else:
                        paths.append(retVal)

        return paths

    def get_scope_for_endpoint(self, endpoint):
        scope = []
        for security in endpoint.info.get('security', []):
            for stype in security:
                scope += security[stype]

        return ' '.join(scope)

    def unmap_model(self, model, data_dict=None):
        if data_dict is None:
            data_dict = {}

        for key_ in model.attribute_map:

            val = getattr(model, key_)
            if isinstance(val, datetime.date):
                val = str(val)

            if isinstance(val, list):
                sub_list = []
                for entry in val:
                    if hasattr(entry, 'swagger_types'):
                        sub_list_dict = {}
                        self.unmap_model(entry, sub_list_dict)
                        sub_list.append(sub_list_dict)
                    else:
                        sub_list.append(entry)
                data_dict[model.attribute_map[key_]] = sub_list
            elif hasattr(val, 'swagger_types'):
                sub_data_dict = {}
                self.unmap_model(val, sub_data_dict)
                data_dict[model.attribute_map[key_]] = sub_data_dict
            else:
                data_dict[model.attribute_map[key_]] = val

        return data_dict

    def get_model_key_map(self, model, key):
        key_underscore = key.replace('-', '_')
        for key_ in model.attribute_map:
            if model.attribute_map[key_] == key or model.attribute_map[key_] == key_underscore:
                return key_

    def tabular_data(self, data, ome):
        tab_data = []
        headers = tabulate_endpoints[ome]
        for i, entry in enumerate(data):
            row_ = [i + 1]
            for header in headers:
                row_.append(entry[header])
            tab_data.append(row_)

        print(tabulate(tab_data, headers, tablefmt="grid"))

    def process_get(self, endpoint, return_value=False):
        clear()
        title = endpoint.name
        if endpoint.name != endpoint.info['description'].strip('.'):
            title += '\n' + endpoint.info['description']

        self.print_underlined(title)

        parameters = self.obtain_parameters(endpoint, single=return_value)

        for param in parameters.copy():
            if not parameters[param]:
                del parameters[param]

        if parameters:
            print("Calling Api with parameters:", parameters)

        print("Please wait while retreiving data ...\n")

        api_caller = self.get_api_caller(endpoint)

        api_response = None
        raise_error = False
        try:
            api_response = api_caller(**parameters)
        except Exception as e:
            if return_value:
                raise_error = True
            else:
                self.print_exception(e)

        if raise_error:
            raise ValueError('Not found')

        if return_value:
            if api_response:
                return api_response
            return False

        selections = ['q', 'b']
        item_counters = []
        tabulated = False

        if api_response:
            try:
                if 'response' in api_response:
                    api_response = api_response['response']
            except:
                pass

            selections.append('w')
            api_response_unmapped = []
            if isinstance(api_response, list):
                for model in api_response:
                    data_dict = self.unmap_model(model)
                    api_response_unmapped.append(data_dict)
            elif isinstance(api_response, dict):
                api_response_unmapped = api_response
            else:
                data_dict = self.unmap_model(api_response)
                api_response_unmapped = data_dict

            op_mode_endpoint = my_op_mode + '.' + endpoint.info['operationId']

            if op_mode_endpoint in tabulate_endpoints:
                self.tabular_data(api_response_unmapped, op_mode_endpoint)
                item_counters = [str(i + 1) for i in range(len(api_response_unmapped))]
                tabulated = True
            else:
                self.print_colored_output(api_response_unmapped)

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
                        json.dump(api_response_unmapped, w, indent=2)
                        print("Output was written to", fn)
                except Exception as e:
                    print("An error ocurred while saving data")
                    self.print_exception(e)
            elif selection in item_counters:
                self.pretty_print(api_response_unmapped[int(selection) - 1])

    def get_schema_from_reference(self, ref):
        schema_path_list = ref.strip('/#').split('/')
        schema = self.cfg_yml[schema_path_list[0]]

        schema_ = schema.copy()

        for p in schema_path_list[1:]:
            schema_ = schema_[p]

        if 'allOf' in schema_:
            all_schema = OrderedDict()

            all_schema['properties'] = OrderedDict()
            for sch in schema_['allOf']:
                if '$ref' in sch:
                    all_schema.update(self.get_schema_from_reference(sch['$ref']))
                elif 'properties' in sch:
                    for sprop in sch['properties']:
                        all_schema['properties'][sprop] = sch['properties'][sprop]

            schema_ = all_schema

        for key_ in schema_['properties']:
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

    def get_scheme_for_endpoint(self, endpoint):
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

    def get_swagger_types(self, model, name):
        for attribute in model.swagger_types:
            if model.swagger_types[attribute] == name:
                return attribute

    def get_input_for_schema_(self, schema, model, spacing=0, initialised=False, getitem=None, required_only=False):
        data = {}
        for prop in schema['properties']:
            item = schema['properties'][prop]
            if getitem and prop != getitem['__name__'] or prop in ('dn', 'inum'):
                continue

            if (required_only and schema.get('required')) and not prop in schema.get('required'):
                continue

            prop_ = self.get_model_key_map(model, prop)
            if item['type'] == 'object' and 'properties' in item:
                print()
                print("Data for object {}. {}".format(prop, item.get('description', '')))

                model_name_str = item.get('__schema_name__') or item.get('title') or item.get('description')
                model_name = self.get_name_from_string(model_name_str)

                if initialised and getattr(model, prop_):
                    sub_model = getattr(model, prop_)
                    self.get_input_for_schema_(item, sub_model, spacing=3, initialised=initialised)
                elif isinstance(model, type) and hasattr(swagger_client.models, model_name):
                    sub_model_class = getattr(swagger_client.models, model_name)
                    result = self.get_input_for_schema_(item, sub_model_class, spacing=3, initialised=initialised)
                    setattr(model, prop_, result)
                elif hasattr(swagger_client.models, model.swagger_types[prop_]):
                    sub_model = getattr(swagger_client.models, model.swagger_types[prop_])
                    result = self.get_input_for_schema_(item, sub_model, spacing=3, initialised=initialised)
                    setattr(model, prop_, result)
                else:
                    sub_model = getattr(model, prop_)
                    self.get_input_for_schema_(item, sub_model, spacing=3, initialised=initialised)
                    # print(self.colored_text("Fix me: can't find model", error_color))

            elif item['type'] == 'array' and '__schema_name__' in item:
                model_name = item['__schema_name__']
                sub_model_class = getattr(swagger_client.models, model_name)
                sub_model_list = []
                sub_model_list_help_text = ''
                sub_model_list_title_text = item.get('title')
                if sub_model_list_title_text:
                    sub_model_list_help_text = item.get('description')
                else:
                    sub_model_list_title_text = item.get('description')

                cur_model_data = getattr(model, prop_)

                if cur_model_data and initialised:
                    for cur_data in cur_model_data:
                        print("\nUpdate {}".format(sub_model_list_title_text))
                        cur_model_data = self.get_input_for_schema_(item, cur_data, spacing=spacing + 3)
                        sub_model_list.append(cur_model_data)

                sub_model_list_selection = self.get_input(text="Add {}?".format(sub_model_list_title_text),
                                                          values=['y', 'n'], help_text=sub_model_list_help_text)

                if sub_model_list_selection == 'y':
                    while True:
                        sub_model_list_data = self.get_input_for_schema_(item, sub_model_class, spacing=spacing + 3)
                        sub_model_list.append(sub_model_list_data)
                        sub_model_list_selection = self.get_input(
                            text="Add another {}?".format(sub_model_list_title_text), values=['y', 'n'])
                        if sub_model_list_selection == 'n':
                            break

                data[prop_] = sub_model_list

            else:
                default = getattr(model, prop_)
                if isinstance(default, property):
                    default = None
                enforce = True if item['type'] == 'boolean' else False

                if prop in schema.get('required', []):
                    enforce = True

                if not default:
                    default = item.get('default')

                values_ = item.get('enum', [])
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
                    spacing=spacing
                )
                data[prop_] = val

        if model.__class__.__name__ == 'type':
            modelObject = model(**data)
            return modelObject
        else:
            for key_ in data:
                # if data[key_]:
                setattr(model, key_, data[key_])

            return model

    def get_api_caller(self, endpoint):
        security = self.get_scope_for_endpoint(endpoint)
        if security.strip():
            self.get_access_token(security)
        client = getattr(swagger_client, self.get_api_class_name(endpoint.parent.name))

        api_instance = client(swagger_client.ApiClient(self.swagger_configuration))
        api_caller = getattr(api_instance, endpoint.info['operationId'].replace('-', '_'))

        return api_caller

    def process_post(self, endpoint):
        schema = self.get_scheme_for_endpoint(endpoint)

        title = schema.get('description') or schema['title']
        data_dict = {}

        model_class = getattr(swagger_client.models, schema['__schema_name__'])

        if my_op_mode == 'scim':
            if endpoint.path == '/jans-scim/restv1/v2/Groups':
                schema['properties']['schemas']['default'] = ['urn:ietf:params:scim:schemas:core:2.0:Group']
            elif endpoint.path == '/jans-scim/restv1/v2/Users':
                schema['properties']['schemas']['default'] = ['urn:ietf:params:scim:schemas:core:2.0:User']
            if endpoint.info['operationId'] == 'create-user':
                schema['required'] = ['userName', 'name', 'displayName', 'emails', 'password']

        model = self.get_input_for_schema_(schema, model_class, required_only=True)

        optional_fields = []
        required_fields = schema.get('required', []) + ['dn', 'inum']
        for field in schema['properties']:
            if not field in required_fields:
                optional_fields.append(field)

        if optional_fields:
            fill_optional = self.get_input(values=['y', 'n'], text='Populate optional fields?')
            fields_numbers = []
            if fill_optional == 'y':
                print("Optiaonal Fields:")
                for i, field in enumerate(optional_fields):
                    print(i + 1, field)
                    fields_numbers.append(str(i + 1))

                while True:
                    optional_selection = self.get_input(values=['q', 'c'] + fields_numbers,
                                                        help_text="c: continue, #: populate field")
                    if optional_selection == 'c':
                        break
                    if optional_selection in fields_numbers:
                        item_name = optional_fields[int(optional_selection) - 1]
                        schema_item = schema['properties'][item_name].copy()
                        schema_item['__name__'] = item_name
                        self.get_input_for_schema_(schema, model, initialised=True, getitem=schema_item)

        print("Obtained Data:\n")
        model_unmapped = self.unmap_model(model)
        self.print_colored_output(model_unmapped)

        selection = self.get_input(values=['q', 'b', 'y', 'n'], text='Continue?')

        if selection == 'y':
            api_caller = self.get_api_caller(endpoint)
            print("Please wait while posting data ...\n")

            try:
                api_response = api_caller(body=model)
            except Exception as e:
                api_response = None
                self.print_exception(e)

            if api_response:
                try:
                    api_response_unmapped = self.unmap_model(api_response)
                    self.print_colored_output(api_response_unmapped)
                except:
                    print(self.colored_text(str(api_response), success_color))

        selection = self.get_input(values=['q', 'b'])
        if selection in ('b', 'n'):
            self.display_menu(endpoint.parent)

    def process_delete(self, endpoint):
        url_param = self.get_endpiont_url_param(endpoint)
        url_param_val = self.get_input(text=url_param['name'], help_text='Entry to be deleted')
        selection = self.get_input(text="Are you sure want to delete {} ?".format(url_param_val),
                                   values=['b', 'y', 'n', 'q'])
        if selection in ('b', 'n'):
            self.display_menu(endpoint.parent)
        elif selection == 'y':
            api_caller = self.get_api_caller(endpoint)
            print("Please wait while deleting {} ...\n".format(url_param_val))
            api_response = '__result__'

            try:
                api_response = api_caller(url_param_val)
            except Exception as e:
                self.print_exception(e)

            if api_response is None:
                print(self.colored_text("\nEntry {} was deleted successfully\n".format(url_param_val), success_color))

        selection = self.get_input(['b', 'q'])
        if selection == 'b':
            self.display_menu(endpoint.parent)

    def process_patch(self, endpoint):

        if 'PatchOperation' in self.cfg_yml['components']['schemas']:
            schema = self.cfg_yml['components']['schemas']['PatchOperation'].copy()
            model = getattr(swagger_client.models, 'PatchOperation')
            for item in schema['properties']:
                if not 'type' in schema['properties'][item]:
                    schema['properties'][item]['type'] = 'string'
        else:
            schema = self.cfg_yml['components']['schemas']['PatchRequest'].copy()
            model = getattr(swagger_client.models, 'PatchRequest')

        url_param_val = None
        url_param = self.get_endpiont_url_param(endpoint)
        if 'name' in url_param:
            url_param_val = self.get_input(text=url_param['name'], help_text='Entry to be patched')

        body = []

        while True:
            data = self.get_input_for_schema_(schema, model)
            if my_op_mode != 'scim' and not data.path.startswith('/'):
                data.path = '/' + data.path

            if my_op_mode == 'scim':
                data.path = data.path.replace('/', '.')

            body.append(data)
            selection = self.get_input(text='Another patch operation?', values=['y', 'n'])
            if selection == 'n':
                break

        unmapped_body = []
        for item in body:
            unmapped_body.append(self.unmap_model(item))

        self.print_colored_output(unmapped_body)

        selection = self.get_input(values=['y', 'n'], text='Continue?')

        if selection == 'y':

            api_caller = self.get_api_caller(endpoint)

            print("Please wait patching...\n")

            if my_op_mode == 'scim':
                body = {'schemas': ['urn:ietf:params:scim:api:messages:2.0:PatchOp'], 'Operations': body}

            try:
                if url_param_val:
                    payload = {url_param['name']: url_param_val, 'body': body}
                    api_response = api_caller(**payload)
                else:
                    api_response = api_caller(body=body)
            except Exception as e:
                api_response = None
                self.print_exception(e)

            if api_response:
                api_response_unmapped = self.unmap_model(api_response)
                self.print_colored_output(api_response_unmapped)

        selection = self.get_input(['b'])
        if selection == 'b':
            self.display_menu(endpoint.parent)

    def process_put(self, endpoint):

        schema = self.get_scheme_for_endpoint(endpoint)

        initialised = False
        cur_model = None
        go_back = False
        if 'x-cli-getdata' in endpoint.info:
            for m in endpoint.parent:
                if m.info['operationId'] == endpoint.info['x-cli-getdata']:
                    while True:
                        try:
                            cur_model = self.process_get(m, return_value=True)
                            break
                        except ValueError as e:
                            print(self.colored_text("Server returned no data", error_color))
                            retry = self.get_input(values=['y', 'n'], text='Retry?')
                            if retry == 'n':
                                self.display_menu(endpoint.parent)
                                break
                    initialised = True
                    get_endpoint = m
                    break

        else:
            for m in endpoint.parent:
                if m.method == 'get' and m.path.endswith('}'):
                    while True:
                        while True:
                            try:
                                cur_model = self.process_get(m, return_value=True)
                                break
                            except ValueError as e:
                                print(self.colored_text("Server returned no data", error_color))
                                retry = self.get_input(values=['y', 'n'], text='Retry?')
                                if retry == 'n':
                                    self.display_menu(endpoint.parent)
                                    break

                        if not cur_model is False:
                            break

                    initialised = True
                    get_endpoint = m
                    break

        if not cur_model:
            for m in endpoint.parent:
                if m.method == 'get' and not m.path.endswith('}'):
                    cur_model = self.process_get(m, return_value=True)
                    get_endpoint = m

        if not cur_model:
            cur_model = getattr(swagger_client.models, schema['__schema_name__'])

        end_point_param = self.get_endpiont_url_param(get_endpoint)

        if cur_model:
            end_point_param_val = None
            if end_point_param:
                end_point_param_val = getattr(cur_model, end_point_param['name'])

            attr_name_list = []
            for attr_name in cur_model.attribute_map:
                if attr_name != 'dn':
                    attr_name_list.append(cur_model.attribute_map[attr_name])

            attr_name_list.sort()
            item_numbers = []

            def print_fields():
                print("Fields:")
                for i, attr_name in enumerate(attr_name_list):
                    print(str(i + 1).rjust(2), attr_name)
                    item_numbers.append(str(i + 1))

            print_fields()
            changed_items = []
            selection_list = ['q', 'b', 'v', 's', 'l'] + item_numbers
            help_text = 'q: quit, v: view, s: save, l: list fields #: update fieled'

            while True:
                selection = self.get_input(values=selection_list, help_text=help_text)
                if selection == 'v':
                    self.pretty_print(self.unmap_model(cur_model))
                elif selection == 'l':
                    print_fields()
                elif selection in item_numbers:
                    item = attr_name_list[int(selection) - 1]
                    item_unmapped = self.get_model_key_map(cur_model, item)
                    schema_item = schema['properties'][item]
                    schema_item['__name__'] = item
                    self.get_input_for_schema_(schema, cur_model, initialised=initialised, getitem=schema_item)
                    changed_items.append(item)

                if selection == 'b':
                    self.display_menu(endpoint.parent)
                    break
                elif selection == 's':
                    print('Changes:')
                    for ci in changed_items:
                        model_key = self.get_model_key_map(cur_model, ci)
                        str_val = str(getattr(cur_model, model_key))
                        print(self.colored_text(ci, bold_color) + ':', self.colored_text(str_val, success_color))

                    selection = self.get_input(values=['y', 'n'], text='Continue?')
                    if selection == 'y':
                        print("Please wait while posting data ...\n")
                        api_caller = self.get_api_caller(endpoint)
                        put_pname = self.get_url_param(endpoint.path)
                        try:
                            if put_pname:
                                args_ = {'body': cur_model, put_pname: end_point_param_val}
                                api_response = api_caller(**args_)
                            else:
                                api_response = api_caller(body=cur_model)
                        except Exception as e:
                            api_response = None
                            self.print_exception(e)

                        if api_response:
                            api_response_unmapped = self.unmap_model(api_response)
                            self.print_colored_output(api_response_unmapped)
                            go_back = True
                            break
        if go_back:
            selection = self.get_input(values=['q', 'b'])
            if selection == 'b':
                self.display_menu(endpoint.parent)
        else:
            self.get_input_for_schema_(schema, cur_model, initialised=initialised)

    def display_menu(self, menu):
        clear()
        print(self.colored_text("*** WARNING! This software is experimental ***", warning_color))
        self.current_menu = menu

        self.print_underlined(menu.name)

        selection_values = ['q', 'b']

        menu_numbering = {}

        c = 0
        for i, item in enumerate(menu):
            if item.info.get('x-cli-ignore'):
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
        paths = self.get_tag_from_api_name(op_name + 'Api')

        schema_path = None

        for path in paths:
            if 'tags' in path:
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

    def get_path_api_caller_for_path(self, path):

        dummy_enpoint = Menu(name='', info=path)
        security = self.get_scope_for_endpoint(dummy_enpoint)
        if security.strip():
            self.get_access_token(security)
        class_name = self.get_api_class_name(path['tags'][0])
        client = getattr(swagger_client, class_name)
        api_instance = client(swagger_client.ApiClient(self.swagger_configuration))
        api_caller = getattr(api_instance, path['operationId'].replace('-', '_'))

        return api_caller

    def process_command_get(self, path, suffix_param, endpoint_params, data_fn, data=None):

        api_caller = self.get_path_api_caller_for_path(path)
        api_response = None
        encoded_param = urlencode(endpoint_params)

        if encoded_param:
            sys.stderr.write("Calling with params {}\n".format(encoded_param))

        try:
            if path.get('__urlsuffix__'):
                api_response = api_caller(suffix_param[path['__urlsuffix__']], **endpoint_params)
            else:
                api_response = api_caller(**endpoint_params)
        except Exception as e:
            if hasattr(e, 'reason'):
                sys.stderr.write(e.reason)
            if hasattr(e, 'body'):
                sys.stderr.write(e.body)
                sys.stderr.write('\n')
            sys.exit()

        api_response_unmapped = []
        if isinstance(api_response, list):
            for model in api_response:
                data_dict = self.unmap_model(model)
                api_response_unmapped.append(data_dict)
        else:
            data_dict = self.unmap_model(api_response)
            api_response_unmapped = data_dict

        print(json.dumps(api_response_unmapped, indent=2))

    def get_sub_model(self, field):
        sub_model_name_str = field.get('title') or field.get('description')
        sub_model_name = self.get_name_from_string(sub_model_name_str)
        if hasattr(swagger_client.models, sub_model_name):
            return getattr(swagger_client.models, sub_model_name)

    def exit_with_error(self, error_text):
        error_text += '\n'
        sys.stderr.write(self.colored_text(error_text, error_color))
        print()
        sys.exit()

    def process_command_post(self, path, suffix_param, endpoint_params, data_fn, data):
        api_caller = self.get_path_api_caller_for_path(path)

        endpoint = Menu(name='', info=path)
        schema = self.get_scheme_for_endpoint(endpoint)
        model_name = schema['__schema_name__']

        model = getattr(swagger_client.models, model_name)

        if not data:

            if data_fn.endswith('jwt'):
                with open(data_fn) as reader:
                    data_org = jwt.decode(reader.read(),
                                          options={"verify_signature": False, "verify_exp": False, "verify_aud": False})
            else:
                try:
                    data_org = self.get_json_from_file(data_fn)
                except ValueError as ve:
                    self.exit_with_error(str(ve))

            data = {}

            for k in data_org:
                if k in model.attribute_map:
                    mapped_key = model.attribute_map[k]
                    data[mapped_key] = data_org[k]
                else:
                    data[k] = data_org[k]

        try:
            body = myapi._ApiClient__deserialize_model(data, model)
        except Exception as e:
            self.exit_with_error(str(e))

        try:
            if suffix_param:
                api_response = api_caller(body=body, **suffix_param)
            else:
                api_response = api_caller(body=body)
        except Exception as e:
            self.print_exception(e)
            sys.exit()

        unmapped_response = self.unmap_model(api_response)
        sys.stderr.write("Server Response:\n")
        print(json.dumps(unmapped_response, indent=2))

    def process_command_put(self, path, suffix_param, endpoint_params, data_fn, data=None):
        self.process_command_post(path, suffix_param, endpoint_params, data_fn, data=None)

    def process_command_patch(self, path, suffix_param, endpoint_params, data_fn, data=None):

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

        api_caller = self.get_path_api_caller_for_path(path)

        try:
            if suffix_param:
                api_response = api_caller(suffix_param[path['__urlsuffix__']], body=data)
            else:
                api_response = api_caller(body=data)
        except Exception as e:
            self.print_exception(e)
            sys.exit()

        unmapped_response = self.unmap_model(api_response)
        sys.stderr.write("Server Response:\n")
        print(json.dumps(unmapped_response, indent=2))

    def process_command_delete(self, path, suffix_param, endpoint_params, data_fn, data=None):

        api_caller = self.get_path_api_caller_for_path(path)
        api_response = None

        try:
            api_response = api_caller(suffix_param[path['__urlsuffix__']], **endpoint_params)
        except Exception as e:
            self.print_exception(e)
            sys.exit()

        if api_response:
            unmapped_response = self.unmap_model(api_response)
            sys.stderr.write("Server Response:\n")
            print(json.dumps(unmapped_response, indent=2))

    def process_command_by_id(self, operation_id, url_suffix, endpoint_args, data_fn, data=None):
        path = self.get_path_by_id(operation_id)

        if not path:
            self.exit_with_error("No such Operation ID")

        suffix_param = self.parse_command_args(url_suffix)
        endpoint_params = self.parse_command_args(endpoint_args)

        if path.get('__urlsuffix__') and not path['__urlsuffix__'] in suffix_param:
            self.exit_with_error("This operation requires a value for url-suffix {}".format(path['__urlsuffix__']))

        endpoint = Menu('', info=path)
        schema = self.get_scheme_for_endpoint(endpoint)

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
        caller_function(path, suffix_param, endpoint_params, data_fn, data=data)

    def make_schema_val(self, stype):
        if stype == 'object':
            return {}
        elif stype == 'list':
            return []
        elif stype == 'bool':
            return random.choice((True, False))
        else:
            return None

    def is_native_type(self, model, prop):
        stype = getattr(model, prop)
        if stype.startswith('list['):
            stype = re.match(r'list\[(.*)\]', stype).group(1)
        elif stype.startswith('dict('):
            stype = re.match(r'dict\(([^,]*), (.*)\)', stype).group(2)

        if stype in swagger_client.ApiClient.NATIVE_TYPES_MAPPING:
            return True

    def get_schema_from_model(self, model, schema):

        for key in model.attribute_map:
            stype = model.swagger_types[key]
            mapped_key = model.attribute_map[key]
            if stype in swagger_client.ApiClient.NATIVE_TYPES_MAPPING:
                schema[mapped_key] = self.make_schema_val(stype)
            else:
                if stype.startswith('list['):
                    sub_cls = re.match(r'list\[(.*)\]', stype).group(1)
                    sub_type = 'list'
                elif stype.startswith('dict('):
                    sub_cls = re.match(r'dict\(([^,]*), (.*)\)', stype).group(2)
                    sub_type = 'dict'
                else:
                    sub_cls = stype
                    sub_type = None

                if sub_cls in swagger_client.ApiClient.NATIVE_TYPES_MAPPING:
                    schema[mapped_key] = self.make_schema_val(sub_type)
                else:
                    sub_dict = {}
                    sub_model = getattr(swagger_client.models, sub_cls)
                    sub_schema = self.get_schema_from_model(sub_model, sub_dict)
                    schema[mapped_key] = sub_dict

    def get_swagger_model_attr(self, model, attr):
        stype = model.swagger_types[attr]
        if stype in swagger_client.ApiClient.NATIVE_TYPES_MAPPING:
            return getattr(model, attr)

    def fill_defaults(self, schema, schema_={}):

        for k in schema:
            if isinstance(schema[k], dict):
                sub_schema_ = None
                if '$ref' in schema_['properties'][k]:
                    sub_schema_ = self.cfg_yml['components']['schemas'][schema_['properties'][k]['$ref'].split('/')[-1]]
                elif schema_['properties'][k].get('items', {}).get('$ref'):
                    sub_schema_ = self.cfg_yml['components']['schemas'][
                        schema_['properties'][k]['items']['$ref'].split('/')[-1]]
                elif 'properties' in schema_['properties'][k]:
                    sub_schema_ = schema_['properties'][k]
                if sub_schema_:
                    self.fill_defaults(schema[k], sub_schema_)

            else:
                if 'enum' in schema_['properties'][k]:
                    val = random.choice(schema_['properties'][k]['enum'])
                    if schema_['properties'][k]['type'] == 'array':
                        val = [val]
                    schema[k] = val
                elif 'default' in schema_['properties'][k]:
                    schema[k] = schema_['properties'][k]['default']
                elif 'example' in schema_['properties'][k]:
                    schema[k] = schema_['properties'][k]['example']
                elif k in schema_.get('required', []):
                    schema[k] = schema_['properties'][k]['type']

    def get_sample_schema(self, ref):
        schema_ = self.get_schema_from_reference('#' + args.schema)
        m = getattr(swagger_client.models, schema_['__schema_name__'])
        schema = {}
        self.get_schema_from_model(m, schema)
        self.fill_defaults(schema, schema_)

        print(json.dumps(schema, indent=2))

    def runApp(self):
        clear()
        self.display_menu(self.menu)


def main():

    cliObject = JCA_CLI(host, client_id, client_secret)

    try:
        cliObject.check_connection()
        if not (args.operation_id or args.info or args.schema):
            # reset previous color
            print('\033[0m', end='')
            cliObject.runApp()
        else:
            print()
            if args.info:
                cliObject.help_for(args.info)
            elif args.schema:
                cliObject.get_sample_schema(args.schema)
            elif args.operation_id:
                cliObject.process_command_by_id(args.operation_id, args.url_suffix, args.endpoint_args, args.data)
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
