import os
import requests
import urllib3
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
from requests.auth import HTTPBasicAuth
requests.packages.urllib3.disable_warnings()

class FakeResult:
    ok = False

class CBM:

    def __init__(self, host, admin, password):

        self.auth = HTTPBasicAuth(admin, password)
        self.api_root = 'https://{}:18091/'.format(host)
        self.n1ql_api = 'https://{}:18093/query/service'.format(host)


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
        api = os.path.join(self.api_root, endpoint)
        result = requests.post(api, data=data, auth=self.auth, verify=False)
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
