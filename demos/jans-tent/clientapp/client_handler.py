'''
Project: Test Auth Client
Author: Christian Hawk
Copyright 2023 Christian Hawk

Licensed under the Apache License, Version 2.0 (the 'License');
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an 'AS IS' BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
'''


from flask_oidc import registration, discovery
import json
from httplib2 import RelativeURIError
from typing import Optional


class ClientHandler:
    __client_url = None
    __client_id = None
    __client_secret = None
    __metadata_url= None
    __op_url = None
    op_data = None

    def __init__(self, op_url: str, client_url: str):
        """[intializes]

        :param op_url: [url from oidc provider starting with https]
        :type op_url: str
        :param client_url: [url from client starting with https]
        :type client_url: str
        """
        self.__op_url = op_url
        self.__client_url = client_url
        self.__metadata_url = '%s/.well-known/openid-configuration' % op_url
        self.op_data = self.discover(op_url)
        self.reg_info = self.register_client(op_data=self.op_data,client_url=client_url)
        self.__client_id = self.reg_info['web']['client_id']
        self.__client_secret = self.reg_info['web']['client_secret']



    def get_client_dict(self) -> dict:
        r = {
            'op_metadata_url' : self.__metadata_url,
            'client_id' : self.__client_id,
            'client_secret' : self.__client_secret
        }

        return r


    def register_client(self, op_data: Optional[dict]=op_data, client_url: Optional[str]=__client_url) -> dict:
        """[register client and returns client information]

        :param op_data: [description]
        :type op_data: dict
        :param client_url: [description]
        :type client_url: str
        :return: [client information including client-id and secret]
        :rtype: dict
        """
        redirect_uri = '%s/oidc_callback' % client_url
        reg_info = registration.register_client(op_data, [redirect_uri])
        return reg_info

    def discover(self, op_url: Optional[str]=__op_url, disc:discovery=discovery) -> dict :
        """Discover op information on .well-known/open-id-configuration
        :param op_url: [description], defaults to __op_url
        :type op_url: str, optional
        :param discovery: [flask_oidc.discovery injection], defaults to discovery
        :type discovery: discovery, optional
        :return: [data retrieved from OP url]
        :rtype: dict3
        """

        op_data = {}
        try:
            op_data = disc.discover_OP_information(op_url)
            return op_data

        except json.JSONDecodeError as err:
            print('Error trying to decode JSON: %s' % err)

        except RelativeURIError as err:
            print(err)

        except Exception as e:
            print('An unexpected ocurred: %s' % e)

        return op_data

