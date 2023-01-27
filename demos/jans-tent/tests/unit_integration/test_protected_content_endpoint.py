from unittest import TestCase
from clientapp import create_app, session
from flask import Flask, url_for
from typing import List
from werkzeug import local
import os


def app_endpoint(app: Flask) -> List[str]:
    """ Return all enpoints in app """

    endpoints = []
    for item in app.url_map.iter_rules():
        endpoint = item.endpoint.replace("_", "-")
        endpoints.append(endpoint)
    return endpoints


class FlaskBaseTestCase(TestCase):
    def setUp(self):
        self.app = create_app()
        self.app.testing = True
        self.app_context = self.app.test_request_context(
            base_url="https://chris.testingenv.org")
        self.app_context.push()
        self.client = self.app.test_client()

        #self.oauth = OAuth(self.app)
        os.environ['AUTHLIB_INSECURE_TRANSPORT'] = "1"
        '''
        self.oauth.register('op',
            server_metadata_url = 'https://chris.gluuthree.org/.well-known/openid-configuration',
            client_kwargs = {'scope' : 'openid'})
        '''


class TestProtectedContentEndpoint(FlaskBaseTestCase):
    def test_app_should_contain_protected_content_route(self):

        endpoints = app_endpoint(create_app())
        self.assertIn('protected-content', endpoints,
                      'protected-content route not found in app endpoints')

    def test_app_protected_content_route_should_return_valid_requisition(self):

        response = self.client.get(url_for('protected_content'))

        self.assertIn(
            self.client.get(url_for('protected_content')).status_code,
            range(100, 511),
            'protected content route returned invalid requisition')

    def test_should_return_if_session_exists_in_clientapp(self):
        import clientapp
        self.assertTrue(hasattr(clientapp, 'session'),
                        "session is not an attribute of clientapp")
        del clientapp

    def test_should_check_if_session_is_LocalProxy_instance(self):
        self.assertIsInstance(session, local.LocalProxy)

    def test_protected_content_return_status_200_ir_session_profile_exists(
            self):

        with self.client.session_transaction() as sess:
            sess['user'] = 'foo'

        self.assertEqual(
            self.client.get(url_for('protected_content')).status_code, 200)

    def test_should_return_302_if_no_session_profile(self):
        self.assertEqual(
            self.client.get(url_for('protected_content')).status_code, 302)

    def test_protected_content_should_redirect_to_login_if_session_profile_doesnt_exist(
            self):

        response = self.client.get(url_for('protected_content'))
        self.assertTrue(response.location.endswith(url_for('login')),
                        'Protected page is not redirecting to login page')

    ''' TODO
    def test_should_return_if_user_logged_in_exists(self):
        self.assertTrue(
            hasattr(app,'user_logged_in')
        )
    '''
