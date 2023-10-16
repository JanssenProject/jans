---
tags:
- Casa
- administration
---

# Jans Casa Administration Guide

## Unlocking admin features

Admin capabilities are disabled by default. To unlock admin features follow these steps:

1. Navigate to `/opt/jans/jetty/jans-casa`
1. Create an empty file named `.administrable` (ie. `touch .administrable`)
1. Run `chown casa:casa .administrable` (do this only if you are on FIPS environment)
1. Logout in case you have an open browser session

!!! Warning
    Once you have configured, tailored, and tested your deployment thoroughly, you are strongly encouraged to remove the marker file. This will prevent problems in case a user can escalate privileges or if some administrative account is compromised.

