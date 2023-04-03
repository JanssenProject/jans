from clientapp.utils import dcr_from_config
class TestDrcFromConfig():
    def test_if_setup_logging_exists(self):
        assert hasattr(dcr_from_config, 'setup_logging')

    def test_if_static_variables_exists(self):
        assert hasattr(dcr_from_config, 'OP_URL')
        assert hasattr(dcr_from_config, 'REDIRECT_URIS')
