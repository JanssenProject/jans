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

def download_gcs():
    gcs_dir = os.path.join(base.pylib_dir, 'gcs')

    if os.path.exists(gcs_dir) and not base.argsp.force_download:
        return

    base.logIt("Downloading Spanner modules")
#    gcs_download_url = os.path.join(base.current_app.app_info['EXTERNAL_LIBS'], 'spanner/gcs.tgz')
    gcs_download_url = 'http://192.168.64.4/gcs.tgz'

    with tempfile.TemporaryDirectory() as tmp_dir:

        target_fn = os.path.join(tmp_dir, 'gcs.tgz')
        base.download(gcs_download_url, target_fn, verbose=True)
        shutil.unpack_archive(target_fn, base.pylib_dir)

        grpcio_fn = os.path.join(tmp_dir, 'grpcio_fn.json')
        base.download('https://pypi.org/pypi/grpcio/1.46.0/json', grpcio_fn, verbose=True)
        data = base.readJsonFile(grpcio_fn)

        package = get_grpcio_package(data)

        if package.get('url'):
            target_whl_fn = os.path.join(tmp_dir, os.path.basename(package['url']))
            base.download(package['url'], target_whl_fn, verbose=True)
            whl_zip = zipfile.ZipFile(target_whl_fn)

            for member in  whl_zip.filelist:
                whl_zip.extract(member, gcs_dir)

            whl_zip.close()


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
    if base.current_app.profile == 'jans' or base.current_app.profile == 'disa-stig':
        download_gcs()
    download_sqlalchemy()
