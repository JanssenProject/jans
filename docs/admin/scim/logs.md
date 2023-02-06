---
tags:
  - administration
  - scim
  - Log
---

## SCIM Logs
    Jans Server logs usually reveal the source of problems when things are going wrong: the first place to look is the SCIM log.  
   Authorization issues (access tokens problems, for instance) are on the side of oxAuth (the authorization server)
        • SCIM log is located at  /opt/jans/jetty/jans-scim/logs/scim.log
        • Auth log is at /opt/jans/jetty/jans-auth/logs/jans-auth.log
        • If using the SCIM custom script in order to intercept API calls and apply custom logic, the script log is also        
          useful: /opt/jans/jetty/jans-scim/logs/scim_script.log
   Generally it's convenient to set the logging level for both jans-Auth and SCIM to DEBUG.
## Have questions in the meantime?

While this documentation is in progress, you can ask questions through [GitHub Discussions](https://github.com/JanssenProject/jans/discussion) or the [community chat on Gitter](https://gitter.im/JanssenProject/Lobby). Any questions you have will help determine what information our documentation should cover.

## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).
