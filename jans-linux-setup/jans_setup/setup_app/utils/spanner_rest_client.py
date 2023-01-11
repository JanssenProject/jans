import os
import time
import json
import jwt
import requests
import logging

import http.client
from http.client import HTTPConnection


class SpannerClient:

    def __init__(self, project_id, instance_id, database_id, google_application_credentials=None, emulator_host=None, log_dir='.', emulator_port=9020):
        self.project_id = project_id
        self.instance_id = instance_id
        self.database_id = database_id
        self.google_application_credentials = google_application_credentials
        self.emulator_host = emulator_host
        self.log_dir = log_dir
        self.emulator_port = emulator_port
        self.sessioned_url = None
        self.headers = {}

        if emulator_host:
            self.spanner_base_url = 'http://{}:{}/v1/'.format(emulator_host, emulator_port)
            self.set_spanner_database_url()
        else:
            self.spanner_base_url = 'https://spanner.googleapis.com/v1/'
            self.set_spanner_database_url()
            with open(self.google_application_credentials) as f:
                self.google_creds = json.load(f)
                self.get_google_cloud_spanner_id_token()


        self.set_logging()
        self.get_session()

    def set_spanner_database_url(self):
        self.spanner_database_url = os.path.join(
                        self.spanner_base_url, 
                        'projects/{}/instances/{}/databases/{}/'.format(self.project_id, self.instance_id, self.database_id)
                        )

    def get_google_cloud_spanner_id_token(self):
        aud = 'https://oauth2.googleapis.com/token'
        scopes = ['https://www.googleapis.com/auth/cloud-platform', 'https://www.googleapis.com/auth/spanner.data']
        grant_type = 'urn:ietf:params:oauth:grant-type:jwt-bearer'

        liftetime = 3600
        iat = int(time.time())
        exp = iat + liftetime

        payload = {
            'iat': iat, 
            'exp': exp, 
            'iss': self.google_creds['client_email'], 
            'aud': aud, 
            'scope': ' '.join(scopes)
            }

        headers = {
            'typ': 'JWT',
            'alg': 'RS256',
            'kid': self.google_creds['private_key_id']
            }

        assertion = jwt.encode(
            payload,
            self.google_creds['private_key'],
            headers=headers,
            algorithm='RS256'
            )

        req = requests.post(
            url=aud,
            data={
                'assertion': [assertion],
                'grant_type': [grant_type]
                }
           )

        response = req.json()
        self.id_token = response['access_token']
        self.headers = {'Authorization': 'Bearer {}'.format(self.id_token)}

    def set_logging(self):
        self.logger = logging.getLogger("urllib3")
        self.logger.propagate = True
        self.logger.setLevel(logging.DEBUG)
        http.client.HTTPConnection.debuglevel = 1
        log_fn = os.path.join(self.log_dir, 'spanner-client.log')
        file_handler = logging.FileHandler(log_fn)
        file_handler.setFormatter(logging.Formatter("%(asctime)s [%(levelname)-5.5s]  %(message)s"))
        self.logger.addHandler(file_handler)

        def print_to_log(*args):
            self.logger.debug(" ".join(args))

        http.client.print = print_to_log

    def get_session(self):
        session_url = os.path.join(self.spanner_database_url, 'sessions')
        request = requests.post(session_url, headers=self.headers)
        result = request.json()
        self.sessioned_url = os.path.join(self.spanner_base_url, result['name'])


    def exec_sql(self, sql_cmd):

        if 'select' in sql_cmd.lower().split():
            request = requests.post(
                        url=self.sessioned_url + ':executeSql',
                        json={"sql": sql_cmd},
                        headers=self.headers
                    )

        else:
            request = requests.patch(
                    url=os.path.join(self.spanner_database_url, 'ddl'),
                    json={"statements": [sql_cmd]},
                    headers=self.headers
                    )

        return request.json()



    def write_data(self, table, columns, values, mutation='insert'):
        values = list(values)
        for i,value in enumerate(values):
            if type(value) is int:
                values[i] = str(value)

        data = {
                'singleUseTransaction': {'readWrite': {}},
                "mutations": [
                            {
                            mutation: {
                                'table': table, 
                                'columns': columns,
                                'values': [values]
                                }
                            }
                    ]
                }

        request = requests.post(
                    url=self.sessioned_url+':commit',
                    json=data,
                    headers=self.headers
                    )

        return  request.json()


    def delete_data(self, table, pkey):

        if isinstance(pkey, int):
            pkey = str(pkey)

        data = {
                'singleUseTransaction': {'readWrite': {}},
                "mutations": [
                            {
                            'delete': {
                                'table': table, 
                                'keySet': {'keys': [[pkey]]},
                                }
                            }
                    ]
                }

        request = requests.post(
                    url=self.sessioned_url+':commit',
                    json=data,
                    headers=self.headers
                    )

        return  request.json()


    def get_dict_data(self, sql_cmd):
        result = self.exec_sql(sql_cmd)
        data = []
        if result.get('rows'):
            for row in result['rows']:
                row_data = {}
                for i, field in enumerate(result.get('metadata', {}).get('rowType', {}).get('fields', [])):
                    row_data[field['name']] = int(row[i]) if field['type']['code'] == 'INT64' else row[i]
                data.append(row_data)

        return data

    def get_table_columns(self, table):
        result = self.exec_sql('SELECT * FROM {} LIMIT 0'.format(table))
        col_list = [col['name'] for col in result.get('metadata', {}).get('rowType', {}).get('fields', [])]
        return col_list

    def get_tables(self):
        result = self.exec_sql("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE SPANNER_STATE = 'COMMITTED'")
        if not result.get('rows'):
            return []
        tables = [rowl[0] for rowl in result['rows']]
        return tables


    def __del__(self):
        if self.sessioned_url:
            try:
                requests.delete(self.sessioned_url, headers=self.headers)
            except Exception as e:
                pass

