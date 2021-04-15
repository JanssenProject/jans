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

class Spanner:

    def __init__(self):
        if Config.spanner_emulator:
            self.spanner_base_url = 'http://{}:9020/v1/'.format(Config.spanner_host)
        else:
            self.spanner_base_url = 'https://{}/v1/'.format(Config.spanner_host)

        self.spanner_instance_url = os.path.join(self.spanner_base_url, 'projects/{}/instances/{}/databases'.format(Config.spanner_project, Config.spanner_instance))
        self.spanner_dbase_url = os.path.join(self.spanner_instance_url, Config.spanner_database)

        base.logit("Spanner Api is constructed with base url {}".format(self.spanner_dbase_url))

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
        query_url = os.path.join(self.spanner_dbase_url, 'ddl')
        req = requests.patch(query_url, data=json.dumps(data))
        return req


    def get_tables(self):
        query_url = os.path.join(self.spanner_dbase_url, 'ddl')
        print(query_url)
        req = requests.get(query_url)

        tables = []

        result = req.json()
        for statement in result['statements']:
            table_name_re = re.search('CREATE TABLE (.+) \(', statement)
            if table_name_re:
                table_name = table_name_re.groups()[0]
            tables.append(table_name)
        
        return tables


    def get_databases(self):
        req = requests.get(self.spanner_instance_url)
        return req


s=Spanner()

#result = s.exec_sql('SELECT * from Class WHERE RoomNumber > "B"')
#print(result.json())
"""
print(s.create_table("CREATE TABLE MyClass ( ClassId INT64 NOT NULL, ClassName STRING(50), RoomNumber STRING(10)) PRIMARY KEY (ClassId)"))


#print(result.json())
s.put_data({
          "singleUseTransaction": {
            "readWrite": {}
          },
          "mutations": [
            {
              "insertOrUpdate": {
                "table": "MyClass",
                "columns": [
                  "ClassId",
                  "ClassName",
                  "RoomNumber"
                ],
                "values": [
                  [
                    "8",
                    "2BX",
                    "XA001"
                  ],
                  [
                    "11",
                    "3BY",
                    "YB002"
                  ]
                ]
              }
            }
          ]
        })

result = s.get_data({
                  "table": "MyClass",
                  "columns": [
                    "ClassId",
                    "ClassName",
                    "RoomNumber"
                  ],
                  "keySet": {
                    "all": True
                  }
                })
print(result.json())
"""

req = s.get_schema()
print(req)
