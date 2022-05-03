import os
import json
import sys
import logging

from google.cloud import spanner
from google.cloud.spanner_v1 import session
from google.auth.credentials import AnonymousCredentials

from setup_app import paths
from setup_app.config import Config
from setup_app.utils import base

class Spanner:

    def __init__(self):
        if Config.spanner_emulator_host:
            logging.info("Using spanner emulator at %s", Config.spanner_emulator_host)
            self.client = spanner.Client(
                        project=Config.spanner_project,
                        client_options={'api_endpoint': '{}:9010'.format(Config.spanner_emulator_host)},
                        credentials=AnonymousCredentials()
                        )
        else:
            logging.info("Using spanner with credidentals %s", Config.google_application_credentials)
            os.environ["GOOGLE_APPLICATION_CREDENTIALS"] = Config.google_application_credentials
            self.client = spanner.Client()

        self.instance = self.client.instance(Config.spanner_instance)
        self.database = self.instance.database(Config.spanner_database)

    def get_session(self):
        logging.info("Getting session")
        ses = session.Session(self.database)
        ses.create()
        logging.debug("Sesion ID is %s", ses.session_id)
        return ses

    def get_transaction(self):
        logging.info("Getting transaction")
        ses = self.get_session()
        tr = ses.transaction()
        tr.begin()
        logging.info("Transaction started")
        return tr

    def exec_sql(self, cmd):
        logging.info("Executing SQL query %s", cmd)
        ses = self.get_session()
        data = {'fields': [], 'rows':[]}
        with ses.transaction() as tr:
            try:
                result = tr.execute_sql(cmd)
                data['rows'] = list(result)
                for f in result.fields:
                    data['fields'].append({'name': f.name, 'type': f.type_.code.name})
            except Exception as e:
                logging.error(e)
        logging.debug("Data received: %s", data)
        return data


    def insert_data(self, table, columns, values):
        logging.info("Inserting data table: %s, columns: %s, values: %s", table, columns, values)
        ses = self.get_session()
        with ses.transaction() as tr:
            tr.insert(table, columns=columns, values=values)
        logging.info("Committed %s", tr.committed)
        
    def update_data(self, table, columns, values):
        logging.info("Updating data table: %s, columns: %s, values: %s", table, columns, values)
        ses = self.get_session()
        with ses.transaction() as tr:
            tr.update(table, columns=columns, values=values)
        logging.info("Committed %s", tr.committed)

    def create_table(self, cmd):
        logging.info("Executing update_ddl: %s", cmd)
        operation = self.database.update_ddl([cmd])
        operation.result()
        logging.info("Update metadata: %s", operation.metadata)

    def get_tables(self):
        logging.info("Getting list of tables")
        tables = []
        for tbl in self.database.list_tables():
            tables.append(tbl.table_id)
        logging.info("Tables: %s", tables)
        return list(tables)

    def get_databases(self):
        logging.info("Getting databases")
        databases = []
        for db in self.instance.list_databases():
            databases.append(os.path.split(db.name)[1])
        logging.info("Databases %s", databases)
        return databases
