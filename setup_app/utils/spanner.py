import os
import json
import sys


from setup_app import paths
from setup_app.config import Config
from setup_app.utils import base

sys.path.append(os.path.join(paths.PYLIB_DIR, 'gcs/google'))
sys.path.append(os.path.join(paths.PYLIB_DIR, 'gcs'))

from google.cloud import spanner
from google.cloud.spanner_v1 import session
from google.auth.credentials import AnonymousCredentials

class FakeResult:
    ok = False
    reason = ''
    text = ''

    def json(self):
        return {'error': True}

# TODO: add logging to all functions

class Spanner:

    def __init__(self):
        if Config.spanner_emulator_host:
            base.logIt("Using spanner emulator at {}".format(Config.spanner_emulator_host))
            self.client = spanner.Client(
                        project=Config.spanner_project,
                        client_options={'api_endpoint': '{}:9010'.format(Config.spanner_emulator_host)},
                        credentials=AnonymousCredentials()
                        )
        else:
            base.logIt("Using spanner with credidentals".format(Config.google_application_credentials))
            os.environ["GOOGLE_APPLICATION_CREDENTIALS"] = Config.google_application_credentials
            self.client = spanner.Client()

        self.instance = self.client.instance(Config.spanner_instance)
        self.database = self.instance.database(Config.spanner_database)

    def get_session(self):
        ses = session.Session(self.database)
        ses.create()
        return ses

    def get_transaction(self):
        ses = self.get_session()
        tr = ses.transaction()
        tr.begin()
        return tr

    def exec_sql(self, cmd):
        base.logIt("Executing SQL query: {}".format(cmd))
        ses = self.get_session()
        data = {'fields': [], 'rows':[]}
        with ses.transaction() as tr:
            try:
                result = tr.execute_sql(cmd)
                data['rows'] = list(result)
                for f in result.fields:
                    data['fields'].append({'name': f.name, 'type': f.type_.code.name})
            except:
                pass

        return data


    def insert_data(self, table, columns, values):
        ses = self.get_session()
        with ses.transaction() as tr:
            tr.insert(table, columns=columns, values=values)

    def update_data(self, table, columns, values):
        ses = self.get_session()
        with ses.transaction() as tr:
            tr.update(table, columns=columns, values=values)

    def create_table(self, cmd):
        operation = self.database.update_ddl([cmd])
        operation.result()

    def get_tables(self):
        tables = []
        for tbl in self.database.list_tables():
            tables.append(tbl.table_id)
        return list(tables)

    def get_databases(self):
        databases = []
        for db in self.instance.list_databases():
            databases.append(os.path.split(db.name)[1])
        return databases

"""
gcspanner = Spanner()

#gcspanner.create_table('create table car (car_id INT64, make STRING(20), model STRING(20)) PRIMARY KEY(car_id)')

gcspanner.create_table('create table ailem (doc_id INT64, name STRING(50)) PRIMARY KEY(doc_id)')
print(gcspanner.get_tables())
#print(gcspanner.get_databases())

gcspanner.insert_data('ailem', ['doc_id', 'name'], [[1, 'Fatih'], [2, 'Melike'], [3, 'Dilek']])

#gcspanner.update_data('ailem', ['doc_id', 'name'], [[4, 'Devrim']])

print(gcspanner.exec_sql('select * from ailem'))
"""
