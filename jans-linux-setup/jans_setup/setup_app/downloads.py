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


def download_sqlalchemy():
    sqlalchemy_dir = os.path.join(base.pylib_dir, 'sqlalchemy')

    if os.path.exists(sqlalchemy_dir) and not base.argsp.force_download:
        return

    with tempfile.TemporaryDirectory() as tmp_dir:
        sqlalchemy_zip_file = os.path.join(tmp_dir, os.path.basename(base.current_app.app_info['SQLALCHEMY']))
        base.download(base.current_app.app_info['SQLALCHEMY'], sqlalchemy_zip_file, verbose=True)
        base.extract_subdir(sqlalchemy_zip_file, 'lib/sqlalchemy', sqlalchemy_dir)


def download_cryptography():
    cryptography_dir = os.path.join(base.pylib_dir, 'cryptography')

    if os.path.exists(cryptography_dir) and not base.argsp.force_download:
        return

    with tempfile.TemporaryDirectory() as tmp_dir:
        cryptography_zip_file = os.path.join(tmp_dir, os.path.basename(base.current_app.app_info['CRYPTOGRAPHY']))
        base.download(base.current_app.app_info['CRYPTOGRAPHY'], cryptography_zip_file, verbose=True)
        base.extract_subdir(cryptography_zip_file, 'cryptography', cryptography_dir, par_dir='')

def download_pyjwt():
    pyjwt_dir = os.path.join(base.pylib_dir, 'jwt')

    if os.path.exists(pyjwt_dir) and not base.argsp.force_download:
        return

    with tempfile.TemporaryDirectory() as tmp_dir:
        pyjwt_dir_zip_file = os.path.join(tmp_dir, os.path.basename(base.current_app.app_info['PYJWT']))
        base.download(base.current_app.app_info['PYJWT'], pyjwt_dir_zip_file, verbose=True)
        base.extract_subdir(pyjwt_dir_zip_file, 'jwt', pyjwt_dir)

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
    download_jans_acrhieve()
    download_sqlalchemy()
    download_cryptography()
    download_pyjwt()
