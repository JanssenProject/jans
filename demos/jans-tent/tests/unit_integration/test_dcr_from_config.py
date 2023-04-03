from clientapp.utils import dcr_from_config
from clientapp import config as cfg
from unittest.mock import MagicMock, patch
from unittest import TestCase
from clientapp.helpers.client_handler import ClientHandler

class TestDrcFromConfig(TestCase):
    def test_if_setup_logging_exists(self):
        assert hasattr(dcr_from_config, 'setup_logging')

    def test_if_static_variables_exists(self):
        assert hasattr(dcr_from_config, 'OP_URL')
        assert hasattr(dcr_from_config, 'REDIRECT_URIS')

    def test_if_static_variables_from_config(self):
        assert dcr_from_config.OP_URL == cfg.ISSUER
        assert dcr_from_config.REDIRECT_URIS == cfg.REDIRECT_URIS

    def test_register_should_be_calable(self):
        assert callable(dcr_from_config.register), 'not callable'

    @patch('clientapp.helpers.client_handler.ClientHandler.__init__', MagicMock(return_value=None))
    def test_register_should_call_ClientHandler(self):
        dcr_from_config.register()
        ClientHandler.__init__.assert_called_once()

    @patch('clientapp.helpers.client_handler.ClientHandler.__init__', MagicMock(return_value=None))
    def test_register_should_call_ClientHandler_with_params(self):
        dcr_from_config.register()
        ClientHandler.__init__.assert_called_once_with(cfg.ISSUER, cfg.REDIRECT_URIS)

