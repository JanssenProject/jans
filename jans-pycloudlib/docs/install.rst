Installation
~~~~~~~~~~~~

Python version
==============

Starting from v2, jans-pycloudlib only supports Python 3.6 and above.

Dependencies
============

These distributions will be installed automatically when installing jans-pycloudlib.

- `requests <https://requests.readthedocs.io/>`_ acts as HTTP client.
- `python-consul <https://python-consul.readthedocs.io/>`_ acts as client to interact with `Consul <https://www.consul.io/>`_.
- `hvac <https://python-hvac.org/>`_ acts as client to interact with `Vault <https://www.vaultproject.io/>`_.
- `kubernetes <https://github.com/kubernetes-client/python>`_ acts as client to interact with Kubernetes API.
- `ldap3 <https://ldap3.readthedocs.io>`_ provides utilities to interact with LDAP server.
- `backoff <https://github.com/trendmicro/backoff-python>`_ provides utilities to implement *backoff/retries* when running a callable.
- `docker <https://docker-py.readthedocs.io>`_ acts as client to interact with Docker API.
- `requests-toolbelt <https://toolbelt.readthedocs.io/en/latest/>`_ provides utilities to modify Host header to interact with CouchBase server.
- `cryptography <https://cryptography.io/en/latest/>`_ provides utilities for fast *encode/decode* text.

Install jans-pycloudlib
===========================

Preferred installation method is using `pip3`:

.. code-block:: sh

    pip3 install -e git+https://github.com/JanssenProject/jans-pycloudlib@master#egg=jans-pycloudlib
