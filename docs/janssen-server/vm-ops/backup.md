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


## LDIF Data Backup

From time to time (daily or weekly), the LDAP database should be exported in a standard LDIF format. Having the data in plain text offers some options for recovery that are not possible with a binary backup.

Instructions are provided below for exporting OpenDJ data. The below instructions address situations where unused and expired cache and session related entries are piling and causing issues with functionality.


### OpenDj

If your Jans Server is backed by OpenDJ, follow these steps to backup your data:

1. First check your cache entries by running the following command:

    ```
    /opt/opendj/bin/ldapsearch -h localhost -p 1636 -Z -X -D "cn=directory manager" -w <pass> -b 'o=jans' 'grtId=*' dn | grep 'dn:' | wc -l
    ```

2. Dump the data as LDIF :

    * Stop the services using `systemctl stop opendj`

    * Now export the LDIF and save it in appropriate place safe

    ```
    /opt/opendj/bin/export-ldif -n userRoot --offline -l databackup_date.ldif
    ```
    * Now exclude jansGrant(grntId) so the command becomes:

    ```
    /opt/opendj/bin/export-ldif -n userRoot --offline -l yourdata_withoutoxAuthGrantId.ldif --includeFilter '(!(grtId=*))'
    ```

    * You may also wish to exclude jansMetric so the command becomes:

    ```
    /opt/opendj/bin/export-ldif -n userRoot --offline -l yourdata_withoutGrantIdMetic.ldif --includeFilter '(&(!(grtId=*))(!(objectClass=jansMetric)))'
    ```

3. Now, only if needed, rebuild indexes:

    * Check status of indexes:
    ```
    /opt/opendj/bin/backendstat show-index-status --backendID userRoot --baseDN o=jans
    ```

    Take note of all indexes that need to be rebuilt. If no indexing is needed, move on to step 4.

    * Start the opendj service `systemctl start opendj`

    * Build backend index for all indexes that need it accoring to previous status command, change passoword -w and index name accordingly. This command has to be run for every index separately:

    ```
    /opt/opendj/bin/dsconfig create-backend-index --port 4444 --hostname localhost --bindDN "cn=directory manager" -w password --backend-name userRoot --index-name iname --set index-type:equality --set index-entry-limit:4000 --trustAll --no-prompt
    ```

    * Stop the opendj service `systemctl stop opendj`

    * Rebuild the indexes as needed, here are examples :

    ```
    /opt/opendj/bin/rebuild-index --baseDN o=jans --index iname
    /opt/opendj/bin/rebuild-index --baseDN o=jans --index uid
    /opt/opendj/bin/rebuild-index --baseDN o=jans --index mail
    ```

    * Check status again :

    ```
    /opt/opendj/bin/backendstat show-index-status --backendID userRoot --baseDN o=jans
    ```

    * Verify indexes:

    ```
    /opt/opendj/bin/verify-index --baseDN o=jans --countErrors
    ```

4. Next import your previously exported LDIF.

    ```
    /opt/opendj/bin/import-ldif -n userRoot --offline -l your-backup.ldif
    ```

If you moved to a new LDAP, copy back your schema files to this directory:

```
/opt/opendj/config/schema/
```

 * Start the `opendj` and other services
 * Finally, verify the cache entries have been removed:

    ```
    /opt/opendj/bin/ldapsearch -h localhost -p 1636 -Z -X -D "cn=directory manager" -w <pass> -b 'o=jans' 'grtId=*' dn | grep 'dn:' | wc -l
    ```

You should be done and everything should be working perfectly. You may notice your Jans Server responding slower than before. That is expected -- your LDAP is adjusting to the new data, and indexing might be in process. Give it some time and it should be back to normal.
