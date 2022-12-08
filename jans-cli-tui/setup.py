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
        yaml_dir = os.path.join(self.install_lib, 'cli_tui/cli/ops/jca')
        if not os.path.exists(yaml_dir):
            os.makedirs(yaml_dir, exist_ok=True)

        print("downloding", 'jans-config-api-swagger-auto.yaml')

        urlretrieve(
            'https://raw.githubusercontent.com/JanssenProject/jans/main/jans-config-api/docs/jans-config-api-swagger-auto.yaml',
            os.path.join(yaml_dir, 'jans-config-api-swagger-auto.yaml')
            )

        for plugin_yaml_file in ('fido2-plugin-swagger.yaml', 'jans-admin-ui-plugin-swagger.yaml', 'scim-plugin-swagger.yaml', 'user-mgt-plugin-swagger.yaml'):
            print("downloding", plugin_yaml_file)
            urlretrieve(
                'https://raw.githubusercontent.com/JanssenProject/jans/main/jans-config-api/plugins/docs/' + plugin_yaml_file,
                os.path.join(yaml_dir, plugin_yaml_file)
                )

        scim_yaml_dir = os.path.join(self.install_lib, 'cli_tui/cli/ops/scim')
        if not os.path.exists(scim_yaml_dir):
            os.makedirs(scim_yaml_dir, exist_ok=True)

        scim_plugin_yaml_file = 'https://raw.githubusercontent.com/JanssenProject/jans/main/jans-scim/server/src/main/resources/jans-scim-openapi.yaml'
        print("downloding", os.path.basename(scim_plugin_yaml_file))
        urlretrieve(
            scim_plugin_yaml_file,
            os.path.join(scim_yaml_dir, os.path.basename(scim_plugin_yaml_file))
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
    name="jans-cli-tui",
    version=find_version("cli_tui", "version.py"),
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
    package_data={'': ['*.yaml', '.enabled']},
    zip_safe=False,
    install_requires=[
        "ruamel.yaml>=0.16.5",
        "PyJWT==2.4.0",
        "pygments",
        "prompt_toolkit==3.0.33",
        "requests",
        "urllib3",
        "pyDes",
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
            "config-cli-tui=cli_tui.jans_cli_tui:run",
            "config-cli=cli_tui.cli.config_cli:main",
            "scim-cli=cli_tui.cli.config_cli:main",
        ],
    },
    cmdclass={
        'install': PostInstallCommand,
    },
)
