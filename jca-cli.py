#!/usr/bin/env python3

import readline
import time
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

from pprint import pprint
from functools import partial
from urllib.parse import urljoin, urlencode
from collections import OrderedDict

import swagger_client

cur_dir = os.path.dirname(os.path.realpath(__file__))
warning_color = 214
error_color = 196

clear = lambda: os.system('clear')
urllib3.disable_warnings()
config = configparser.ConfigParser()

host =  os.environ.get('jans_host')
client_id = os.environ.get('jans_client_id')
client_secret = os.environ.get('jans_client_secret')
debug = os.environ.get('jans_client_debug')
debug_log_file = os.environ.get('jans_debug_log_file')

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
    if api_name.endswith and inspect.isclass(getattr(swagger_client.api, api_name)):
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
parser.add_argument("--op-mode", choices=['get', 'post', 'put', 'patch', 'delete'], default='get', help="Operation mode to be done")
parser.add_argument("--endpoint-args", help="Arguments to pass endpoint separated by comma. For example limit:5,status:INACTIVE")
parser.add_argument("--schema", help="Get sample json schema")
#parser.add_argument("-show-data-type", help="Show data type in schema query", action='store_true')
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
    config_ini_fn = os.path.join(cur_dir, 'config.ini')
    if os.path.exists(config_ini_fn):
        config.read(config_ini_fn)
        host = config['DEFAULT']['jans_host']
        client_id = config['DEFAULT']['jans_client_id']
        if config['DEFAULT'].get('jans_client_secret'):
            client_secret = config['DEFAULT']['jans_client_secret']
        elif config['DEFAULT'].get('jans_client_secret_enc'):
            client_secret_enc = config['DEFAULT']['jans_client_secret_enc']
            client_secret = os.popen('/opt/jans/bin/encode.py -D ' + client_secret_enc).read().strip()
        debug = config['DEFAULT'].get('debug')
        debug_log_file = config['DEFAULT'].get('debug_log_file')
    else:
        config['DEFAULT'] = {'jans_host': 'jans server hostname,e.g, jans.foo.net', 'jans_client_id':'your client id', 'jans_client_secret': 'client secret for you client id'}
        with open(config_ini_fn, 'w') as configfile:
            config.write(configfile)

        print("Pelase fill config.ini or set environmental variables jans_host, jans_client_id ,and jans_client_secret and re-run")
        sys.exit()

if str(debug).lower() in ('yes', 'true', '1', 'on'):
    debug = True
else:
    debug = False

