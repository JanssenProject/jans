import readline
import time
import sys
import os
import json
import re
import urllib3
import configparser

import ruamel.yaml
from pprint import pprint
from functools import partial
from urllib.parse import urljoin

import swagger_client

clear = lambda: os.system('clear')
urllib3.disable_warnings()
config = configparser.ConfigParser()

host =  os.environ.get('jans_host')
client_id = os.environ.get('jans_client_id')
client_secret = os.environ.get('jans_client_secret')

if not (host and client_secret and client_secret):
    if os.path.exists('config.ini'):
        config.read('config.ini')
        host = config['DEFAULT']['jans_host']
        client_id = config['DEFAULT']['jans_client_id']
        client_secret = config['DEFAULT']['jans_client_secret']
    else:
        config['DEFAULT'] = {'jans_host': 'jans server hostname,e.g, jans.foo.net', 'jans_client_id':'your client id', 'jans_client_secret': 'client secret for you client id'}
        with open('config.ini', 'w') as configfile:
            config.write(configfile)

        print("Pelase fill config.ini or set environmental variables jans_host, jans_client_id ,and jans_client_secret and re-run")
        sys.exit()

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
        self.swagger_configuration.debug = False
        if self.swagger_configuration.debug:
            configuration.logger_file='swagger.log'

        self.swagger_yaml_fn = 'myswagger.yaml'
        self.cfg_yml = self.get_yaml()
        self.make_menu()
        self.current_menu = self.menu


    def get_yaml(self):

        with open(self.swagger_yaml_fn) as f:
            self.cfg_yml = ruamel.yaml.load(f.read(), ruamel.yaml.RoundTripLoader)
        
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
        print("Getting access token for scope", scope)
        
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
                print("Error while getting access token")
                print(data)
        except:
            print("Error while getting access token")
            print(response.data)

    def check_type(self, val, vtype):
        if vtype == 'string':
            return str(val)
        elif vtype == 'integer':
            if isinstance(val, int):
                return val
            if val.isnumeric():
                return int(val)
        elif vtype=='boolean':
            if val == '_false':
                return False
            if val == '_true':
                return True

        error_text = "Please enter a(n) {} value".format(vtype)
        if vtype == 'boolean':
            error_text += ': _true, _false'

        raise TypeError(error_text)

    def get_input(self, values=[], text='Selection', default=None, itype=None, help_text=None, sitype=None, enforce=True):

        type_text = ''
        if itype:
            if itype == 'array':
                type_text = "Type: array of {} seperated by _,".format(sitype)
            else:
                type_text = "Type: " + itype

        if help_text:
            help_text = help_text.strip('.') + '. ' + type_text
        else:
            help_text = type_text

        if help_text:
            print(u"\u001b[38;5;244m«{}»\u001b[0m".format(help_text))

        if default:
            text += ' [{}]'.format(default)
            if itype=='integer':
                default=int(default)
        text += ': '

        if itype=='boolean' and not values:
            values = ['_true', '_false']

        while True:
            selection = input(text)
            selection = selection.strip()

            if enforce and not selection:
                continue

            if 'q' in values and selection == 'q':
                print("Quiting...")
                sys.exit()

            if default and not selection:
                selection = default

            if itype == 'array' and sitype:
                selection = selection.split('_,')
                for i, item in enumerate(selection):
                    data_ok = True
                    try:
                        selection[i] = self.check_type(item.strip(), sitype)
                        if selection[i] == '_null':
                            selection[i] = None
                    except TypeError as e:
                        print(e)
                        data_ok = False
                if data_ok:
                    break
            else:
                if not itype is None:
                    try:
                        selection = self.check_type(selection, itype)
                    except TypeError as e:
                        print(e)

                if values:
                    if selection in values:
                        break
                    elif itype=='boolean':
                        if isinstance(selection, bool):
                            break
                        else:
                            continue
                    else:
                        print('Please enter one of ', ', '.join(values))

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

    def get_endpiont_url_param(self, endpoint):
        param = {}
        if endpoint.path.endswith('}'):
            pname = re.findall('/\{(.*?)\}$', endpoint.path)[0]
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

    def get_api_class_name(self, name):
        namle_list = name.replace('-','').split()
        for i, w in enumerate(namle_list[:]):
            if len(w) > 1:
                w = w[0].upper()+w[1:]
            else:
                w = w.upper()
            
            namle_list[i] = w

        return ''.join(namle_list)+'Api'


    def get_scope_for_endpoint(self, endpoint):
        for security in endpoint.info['security']:
            if 'jans-auth' in security:
                return security['jans-auth'][0]


    def unmap_model(self, model):
        data_unmaped = {}
        data = model.to_dict()

        for key_ in data:
            data_unmaped[model.attribute_map[key_]] = data[key_]

        return data_unmaped

    def process_get(self, endpoint, return_value=False):
        clear()
        title = endpoint.name
        if endpoint.name != endpoint.info['description'].strip('.'):
            title += '\n' + endpoint.info['description']

        self.print_underlined(title)

        client = getattr(swagger_client, self.get_api_class_name(endpoint.parent.name))

        api_instance = client(swagger_client.ApiClient(self.swagger_configuration))

        security = self.get_scope_for_endpoint(endpoint)

        parameters = self.obtain_parameters(endpoint)
        
        for param in parameters.copy():
            if not parameters[param]:
                del parameters[param]
        
        self.get_access_token(security)

        if parameters:
            print("Calling Api with parameters:", parameters)

        print("Please wait while retreiving data ...\n")

        api_caller = getattr(api_instance, endpoint.info['operationId'].replace('-','_'))

        api_response = api_caller(**parameters)

        api_response_unmapped = []
        if isinstance(api_response, list):
            for model in api_response:
                api_response_unmapped.append(self.unmap_model(model))
        else:
            api_response_unmapped = self.unmap_model(api_response)

        if return_value:
            return api_response_unmapped

        print()
        print(json.dumps(api_response_unmapped, indent=2))

        while True:
            selection = self.get_input(['q', 'b', 'w', 'r'])
            if selection == 'b':
                self.display_menu(endpoint.parent)
                break
            elif selection == 'r':
                self.process_get(endpoint)
            elif selection == 'w':
                fn = input('File name: ')
                try:
                    with open(fn, 'w') as w:
                        json.dump(api_response_unmapped, w, indent=2)
                        print("Output was written to", fn)
                except Exception as e:
                    print("An error ocurred while saving data")
                    print(e)

    def get_scheme_for_endpoint(self, endpoint):
        schema = endpoint.info['requestBody']['content']['application/json']['schema']

        if '$ref' in schema:
            schema_path_list = schema.pop('$ref').strip('/#').split('/')
            schema = self.cfg_yml[schema_path_list[0]]
            for p in schema_path_list[1:]:
                schema = schema[p]
        
        return schema


    def get_swagger_name(self, model, name):
        for attribute in model.attribute_map:
            if model.attribute_map[attribute] == name:
                return attribute

    def get_swagger_types(self, model, name):
        for attribute in model.swagger_types:
            if model.swagger_types[attribute] == name:
                return attribute


    def get_input_for_schema_(self, schema, model):
        
        for prop in schema['properties']:
            item = schema['properties'][prop]

            if item['type'] == 'object':
                sub_model_class = getattr(swagger_client.models, item['description'])
                sub_model = sub_model_class()
                self.get_input_for_schema_(item, sub_model)
                swagger_name = self.get_swagger_types(model, item['description'])
                setattr(model, swagger_name, sub_model)

            else:
                val = self.get_input(
                        values=item.get('enum', []),
                        text=prop,
                        #default=default,
                        itype=item['type'],
                        help_text=item.get('description'),
                        sitype=item.get('items', {}).get('type')
                        )

                swagger_name = self.get_swagger_name(model, prop)
                setattr(model, swagger_name, val)

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
        model_class = getattr(swagger_client.models, schema['title'])
        model = model_class()
        self.get_input_for_schema_(schema, model)

        print("Obtained Data:")
        print(model)

        selection = self.get_input(values=['q', 'b', 'y', 'n'], text='Coninue?')
        
        if selection == 'y':
            api_caller = self.get_api_caller(endpoint)
            print("Please wait while posting data ...\n")
            api_response = api_caller(body=model)
            pprint(api_response)
            
            
        if selection in ('b', 'n'):
            self.display_menu(endpoint.parent)
            
    def process_delete(self, endpoint):
        print("DELETE mehod for '{}' is not implemented yet".format(endpoint))
        
        selection = self.get_input(['b'])
        if selection == 'b':
            self.display_menu(endpoint.parent)

    def process_patch(self, endpoint):
        print("PATCH mehod for '{}' is not implemented yet".format(endpoint))
        
        selection = self.get_input(['b'])
        if selection == 'b':
            self.display_menu(endpoint.parent)

    def process_put(self, endpoint):

        """
        cur_values = None
        for m in endpoint.parent:
            if m.method=='get' and m.path.endswith('}'):
                cur_values = self.process_get(m, return_value=True)
        
        if cur_values:
            print(cur_values)

        schema = endpoint.info['requestBody']['content']['application/json']['schema']

        if '$ref' in schema:
            schema_path_list = schema['$ref'].strip('/#').split('/')
            schema = self.cfg_yml[schema_path_list[0]]
            for p in schema_path_list[1:]:
                schema = schema[p]

        print(json.dumps(schema, indent=2))
        """
        print("PUT mehod for '{}' is not implemented yet".format(endpoint))
        
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


    def runApp(self):
        clear()
        self.display_menu(self.menu)
            

cliObject = JCA_CLI(host, client_id, client_secret)
cliObject.runApp()


