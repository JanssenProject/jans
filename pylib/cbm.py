import os
import requests
import urllib3
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
from requests.auth import HTTPBasicAuth

try:
    requests.packages.urllib3.disable_warnings()
except:
    pass

class FakeResult:
    ok = False

class CBM:

    def __init__(self, host, admin, password, port=18091, n1qlport=18093):
        self.host = host
        self.port = port
        self.n1qlport = n1qlport
        self.auth = HTTPBasicAuth(admin, password)
        self.set_api_root()

    def set_api_root(self):
        self.api_root = 'https://{}:{}/'.format(self.host, self.port)
        self.n1ql_api = 'https://{}:{}/query/service'.format(self.host, self.n1qlport)

    def _get(self, endpoint):
        api = os.path.join(self.api_root, endpoint)

        try:
            result = requests.get(api, auth=self.auth, verify=False)
        except:
            result = FakeResult()
            result.reason = 'Connection failed'

        return result

    def _delete(self, endpoint):
        api = os.path.join(self.api_root, endpoint)
        result = requests.delete(api, auth=self.auth, verify=False)
        return result


    def _post(self, endpoint, data):
        url = os.path.join(self.api_root, endpoint)
        result = requests.post(url, data=data, auth=self.auth, verify=False)
        return result
        
    
    def get_buckets(self):
        
        return self._get('pools/default/buckets')


    def delete_bucket(self, bucket_name):
        return self._delete('pools/default/buckets/'+bucket_name)


    def add_bucket(self, bucket_name, ram_quota, bucket_type='couchbase'):
        data = {
                'name': bucket_name,
                'bucketType': bucket_type,
                'ramQuotaMB': ram_quota,
                'authType': 'sasl',
                
                }
        
        return self._post('pools/default/buckets', data)
        
    def get_certificate(self):

        result = self._get('pools/default/certificate')
        if result.ok:
            return result.text

        return ''
        

    def exec_query(self, query):
        data = {'statement': query}
        result = requests.post(self.n1ql_api, data=data, auth=self.auth, verify=False)

        return result

    def test_connection(self):
        result = self._get('pools/')

        return result.ok


    def initialize_node(self, path='/opt/couchbase/var/lib/couchbase/data',
                            index_path='/opt/couchbase/var/lib/couchbase/data'):

        data = {'path':path, 'index_path':index_path}
        result = self._post('nodes/self/controller/settings', data)

        return result


    def rename_node(self, hostname='127.0.0.1'):
        data = {'hostname': hostname}
        result = self._post('node/controller/rename', data)

        return result

    def set_index_storage_mode(self, mode='memory_optimized'):
        data = {'storageMode': mode}
        result = self._post('settings/indexes', data)

        return result


    def setup_services(self, services=['kv','n1ql','index','fts']):
        data = {'services': ','.join(services)}
        result = self._post('node/controller/setupServices', data)

        return result

    def set_admin_password(self):
        data = {
                    'password': self.auth.password,
                    'username': self.auth.username,
                     'port': 'SAME',
                 }

        result = self._post('settings/web', data)

        return result
