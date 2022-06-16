Persistence
~~~~~~~~~~~

.. module:: jans.pycloudlib.persistence

.. autofunction:: render_salt

.. autofunction:: render_base_properties

LDAP
====

.. module:: jans.pycloudlib.persistence.ldap

.. autoclass:: LdapClient
    :members:
    :private-members:
    :undoc-members:

.. autofunction:: render_ldap_properties

.. autofunction:: sync_ldap_truststore

Couchbase
=========

.. module:: jans.pycloudlib.persistence.couchbase

.. autofunction:: render_couchbase_properties

.. autofunction:: get_couchbase_user

.. autofunction:: get_couchbase_password

.. autofunction:: get_couchbase_superuser

.. autofunction:: get_couchbase_superuser_password

.. autofunction:: get_couchbase_conn_timeout

.. autofunction:: get_couchbase_conn_max_wait

.. autofunction:: get_couchbase_scan_consistency

.. autofunction:: sync_couchbase_truststore

.. autoclass:: BaseClient
    :members:
    :private-members:
    :undoc-members:

.. autoclass:: RestClient
    :members:
    :private-members:
    :undoc-members:

.. autoclass:: N1qlClient
    :members:
    :private-members:
    :undoc-members:

.. autoclass:: CouchbaseClient
    :members:
    :private-members:
    :undoc-members:

SQL
===

.. module:: jans.pycloudlib.persistence.sql

.. autoclass:: MysqlAdapter
    :members:
    :private-members:
    :undoc-members:

.. autoclass:: SqlClient
    :members:
    :private-members:
    :undoc-members:

Spanner
=======

.. module:: jans.pycloudlib.persistence.spanner

.. autoclass:: SpannerClient
    :members:
    :private-members:
    :undoc-members:

Hybrid
======

.. module:: jans.pycloudlib.persistence.hybrid

.. autofunction:: render_hybrid_properties
