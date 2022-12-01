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
    name="jans-pycloudlib",
    version=find_version("jans", "pycloudlib", "version.py"),
    url="",
    license="Apache",
    author="Janssen",
    author_email="isman@gluu.org",
    description="",
    long_description=__doc__,
    packages=find_packages(),
    zip_safe=False,
    install_requires=[
        "requests>=2.22.0",
        "python-consul>=1.0.1",
        "hvac>=0.7.0",
        "kubernetes>=11.0",
        "ldap3>=2.5",
        "backoff>=2.1.2",
        "docker>=3.7.2",
        "requests-toolbelt>=0.9.1",
        "cryptography>=2.8",
        "google-cloud-secret-manager>=2.2.0",
        "pymysql>=1.0.2",
        "sqlalchemy>=1.3,<1.4",
        "psycopg2>=2.8.6",
        "google-cloud-spanner>=3.3.0",
        "Click>=6.7",
        "ldif>=4.1.1",
        # handle CVE-2022-36087
        "oauthlib>=3.2.1",
        "boto3",
    ],
    classifiers=[
        "Intended Audience :: Developers",
        "License :: OSI Approved :: MIT License",
        "Topic :: Software Development :: Libraries :: Python Modules",
        "Programming Language :: Python",
        "Programming Language :: Python :: 3",
        "Programming Language :: Python :: 3.6",
        "Programming Language :: Python :: 3.7",
        "Programming Language :: Python :: 3.8",
        "Programming Language :: Python :: 3.9",
        "Programming Language :: Python :: 3.10",
    ],
    include_package_data=True,
    entry_points={
        "console_scripts": ["jans-pycloudlib=jans.pycloudlib.cli:cli"],
    },
    package_data={
        "jans.pycloudlib": ["py.typed"],
    },
)
