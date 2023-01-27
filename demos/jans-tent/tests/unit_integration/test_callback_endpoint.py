from unittest import TestCase
import clientapp
from flask import Flask, url_for
from typing import List
import os


def app_endpoints(app: Flask) -> List[str]:
    """ Return all enpoints in app """

    endpoints = []
    for item in app.url_map.iter_rules():
        endpoint = item.endpoint.replace("_", "-")
        endpoints.append(endpoint)
    return endpoints


class FlaskBaseTestCase(TestCase):
    def setUp(self):
        self.app = clientapp.create_app()
        self.app.testing = True
        self.app_context = self.app.test_request_context(
            base_url="https://chris.testingenv.org")
        self.app_context.push()
        self.client = self.app.test_client()
        #self.oauth = OAuth(self.app)
        os.environ['AUTHLIB_INSECURE_TRANSPORT'] = "1"


class TestCallbackEndpoint(FlaskBaseTestCase):
    def test_oidc_callback_endpoint_exist(self):
        endpoints = []
        for item in clientapp.create_app().url_map.iter_rules():
            endpoint = item.rule
            endpoints.append(endpoint)

        self.assertTrue('/oidc_callback' in endpoints,
                        "enpoint /oidc_callback knão existe no app")

    def test_callback_endpoint_should_exist(self):

        self.assertTrue('callback' in app_endpoints(clientapp.create_app()),
                        'endpoint /callback does not exist in app')

    def test_endpoint_args_without_code_should_return_400(self):
        resp = self.client.get(url_for('callback'))

        self.assertEqual(resp.status_code, 400)


'''
    def test_endpoint_should_return_status_code_302(self):
        # if there is

        self.assertEqual(
            self.client.get(url_for('callback')).status_code,
            302,
            'Callback endpoint is not returning 302 status_code'
            )


    #def test_endpoint_should_return_
'''
