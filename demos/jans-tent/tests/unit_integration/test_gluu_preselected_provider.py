import clientapp
from helper import FlaskBaseTestCase


class TestPreselectedProvider(FlaskBaseTestCase):

    # """
    # We should be able to send Preselected passport provider to gluu OIDC as a authorization param
    # like this: preselectedExternalProvider=<base64-url-encoded-provider-object>
    # Where <base64-url-encoded-provider-object> is the Base64-encoded representation of a small JSON
    # content that looking like this:
    # { "provider" : <provider-ID> }
    # """
    def setUp(self):
        clientapp.cfg.PRE_SELECTED_PROVIDER = True
        FlaskBaseTestCase.setUp(FlaskBaseTestCase)

    def test_config_should_have_preselected_provider_option(self):
        self.assertTrue(hasattr(clientapp.cfg, 'PRE_SELECTED_PROVIDER'),
                        'cfg doesnt have PRE_SELECTED_PROVIDER attribute')

    def test_config_pre_selected_provider_should_be_boolean(self):
        self.assertTrue(
            type(clientapp.cfg.PRE_SELECTED_PROVIDER) == bool,
            'cfg.PRE_SELECTED_PROVIDER is not bool')

    def test_preselected_provider_id_should_exist_in_cfg(self):
        self.assertTrue(hasattr(clientapp.cfg, 'PRE_SELECTED_PROVIDER_ID'))

    def test_clientapp_should_have_get_preselected_provider(self):
        self.assertTrue(
            hasattr(clientapp, 'get_preselected_provider'),
            'client app does not have get_preselected_provider attr')

    def test_get_preselected_provider_should_be_callable(self):
        self.assertTrue(callable(clientapp.get_preselected_provider),
                        'get_preselected_provider is not callable')

    def test_get_selected_provider_should_return_base64(self):

        clientapp.cfg.PRE_SELECTED_PROVIDER_ID = 'saml-emaillink'
        expected_response = "eyAicHJvdmlkZXIiIDogInNhbWwtZW1haWxsaW5rIiB9"
        self.assertEqual(clientapp.get_preselected_provider(),
                         expected_response)


