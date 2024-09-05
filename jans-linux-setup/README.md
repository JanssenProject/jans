Janssen Project-setup
=======================

Scripts and templates to automate deployment and configuration of the Janssen Project

## Build `jans-setup.pyz` manually

## Prerequisites

1. Python 3.6+.
2. Python `pip3` package.

## Installation

### Standard Python package

1. Create virtual environment and activate:

    ```sh
    python3 -m venv .venv
    source .venv/bin/activate
    ```

2. Install the package:

    ```
    make install
    ```

    This command will install executable called `jans-setup` available in virtual environment `PATH`.

### Python zipapp

1. Install [shiv](https://shiv.readthedocs.io/) using `pip3`:

    ```sh
    pip3 install shiv
    ```

2. Install the package:

    ```sh
    make zipapp
    ```

    This command will generate executable called `jans-linux-setup.pyz` under the same directory.

Installing Janssen Server
-----------------------

Tested on CentOS 8, Ubuntu 18 and Ubuntu 20.

1. Build the installer as mentioned above

2. Run

    `python3 jans-setup.pyz`
    
    or if you used the `make install` command run `jans-setup`.

Uninstalling Janssen Server
------------------------
Execute installer with `-uninstall` argument

Run

    `python3 jans-linux-setup.pyz -uninstall`

    or if you used the `make install` command run `jans-setup -uninstall`.
    
Reinstalling Janssen Server
------------------------
First uninstall and then install
