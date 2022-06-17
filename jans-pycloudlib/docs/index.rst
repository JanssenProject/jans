.. jans.pycloudlib documentation master file, created by
   sphinx-quickstart on Sat Jul 11 03:49:21 2020.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.

jans.pycloudlib
~~~~~~~~~~~~~~~~~~~

jans-pycloudlib contains shared classes and functions to develop Janssen Authorization Server app container runs on the following runtimes:

- `containerd <https://containerd.io/>`_
- `CRI-O <https://cri-o.io/>`_

That being said, jans-pycloudlib is targetting the following platforms:

- `Kubernetes <https://kubernetes.io/>`_ (first-class citizen)
- `Docker <https://www.docker.com/>`_

We highly recommend to build app container that runs in Kubernetes as it provides more features compared to Docker.

User's Guide
============

This part of the documentation focuses on step-by-step instructions for app container development using jans-pycloudlib.

.. toctree::
   :maxdepth: 2

   install
   quickstart

API Reference
=============

This part of the documentation lists the API reference of public classes and functions.

.. toctree::
   :maxdepth: 2

   manager
   config
   secret
   persistence
   wait
   meta
   utils
   validators
