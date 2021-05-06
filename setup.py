"""
 License terms and conditions for Janssen:
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
    zip_safe=False,
    install_requires=[
        "ruamel.yaml>=0.16.5"
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
    # The below should be removed in favor of entrypoints
    scripts=['cli/config-cli.py'],
    #entry_points={
    #    "console_scripts": [
    #        "jans-cli=cli.config-cli:",
    #    ],
    #},
)
