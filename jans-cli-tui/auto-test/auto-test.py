#!/usr/bin/env python3

import os
import re
import sys
import json
import time
import pathlib
import argparse
import tempfile
import html

import ruamel.yaml
import requests

from enum import Enum

icon_url = 'https://raw.githubusercontent.com/ubuntu/yaru/refs/heads/master/icons/Yaru/22x22/actions/cancel.png'
cur_path = pathlib.Path(__file__).parent.resolve()
cli_src_path = cur_path.parent.joinpath('cli_tui').as_posix()
cli_install_path = '/opt/jans/jans-cli'
pydir_path = cur_path.joinpath('pylib')

for package in ('jsonpointer', 'jsonschema', 'py-markdown-table', 'markdown'):
    install_dir = package.replace('-','_')
    if not (pydir_path.joinpath(install_dir).exists() or pydir_path.joinpath(install_dir+'.py').exists()):
        print(f"Installing {package}")
        cmd = f'pip3 install {package} --target {pydir_path}'
        os.system(cmd)

sys.path.insert(0, pydir_path.as_posix())

from jsonpointer import resolve_pointer, set_pointer, remove_pointer
from py_markdown_table.markdown_table import markdown_table
import jsonschema
import markdown

data_format_dict = {'base_dir': cur_path.as_posix()}
output_store_dict = {}
default_output_file = cur_path.joinpath('jans-config-api-auto-test.md').as_posix()
parser = argparse.ArgumentParser(description="This script auotmatically test endpoints of jans-config-api using CLI")
parser.add_argument('-output-file', help="Path of output file", default=default_output_file)
parser.add_argument('-input-file', help="Path of input files", nargs='+', required=False)

argsp = parser.parse_args()

for arg in sys.argv[:]:
    if arg.startswith(('-output-file', '-input-file')):
        sys.argv.remove(arg)
        arg_name = arg.strip('-').replace('-', '_')
        arg_vals = getattr(argsp, arg_name)
        if isinstance(arg_vals, str):
             arg_vals = [arg_vals]
        for av in arg_vals:
            sys.argv.remove(av)

input_file = argsp.input_file
output_file = argsp.output_file

if input_file:
    for i, ifp in enumerate(input_file[:]):
        if ifp.startswith('/'):
            input_file[i] = pathlib.Path(ifp)
        else:
            input_file[i] = cur_path.joinpath(ifp)


class Status(Enum):
    SUCCESS = 'Successfull'
    FAIL = 'Failed'
    VERIFY_FAILED = 'Response Verification Failed'
    OUTPUT_VERIFY_FAILED = 'Output Verification Failed'


sys.path.append(cli_src_path)
sys.path.append(cli_install_path)

print("Importing CLI")
from cli import config_cli

output_store_dict['hostname'] = config_cli.host


print("DDD", output_store_dict)


def get_cli_object():

     return config_cli.JCA_CLI(
                host=config_cli.host,
                client_id=config_cli.client_id,
                client_secret=config_cli.client_secret,
                access_token=config_cli.access_token
            )

output_data = []

def write_output():
    with open(output_file, 'w') as out_buffer:
        out_table = markdown_table(output_data).set_params(row_sep = 'markdown', quote = False).get_markdown()
        if output_file.endswith('.html'):
            out_table=markdown.markdown(out_table, extensions=['markdown.extensions.tables'])
        out_buffer.write(out_table)

    print(f"Output was dumped to {output_file}")

def cli_requests(cli_object, args):
    print("Data to send:", args)
    response = cli_object.process_command_by_id(
                        operation_id=args['operation_id'],
                        url_suffix=args.get('url_suffix', ''),
                        endpoint_args=args.get('endpoint_args', ''),
                        data_fn=args.get('data_fn'),
                        data=args.get('data', {})
                        )
    return response


yaml_obj = ruamel.yaml.YAML()

if not input_file:
    input_file = cur_path.glob('**/*.yaml')


def resolve_string_reference(rkey):
    print("resolve_string_reference", rkey)
    retval = rkey
    if re.findall(r"%\((\w+)\)", rkey):
        retval = rkey % output_store_dict

    if retval.startswith('$'):
        print("retval", retval)
        if ':' in retval:
            store_name, spath = retval[1:].split(':')
            print("store_name, spath", store_name, spath)
            retval = resolve_pointer(output_store_dict[store_name], spath)
        else:
            retval = output_store_dict[retval[1:]]

    return retval


def resolve_references(data_dict):
    print("RR Data Dictionary:", data_dict)
    if isinstance(data_dict, list):
        for dd in data_dict:
            resolve_references(dd)
    elif isinstance(data_dict, str):
        return resolve_string_reference(data_dict)
    else:
        for dkey in data_dict:
            print(dkey, data_dict)
            cval = data_dict[dkey]
            if isinstance(cval, str):
                data_dict[dkey] = resolve_string_reference(cval)

def make_cli_params(params):
    param_list = []
    resolve_references(params)
    for k, v in params.items():
        param_list.append(f'{k}:{v}')
    return  ','.join(param_list)


