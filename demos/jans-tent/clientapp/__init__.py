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
import base64
import json
import os
from urllib.parse import urlparse
from authlib.integrations.flask_client import OAuth
from flask import (Flask, jsonify, redirect, render_template, request, session,
                   url_for)
from . import config as cfg
from .helpers.client_handler import ClientHandler
from .helpers.cgf_checker import register_client_if_no_client_info
from .utils.logger import setup_logger

setup_logger()

oauth = OAuth()


def add_config_from_json():
    with open('client_info.json', 'r') as openfile:
        client_info = json.load(openfile)
        cfg.SERVER_META_URL = client_info['op_metadata_url']
        cfg.CLIENT_ID = client_info['client_id']
        cfg.CLIENT_SECRET = client_info['client_secret']


def get_preselected_provider():
    provider_id_string = cfg.PRE_SELECTED_PROVIDER_ID
    provider_object = '{ "provider" : "%s" }' % provider_id_string
    provider_object_bytes = provider_object.encode()
    base64url_bytes = base64.urlsafe_b64encode(provider_object_bytes)
    base64url_value = base64url_bytes.decode()
    # if base64url_value.endswith('='):
    #     base64url_value_unpad = base64url_value.replace('=', '')
    #     return base64url_value_unpad
    return base64url_value


def get_provider_host():
    provider_host_string = cfg.PROVIDER_HOST_STRING
    provider_object = '{ "providerHost" : "%s" }' % provider_host_string
    provider_object_bytes = provider_object.encode()
    base64url_bytes = base64.urlsafe_b64encode(provider_object_bytes)
    base64url_value = base64url_bytes.decode()
    # if base64url_value.endswith('='):
    #     base64url_value_unpad = base64url_value.replace('=', '')
    #     return base64url_value_unpad
    return base64url_value


def ssl_verify(ssl_verify=cfg.SSL_VERIFY):
    if ssl_verify is False:
        os.environ['CURL_CA_BUNDLE'] = ""


class BaseClientErrors(Exception):
    status_code = 500


def create_app():
    register_client_if_no_client_info()
    add_config_from_json()
    ssl_verify()

    app = Flask(__name__)

    app.secret_key = b'fasfafpj3rasdaasfglaksdgags331s'
    app.config['OP_CLIENT_ID'] = cfg.CLIENT_ID
    app.config['OP_CLIENT_SECRET'] = cfg.CLIENT_SECRET
    oauth.init_app(app)
    oauth.register(
            'op',
            server_metadata_url=cfg.SERVER_META_URL,
            client_kwargs={
                'scope': cfg.SCOPE
            },
            token_endpoint_auth_method=cfg.SERVER_TOKEN_AUTH_METHOD
            )

    @app.route('/')
    def index():
        user = session.get('user')
        id_token = session.get('id_token')
        return render_template("home.html", user=user, id_token=id_token)

    @app.route('/register', methods=['POST'])
    def register():
        app.logger.info('/register called')
        content = request.json
        app.logger.debug('data = %s' % content)
        status = 0
        data = ''
        if content is None:
            status = 400
            # message = 'No json data posted'
        elif 'op_url' and 'redirect_uris' not in content:
            status = 400
            # message = 'Not needed keys found in json'
        else:
            app.logger.info('Trying to register client %s on %s' %
                            (content['redirect_uris'], content['op_url']))
            op_url = content['op_url']
            redirect_uris = content['redirect_uris']

            op_parsed_url = urlparse(op_url)
            client_parsed_redirect_uri = urlparse(redirect_uris[0])

            if op_parsed_url.scheme != 'https' or client_parsed_redirect_uri.scheme != 'https':
                status = 400

            elif (((
                           op_parsed_url.path != '' or op_parsed_url.query != '') or client_parsed_redirect_uri.path == '') or client_parsed_redirect_uri.query != ''):
                status = 400

            else:
                additional_metadata = {}
                if 'additional_params' in content.keys():
                    additional_metadata = content['additional_params']
                client_handler = ClientHandler(
                   content['op_url'], content['redirect_uris'], additional_metadata
                )
                data = client_handler.get_client_dict()
                status = 200
        return jsonify(data), status

    @app.route('/protected-content', methods=['GET'])
    def protected_content():
        app.logger.debug('/protected-content - cookies = %s' % request.cookies)
        app.logger.debug('/protected-content - session = %s' % session)
        if 'user' in session:
            return session['user']

        return redirect(url_for('login'))

    @app.route('/login')
    def login():
        app.logger.info('/login requested')
        redirect_uri = cfg.REDIRECT_URIS[0]
        app.logger.debug('/login redirect_uri = %s' % redirect_uri)
        # response = oauth.op.authorize_redirect()
        query_args = {
            'redirect_uri': redirect_uri,
        }

        if cfg.ACR_VALUES is not None:
            query_args['acr_values'] = cfg.ACR_VALUES

        # used for inbound-saml, uncomment and set config.py to use it
        # if cfg.PRE_SELECTED_PROVIDER is True:
        #     query_args[
        #         'preselectedExternalProvider'] = get_preselected_provider()

        # used for gluu-passport, , uncomment and set config.py to use it
        # if cfg.PROVIDER_HOST_STRING is not None:
        #     query_args["providerHost"] = get_provider_host()

        if cfg.ADDITIONAL_PARAMS is not None:
            query_args |= cfg.ADDITIONAL_PARAMS

        response = oauth.op.authorize_redirect(**query_args)

        app.logger.debug('/login authorize_redirect(redirect_uri) url = %s' %
                         (response.location))

        return response

    @app.route('/oidc_callback')
    @app.route('/callback')
    def callback():
        try:
            if not request.args['code']:
                return {}, 400

            app.logger.info('/callback - received %s - %s' %
                            (request.method, request.query_string))
            token = oauth.op.authorize_access_token()
            app.logger.debug('/callback - token = %s' % token)
            user = oauth.op.userinfo()
            app.logger.debug('/callback - user = %s' % user)
            session['user'] = user
            app.logger.debug('/callback - cookies = %s' % request.cookies)
            app.logger.debug('/callback - session = %s' % session)
            session['id_token'] = token['userinfo']

            return redirect('/')

        except Exception as error:
            app.logger.error(str(error))
            return {'error': str(error)}, 400

    @app.route("/configuration", methods=["POST"])
    def configuration():
        # Receives client configuration via API
        app.logger.info('/configuration called')
        content = request.json
        app.logger.debug("content = %s" % content)
        if content is not None:
            if 'provider_id' in content:
                cfg.PRE_SELECTED_PROVIDER_ID = content['provider_id']
                cfg.PRE_SELECTED_PROVIDER = True
                app.logger.debug('/configuration: provider_id = %s' %
                                 content['provider_id'])

                return jsonify({"provider_id": content['provider_id']}), 200

            if "client_id" in content and "client_secret" in content:
                # Setup client_id and client_secret
                oauth.op.client_id = content['client_id']
                oauth.op.client_secret = content['client_secret']
                return {}, 200
        else:
            return {}, 400

    return app
