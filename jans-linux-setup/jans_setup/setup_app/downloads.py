import os
import sys
import glob
import shutil
import zipfile
import tempfile
import importlib.util

from setup_app.utils import base

def download_jans_archive():

    if not base.argsp.force_download and os.path.exists(base.current_app.jans_zip):
        return

    base.download(
            'https://github.com/JanssenProject/jans/archive/refs/heads/{}.zip'.format(base.current_app.app_info['SETUP_BRANCH']),
            base.current_app.jans_zip,
            verbose=True
            )

def download_zip_app(target_dir, app_info_key, source, *, is_file=False, par_dir=None):
    pylib_dir = os.path.join(base.pylib_dir, target_dir)

    if os.path.exists(pylib_dir) and not base.argsp.force_download:
        return

    with tempfile.TemporaryDirectory() as tmp_dir:
        pylib_dir_zip_file = os.path.join(tmp_dir, os.path.basename(base.current_app.app_info[app_info_key]))
        base.download(base.current_app.app_info[app_info_key], pylib_dir_zip_file, verbose=True)
        if is_file:
            base.extract_file(pylib_dir_zip_file, source, base.pylib_dir)
        else:
            base.extract_subdir(pylib_dir_zip_file, source, pylib_dir, par_dir)

def download_all():
    download_files = []
    modules = glob.glob(os.path.join(base.ces_dir, 'installers/*.py'))

    for installer in modules:
        if installer.startswith('__') or not os.path.isfile(installer):
            continue
        spec = importlib.util.spec_from_file_location('module.name', installer)
        foo = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(foo)

        for m in dir(foo):
            module = getattr(foo, m)
            if hasattr(module, 'source_files'):
                download_files += module.source_files

    for path, url in download_files:
        base.download(url, path, verbose=True)


def download_apps():
    download_jans_archive()
    download_zip_app('sqlalchemy', 'SQLALCHEMY', 'lib/sqlalchemy')
    download_zip_app('cryptography', 'CRYPTOGRAPHY', 'cryptography', par_dir='')
    download_zip_app('jwt', 'PYJWT', 'jwt')
    download_zip_app('pymysql', 'PYMYSQL', 'pymysql')
    download_zip_app('crontab.py', 'PYCRONTAB', 'crontab.py', is_file=True)
    download_zip_app('markupsafe', 'MARKUPSAFE', 'src/markupsafe')
    download_zip_app('mako', 'MAKO', 'mako')
