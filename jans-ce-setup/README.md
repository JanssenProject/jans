[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=JanssenProject_jans-setup&metric=bugs)](https://sonarcloud.io/dashboard?id=JanssenProject_jans-setup)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=JanssenProject_jans-setup&metric=code_smells)](https://sonarcloud.io/dashboard?id=JanssenProject_jans-setup)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=JanssenProject_jans-setup&metric=coverage)](https://sonarcloud.io/dashboard?id=JanssenProject_jans-setup)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=JanssenProject_jans-setup&metric=duplicated_lines_density)](https://sonarcloud.io/dashboard?id=JanssenProject_jans-setup)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=JanssenProject_jans-setup&metric=ncloc)](https://sonarcloud.io/dashboard?id=JanssenProject_jans-setup)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=JanssenProject_jans-setup&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=JanssenProject_jans-setup)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=JanssenProject_jans-setup&metric=alert_status)](https://sonarcloud.io/dashboard?id=JanssenProject_jans-setup)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=JanssenProject_jans-setup&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=JanssenProject_jans-setup)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=JanssenProject_jans-setup&metric=security_rating)](https://sonarcloud.io/dashboard?id=JanssenProject_jans-setup)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=JanssenProject_jans-setup&metric=sqale_index)](https://sonarcloud.io/dashboard?id=JanssenProject_jans-setup)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=JanssenProject_jans-setup&metric=vulnerabilities)](https://sonarcloud.io/dashboard?id=JanssenProject_jans-setup)

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

