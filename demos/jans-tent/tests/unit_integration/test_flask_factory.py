from unittest import TestCase
import clientapp
from flask import Flask
import os


class TestFlaskApp(TestCase):
    def test_create_app_should_exist(self):
        self.assertEqual(hasattr(clientapp, 'create_app'), True,
                         'app factory does not exists')

    def test_create_app_should_be_inokable(self):
        self.assertEqual(callable(clientapp.create_app), True,
                         'cannot invoke create_app from clientapp')

    def test_create_app_should_return_a_flask_app(self):

        self.assertIsInstance(clientapp.create_app(), Flask,
                              'create_app is not returning a Flask instance')

    def test_if_app_has_secret_key(self):
        self.assertTrue(hasattr(clientapp.create_app(), 'secret_key'), )

    def test_if_secret_key_not_none(self):
        self.assertIsNotNone(clientapp.create_app().secret_key,
                             'app secret key is unexpectedly None')

    def test_if_oauth_is_app_extension(self):
        self.assertTrue('authlib.integrations.flask_client' in
                        clientapp.create_app().extensions)

    def test_if_settings_py_exists(self):
        self.assertTrue(os.path.exists('clientapp/config.py'),
                        'File clientapp/config.py does not exist')

    def test_if_op_client_id_exists_in_app_configuration(self):
        self.assertTrue('OP_CLIENT_ID' in clientapp.create_app().config,
                        'No OP_CLIENT_ID in app.config')

    def test_if_clientapp_has_cfg(self):
        self.assertTrue(hasattr(clientapp, 'cfg'))

    def test_if_cfg_is_module_from_configpy(self):
        self.assertTrue(
            os.path.relpath(clientapp.cfg.__file__) == 'clientapp/config.py')

        ...

    def test_if_OP_CLIENT_ID_is_equal_cfg_CLIENT_ID(self):
        self.assertEqual(clientapp.create_app().config['OP_CLIENT_ID'],
                         clientapp.cfg.CLIENT_ID)

    def test_if_OP_CLIENT_SECRET_exists_in_app_configuration(self):
        self.assertTrue('OP_CLIENT_SECRET' in clientapp.create_app().config,
                        'No OP_CLIENT_SECRET in app.config')

    def test_if_OP_CLIENT_SECRET_is_equal_cfg_CLIENT_ID(self):
        self.assertEqual(clientapp.create_app().config['OP_CLIENT_SECRET'],
                         clientapp.cfg.CLIENT_SECRET)

    def test_if_has_attr_ssl_verify(self):
        self.assertTrue(hasattr(clientapp, 'ssl_verify'),
                        'There is no ssl_verify in clientapp')

    def test_should_have_method_to_set_CA_CURL_CERT(self):
        self.assertTrue(clientapp.ssl_verify.__call__)
