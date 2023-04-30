'''
Project: Test Auth Client
Author: Christian Hawk


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
import logging
import json
from httplib2 import RelativeURIError
from typing import Optional, Dict, Any

from oic.oauth2 import ASConfigurationResponse
from oic.oic import Client
from oic.utils.authn.client import CLIENT_AUTHN_METHOD
from .custom_msg_factory import CustomMessageFactory


logger = logging.getLogger(__name__)


class ClientHandler:
    __redirect_uris = None
    __client_id = None
    __client_secret = None
    __metadata_url = None
    __op_url = None
    __additional_metadata = None
    op_data = None

    def __init__(self, op_url: str, redirect_uris: list[str], additional_metadata: dict):
        """[initializes]

        :param op_url: [url from oidc provider starting with https]
        :type op_url: str
        :param redirect_uris: [url from client starting with https]
        :type redirect_uris: list
        :param additional_metadata: additional client metadata
        :type additional_metadata: dict
        """
        self.__additional_metadata = additional_metadata
        self.clientAdapter = Client(client_authn_method=CLIENT_AUTHN_METHOD, message_factory=CustomMessageFactory)
        self.__op_url = op_url
        self.__redirect_uris = redirect_uris
        self.__metadata_url = '%s/.well-known/openid-configuration' % op_url
        self.op_data = self.discover(op_url)
        self.reg_info = self.register_client(op_data=self.op_data, redirect_uris=redirect_uris)
        self.__client_id = self.reg_info['client_id']
        self.__client_secret = self.reg_info['client_secret']

    def get_client_dict(self) -> dict:
        r = {
            'op_metadata_url': self.__metadata_url,
            'client_id': self.__client_id,
            'client_secret': self.__client_secret
        }

        return r

    def register_client(self, op_data: ASConfigurationResponse = op_data, redirect_uris: Optional[list[str]] = __redirect_uris) -> dict:
        """[register client and returns client information]

        :param op_data: [description]
        :type op_data: dict
        :param redirect_uris: [description]
        :type redirect_uris: list[str]
        :return: [client information including client-id and secret]
        :rtype: dict
        """
        registration_args = {'redirect_uris': redirect_uris,
                             'response_types': ['code'],
                             'grant_types': ['authorization_code'],
                             'application_type': 'web',
                             'client_name': 'Jans Tent',
                             'token_endpoint_auth_method': 'client_secret_post',
                             **self.__additional_metadata
                             }
        reg_info = self.clientAdapter.register(op_data['registration_endpoint'], **registration_args)

        return reg_info

    def discover(self, op_url: Optional[str] = __op_url) -> ASConfigurationResponse:
        """Discover op information on .well-known/open-id-configuration
        :param op_url: [description], defaults to __op_url
        :type op_url: str, optional
        :return: [data retrieved from OP url]
        :rtype: ASConfigurationResponse
        """
        logger.debug('called discover')
        try:
            op_data = self.clientAdapter.provider_config(op_url)
            return op_data

        except json.JSONDecodeError as err:
            logger.error('Error trying to decode JSON: %s' % err)

        except RelativeURIError as err:
            logger.error(err)

        except Exception as e:
            logging.error('An unexpected ocurred: %s' % e)

