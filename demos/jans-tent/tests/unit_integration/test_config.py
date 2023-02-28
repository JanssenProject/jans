import clientapp.config as cfg
from unittest import TestCase


class TestConfig(TestCase):
    def test_has_attribute_SSL_VERIFY(self):
        self.assertTrue(hasattr(cfg, 'SSL_VERIFY'),
                        'SSL_VERIFY attribute is missing in config.')

    def test_SSL_VERIFY_has_boolean_value(self):
        self.assertTrue('__bool__' in cfg.SSL_VERIFY.__dir__(),
                        'SSL_VERIFY is not boolean.')
