from nose.tools import assert_equal
from mock import patch

from setup import Setup


@patch.object(Setup, 'logIt')
def test_setup_load_properties(mock_logIt):
    obj = Setup()

    assert_equal(obj.installOxAuth, True)
    assert_equal(obj.installOxTrust, True)
    assert_equal(obj.installLdap, True)
    assert_equal(obj.installHttpd, True)
    assert_equal(obj.installSaml, False)
    assert_equal(obj.installCas, False)
    assert_equal(obj.installOxAuthRP, False)

    # all false
    obj.load_properties('tests/sample1.properties')
    assert_equal(obj.installOxAuth, False)
    assert_equal(obj.installOxTrust, False)
    assert_equal(obj.installLdap, False)
    assert_equal(obj.installHttpd, False)
    assert_equal(obj.installSaml, False)
    assert_equal(obj.installCas, False)
    assert_equal(obj.installOxAuthRP, False)

    # all true
    obj.load_properties('tests/sample2.properties')
    assert_equal(obj.installOxAuth, True)
    assert_equal(obj.installOxTrust, True)
    assert_equal(obj.installLdap, True)
    assert_equal(obj.installHttpd, True)
    assert_equal(obj.installSaml, True)
    assert_equal(obj.installCas, True)
    assert_equal(obj.installOxAuthRP, True)

    # mix of both true and false
    obj.load_properties('tests/sample3.properties')
    assert_equal(obj.installOxAuth, False)
    assert_equal(obj.installOxTrust, True)
    assert_equal(obj.installLdap, False)
    assert_equal(obj.installHttpd, True)
    assert_equal(obj.installSaml, False)
    assert_equal(obj.installCas, False)
    assert_equal(obj.installOxAuthRP, True)
