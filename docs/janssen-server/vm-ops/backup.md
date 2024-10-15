---
tags:
  - administration
  - vm
  - operations
  - backup
---

# Janssen VM Backup

Best method of Janssen Backup is "VM Snapshot" backup. That means, backup / take snapshot of whole virtual machine / droplet / instance which is hosting your Gluu Janssen software. In the event of a production outage, a proper snapshot of the last working condition will help rapidly restore service.

Most platform virtualization software and cloud vendors have snapshot backup features. For instance, Digital Ocean has Live Snapshot and Droplet Snapshot; VMWare has Snapshot Manager, etc. 

The Gluu Server should be backed up frequently--**we recommend at least one daily and one weekly backup of Gluu's data and/or VM**.

It's also good to have some partial configuration backup as well. Such as: 

 - Tarball `/opt`
 - Tarball `/etc/jans`
 - Tarball `/etc/apache2`
 - Tarball `/var/jans`
 
## Tarball Method

All Jans Server files live in a single folder: /opt. The entire Jans Server folder can be archived using the tar command:

* Stop the server: `systemctl stop list-units --all "jans*"`

* Use tar to take a backup: `tar cvf jans-backup.tar /opt/jans/`

* Start the server again: `systemctl start list-units --all "jans*"`
