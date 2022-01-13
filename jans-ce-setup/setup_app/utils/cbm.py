import os
import requests
import urllib3
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
from requests.auth import HTTPBasicAuth
from setup_app.utils.base import logIt

try:
    requests.packages.urllib3.disable_warnings()
except:
    pass

class FakeResult:
    ok = False
    reason = ''
    text = ''

    def json(self):
        return {'error': True}

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
        except Exception as e:
            result = FakeResult()
            result.reason = 'Connection failed. Reason: ' + str(e)
        self.logIfError(result)
        return result

    def _delete(self, endpoint):
        api = os.path.join(self.api_root, endpoint)
        result = requests.delete(api, auth=self.auth, verify=False)
        self.logIfError(result)
        return result


    def _post(self, endpoint, data):
        url = os.path.join(self.api_root, endpoint)
        result = requests.post(url, data=data, auth=self.auth, verify=False)
        self.logIfError(result)
        return result
    
    def _put(self,  endpoint, data):
        url = os.path.join(self.api_root, endpoint)
        result = requests.put(url, data=data, auth=self.auth, verify=False)
        self.logIfError(result)
        return result

    def get_system_info(self):
        result = self._get('pools/default')
        if result.ok:
            return result.json()
        return {}

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
        logIt("Executing n1ql {}".format(query))
        data = {'statement': query}
        result = requests.post(self.n1ql_api, data=data, auth=self.auth, verify=False)
        self.logIfError(result)
        return result

    def test_connection(self):
        result = self._get('pools/')
        return result 


    def initialize_node(self, path='/opt/couchbase/var/lib/couchbase/data',
                            index_path='/opt/couchbase/var/lib/couchbase/data'):

        data = {'path':path, 'index_path':index_path}
        result = self._post('nodes/self/controller/settings', data)

        return result


    def rename_node(self, hostname='127.0.0.1'):
        data = {'hostname': hostname}
        try:
            result = self._post('node/controller/rename', data)
        except Exception as e:
            result = FakeResult()
            result.reason = 'Node rename failed. Reason: ' + str(e)

        return result

    def set_index_storage_mode(self, mode='plasma'):
        data = {'storageMode': mode}
        result = self._post('settings/indexes', data)

        return result

    def set_index_memory_quta(self, ram_quota=256):
        data = {'indexMemoryQuota': ram_quota}
        result = self._post('pools/default', data)

        return result



    def setup_services(self, services=['kv','n1ql','index']):
        data = {'services': ','.join(services)}
        result = self._post('node/controller/setupServices', data)

        return result

    def get_services(self):
        result = self._get('pools/default/nodeServices')
        return result

    def set_admin_password(self):
        data = {
                    'password': self.auth.password,
                    'username': self.auth.username,
                    'port': 'SAME',
                 }

        result = self._post('settings/web', data)

        return result

    def create_user(self, username, password, fullname, roles):
        data = {
                    'name': fullname,
                    'password': password,
                    'roles': roles,
                 }

        result = self._put('settings/rbac/users/local/'+username, data)

        return result

    def whoami(self):
        result = self._get('whoami')
        return result.json()

    def logIfError(self, result):
        try:
            js = result.json()
            if 'errors' in js:
                msg = "Error executing query: {}".format(', '.join([err['msg'] for err in js['errors']]))
                logIt(msg)
                logIt(msg, True)
            else:
                logIt("Query Result: {}".format(str(js)))
        except:
            pass

