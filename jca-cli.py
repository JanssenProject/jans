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

#reset previous color
print('\033[0m',end='')

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
        #self.swagger_configuration.debug = True
        if self.swagger_configuration.debug:
            self.swagger_configuration.logger_file='swagger.log'

        self.swagger_yaml_fn = 'jans-config-api-swagger.yaml'
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

    def colored_text(self, text, color=255):
        return u"\u001b[38;5;{}m{}\u001b[0m".format(color, text)

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
        print()
        type_text = ''
        if itype:
            if itype == 'array':
                type_text = "Type: array of {} seperated by _,".format(sitype)
            elif itype == 'boolean':
                if default is None:
                    default = False
            else:
                type_text = "Type: " + itype

        if help_text:
            help_text = help_text.strip('.') + '. ' + type_text
        else:
            help_text = type_text

        if help_text:
            print(self.colored_text('«{}»'.format(help_text), 244))
        
        if not default is None:
            default_text = str(default).lower() if itype == 'boolean' else str(default)
            text += ' [{}]'.format(self.colored_text(default_text, 11))
            if itype=='integer':
                default=int(default)
        else:
            enforce = False
        if not text.endswith('?'):
            text += ':'

        if itype=='boolean' and not values:
            values = ['_true', '_false']

        while True:
            selection = input(self.colored_text(text, 15)+' ')
            selection = selection.strip()

            if itype == 'boolean' and not selection:
                return False

            if enforce and not selection:
                continue

            if 'q' in values and selection == 'q':
                print("Quiting...")
                sys.exit()

            if itype == 'array' and default and not selection:
                return default

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

    def print_colored_output(self, data):
        data_json = json.dumps(data, indent=2)
        print(self.colored_text(data_json, 10))

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
        namle_list = name.replace('-','').replace('–','').split()
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


    def unmap_model(self, model, data_dict=None):
        if data_dict is None:
            data_dict = {}
        for key_ in model.attribute_map:
            
            val = getattr(model, key_)
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
        
        if not 'title' in schema_:
            schema_['title'] = p
            
        schema_['__schema_name__'] = p

        return schema_


    def get_scheme_for_endpoint(self, endpoint):
        for content_type in endpoint.info['requestBody']['content']:
            if 'schema' in endpoint.info['requestBody']['content'][content_type]:
                schema = endpoint.info['requestBody']['content'][content_type]['schema']
                break

        if '$ref' in schema:
            schema_ = schema.copy()
            schema_ref_ = self.get_schema_from_reference(schema_.pop('$ref'))
            schema_.update(schema_ref_)

        return schema_

    def get_swagger_types(self, model, name):
        for attribute in model.swagger_types:
            if model.swagger_types[attribute] == name:
                return attribute


    def get_input_for_schema_(self, schema, model):

        data = {}

        for prop in schema['properties']:
            item = schema['properties'][prop]
            prop_ = self.get_model_key_map(model, prop)

            if item['type'] == 'object':
                if model.__class__.__name__ == 'type':
                    sub_model_class = getattr(swagger_client.models, item['description'])
                    result = self.get_input_for_schema_(item, sub_model_class)
                    setattr(model, prop_, result)
                else:
                    sub_model = getattr(model, prop_)
                    self.get_input_for_schema_(item, sub_model)
            else:
                default = getattr(model, prop_)
                if isinstance(default, property):
                    default = None
                enforce = True if item['type'] == 'boolean' else False
                val = self.get_input(
                        values=item.get('enum', []),
                        text=prop,
                        default=default,
                        itype=item['type'],
                        help_text=item.get('description'),
                        sitype=item.get('items', {}).get('type'),
                        enforce=enforce
                        )
                data[prop_] = val

        if model.__class__.__name__ == 'type':
            modelObject = model(**data)
            return modelObject
        else:
            for key_ in data:
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
        model_class = getattr(swagger_client.models, schema['title'])
        
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


    def runApp(self):
        clear()
        self.display_menu(self.menu)
            

cliObject = JCA_CLI(host, client_id, client_secret)
cliObject.runApp()


