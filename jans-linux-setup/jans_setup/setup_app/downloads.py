import os
import sys
import glob
import shutil
import zipfile
import tempfile
import importlib.util

from setup_app.utils import base

def download_jans_acrhieve():

    if not base.argsp.force_download and os.path.exists(base.current_app.jans_zip):
        return

    base.download(
            'https://github.com/JanssenProject/jans/archive/refs/heads/{}.zip'.format(base.current_app.app_info['SETUP_BRANCH']),
            base.current_app.jans_zip,
            verbose=True
            )

def get_grpcio_package(data):

    pyversion = 'cp{0}{1}'.format(sys.version_info.major, sys.version_info.minor)

    package = {}

    for package_ in data['urls']:

        if package_['python_version'] == pyversion and 'manylinux' in package_['filename'] and package_['filename'].endswith('x86_64.whl'):
            if package_['upload_time'] > package.get('upload_time',''):
                package = package_
                break

    return package


def download_sqlalchemy():
    sqlalchemy_dir = os.path.join(base.pylib_dir, 'sqlalchemy')

    if os.path.exists(sqlalchemy_dir) and not base.argsp.force_download:
        return

    with tempfile.TemporaryDirectory() as tmp_dir:
        sqlalchemy_zip_file = os.path.join(tmp_dir, os.path.basename(base.current_app.app_info['SQLALCHEMY']))
        base.download(base.current_app.app_info['SQLALCHEMY'], sqlalchemy_zip_file, verbose=True)
        base.extract_subdir(sqlalchemy_zip_file, 'lib/sqlalchemy', sqlalchemy_dir)


def download_all():
    download_files = []
    sys.path.insert(0, os.path.join(base.pylib_dir, 'gcs'))
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
    download_jans_acrhieve()
    download_sqlalchemy()
