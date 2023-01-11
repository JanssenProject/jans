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
import base64
import logging
import os
from urllib.parse import urlparse

from authlib.integrations.flask_client import OAuth
from flask import (Flask, jsonify, redirect, render_template, request, session,
                   url_for)

from . import config as cfg
from .client_handler import ClientHandler

oauth = OAuth()

logging.basicConfig(
    level=logging.DEBUG,
    format='[%(asctime)s] %(levelname)s %(name)s in %(module)s : %(message)s',
    filename='test-client.log')
'''
dictConfig({
    'version': 1,
    'formatters': {'default': {
        'format': '[%(asctime)s] %(levelname)s %(name)s in %(module)s %(threadName)s: %(message)s',
    }},
    'handlers':
        {
        'wsgi': {
            'class': 'logging.StreamHandler',
            'stream': 'ext://flask.logging.wsgi_errors_stream',
            'formatter': 'default'
            },
        'file_handler': {
            'level': 'DEBUG',
            'filename': 'mylogfile.log',
            'class': 'logging.FileHandler',
            'formatter': 'default'

            }
        },

    'root': {
        'level': 'DEBUG',
        'handlers': ['file_handler'],
        'filename': 'demo.log'
    }

})
'''


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
    provider_host_string  = cfg.PROVIDER_HOST_STRING
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
        print("Here!")
        os.environ['CURL_CA_BUNDLE'] = ""


class BaseClientErrors(Exception):
    status_code = 500


def create_app():
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
                       'scope': 'openid profile email',
                       'acr_value': cfg.ACR_VALUES
                   },
                   token_endpoint_auth_method=cfg.SERVER_TOKEN_AUTH_METHOD)

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
        elif 'op_url' and 'client_url' not in content:
            status = 400
            # message = 'Not needed keys found in json'
        else:
            app.logger.info('Trying to register client %s on %s' %
                            (content['client_url'], content['op_url']))
            op_url = content['op_url']
            client_url = content['client_url']

            op_parsed_url = urlparse(op_url)
            client_parsed_url = urlparse(client_url)

            if op_parsed_url.scheme != 'https' or client_parsed_url.scheme != 'https':
                status = 400

            elif (((op_parsed_url.path != '' or op_parsed_url.query != '') or client_parsed_url.path != '') or client_parsed_url.query != ''):
                status = 400

            else:
                client_handler = ClientHandler(
                    content['op_url'],
                    content['client_url']
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
        if cfg.PRE_SELECTED_PROVIDER is True:
            query_args[
                'preselectedExternalProvider'] = get_preselected_provider()

        if cfg.ACR_VALUES is not None:
            query_args['acr_values'] = cfg.ACR_VALUES
        
        if cfg.PROVIDER_HOST_STRING is not None:
            query_args["providerHost"] = get_provider_host()

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
            print('exception!')
            print(error)
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
