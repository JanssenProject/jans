---
tags:
  - administration
  - vm
  - operations
---

Best method of Janssen Backup is "VM Snapshot" backup. That means, backup / take snapshot of whole virtual machine / droplet / instance which is hosting your Gluu Janssen software. In the event of a production outage, a proper snapshot of the last working condition will help rapidly restore service.

Most platform virtualization software and cloud vendors have snapshot backup features. For instance, Digital Ocean has Live Snapshot and Droplet Snapshot; VMWare has Snapshot Manager, etc. 

The Gluu Server should be backed up frequently--**we recommend at least one daily and one weekly backup of Gluu's data and/or VM**.

It's also good to have some partial configuration backup as well. Such as: 

 - Tarball `/opt`
 - Tarball `/etc/jans`
 - Tarball `/etc/apache2`
 - Tarball `/var/jans`
 
