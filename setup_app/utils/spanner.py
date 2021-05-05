import os
import json
import sys

sys.path.insert(0, '/opt/dist/app/gcs')
sys.path.insert(0, '/opt/dist/app/gcs/google')

from google.cloud import spanner
from google.cloud.spanner_v1 import session
from google.auth.credentials import AnonymousCredentials

from setup_app import paths
from setup_app.config import Config
from setup_app.utils import base

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
