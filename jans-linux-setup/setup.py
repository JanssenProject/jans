"""
 License terms and conditions for Janssen:
 https://www.apache.org/licenses/LICENSE-2.0
"""

import codecs
import os
import re
from setuptools import setup
from setuptools.command.install import install
from setuptools import find_packages


def find_version(*file_paths):
    here = os.path.abspath(os.path.dirname(__file__))
    with codecs.open(os.path.join(here, *file_paths), 'r') as f:
        version_file = f.read()
    version_match = re.search(r"^__version__ = ['\"]([^'\"]*)['\"]",
                              version_file, re.M)
    if version_match:
        return version_match.group(1)
    raise RuntimeError("Unable to find version string.")

setup(
    name="jans-setup",
    version=find_version("jans_setup/setup_app/version.py"),
    url="",
    license="Apache 2.0 <https://www.apache.org/licenses/LICENSE-2.0>",
    author="Janssen",
    author_email="",
    maintainer="",
    description="",
    long_description=__doc__,
    packages=['jans_setup'],
    package_dir={'jans_setup': 'jans_setup'},
    zip_safe=False,
    install_requires=[
        "setuptools>=59.6.0",
        "prompt-toolkit==3.0.29",
        "pyasn1==0.4.8",
        "ruamel.yaml>=0.16.5",
        "sqlalchemy==1.3.23",
        "cryptography==36.0.1",
        "protobuf",
        "google-cloud-spanner==3.13.0",
        "ldap3",
        "PyMySQL",
        "pycrypto",
        "PyJWT>=2.3.0",
    ],
    classifiers=[
        "Intended Audience :: Developers",
        "License :: OSI Approved :: Apache 2.0 License",
        "Topic :: Software Development :: Libraries :: Python Modules",
        "Programming Language :: Python",
        "Programming Language :: Python :: 3.6+",
    ],
    include_package_data=True,

    entry_points={
        "console_scripts": [
            "jans-setup=jans_setup.jans_setup:main",
        ],
    },
)
