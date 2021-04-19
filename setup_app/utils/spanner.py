import os
import requests
import json
import re

from setup_app.config import Config
from setup_app.utils import base

class FakeResult:
    ok = False
    reason = ''
    text = ''

    def json(self):
        return {'error': True}

# TODO: add logging to all functions

class Spanner:

    def __init__(self):
        if Config.spanner_emulator:
            self.spanner_base_url = 'http://{}:9020/v1/'.format(Config.spanner_host)
        else:
            self.spanner_base_url = 'https://{}/v1/'.format(Config.spanner_host)

        self.spanner_instance_url = os.path.join(self.spanner_base_url, 'projects/{}/instances/{}/databases'.format(Config.spanner_project, Config.spanner_instance))
        self.spanner_dbase_url = os.path.join(self.spanner_instance_url, Config.spanner_database)

        base.logIt("Spanner Api is constructed with base url {}".format(self.spanner_dbase_url))
        self.c = 1


    def set_sessioned_url(self):
        session_url = os.path.join(self.spanner_dbase_url, 'sessions')
        req = requests.post(session_url)
        result = req.json()
        session = result['name']
        self.sessioned_url = os.path.join(self.spanner_base_url, session)

    def del_sessioned_url(self):
        requests.delete(self.sessioned_url)

    def exec_sql(self, cmd):
        base.logit("Executing SQL query: {}".format(cmd))
        self.set_sessioned_url()
        data = {"sql": cmd}
        query_url = self.sessioned_url + ':executeSql'
        req = requests.post(query_url, data=json.dumps(data))
        self.del_sessioned_url()
        return req

    def get_data(self, data):
        self.set_sessioned_url()
        query_url = self.sessioned_url + ':read'
        req = requests.post(query_url, data=json.dumps(data))
        self.del_sessioned_url()
        return req

    def put_data(self, data):
        self.set_sessioned_url()
        query_url = self.sessioned_url + ':commit'
        req = requests.post(query_url, data=json.dumps(data))
        self.del_sessioned_url()
        return req

    def create_table(self, cmd):
        data = {"statements": [cmd]}
        base.logIt("CREATEING TABLE", cmd)
        self.c += 1
        query_url = os.path.join(self.spanner_dbase_url, 'ddl')
        req = requests.patch(query_url, data=json.dumps(data))
        return req

    def get_tables(self):
        query_url = os.path.join(self.spanner_dbase_url, 'ddl')

        req = requests.get(query_url)

        tables = []

        result = req.json()

        if 'statements' in result:
            for statement in result['statements']:
                table_name_re = re.search('CREATE TABLE (.+) \(', statement)
                if table_name_re:
                    table_name = table_name_re.groups()[0]
                tables.append(table_name)

        return tables

    def get_databases(self):
        req = requests.get(self.spanner_instance_url)
        return req
