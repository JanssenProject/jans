from unittest import TestCase


import clientapp
import helper
import os
from flask import url_for
from clientapp.helpers.client_handler import ClientHandler
from unittest.mock import MagicMock, patch


class TestRegisterEndpoint(TestCase):

    def setUp(self):
        self.app = clientapp.create_app()
        self.app.testing = True
        self.app_context = self.app.test_request_context()
        self.app_context.push()
        self.client = self.app.test_client()
        # self.oauth = OAuth(self.app)
        os.environ['AUTHLIB_INSECURE_TRANSPORT'] = "1"
        # stashing to restore on teardown
        self.CHd = ClientHandler.discover
        self.register = ClientHandler.register_client

        ClientHandler.discover = MagicMock(name='discover')
        # discover will always return a valid data
        ClientHandler.discover.return_value = helper.OP_DATA_DICT_RESPONSE
        ClientHandler.register_client = MagicMock(name='register_client')
        ClientHandler.register_client.return_value = helper.REGISTER_CLIENT_RESPONSE

    def tearDown(self):
        # restoring original method from stash
        ClientHandler.discover = self.CHd
        ClientHandler.register_client = self.register

    def test_if_app_has_register_endpoint(self):
        self.assertIn(
            'register',
            helper.app_endpoints(clientapp.create_app())
        )

    def test_if_endpoint_accepts_post(self):
        methods = None
        for rule in self.app.url_map.iter_rules('register'):
            methods = rule.methods
        self.assertIn(
            'POST',
            methods
        )

    # def test_init_should_call_discover_once(self):
    #     ClientHandler.discover = MagicMock(name='discover')
    #     ClientHandler.discover.return_value = helper.OP_DATA_DICT_RESPONSE
    #     ClientHandler.discover.assert_called_once()

    def test_endpoint_should_return_valid_req(self):
        self.assertIn(
            self.client.post(url_for('register')).status_code,
            range(100, 511),
            '/register returned invalid requisition'
        )

    @patch('clientapp.helpers.client_handler.ClientHandler.__init__', MagicMock(return_value=None))
    def test_endpoint_should_init_client_handler(self):
        self.client.post(url_for('register'), json={
            'op_url': 'https://test.com',
            'redirect_uris': ['https://clienttoberegistered.com/oidc_callback']
        })
        ClientHandler.__init__.assert_called_once()

    @patch('clientapp.helpers.client_handler.ClientHandler.__init__', MagicMock(return_value=None))
    def test_endpoint_should_accept_2_params(self):
        first_value = 'https://op'
        second_value = ['https://client.com.br/oidc_callback']
        self.client.post(url_for('register'), json={
            'op_url': first_value,
            'redirect_uris': second_value
        })
        ClientHandler.__init__.assert_called_once_with(first_value, second_value)

    def test_endpoint_should_return_error_code_400_if_no_data_sent(self):
        self.assertEqual(
            self.client.post(url_for('register')).status_code,
            400,
            'status_code for empty request is NOT 400'
        )

    def test_should_return_400_error_if_no_needed_keys_provided(self):
        self.assertEqual(
            self.client.post(url_for('register'), json={
                'other_key': 'othervalue',
                'another_key': 'another_value'
            }).status_code,
            400,
            'not returning 400 code if no needed keys provided'
        )

    def test_should_return_400_if_values_are_not_valid_urls(self):
        self.assertEqual(
            self.client.post(url_for('register'), json={
                'op_url': 'not_valid_url',
                'redirect_uris': ['https://clienttoberegistered.com/oidc_callback']
            }).status_code,
            400,
            'not returning status 400 if values are not valid urls'
        )

    @patch('clientapp.helpers.client_handler.ClientHandler.get_client_dict', MagicMock(return_value=None))
    def test_valid_post_should_should_call_get_client_dict_once(self):
        op_url = 'https://op.com.br'
        self.client.post(url_for('register'), json={
            'op_url': op_url,
            'redirect_uris': ['https://clienttoberegistered.com/oidc_callback']
        })
        ClientHandler.get_client_dict.assert_called_once()

    def test_should_should_return_200_if_registered(self):
        op_url = 'https://op.com.br'
        test_client_id = '1234-5678-9ten11'
        test_client_secret = 'mysuperprotectedsecret'
        with patch.object(ClientHandler, 'get_client_dict', return_value={
            'op_metadata_url': '%s/.well-known/open-id-configuration' % op_url,
            'client_id': test_client_id,
            'client_secret': test_client_secret
        }) as get_client_dict:
            response = self.client.post(url_for('register'), json={
                'op_url': op_url,
                'redirect_uris': ['https://clienttoberegistered.com/oidc_callback']
            })
            self.assertEqual(response.status_code, 200)
            get_client_dict.reset()

    def test_should_return_expected_keys(self):
        op_url = 'https://op.com.br'
        redirect_uris = ['https://client.com.br/oidc_calback']
        test_client_id = '1234-5678-9ten11'
        test_client_secret = 'mysuperprotectedsecret'

        expected_keys = {'op_metadata_url', 'client_id', 'client_secret'}

        with patch.object(ClientHandler, 'get_client_dict', return_value={
            'op_metadata_url': '%s/.well-known/open-id-configuration' % op_url,
            'client_id': test_client_id,
            'client_secret': test_client_secret
        }) as get_client_dict:
            response = self.client.post(url_for('register'), json={
                'op_url': op_url,
                'redirect_uris': redirect_uris
            })
            print(response)
            assert expected_keys <= response.json.keys(), response.json

            get_client_dict.reset()
