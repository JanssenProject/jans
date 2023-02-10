from unittest import TestCase
from unittest.mock import MagicMock
import sys
import inspect
import clientapp.client_handler as client_handler
from typing import Optional
import helper

ClientHandler = client_handler.ClientHandler


# helper
def get_class_instance(op_url='https://t1.techno24x7.com',
                       client_url='https://mock.test.com'):
    client_handler_obj = ClientHandler(op_url, client_url)
    return client_handler_obj


class TestDynamicClientRegistration(TestCase):

    def test_if_registration_is_imported_in_sys(self):
        self.assertIn('flask_oidc.registration', sys.modules,
                      'flask_oidc.registration not found in sys')

    def test_if_registration_exists(self):
        self.assertTrue(hasattr(client_handler, 'registration'))

    def test_if_registration_is_flask_oidc_package(self):
        self.assertTrue(
            client_handler.registration.__package__ == 'flask_oidc',
            'registration is not from flask_oidc package')

    def test_if_discovery_is_imported(self):
        self.assertIn('flask_oidc.discovery', sys.modules)

    def test_if_discovery_is_flask_oidc_package(self):
        self.assertTrue(client_handler.discovery.__package__ == 'flask_oidc',
                        'discovery is not from flask_oidc package')

    def test_if_discovery_exists(self):
        self.assertTrue(hasattr(client_handler, 'discovery'))

    def test_if_json_exists(self):
        self.assertTrue(hasattr(client_handler, 'json'),
                        'json does not exists in client_handler')

    def test_if_json_is_from_json_package(self):
        self.assertTrue(client_handler.json.__package__ == 'json',
                        'json is not from json')

    # testing ClientHandler class
    def test_if_ClientHandler_is_class(self):
        self.assertTrue(inspect.isclass(ClientHandler))

    def test_if_register_client_exists(self):
        self.assertTrue(hasattr(ClientHandler, 'register_client'),
                        'register_client does not exists in ClientHandler')

    def test_if_register_client_is_callable(self):
        self.assertTrue(callable(ClientHandler.register_client),
                        'register_client is not callable')

    def test_if_register_client_receives_params(self):
        expected_args = ['self', 'op_data', 'client_url']
        self.assertTrue(
            inspect.getfullargspec(
                ClientHandler.register_client).args == expected_args,
            'register_client does not receive expected args')

    def test_if_register_client_params_are_expected_type(self):
        insp = inspect.getfullargspec(ClientHandler.register_client)
        self.assertTrue(
            insp.annotations['op_data'] == Optional[dict]
            and insp.annotations['client_url'] == Optional[str],
            'register_client is not receiving the right params')

    def test_if_class_has_initial_expected_attrs(self):
        initial_expected_attrs = [
            '_ClientHandler__client_id',
            '_ClientHandler__client_secret',
            '_ClientHandler__client_url',
            '_ClientHandler__metadata_url',
            'discover',  # method
            'register_client'  # method
        ]

        self.assertTrue(
            all(attr in ClientHandler.__dict__.keys()
                for attr in initial_expected_attrs),
            'ClientHandler does not have initial attrs')

    def test_if_discover_exists(self):
        self.assertTrue(hasattr(ClientHandler, 'discover'),
                        'discover does not exists in ClientHandler')

    def test_if_discover_is_callable(self):
        self.assertTrue(callable(ClientHandler.discover),
                        'discover is not callable')

    def test_if_discover_receives_params(self):
        expected_args = ['self', 'op_url', 'disc']
        self.assertTrue(
            inspect.getfullargspec(
                ClientHandler.discover).args == expected_args,
            'discover does not receive expected args')

    def test_if_discover_params_are_expected_type(self):
        insp = inspect.getfullargspec(ClientHandler.discover)
        self.assertTrue(
            insp.annotations['op_url'] == Optional[str]
            and insp.annotations['disc'] == client_handler.discovery,
            'discover is not receiving the right params')

    def test_discover_should_not_return_none_when_non_existant_config(self):
        self.assertIsNotNone(
            ClientHandler.discover(ClientHandler, 'https://google.com'))

    def test_discover_should_return_empty_dict_when_error(self):
        self.assertEqual(
            ClientHandler.discover(ClientHandler, 'ggg:asddasd.caccasddas'),
            {}, 'not returning empty dict on error')

    def test_discover_should_return_valid_dict(self):
        """[Checks if returns main keys]
        """

        main_keys = {
            'issuer', 'authorization_endpoint', 'token_endpoint',
            'userinfo_endpoint', 'clientinfo_endpoint',
            'session_revocation_endpoint', 'end_session_endpoint',
            'revocation_endpoint', 'registration_endpoint'
        }

        discover_stash = ClientHandler.discover
        ClientHandler.discover = MagicMock(name='discover')
        ClientHandler.discover.return_value = helper.OP_DATA_DICT_RESPONSE
        op_data = ClientHandler.discover(ClientHandler,
                                         'https://t1.techno24x7.com')
        ClientHandler.discover = discover_stash
        self.assertTrue(main_keys <= set(op_data),
                        'discovery return data does not have main keys')

    def test_if_get_client_dict_exists(self):
        self.assertTrue(hasattr(ClientHandler, 'get_client_dict'),
                        'get_client_dict does not exists in ClientHandler')

    def test_if_get_client_dict_is_callable(self):
        self.assertTrue(callable(ClientHandler.get_client_dict),
                        'get_client_dict is not callable')

    def test_if_get_client_dict_receives_params(self):
        expected_args = ['self']
        self.assertTrue(
            inspect.getfullargspec(
                ClientHandler.get_client_dict).args == expected_args,
            'get_client_dict does not receive expected args')

    def test_client_id_should_return_something(self):
        self.assertIsNotNone(
            ClientHandler.get_client_dict(ClientHandler),
            'get_client_dict returning NoneType. It has to return something!')

    def test_get_client_dict_should_return_a_dict(self):
        self.assertIsInstance(ClientHandler.get_client_dict(ClientHandler),
                              dict, 'get_client_dict is not returning a dict')

    def test_class_init_should_set_op_url(self):
        register_client_stash = ClientHandler.register_client
        ClientHandler.register_client = MagicMock(name='register_client')
        ClientHandler.register_client.return_value = helper.REGISTER_CLIENT_RESPONSE
        discover_stash = ClientHandler.discover
        ClientHandler.discover = MagicMock(name='discover')
        ClientHandler.discover.return_value = helper.OP_DATA_DICT_RESPONSE
        op_url = 'https://t1.techno24x7.com'

        client_handler_obj = get_class_instance(op_url)

        ClientHandler.register_client = register_client_stash
        ClientHandler.discover = discover_stash

        self.assertEqual(client_handler_obj.__dict__['_ClientHandler__op_url'],
                         op_url)

    def test_class_init_should_set_client_url(self):
        register_client_stash = ClientHandler.register_client
        ClientHandler.register_client = MagicMock(name='register_client')
        ClientHandler.register_client.return_value = helper.REGISTER_CLIENT_RESPONSE
        discover_stash = ClientHandler.discover
        ClientHandler.discover = MagicMock(name='discover')
        ClientHandler.discover.return_value = helper.OP_DATA_DICT_RESPONSE

        op_url = 'https://t1.techno24x7.com'
        client_url = 'https://mock.test.com'
        client_handler_obj = ClientHandler(op_url, client_url)

        ClientHandler.register_client = register_client_stash
        ClientHandler.discover = discover_stash

        self.assertEqual(
            client_handler_obj.__dict__['_ClientHandler__client_url'],
            client_url)

    def test_class_init_should_set_metadata_url(self):
        register_client_stash = ClientHandler.register_client
        ClientHandler.register_client = MagicMock(name='register_client')
        ClientHandler.register_client.return_value = helper.REGISTER_CLIENT_RESPONSE
        discover_stash = ClientHandler.discover
        ClientHandler.discover = MagicMock(name='discover')
        ClientHandler.discover.return_value = helper.OP_DATA_DICT_RESPONSE
        op_url = 'https://t1.techno24x7.com'

        client_handler_obj = get_class_instance(op_url)

        ClientHandler.register_client = register_client_stash
        ClientHandler.discover = discover_stash

        expected_metadata_url = op_url + '/.well-known/openid-configuration'

        self.assertEqual(
            client_handler_obj.__dict__['_ClientHandler__metadata_url'],
            expected_metadata_url)

    def test_class_init_should_have_docstring(self):
        self.assertTrue(ClientHandler.__init__.__doc__,
                        'ClientHandler.__init__ has doc')

    def test_if_get_client_dict_return_expected_keys(self):
        expected_keys = [
            'op_metadata_url',
            'client_id',
            'client_secret',
        ]

        register_client_stash = ClientHandler.register_client
        ClientHandler.register_client = MagicMock(name='register_client')
        ClientHandler.register_client.return_value = helper.REGISTER_CLIENT_RESPONSE
        discover_stash = ClientHandler.discover
        ClientHandler.discover = MagicMock(name='discover')
        ClientHandler.discover.return_value = helper.OP_DATA_DICT_RESPONSE

        client_handler_obj = get_class_instance()
        client_dict = client_handler_obj.get_client_dict()

        ClientHandler.register_client = register_client_stash
        ClientHandler.discover = discover_stash

        self.assertTrue(
            all(key in client_dict.keys() for key in expected_keys),
            'there is no %s IN %s: get_client_dict is NOT returning expected keys'
            % (str(expected_keys), str(client_dict.keys())))

    def test_get_client_dict_values_cannot_be_none(self):
        register_client_stash = ClientHandler.register_client
        ClientHandler.register_client = MagicMock(name='register_client')
        ClientHandler.register_client.return_value = helper.REGISTER_CLIENT_RESPONSE
        discover_stash = ClientHandler.discover
        ClientHandler.discover = MagicMock(name='discover')
        ClientHandler.discover.return_value = helper.OP_DATA_DICT_RESPONSE

        op_url = 'https://t1.techno24x7.com'

        client_handler_obj = get_class_instance(op_url)

        client_dict = client_handler_obj.get_client_dict()

        ClientHandler.register_client = register_client_stash
        ClientHandler.discover = discover_stash

        for key in client_dict.keys():
            self.assertIsNotNone(client_dict[key],
                                 'get_client_dict[%s] cannot be None!' % key)

    def test_get_client_dict_should_return_url_metadata_value(self):
        register_client_stash = ClientHandler.register_client
        ClientHandler.register_client = MagicMock(name='register_client')
        ClientHandler.register_client.return_value = helper.REGISTER_CLIENT_RESPONSE
        discover_stash = ClientHandler.discover
        ClientHandler.discover = MagicMock(name='discover')
        ClientHandler.discover.return_value = helper.OP_DATA_DICT_RESPONSE

        client_handler_obj = get_class_instance()

        ClientHandler.register_client = register_client_stash
        ClientHandler.discover = discover_stash

        self.assertEqual(
            client_handler_obj.get_client_dict()['op_metadata_url'],
            client_handler_obj._ClientHandler__metadata_url)

    def test_get_client_dict_should_return_client_id_value(self):
        register_client_stash = ClientHandler.register_client
        ClientHandler.register_client = MagicMock(name='register_client')
        ClientHandler.register_client.return_value = helper.REGISTER_CLIENT_RESPONSE
        discover_stash = ClientHandler.discover
        ClientHandler.discover = MagicMock(name='discover')
        ClientHandler.discover.return_value = helper.OP_DATA_DICT_RESPONSE

        client_handler_obj = get_class_instance()

        ClientHandler.register_client = register_client_stash
        ClientHandler.discover = discover_stash

        self.assertEqual(
            client_handler_obj.get_client_dict()['client_id'],
            client_handler_obj._ClientHandler__client_id
        )

    def test_init_should_call_discover_once(self):
        register_client_stash = ClientHandler.register_client
        ClientHandler.register_client = MagicMock(name='register_client')
        ClientHandler.register_client.return_value = helper.REGISTER_CLIENT_RESPONSE
        discover_stash = ClientHandler.discover
        ClientHandler.discover = MagicMock(name='discover')
        ClientHandler.discover.return_value = helper.OP_DATA_DICT_RESPONSE

        get_class_instance()

        ClientHandler.discover.assert_called_once()

        ClientHandler.register_client = register_client_stash
        ClientHandler.discover = discover_stash

    def test_init_should_call_register_client_once(self):
        register_client_stash = ClientHandler.register_client
        ClientHandler.register_client = MagicMock(name='register_client')
        ClientHandler.register_client.return_value = helper.REGISTER_CLIENT_RESPONSE
        discover_stash = ClientHandler.discover
        ClientHandler.discover = MagicMock(name='discover')
        ClientHandler.discover.return_value = helper.OP_DATA_DICT_RESPONSE

        get_class_instance()
        ClientHandler.register_client = register_client_stash
        ClientHandler.discover = discover_stash

        ClientHandler.register_client.assert_called_once()
