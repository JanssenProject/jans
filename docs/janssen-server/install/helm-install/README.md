---
tags:
  - administration
  - installation
  - helm
---
# Helm Deployments

## Overview

Janssen enables organizations to build a scalable centralized authentication and authorization service using free open source software.

The components of the project include client and server implementations of the OAuth, OpenID Connect, SCIM and FIDO standards.

All these components are deployed using janssen [helm chart](https://github.com/JanssenProject/jans/tree/main/charts/janssen).

You can check the [reference](../../reference/kubernetes/helm-chart.md) guide to view the list of the chart components and values.

## Looking for older helm charts?

If you are looking for older helm charts, you need to build them from the [janssen](https://github.com/JanssenProject/jans/tree/main/charts) repository. We only keep the last 5 versions of the chart up. We support auto-upgrade using helm upgrade and hence want everyone to stay up to date with our charts. 

To build older charts manually from the janssen repository, you can use the following example which assumes we are building for janssen version `v1.0.0`:

```bash
git clone --filter blob:none --no-checkout https://github.com/JanssenProject/jans.git /tmp/jans \
    && cd /tmp/jans \
    && git sparse-checkout init --cone \
    && git checkout v1.0.0 \
    && git sparse-checkout add charts/janssen \
    && cd charts/janssen \
    && helm dependency update \
    && helm package .
```