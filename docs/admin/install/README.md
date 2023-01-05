---
tags:
  - administration
  - installation
---

# Installation Overview

The goal of Janssen Project is to give you a lot of deployment options. This is
a challenge--the more ways to install, the more ways for things to go wrong!
But to build a large community, we need to provide ways to install the software
in enough different ways to make at least the bulk of the community happy.

Currently, that means the following installation options:

1. VM packages for Ubuntu, SUSE and Red Hat
2. Helm deployments for Amazon, Google, Microsoft and Rancher
3. Docker monolith deployment for development / testing (not production)

## Minimal Configuration

It turns out that just installing the Janssen binary object code (i.e. the bits),
is totally useless. That's because in order to do anything useful with the
Janssen Project, you need a minimal amount of configuration. For example,
you need to generate cryptographic key pairs, you need to generate a minimal
amount of data in the database, you need to generate some web server TLS
certificates.  For this reason, for most of the platforms, installation is a
three step process. Step 1, install the bits. Step 2, run "setup" and answer
some basic question (like the hostname of your IDP). Step 3, fire up a
configuration tool to perform any other last mile configuration.

## Databases

The Janssen Project gives you a few options to store data: LDAP, MySQL, Postgres,
Couchbase, Amazon Aurora, and Spanner. You can also configure an in-memory cache
server like Redis. Sometimes installation and configuration of this database
is included in the setup process. Sometimes, you need to setup the database
ahead of time. Please refer to the database instructions specific for your
choice. And of course, you may need to refer to the database documentation
itself--we don't want to duplicate any of that third party content.

## Optimization

Remember, installation is just a starting point. To get peak performance, you
may need to tweak some of the configuration dials for your system or the
database. If you intend to deploy a Janssen Server in production for high
concurrency, make sure you benchmark the exact flows you expect to serve
in production. 
