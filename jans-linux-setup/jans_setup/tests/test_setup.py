from nose.tools import assert_equal
from mock import patch

from setup import Setup


@patch.object(Setup, 'logIt')
def test_setup_load_properties(mock_logIt):
    obj = Setup()

    assert_equal(obj.install_jans_auth, True)
    assert_equal(obj.opendj_install, True)
    assert_equal(obj.install_httpd, True)
    assert_equal(obj.install_casa, False)

    # all false
    obj.load_properties('tests/sample1.properties')
    assert_equal(obj.install_jans_auth, False)
    assert_equal(obj.opendj_install, False)
    assert_equal(obj.install_httpd, False)
    assert_equal(obj.install_casa, False)

    # all true
    obj.load_properties('tests/sample2.properties')
    assert_equal(obj.install_jans_auth, True)
    assert_equal(obj.opendj_install, True)
    assert_equal(obj.install_httpd, True)
    assert_equal(obj.install_casa, True)

    # mix of both true and false
    obj.load_properties('tests/sample3.properties')
    assert_equal(obj.install_jans_auth, False)
    assert_equal(obj.opendj_install, False)
    assert_equal(obj.install_httpd, True)
    assert_equal(obj.install_casa, False)
