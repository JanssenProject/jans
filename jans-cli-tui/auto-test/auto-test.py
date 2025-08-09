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
import jsonschema

from enum import Enum
from jsonpointer import resolve_pointer

default_output_file = os.path.join(tempfile.gettempdir(), 'jans-config-api-auto-test.html')
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

cur_path = pathlib.Path(__file__).parent.resolve()
cli_src_path = cur_path.parent.joinpath('cli_tui').as_posix()
cli_install_path = '/opt/jans/jans-cli'

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

css_style = '''
table {
    border-collapse: collapse;
    border-color: #c7c7c7;
    border-spacing: 0px;
}
table, td, th {
    border: 1px solid;
}
'''

def write_output():
    with open(output_file, 'w') as out_buffer:
        out_buffer.write(f'<html><head><title>Jans Config API Auto Test Resuls</title><style>{css_style}</style></head>\n')
        out_buffer.write('<body>\n')
        out_buffer.write('<table style=><tr><th>Tag</th><th>Endpoint</th><th>Operation</th><th>Operation ID</th><th>Description</th><th>Expected Response</th><th>Actual Response</th><th>Status</th></tr>\n')

        for row in output_data:
            row_bgcolor = '' if row[-2] == Status.SUCCESS else ' bgcolor= "red"'
            out_buffer.write(f'<tr{row_bgcolor}>')
            for i, cell in enumerate(row[:-1]):
                cell_value = cell.value if isinstance(cell, Status) else cell
                title = ''
                if i == len(row)-2 and row[-1]:
                    title = f' title="{html.escape(row[-1])}"' if i == len(row)-2 else ''
                out_buffer.write(f'<td{title}>{cell_value}</td>')
            out_buffer.write('</tr>\n')

        out_buffer.write('<table>\n')

        out_buffer.write('</body>\n</html>')

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
            if 'data' in schema_data[operation_id]:
                cli_data['data'] = schema_data[operation_id]['data']
            elif 'upload-file' in schema_data[operation_id]:
                cli_data['data_fn'] = schema_fp.parent.joinpath(schema_data[operation_id]['upload-file']).as_posix()

        if 'url-suffix' in schema_data[operation_id]:
            cli_data['url_suffix'] = ','.join([f'{k}:{v}' for k, v in schema_data[operation_id]['url-suffix'].items()])

        print("Data to send:", cli_data)

        response = cli_requests(cli_object, cli_data)
        
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
                                resolved_data = resolve_pointer(response_output, key)
                                if item[key] != resolved_data:
                                   output_status = Status.OUTPUT_VERIFY_FAILED
                                   err_msg = f"We Expect {item[key]} for {key} in output but got {resolved_data}"
            else:
                output_status = Status.VERIFY_FAILED

        output_data.append((
            ' '.join(yaml_path['tags']),
            yaml_path['__path__'],
            yaml_path['__method__'].upper(),
            f'{operation_id}:{cli_op_id}',
            yaml_path['description'] or yaml_path['summary'] or "No description found for this endpoint",
            schema_data[operation_id]['expected-status-code'],
            status_code,
            output_status,
            err_msg
            ))

        print()
        sleep_time = schema_data[operation_id].get('sleep')
        if sleep_time:
            print(f"Sleeping for {sleep_time} seconds")
            time.sleep(sleep_time)

write_output()

"""
response = cli_requests({'operation_id': 'get-app-version'})


if response.status_code == 200:
    result = response.json()
    print("Validating", result)
    jsonschema.validate(instance=result, schema=schema)

#print(config_cli.cfg_yaml)
"""
