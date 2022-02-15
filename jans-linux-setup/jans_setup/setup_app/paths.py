import os
import pathlib
import shutil
import site

IAMPACKAGED = False
APP_ROOT = pathlib.Path(__file__).parent.as_posix()
INSTALL_DIR = pathlib.Path(__file__).parent.parent.as_posix()
PYLIB_DIR = os.path.join(APP_ROOT, 'pylib')
DATA_DIR = os.path.join(APP_ROOT, 'data')
LOG_DIR = os.path.join(INSTALL_DIR, 'logs')

LOG_FILE = os.path.join(LOG_DIR, 'setup.log')
LOG_ERROR_FILE = os.path.join(LOG_DIR, 'setup_error.log')
LOG_OS_CHANGES_FILE = os.path.join(LOG_DIR, 'os-changes.log')

cmd_ln = '/bin/ln'
cmd_chmod = '/bin/chmod'
cmd_chown = '/bin/chown'
cmd_chgrp = '/bin/chgrp'
cmd_mkdir = '/bin/mkdir'
cmd_rpm = '/bin/rpm'
cmd_dpkg = '/usr/bin/dpkg'
cmd_rm = '/bin/rm'
cmd_py3 = shutil.which('python3')
cmd_openssl = shutil.which('openssl')
cmd_wget = shutil.which('wget')
cmd_sed = shutil.which('sed')
cmd_tar = shutil.which('tar')
cmd_unzip = shutil.which('unzip')
cmd_update_rc = shutil.which('update-rc.d')
