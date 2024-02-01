# Sample cred plugin

This folder contains a Casa plugin that can be used as a starting point to add a new authentication mechanism to Casa. The plugin is a maven project (so Java knowledge is required) and covers introductory aspects for enrollment logic.

Note authentication logic is performed at the authorization server - not Casa. For this, creating a person authentication [jython script](https://docs.jans.io/head/admin/developer/scripts/person-authentication/) and building custom pages are necessary. Use the `script.py` file found in this directory to start coding.

In both scenarios, enrollment and authentication, you will resort to checking how already supported authentication methods work. These tasks will demand skills related to:

- Python and Java
- HTML development 
- Java Server Faces
- [ZK](https://www.zkoss.org/) framework 9

Happy coding!
