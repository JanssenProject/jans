---
tags:
  - administration
  - vm
  - operations
---

# Data Cleaning

Janssen setup creates a crontab for cleaning data from database backend. The data cleaning process cleans those
expired data from SQL tables. In general temporary data has the following fields set:


__del__This is a boolean field indicating that the data can be deleted when it expires.

__exp__This is a datetime field indicating expire time.


The cleaning script examines the following tables and deletes row whose `del` field is `TRUE`
and `exp` field contains a date-time that is older than current time. The following tables are examined:

- jansSsa
- jansClnt
- jansScope
- jansSessId
- jansAuthChallSess
- jansArchJwk
- jansUmaResource
- jansUmaResourcePermission
- jansToken
- jansUmaRPT
- jansDeviceRegistration
- jansU2fReq
- jansMetric
- jansClntAuthz
- jansUmaPCT
- jansCache
- jansFido2AuthnEntry
- jansFido2RegistrationEntry
- jansPar

Setup creates a crontab entry named `/etc/cron.d/jans-session` with the following content:

```
@hourly root python3 /opt/jans/data-cleaner/clean-data.py --yes -limit=1000
```

This means, script `/opt/jans/data-cleaner/clean-data.py` is executed hourly with the deletion limit set to `1000`.
You can change the content of crontab entry if you know what to do, but rather we recommend using a script.

```bash  title="Command"
/opt/jans/data-cleaner/jans-clean-data-crontab.py
```

This script creates a crontab entry with the following argument:

```
__-limit__Limit to delete entry per execution

__-interval__{everyminute, hourly, daily, weekly, monthly, midnight}
```

For example, to create a crontab entry that is executed daily with deleting 5000 data, you can execute the following command:

```bash title="Command"
/opt/jans/data-cleaner/jans-clean-data-crontab.py -interval daily -limit 5000
```

The data cleaning script `clean-data.py` determines the number of entries deleted from its argument `-limit` and
reads configuration from `/opt/jans/data-cleaner/data-clean.ini`. Example configuration:

```
[main]
tables = jansSsa jansClnt jansScope jansSessId jansAuthChallSess jansArchJwk jansUmaResource jansUmaResourcePermission jansToken jansUmaRPT jansDeviceRegistration jansU2fReq jansMetric jansClntAuthz jansUmaPCT jansCache jansFido2AuthnEntry jansFido2RegistrationEntry jansPar
cleanUpInactiveClientAfterHoursOfInactivity=2
```

As you can infer, the script deletes expired data from tables listed in key `tables`. Tables should be separated by space.
There is one more cleaning process for table `jansClnt`. This table contains clients that may be deleted if the last access time
is older than the current time before a specific hour that is determined from `cleanUpInactiveClientAfterHoursOfInactivity`.
For example, if `del` is set to `TRUE` for the client and `jansLastAccessTime` is 2 hours older than current time,
the client is deleted.

The data cleaning script logs all its operations as well as SQL queries and query outputs to `/opt/jans/data-cleaner/logs/data-clean.log`

You can safely execute `clean-data.py` script manually whenever you need cleaning data.
