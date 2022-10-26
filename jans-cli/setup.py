"""
 License terms and conditions for Janssen:
 https://www.apache.org/licenses/LICENSE-2.0
"""

import codecs
import os
import re
from setuptools import setup
from setuptools import find_packages
from setuptools.command.install import install
from urllib.request import urlretrieve

class PostInstallCommand(install):
    """Post-installation for installation mode."""
    def run(self):
        install.run(self)

        urlretrieve(
             'https://raw.githubusercontent.com/JanssenProject/jans/main/jans-config-api/docs/jans-config-api-swagger.yaml',
             os.path.join(self.install_lib, 'cli/jca.yaml')
            )

        urlretrieve(
             'https://raw.githubusercontent.com/JanssenProject/jans/main/jans-scim/server/src/main/resources/jans-scim-openapi.yaml',
             os.path.join(self.install_lib, 'cli/scim.yaml')
            )


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
    name="jans-cli",
    version=find_version("cli", "version.py"),
    url="",
    copyright="Copyright 2021, Janssen",
    license="Apache 2.0 <https://www.apache.org/licenses/LICENSE-2.0>",
    author="Janssen",
    author_email="",
    maintainer="",
    status="Dev",
    description="",
    long_description=__doc__,
    packages=find_packages(),
    package_data={'': ['*.yaml']},
    zip_safe=False,
    install_requires=[
        "ruamel.yaml>=0.16.5",
        "PyJWT==2.3.0",
        "certifi",
        "six",
        "prompt_toolkit",
        "jca-swagger-client @ https://ox.gluu.org/icrby8xcvbcv/cli-swagger/jca_swagger_client.zip",
        "scim_swagger_client @ https://ox.gluu.org/icrby8xcvbcv/cli-swagger/scim_swagger_client.zip",

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
            "config-cli=cli.config_cli:main",
            "scim-cli=cli.config_cli:main",
        ],
    },
    cmdclass={
        'install': PostInstallCommand,
    },
)
