#!/usr/bin/env python3

import os
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

for package in ('jsonpointer', 'jsonschema', 'py-markdown-table'):
    install_dir = package.replace('-','_')
    if not (pydir_path.joinpath(install_dir).exists() or pydir_path.joinpath(install_dir+'.py').exists()):
        print(f"Installing {package}")
        cmd = f'pip3 install {package} --target {pydir_path}'
        os.system(cmd)

sys.path.insert(0, pydir_path.as_posix())

from jsonpointer import resolve_pointer
from py_markdown_table.markdown_table import markdown_table
import jsonschema

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
        markdown = markdown_table(output_data).set_params(row_sep = 'markdown', quote = False).get_markdown()
        out_buffer.write(markdown)

    print(f"Output was dumped to {output_file}")

def cli_requests(cli_object, args):
    
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

for schema_fp in sorted(input_file):
    print(schema_fp)
    schema_data = yaml_obj.load(schema_fp.read_text())
    cur_schem_dir = schema_fp.parent.as_posix()
    #print(json.dumps(schema_data, indent=2))
    cli_object = get_cli_object()

    for operation_id in schema_data:
        if operation_id.startswith('yaml-variable-'):
            continue
        if schema_data[operation_id].get('pass-this'):
            continue
        cli_op_id = schema_data[operation_id]['id']
        yaml_path = cli_object.get_path_by_id(cli_op_id)
        print(f"Executing request for {yaml_path['__method__'].upper()} method of {yaml_path['__path__']} with operatiın ID {operation_id}:{cli_op_id}")

        cli_data = {'operation_id': cli_op_id}
        if yaml_path['__method__'] in ('put', 'post'):
            if 'json-data' in schema_data[operation_id]:
                cli_data['data'] = json.loads(schema_data[operation_id]['json-data'])
            elif 'data' in schema_data[operation_id]:
                cli_data['data'] = schema_data[operation_id]['data']
            if 'upload-file' in schema_data[operation_id]:
                cli_data['data_fn'] = schema_fp.parent.joinpath(schema_data[operation_id]['upload-file']).as_posix()

        if 'url-suffix' in schema_data[operation_id]:
            cli_data['url_suffix'] = ','.join([f'{k}:{v}' for k, v in schema_data[operation_id]['url-suffix'].items()])

        print("Data to send:", cli_data)

        response = cli_requests(cli_object, cli_data)

        if isinstance(response, requests.models.Response):
            print("Data received:", response.status_code, response.text)
        else:
            print("Data received:", response)

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
            status_code = response.status_code
            err_msg = ''
            print(f"Status Code {response.status_code}")
            if status_code == schema_data[operation_id]['expected-status-code']:
                output_status = Status.SUCCESS
                print(yaml_path['responses'][str(status_code)])
                if 'application/json' in yaml_path['responses'][str(status_code)]['content']:
                    print("Checking Output")
                    response_output = response.json()
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

                    if schema_data[operation_id].get('expected-output'):
                        for item in schema_data[operation_id]['expected-output']:
                            print(item, schema_data[operation_id]['expected-output'])
                            for key in item:
                                try:
                                    resolved_data = resolve_pointer(response_output, key)
                                except Exception as e:
                                    err_msg += str(e)
                                if item[key] != resolved_data:
                                   output_status = Status.OUTPUT_VERIFY_FAILED
                                   err_msg += f"We Expect `{item[key]}` for `{key}` in output but got `{resolved_data}`"
            else:
                output_status = Status.VERIFY_FAILED
                if isinstance(response, requests.models.Response):
                    err_msg = response.text
                else:
                    err_msg = response

        output_status_str = output_status.value
        if err_msg:
            err_msg = err_msg.replace('"',"`")
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

write_output()