for schema_fp in sorted(input_file):
    print("Schema File:", schema_fp)
    schema_data = yaml_obj.load(schema_fp.read_text())
    cur_schem_dir = schema_fp.parent.as_posix()
    #print("Input Schema Data", json.dumps(schema_data, indent=2))
    cli_object = get_cli_object()

    for operation_id in schema_data:
        if operation_id.startswith('yaml-variable-'):
            continue
        if schema_data[operation_id].get('pass-this'):
            continue
        cli_op_id = schema_data[operation_id]['id']
        yaml_path = cli_object.get_path_by_id(cli_op_id)
        print(f"Executing request for {yaml_path['__method__'].upper()} method of {yaml_path['__path__']} with operatiın ID {operation_id}:{cli_op_id}")

        modify_output = schema_data[operation_id].get('modify-output')

        if modify_output:
            output_name = modify_output['output-name']
            setdata = modify_output['modifiers'].get('set', {})
            for skey in setdata:
                print(f"Setting {skey} to {setdata[skey]}")
                set_pointer(output_store_dict[output_name], skey, setdata[skey])
            for remkey in modify_output['modifiers'].get('remove', []):
                print(f"Removing {remkey}")
                remove_pointer(output_store_dict[output_name], remkey)
            for sortkey in modify_output['modifiers'].get('sort', []):
                print(f"Sorting {sortkey}")
                sort_data = remove_pointer(output_store_dict[output_name], sortkey)
                sort_data.sort()

            print("Modified data", output_store_dict[output_name])

        cli_data = {'operation_id': cli_op_id}
        if yaml_path['__method__'] in ('put', 'post', 'patch'):
            if 'json-data' in schema_data[operation_id]:
                cli_data['data'] = json.loads(schema_data[operation_id]['json-data'] % data_format_dict)
            elif 'data' in schema_data[operation_id]:
                cli_data['data'] = schema_data[operation_id]['data']
                resolved_data = resolve_references(cli_data['data'])
                if resolved_data:
                    cli_data['data'] = resolved_data
            if 'upload-file' in schema_data[operation_id]:
                cli_data['data_fn'] = schema_fp.parent.joinpath(schema_data[operation_id]['upload-file']).as_posix()

        if 'url-suffix' in schema_data[operation_id]:
            print("schema_data[operation_id]['url-suffix']", schema_data[operation_id]['url-suffix'])
            cli_data['url_suffix'] = make_cli_params(schema_data[operation_id]['url-suffix'])

        if 'endpoint-args' in schema_data[operation_id]:
            print("schema_data[operation_id]['endpoint-args']", schema_data[operation_id]['endpoint-args'])
            cli_data['endpoint_args'] = make_cli_params(schema_data[operation_id]['endpoint-args'])


        response = cli_requests(cli_object, cli_data)

        store_output = schema_data[operation_id].get('store-output-name')

        if isinstance(response, requests.models.Response):
            print("Data received:", response.status_code, response.text)
            status_code = response.status_code
        else:
            print("Data received:", response)
            if store_output:
                output_store_dict[store_output] = response

        try:
            response_output = response.json()
            if store_output:
                output_store_dict[store_output] = response_output
        except Exception:
            response_output = None

        if not schema_data[operation_id]['expected-status-code']:
            status_code = None
            if response == schema_data[operation_id]['expected-response-text']:
                err_msg = ''
                output_status = Status.SUCCESS
                print("SUCCESS: Operation was successfull")
            else:
                err_msg = response
                output_status = Status.FAIL
                print("FAILED:", response)
        else:
            err_msg = ''
            print(f"Status Code {response.status_code}")
            if status_code == schema_data[operation_id]['expected-status-code']:
                output_status = Status.SUCCESS
                print(yaml_path['responses'][str(status_code)])
                if response_output:
                    print("Validation server response")
                    try:
                        jsonschema.validate(instance=response_output, schema=schema_data[operation_id]['schema'])
                        print("SUCCESS: Server responded what we expected")
                        response_verified = True
                    except jsonschema.exceptions.ValidationError as e:
                        print(f"FAILED: Server response is invalid: {e.message}", e)
                        response_verified = False
                        err_msg = str(e)

                    if not response_verified:
                        output_status = Status.VERIFY_FAILED
                    else:
                        if status_code == schema_data[operation_id]['expected-status-code']:
                            output_status = Status.SUCCESS
                        else:
                            output_status = Status.FAIL

            else:
                output_status = Status.VERIFY_FAILED
                if isinstance(response, requests.models.Response):
                    err_msg = response.text
                else:
                    err_msg = response

            if response_output and 'expected-output' in schema_data[operation_id]:
                print("Checking expected output")
                expected_data_dict = schema_data[operation_id]['expected-output']
                resolve_references(expected_data_dict)
                for expkey in expected_data_dict:
                    expeval = expected_data_dict[expkey]
                    print("Item:", expkey, "Expected data:", expeval)
                    try:
                        resolved_data = resolve_pointer(response_output, expkey)
                    except Exception as e:
                        err_msg += str(e)
                    else:
                        if expeval != resolved_data:
                           output_status = Status.OUTPUT_VERIFY_FAILED
                           err_msg += f"We Expect `{expeval}` for `{expkey}` in output but got `{resolved_data}` "


        output_status_str = output_status.value
        if err_msg:
            err_msg = err_msg.replace('"',"`").replace("'", "`").replace('\n', ' ')
            output_status_str += f' ![]({icon_url} "{err_msg}")'

        output_data.append({
            "Tag": ' '.join(yaml_path['tags']),
            "Endpoint": yaml_path['__path__'],
            "Operation": yaml_path['__method__'].upper(),
            "Operation ID": f'{operation_id}:{cli_op_id}',
            "Description": yaml_path['description'] or yaml_path['summary'] or "No description found for this endpoint",
            "Expected Response": schema_data[operation_id]['expected-status-code'],
            "Actual Response": status_code,
            "Status": output_status_str
            #"Error Message": err_msg
            })

        print()
        sleep_time = schema_data[operation_id].get('sleep')
        if sleep_time:
            print(f"Sleeping for {sleep_time} seconds")
            time.sleep(sleep_time)

        print(output_store_dict)


write_output()

