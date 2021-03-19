[![Codacy Badge](https://app.codacy.com/project/badge/Grade/3275f8f15d584141a3c602002afa1e87)](https://www.codacy.com/gh/JanssenProject/jans-setup/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=JanssenProject/jans-setup&amp;utm_campaign=Badge_Grade)

Janssen Project-setup
=======================

Scripts and templates to automate deployment and configuration of the Janssen Project,

Installing Janssen Server
-----------------------

We tested installation on CentOS 8, Ubuntu 18 and Ubuntu 20.
Just two steps:
1. Download installer

    `curl https://raw.githubusercontent.com/JanssenProject/jans-setup/master/install.py > install.py`

2. Execute installer

    `python3 install.py`

Uninstalling Janssen Server
------------------------
Execute installer with `-uninstall` argument

`python3 install.py -uninstall`

Reinstalling Janssen Server
------------------------
First uninstall and then install

