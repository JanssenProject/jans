import os
import sys
import shutil
import tempfile
import zipfile

from setup_app.utils import base

def download_jans_acrhieve():

    if base.argsp.use_downloaded and os.path.exists(base.current_app.jans_zip):
        return

    base.download(
            'https://github.com/JanssenProject/jans/archive/refs/heads/{}.zip'.format(base.current_app.app_info['SETUP_BRANCH']),
            base.current_app.jans_zip,
            verbose=True
            )

def download_gcs():
    gcs_dir = os.path.join(base.pylib_dir, 'gcs')

    if os.path.exists(gcs_dir) and not base.argsp.force_download:
        return

    base.logIt("Downloading Spanner modules")
    gcs_download_url = os.path.join(base.current_app.app_info['EXTERNAL_LIBS'], 'spanner/gcs.tgz')

    with tempfile.TemporaryDirectory() as tmp_dir:

        target_fn = os.path.join(tmp_dir, 'gcs.tgz')
        base.download(gcs_download_url, target_fn, verbose=True)
        shutil.unpack_archive(target_fn, base.pylib_dir)

        grpcio_fn = os.path.join(tmp_dir, 'grpcio_fn.json')
        base.download('https://pypi.org/pypi/grpcio/1.37.0/json', grpcio_fn, verbose=True)
        data = base.readJsonFile(grpcio_fn)

        pyversion = 'cp{0}{1}'.format(sys.version_info.major, sys.version_info.minor)

        package = {}

        for package_ in data['urls']:

            if package_['python_version'] == pyversion and 'manylinux' in package_['filename'] and package_['filename'].endswith('x86_64.whl'):
                if package_['upload_time'] > package.get('upload_time',''):
                    package = package_

        if package.get('url'):
            target_whl_fn = os.path.join(tmp_dir, os.path.basename(package['url']))
            base.download(package['url'], target_whl_fn, verbose=True)
            whl_zip = zipfile.ZipFile(target_whl_fn)

            for member in  whl_zip.filelist:
                fn = os.path.basename(member.filename)
                if fn.startswith('cygrpc.cpython') and fn.endswith('x86_64-linux-gnu.so'):
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

def download_apps():
    download_jans_acrhieve()
    if base.Config.profile == 'jans':
        download_gcs()
    download_sqlalchemy()
