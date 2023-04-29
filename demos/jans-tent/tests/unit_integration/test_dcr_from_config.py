from clientapp.utils import dcr_from_config
from clientapp import config as cfg
from unittest.mock import MagicMock, patch, mock_open
from unittest import TestCase
from clientapp.helpers.client_handler import ClientHandler
import helper
import json
import builtins

class TestDrcFromConfig(TestCase):

    def setUp(self) -> None:
        # stashing to restore on teardown
        self.stashed_discover = ClientHandler.discover
        self.stashed_register_client = ClientHandler.register_client
        self.stashed_open = builtins.open
        ClientHandler.discover = MagicMock(name='discover')
        ClientHandler.discover.return_value = helper.OP_DATA_DICT_RESPONSE
        ClientHandler.register_client = MagicMock(name='register_client')
        ClientHandler.register_client.return_value = helper.REGISTER_CLIENT_RESPONSE
        builtins.open = MagicMock(name='open')

    def tearDown(self) -> None:
        ClientHandler.discover = self.stashed_discover
        ClientHandler.register_client = self.stashed_register_client
        builtins.open = self.stashed_open

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
        ClientHandler.__init__.assert_called_once_with(cfg.ISSUER, cfg.REDIRECT_URIS, {'scope': cfg.SCOPE.split(" ")})

    def test_register_should_call_open(self):
        with patch('builtins.open', mock_open()) as open_mock:
            dcr_from_config.register()

        open_mock.assert_called_once()

    def test_register_should_call_open_with_correct_params(self):
        with patch('builtins.open', mock_open()) as open_mock:
            dcr_from_config.register()
        open_mock.assert_called_once_with('client_info.json', 'w')

    def test_register_should_call_write_with_client_info(self):
        client = ClientHandler(cfg.ISSUER, cfg.REDIRECT_URIS, {})
        expected_json_client_info = json.dumps(client.get_client_dict(), indent=4)
        with patch('builtins.open', mock_open()) as open_mock:
            dcr_from_config.register()
        open_mock_handler = open_mock()
        open_mock_handler.write.assert_called_once_with(expected_json_client_info)


