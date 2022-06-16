Wait
~~~~

.. module:: jans.pycloudlib.wait

.. note::
    Most of the time, users may only need to run ``wait_for``, ``wait_for_persistence``, and ``wait_for_persistence_conn`` functions to add readiness check.

.. autofunction:: wait_for

.. autofunction:: wait_for_persistence

.. autofunction:: wait_for_persistence_conn

.. autofunction:: wait_for_config

.. autofunction:: wait_for_secret

.. autofunction:: wait_for_ldap

.. autofunction:: wait_for_ldap_conn

.. autofunction:: wait_for_couchbase

.. autofunction:: wait_for_couchbase_conn

.. autofunction:: wait_for_sql

.. autofunction:: wait_for_sql_conn

.. autofunction:: wait_for_spanner

.. autofunction:: wait_for_spanner_conn

.. autofunction:: get_wait_max_time

.. autofunction:: get_wait_interval

.. autoexception:: WaitError

.. autodata:: retry_on_exception