class Menu(object):

    def __init__(self, name, method='', info='', path=''):
        self.name = name
        self.method = method
        self.info = info
        self.path = path
        self.children = []
        self.parent = None
    
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
                print(' ' * self.__get_parent_number(child)*2, child)
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

        self.swagger_configuration = swagger_client.Configuration()
        self.swagger_configuration.host = 'https://{}'.format(self.host)

        self.swagger_configuration.verify_ssl = False
        self.swagger_configuration.debug = debug
        if self.swagger_configuration.debug:
            self.swagger_configuration.logger_file = debug_log_file

        self.swagger_yaml_fn = os.path.join(cur_dir, 'jans-config-api-swagger.yaml')
        self.cfg_yml = self.get_yaml()
        self.make_menu()
        self.current_menu = self.menu


    def get_yaml(self):
        debug_json = 'swagger_yaml.json'
        if os.path.exists(debug_json):
            with open(debug_json) as f:
                return json.load(f, object_pairs_hook=OrderedDict)

        with open(self.swagger_yaml_fn) as f:
            self.cfg_yml = ruamel.yaml.load(f.read().replace('\t',''), ruamel.yaml.RoundTripLoader)
            if os.environ.get('dump_yaml'):
                with open(debug_json, 'w') as w:
                    json.dump(self.cfg_yml, w, indent=2)
        return self.cfg_yml


    def make_menu(self):

        menu = Menu('Main Menu')
        
        for tag in self.cfg_yml['tags']:
            if tag['name'] != 'developers':
                m = Menu(name=tag['name'])
                menu.add_child(m)
                for path in self.cfg_yml['paths']:
                    for method in self.cfg_yml['paths'][path]:
                        if 'tags' in self.cfg_yml['paths'][path][method] and m.name in self.cfg_yml['paths'][path][method]['tags'] and 'operationId' in self.cfg_yml['paths'][path][method]:
                            sm = Menu(
                                    name=self.cfg_yml['paths'][path][method]['summary'].strip('.'),
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
        url = urljoin(self.swagger_configuration.host, 'jans-auth/restv1/token')
        headers['Content-Type'] = 'application/x-www-form-urlencoded'

        response = rest.POST(
                    url, 
                    headers=headers,
                    post_params={
                        "grant_type": "client_credentials",
                        "scope": scope,
                    })

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
        elif vtype=='object':
            try:
                retVal = json.loads(val)
                if isinstance(retVal, dict):
                    return retVal
            except:
                pass
        elif vtype=='boolean':
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
            print(' '*spacing, self.colored_text('«{}»'.format(help_text), 244), sep='')
        
        if example:
            if isinstance(example, list):
                example_str = ', '.join(example)
            else:
                example_str = str(example)
            print(' '*spacing, self.colored_text('Example: {}'.format(example_str), 244), sep='')
        
        if not default is None:
            default_text = str(default).lower() if itype == 'boolean' else str(default)
            text += ' [{}]'.format(self.colored_text(default_text, 11))
            if itype=='integer':
                default=int(default)

        if not text.endswith('?'):
            text += ':'

        if itype=='boolean' and not values:
            values = ['_true', '_false']

        while True:
            selection = input(' '*spacing + self.colored_text(text, 15)+' ')
            selection = selection.strip()

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
                    print(' '*spacing, e, sep='')
                    continue

                data_ok = True
                for items in object_:
                    try:
                        self.check_type(object_[items], sitype)
                    except Exception as e:
                        print(' '*spacing, e, sep='')
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
                                print(' '*spacing, self.colored_text("Please enter array of {} separated by _,".format(', '.join(values)), warning_color), sep='')
                                break
                    except TypeError as e:
                        print(' '*spacing, e, sep='')
                        data_ok = False
                if data_ok:
                    break
            else:
                if not itype is None:
                    try:
                        selection = self.check_type(selection, itype)
                    except TypeError as e:
                        if enforce:
                            print(' '*spacing, e, sep='')
                            continue

                if values:
                    if selection in values:
                        break
                    elif itype=='boolean':
                        if isinstance(selection, bool):
                            break
                        else:
                            continue
                    else:
                        print(' '*spacing, self.colored_text('Please enter one of {}'.format(', '.join(values)), warning_color), sep='')

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
        data_json = json.dumps(data, indent=2)
        print(self.colored_text(data_json, 10))


    def get_url_param(self, url):
        if url.endswith('}'):
            pname = re.findall('/\{(.*?)\}$', url)[0]
            return pname


    def get_endpiont_url_param(self, endpoint):
        param = {}
        pname = self.get_url_param(endpoint.path)
        if pname:
            param = {'name': pname, 'description':pname, 'schema': {'type': 'string'}}

        return param


    def obtain_parameters(self, endpoint):
        parameters = {}

        endpoint_parameters = []
        if 'parameters' in endpoint.info:
            endpoint_parameters = endpoint.info['parameters']
        
        end_point_param = self.get_endpiont_url_param(endpoint)
        if end_point_param:
            endpoint_parameters.insert(0, end_point_param)

        for param in endpoint_parameters:
            parameters[param['name']] = self.get_input(
                        text=param['description'].strip('.'), 
                        itype=param['schema']['type'],
                        default = param['schema'].get('default'),
                        enforce=False
                        )

        return parameters


    def get_name_from_string(self, txt):
        return re.sub(r'[^0-9a-zA-Z\s]+','', txt)

    def get_api_class_name(self, name):
        namle_list = self.get_name_from_string(name).split()
        for i, w in enumerate(namle_list[:]):
            if len(w) > 1:
                w = w[0].upper()+w[1:]
            else:
                w = w.upper()
            
            namle_list[i] = w

        return ''.join(namle_list)+'Api'


    def get_path_by_id(self, operation_id):
        retVal = {}
        for path in self.cfg_yml['paths']:
            for method in self.cfg_yml['paths'][path]:
                if 'operationId' in self.cfg_yml['paths'][path][method] and self.cfg_yml['paths'][path][method]['operationId'] == operation_id:
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
                
                if 'tags' in self.cfg_yml['paths'][path][method] and tag['name'] in self.cfg_yml['paths'][path][method]['tags'] and 'operationId' in self.cfg_yml['paths'][path][method]:
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
        for security in endpoint.info['security']:
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
        for key_ in model.attribute_map:
            if model.attribute_map[key_] == key:
                return key_

    def process_get(self, endpoint, return_value=False):
        clear()
        title = endpoint.name
        if endpoint.name != endpoint.info['description'].strip('.'):
            title += '\n' + endpoint.info['description']

        self.print_underlined(title)

        parameters = self.obtain_parameters(endpoint)
        
        for param in parameters.copy():
            if not parameters[param]:
                del parameters[param]

        if parameters:
            print("Calling Api with parameters:", parameters)

        print("Please wait while retreiving data ...\n")


        api_caller = self.get_api_caller(endpoint)

        api_response = None

        try:
            api_response = api_caller(**parameters)
        except swagger_client.rest.ApiException as e:
            print('\u001b[38;5;196m')
            print(e.reason)
            print(e.body)
            print('\u001b[0m')

        if return_value:
            return api_response

        selections = ['q', 'b']

        if api_response:
            selections.append('w')
            api_response_unmapped = []
            if isinstance(api_response, list):
                for model in api_response:
                    data_dict = self.unmap_model(model)
                    api_response_unmapped.append(data_dict)
            else:
                data_dict = self.unmap_model(api_response)
                api_response_unmapped = data_dict

            print()
            self.print_colored_output(api_response_unmapped)

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
                    print(e)


    def get_schema_from_reference(self, ref):
        schema_path_list = ref.strip('/#').split('/')
        schema = self.cfg_yml[schema_path_list[0]]
        schema_ = schema.copy()

        for p in schema_path_list[1:]:
            schema_ = schema_[p]

        for key_ in schema_['properties']:
            if '$ref' in schema_['properties'][key_]:
                schema_['properties'][key_] = self.get_schema_from_reference(schema_['properties'][key_]['$ref'])
            elif schema_['properties'][key_].get('type') == 'array' and '$ref' in schema_['properties'][key_]['items']:
                ref_path = schema_['properties'][key_]['items'].pop('$ref')
                ref_schema = self.get_schema_from_reference(ref_path)
                schema_['properties'][key_]['properties'] = ref_schema['properties']
                schema_['properties'][key_]['title'] = ref_schema['title']
                schema_['properties'][key_]['description'] = ref_schema['description']
                schema_['properties'][key_]['__schema_name__'] = ref_schema['__schema_name__']
                
        if not 'title' in schema_:
            schema_['title'] = p
            
        schema_['__schema_name__'] = p

        return schema_


    def get_scheme_for_endpoint(self, endpoint):
        schema_ = {}
        for content_type in endpoint.info.get('requestBody', {}).get('content',{}):
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


    def get_input_for_schema_(self, schema, model, spacing=0):

        data = {}
        for prop in schema['properties']:
            item = schema['properties'][prop]
            prop_ = self.get_model_key_map(model, prop)

            if item['type'] == 'object' and 'properties' in item:
                print()
                print("Data for object {}. {}".format(prop, item.get('description','')))

                if getattr(model, prop_).__class__.__name__ == 'type':
                    model_name_str = item.get('__schema_name__') or item.get('title') or item.get('description')
                    model_name = self.get_name_from_string(model_name_str)
                    sub_model_class = getattr(swagger_client.models, model_name)
                    result = self.get_input_for_schema_(item, sub_model_class, spacing=3)
                    setattr(model, prop_, result)
                elif hasattr(swagger_client.models, model.swagger_types[prop_]):
                    sub_model = getattr(swagger_client.models, model.swagger_types[prop_])
                    result = self.get_input_for_schema_(item, sub_model, spacing=3)
                    setattr(model, prop_, result)
                else:
                    sub_model = getattr(model, prop_)
                    self.get_input_for_schema_(item, sub_model, spacing=3)

            elif item['type'] == 'array' and '__schema_name__' in item:
                model_name = item['__schema_name__']
                sub_model_class = getattr(swagger_client.models, model_name)
                sub_model_list = []
                
                sub_model_list_title_text = item.get('description') or item.get('title') or item.get('__schema_name__')
                
                sub_model_list_selection = self.get_input(text="Add {}?".format(sub_model_list_title_text), values=['y','n'])
                if sub_model_list_selection == 'y':
                    while True:
                        sub_model_list_data = self.get_input_for_schema_(item, sub_model_class)
                        sub_model_list.append(sub_model_list_data)
                        sub_model_list_selection = self.get_input(text="Add another {}?".format(sub_model_list_title_text), values=['y','n'])
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
                if data[key_]:
                    setattr(model, key_, data[key_])
            
            
            return model


    def get_api_caller(self, endpoint):
        security = self.get_scope_for_endpoint(endpoint)
        self.get_access_token(security)
        client = getattr(swagger_client, self.get_api_class_name(endpoint.parent.name))

        api_instance = client(swagger_client.ApiClient(self.swagger_configuration))
        api_caller = getattr(api_instance, endpoint.info['operationId'].replace('-','_'))

        return api_caller


    def process_post(self, endpoint):
        
        schema = self.get_scheme_for_endpoint(endpoint)
        
        title = schema.get('description') or schema['title']
        data_dict = {}
        
        model_class = getattr(swagger_client.models, schema['__schema_name__'])
        
        model = self.get_input_for_schema_(schema, model_class)

        print("Obtained Data:\n")
        model_unmapped = self.unmap_model(model)
        self.print_colored_output(model_unmapped)

        selection = self.get_input(values=['q', 'b', 'y', 'n'], text='Coninue?')

        if selection == 'y':
            api_caller = self.get_api_caller(endpoint)
            print("Please wait while posting data ...\n")

            try:
                api_response = api_caller(body=model)
            except swagger_client.rest.ApiException as e:
                api_response = None
                print('\u001b[38;5;196m')
                print(e.reason)
                print(e.body)
                print('\u001b[0m')
            
            if api_response:
                try:
                    api_response_unmapped = self.unmap_model(api_response)
                    self.print_colored_output(api_response_unmapped)
                except:
                    print(self.colored_text(str(api_response), 10))

        selection = self.get_input(values=['q', 'b'])
        if selection in ('b', 'n'):
            self.display_menu(endpoint.parent)


    def process_delete(self, endpoint):
        url_param = self.get_endpiont_url_param(endpoint)
        url_param_val = self.get_input(text=url_param['name'], help_text='Entry to be deleted')
        selection = self.get_input(text="Are you sure want to delete {} ?".format(url_param_val), values=['b','y','n','q'])
        if selection in ('b', 'n'):
            self.display_menu(endpoint.parent)
        elif selection == 'y':
            api_caller = self.get_api_caller(endpoint)
            print("Please wait while deleting {} ...\n".format(url_param_val))
            api_response = '__result__'

            try:
                api_response = api_caller(url_param_val)
            except swagger_client.rest.ApiException as e:
                print('\u001b[38;5;196m')
                print(e.reason)
                print(e.body)
                print('\u001b[0m')

            if api_response is None:
                print(self.colored_text("\nEntry {} was deleted successfully\n".format(url_param_val), 10))


        selection = self.get_input(['b', 'q'])
        if selection == 'b':
            self.display_menu(endpoint.parent)


    def process_patch(self, endpoint):
        schema = self.cfg_yml['components']['schemas']['PatchRequest']['properties']

        url_param_val = None
        url_param = self.get_endpiont_url_param(endpoint)
        if 'name' in url_param:
            url_param_val = self.get_input(text=url_param['name'], help_text='Entry to be patched')

        body = []

        while True:
            data = {}
            for param in ('op', 'path', 'value'):
                itype = schema[param]['type']
                if itype=='object':
                    itype = 'string'
                val_ = self.get_input(text=schema[param]['description'].strip('.'), values=schema[param].get('enum',[]), itype=itype)
                data[param] = val_
                if param == 'path' and data['op'] == 'remove':
                    break
            if not data['path'].startswith('/'):
                data['path'] = '/' + data['path']
            
            model = swagger_client.PatchRequest(**data)
            body.append(model)
            selection = self.get_input(text='Patch another param?', values=['y', 'n'])
            if selection == 'n':
                break

        api_input_unmapped = self.unmap_model(model)
        self.print_colored_output(api_input_unmapped)
            
        selection = self.get_input(values=['y', 'n'], text='Coninue?')

        if selection == 'y':

            api_caller = self.get_api_caller(endpoint)
            
            print("Please wait patching...\n")

            try:
                if url_param_val:
                    api_response = api_caller(url_param_val, body=body)
                else:
                    api_response = api_caller(body=body)
            except swagger_client.rest.ApiException as e:
                api_response = None
                print('\u001b[38;5;196m')
                print(e.reason)
                print(e.body)
                print('\u001b[0m')

            if api_response:
                api_response_unmapped = self.unmap_model(api_response)
                self.print_colored_output(api_response_unmapped)

        selection = self.get_input(['b'])
        if selection == 'b':
            self.display_menu(endpoint.parent)


    def process_put(self, endpoint):

        schema = self.get_scheme_for_endpoint(endpoint)
        
        cur_model = None
        for m in endpoint.parent:
            if m.method=='get' and m.path.endswith('}'):
                cur_model = self.process_get(m, return_value=True)

        if not cur_model:
            for m in endpoint.parent:
                if m.method=='get' and not m.path.endswith('}'):
                    cur_model = self.process_get(m, return_value=True)

        if not cur_model:            
            schema = self.get_scheme_for_endpoint(endpoint)
            cur_model = getattr(swagger_client.models, schema['__schema_name__'])
        
        if cur_model:
            self.get_input_for_schema_(schema, cur_model)
        
            print("Obtained Data:")
            print()
            model_unmapped = self.unmap_model(cur_model)
            self.print_colored_output(model_unmapped)

            selection = self.get_input(values=['q', 'b', 'y', 'n'], text='Continue?')
            
            if selection == 'y':
                api_caller = self.get_api_caller(endpoint)
                print("Please wait while posting data ...\n")                

                try:
                    api_response = api_caller(body=cur_model)
                except swagger_client.rest.ApiException as e:
                    api_response = None
                    print('\u001b[38;5;196m')
                    print(e.reason)
                    print(e.body)
                    print('\u001b[0m')
                
                if api_response:
                    api_response_unmapped = self.unmap_model(api_response)
                    self.print_colored_output(api_response_unmapped)


        selection = self.get_input(['b'])
        if selection == 'b':
            self.display_menu(endpoint.parent)


    def display_menu(self, menu):
        clear()
        self.current_menu = menu

        self.print_underlined(menu.name)

        selection_values = ['q', 'b']

        for i, item in enumerate(menu):
            print(i+1, item)
            selection_values.append(str(i+1))
        
        selection = self.get_input(selection_values)

        if selection == 'b' and not menu.parent:
            print("Quiting...")
            sys.exit()
        elif selection == 'b':
            self.display_menu(menu.parent)
        elif menu.get_child(int(selection) -1).children:
            self.display_menu(menu.get_child(int(selection) -1))
        else:
            m = menu.get_child(int(selection) -1)
            #endpoint = self.get_endpoint(m)
            getattr(self, 'process_' + m.method)(m)

    def parse_command_args(self, args):
        args_dict = {}

        if args:
            for arg in args.split(','):
                neq = arg.find(':')
                if neq > 1:
                    arg_name = arg[:neq].strip()
                    arg_val = arg[neq+1:].strip()
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
        paths = self.get_tag_from_api_name(op_name+'Api')

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
                        desc = param['description']
                        param_type = param.get('schema', {}).get('type')
                        if param_type:
                            desc += ' [{}]'.format(param_type)
                        param_names.append((param['name'], desc))
                    if param_names:
                        print('  Parameters:')
                        for param in param_names:
                            print('  {}: {}'.format(param[0], param[1]))

                if 'requestBody' in path:
                    for apptype in path['requestBody'].get('content',{}):
                        if 'schema' in path['requestBody']['content'][apptype]:
                            if path['requestBody']['content'][apptype]['schema'].get('type') == 'array':
                                schema_path = path['requestBody']['content'][apptype]['schema']['items']['$ref']
                                print('  Schema: Array of {}'.format(schema_path[1:]))
                            else:
                                schema_path = path['requestBody']['content'][apptype]['schema']['$ref']
                                print('  Schema: {}'.format(schema_path[1:]))

        if schema_path:
            print()
            print("To get sample shema type {0} --schema <schma>, for example {0} --schema {1}".format(sys.argv[0], schema_path[1:]))


    def get_json_from_file(self, data_fn):
    
        if not os.path.exists(data_fn):
            self.exit_with_error("Can't find file {}".format(data_fn))

        try: 
            with open(data_fn) as f:
                data = json.load(f)
        except:
            self.exit_with_error("Error parsing json file {}".format(data_fn))

        return data


    def get_path_api_caller_for_path(self, path):
        
        dummy_enpoint = Menu(name='', info=path)
        security = self.get_scope_for_endpoint(endpoint)
        self.get_access_token(security)
        class_name = self.get_api_class_name(path['tags'][0])
        client = getattr(swagger_client, class_name)
        api_instance = client(swagger_client.ApiClient(self.swagger_configuration))
        api_caller = getattr(api_instance, path['operationId'].replace('-','_'))
        
        return api_caller


    def process_command_get(self, path, suffix_param, endpoint_params, data_fn):

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
        except swagger_client.rest.ApiException as e:

            sys.stderr.write(e.reason)
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

    def process_command_post(self, path, suffix_param, endpoint_params, data_fn):
        api_caller = self.get_path_api_caller_for_path(path)

        endpoint = Menu(name='', info=path)
        schema = self.get_scheme_for_endpoint(endpoint)
        model_name = schema['__schema_name__']

        model = getattr(swagger_client.models, model_name)

        data = self.get_json_from_file(data_fn)
        
        try:
            body = myapi._ApiClient__deserialize_model(data, model)
        except Exception as e:
            self.exit_with_error(str(e))

        try:
            api_response = api_caller(body=body)
        except swagger_client.rest.ApiException as e:
            print(e.reason)
            print(e.body)
            sys.exit()
        
        unmapped_response = self.unmap_model(api_response)
        sys.stderr.write("Server Response:\n")
        print(json.dumps(unmapped_response, indent=2))


    def process_command_put(self, path, suffix_param, endpoint_params, data_fn):
        self.process_command_post(path, suffix_param, endpoint_params, data_fn)


    def process_command_patch(self, path, suffix_param, endpoint_params, data_fn):

        data = self.get_json_from_file(data_fn)

        if not isinstance(data, list):
            self.exit_with_error("{} must be array of /components/schemas/PatchRequest".format(data_fn))

        op_modes = ('add', 'remove', 'replace', 'move', 'copy', 'test')

        for item in data:
            if not item['op'] in op_modes:
                print("op must be one of {}".format(', '.join(op_modes)))
                sys.exit()
            if not item['path'].startswith('/'):
                item['path'] = '/'+item['path']

        api_caller = self.get_path_api_caller_for_path(path)

        try:
            if suffix_param:
                api_response = api_caller(suffix_param[path['__urlsuffix__']], body=data)
            else:
                api_response = api_caller(body=data)
        except swagger_client.rest.ApiException as e:
            print(e.reason)
            print(e.body)
            sys.exit()

        unmapped_response = self.unmap_model(api_response)
        sys.stderr.write("Server Response:\n")
        print(json.dumps(unmapped_response, indent=2))



    def process_command_delete(self, path, suffix_param, endpoint_params, data_fn):

        api_caller = self.get_path_api_caller_for_path(path)
        api_response = None

        try:
            api_response = api_caller(suffix_param[path['__urlsuffix__']], **endpoint_params)
        except swagger_client.rest.ApiException as e:
            print(e.reason)
            print(e.body)
            sys.exit()

        if api_response:
            unmapped_response = self.unmap_model(api_response)
            sys.stderr.write("Server Response:\n")
            print(json.dumps(unmapped_response, indent=2))

    def process_command_by_id(self, operation_id, url_suffix, endpoint_args, data_fn):
        path = self.get_path_by_id(operation_id)

        if not path:
            self.exit_with_error("No such Operation ID")

        suffix_param = self.parse_command_args(url_suffix)
        endpoint_params = self.parse_command_args(endpoint_args)

        if path.get('__urlsuffix__') and not path['__urlsuffix__'] in suffix_param:
            self.exit_with_error("This operation requires a value for url-suffix {}".format(path['__urlsuffix__']))

        endpoint = Menu('', info=path)
        schema = self.get_scheme_for_endpoint(endpoint)

        if schema and not data_fn:
            self.exit_with_error("Please provide schema with --data argument")

        caller_function = getattr(self, 'process_command_' + path['__method__'])
        caller_function(path, suffix_param, endpoint_params, data_fn)


    def make_schema_val(self, stype):
        if stype == 'object':
            return {}
        elif stype == 'list':
            return []
        elif stype == 'bool':
            return random.choice((True, False))
        else:
            return None

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


    def fill_defaults(self, schema, schema_={}):

        for k in schema:
            if isinstance(schema[k], dict):
                sub_schema_ = None
                if '$ref' in schema_['properties'][k]:
                    sub_schema_ = self.cfg_yml['components']['schemas'][schema_['properties'][k]['$ref'].split('/')[-1]]
                elif schema_['properties'][k].get('items', {}).get('$ref'):
                    sub_schema_ = self.cfg_yml['components']['schemas'][schema_['properties'][k]['items']['$ref'].split('/')[-1]]
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
                elif k in schema_.get('required',[]):
                    schema[k] = schema_['properties'][k]['type']



    def get_sample_schema(self, ref):
        schema_ = self.get_schema_from_reference('#'+args.schema)
        m = getattr(swagger_client.models, schema_['__schema_name__'])
        schema = {}
        self.get_schema_from_model(m, schema)
        self.fill_defaults(schema, schema_)

        print(json.dumps(schema, indent=2))

    def runApp(self):
        clear()
        self.display_menu(self.menu)

cliObject = JCA_CLI(host, client_id, client_secret)

if not (args.operation_id or args.info or args.schema):
    #reset previous color
    print('\033[0m',end='')
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
