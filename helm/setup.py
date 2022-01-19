"""
 License terms and conditions for Gluu Cloud Native Edition:
 https://www.apache.org/licenses/LICENSE-2.0
"""

import codecs
import os
import re
from setuptools import setup
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
    name="pygluu-kubernetes",
    version=find_version("pygluu", "kubernetes", "__init__.py"),
    url="https://gluu.org",
    copyright="Copyright 2020, Gluu Cloud Native Edition",
    license="Apache 2.0 <https://www.apache.org/licenses/LICENSE-2.0>",
    author="Gluu",
    author_email="mo@gluu.org",
    maintainer="Mohammad Abudayyeh",
    status="Dev",
    description="",
    long_description=__doc__,
    packages=find_packages(),
    zip_safe=False,
    install_requires=[
        "ruamel.yaml>=0.16.5",
        "pyOpenSSL>=19.1.0",
        "cryptography>=2.8",
        "kubernetes==18.20.0",
        "Click!=7.0,>=6.7",
        "email_validator >= 1.1.0",
        "Flask-SocketIO >= 4.3.1",
        "Pygtail >= 0.11.1",
        "gevent >= 20.9.0",
        "jsonschema >= 3.2.0",
        "dotty-dict >= 1.3.0",
        "PyYAML >= 5.4.1"
    ],
    classifiers=[
        "Intended Audience :: Developers",
        "License :: OSI Approved :: Apache 2.0 License",
        "Topic :: Software Development :: Libraries :: Python Modules",
        "Programming Language :: Python",
        "Programming Language :: Python :: 3s",
        "Programming Language :: Python :: 3.6",
    ],
    include_package_data=True,
    entry_points={
        "console_scripts": [
            "pygluu-kubernetes=pygluu.kubernetes.create:main",
        ],
    },
)
