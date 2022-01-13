from nose.tools import assert_true, assert_equal, assert_is_none, assert_false
from setup import getOpts


def test_getOpts():
    """Options:
        -r   Install oxAuth RP
        -c   Install CAS
        -d   specify the directory where community-edition-setup is located.
        -f   specify setup.properties file
        -h   Help
        -n   No interactive prompt before install starts. Run with -f
        -N   No apache httpd server
        -s   Install the Shibboleth IDP
        -u   Update hosts file with IP address / hostname
        -w   Get the development head war files
    """

    setupOptions = {
        'install_dir': '.',
        'setup_properties': None,
        'noPrompt': False,
        'downloadWars': False,
        'installOxAuth': True,
        'installOxTrust': True,
        'installLDAP': True,
        'installHTTPD': True,
        'installSaml': False,
        'installCas': False,
        'installOxAuthRP': False
    }

    setupOptions = getOpts(['-r'], setupOptions)
    assert_true(setupOptions['installOxAuthRP'])

    setupOptions = getOpts(['-c'], setupOptions)
    assert_true(setupOptions['installCas'])

    # existing path
    setupOptions = getOpts(['-d', '/tmp'], setupOptions)
    assert_equal(setupOptions['install_dir'], '/tmp')
    # non existing path
    setupOptions = getOpts(['-d', '/dummy'], setupOptions)
    assert_equal(setupOptions['install_dir'], '/tmp')

    # actual file available
    setupOptions = getOpts(['-f', 'tests/dummyfile'], setupOptions)
    assert_equal(setupOptions['setup_properties'], 'tests/dummyfile')
    # file not available
    setupOptions = getOpts(['-f', 'non_existing_file'], setupOptions)
    assert_equal(setupOptions['setup_properties'], 'tests/dummyfile')  # Preval

    setupOptions = getOpts(['-n'], setupOptions)
    assert_true(setupOptions['noPrompt'])

    setupOptions = getOpts(['-N'], setupOptions)
    assert_false(setupOptions['installHTTPD'])

    setupOptions = getOpts(['-s'], setupOptions)
    assert_true(setupOptions['installSaml'])

    setupOptions = getOpts(['-w'], setupOptions)
    assert_true(setupOptions['downloadWars'])
