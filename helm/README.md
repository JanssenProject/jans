![microk8s](https://github.com/GluuFederation/cloud-native-edition/workflows/microk8s/badge.svg?branch=5.0)
![minikube](https://github.com/GluuFederation/cloud-native-edition/workflows/minikube/badge.svg?branch=5.0)
![awseks](https://github.com/GluuFederation/cloud-native-edition/workflows/awseks/badge.svg?branch=5.0)
![googlegke](https://github.com/GluuFederation/cloud-native-edition/workflows/googlegke/badge.svg?branch=5.0)
![testcases](https://github.com/GluuFederation/cloud-native-edition/workflows/testcases/badge.svg?branch=5.0)
[![codecov](https://codecov.io/gh/GluuFederation/cloud-native-edition/branch/master/graph/badge.svg)](https://codecov.io/gh/GluuFederation/cloud-native-edition)
[![Artifact HUB](https://img.shields.io/endpoint?url=https://artifacthub.io/badge/repository/gluu)](https://artifacthub.io/packages/search?repo=gluu)
# pygluu-kubernetes

## Kubernetes recipes

- Install [Gluu](https://gluu.org/docs/gluu-server/latest/installation-guide/install-kubernetes/)

## Build `pygluu-kubernetes.pyz` manually

## Prerequisites

1.  Python 3.6+.
1.  Python `pip3` package.

## Installation

### Standard Python package

1.  Create virtual environment and activate:

    ```sh
    python3 -m venv .venv
    source .venv/bin/activate
    ```

1.  Install the package:

    ```
    make install
    ```

    This command will install executable called `pygluu-kubernetes` and `pygluu-kubernetes-gui` available in virtual environment `PATH`.

### Python zipapp

1.  Install [shiv](https://shiv.readthedocs.io/) using `pip3`:

    ```sh
    pip3 install shiv
    ```

1.  Install the package:

    ```sh
    make zipapp
    ```

    This command will generate executable called `pygluu-kubernetes.pyz` under the same directory.