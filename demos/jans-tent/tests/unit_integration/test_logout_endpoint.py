import clientapp
from helper import FlaskBaseTestCase, app_endpoints
from flask import url_for, session
from urllib import parse
from clientapp import config as cfg

class TestLogoutEndpoint(FlaskBaseTestCase):
    def authenticated_session_mock(self):
        with self.client.session_transaction() as session:
            session['id_token'] = 'id_token_stub'

    def test_endpoint_exists(self):
        self.assertIn(
            'logout',
            app_endpoints(clientapp.create_app())
        )

    def test_endpoint_should_require_authentication(self):
        ...
    def test_logout_endpoint_should_redirect_to_home_if_unauthenticated(self):
        # print(self.client.get(url_for('logout')).response)
        response = self.client.get(url_for('logout'))
        assert(response.status_code == 302)
        assert(response.location == url_for('index'))


    def test_logout_endpoint_should_clear_session(self):
        with self.client.session_transaction() as sess:
            sess['id_token'] = 'id_token_stub'
            sess['user'] = 'userinfo stub'

        with self.client:
            self.client.get(url_for('logout'))
            assert 'id_token' not in session
            assert 'user' not in session

    def test_endpoint_should_redirect_to_end_session_endpoint(self):
        with self.client.session_transaction() as session:
            session['id_token'] = 'id_token_stub'
            session['user'] = 'userinfo stub'

        response = self.client.get(url_for('logout'))

        parsed_location = parse.urlparse(response.location)
        assert parsed_location.scheme == 'https'
        assert parsed_location.netloc == 'ophostname.com'
        assert parsed_location.path == '/end_session_endpoint'



    def test_endpoint_should_redirect_to_end_session_endpoint_with_params(self):
        token_stub = 'id_token_stub'
        with self.client.session_transaction() as session:
            session['id_token'] = token_stub
            session['user'] = 'userinfo stub'

        parsed_redirect_uri = parse.urlparse(cfg.REDIRECT_URIS[0])
        post_logout_uri = '%s://%s' % (parsed_redirect_uri.scheme, parsed_redirect_uri.netloc)

        expected_query = 'post_logout_redirect_uri=%s&token_hint=%s' % (post_logout_uri, token_stub)
        response = self.client.get(url_for('logout'))

        parsed_location = parse.urlparse(response.location)
        assert parsed_location.query == expected_query

