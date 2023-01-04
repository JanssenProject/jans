class InstallTypes:
    NONE   =  0
    LOCAL  = '1'
    REMOTE = '2'

class colors:
    HEADER      = '\033[95m'
    OKBLUE      = '\033[94m'
    OKGREEN     = '\033[92m'
    WARNING     = '\033[93m'
    FAIL        = '\033[91m'
    ENDC        = '\033[0m'
    BOLD        = '\033[1m'
    UNDERLINE   = '\033[4m'
    DANGER      = '\033[31m'

class BackendTypes:
    LDAP      = 1
    COUCHBASE = 2
    MYSQL     = 3
    PGSQL     = 4
    SPANNER   = 5

class BackendStrings:
    LOCAL_OPENDJ     = 'Local OpenDj'
    REMOTE_OPENDJ    = 'Remote OpenDj'
    LOCAL_COUCHBASE  = 'Local Couchbase'
    REMOTE_COUCHBASE = 'Remote Couchbase'
    LOCAL_MYSQL      = 'Local MySQL'
    REMOTE_MYSQL     = 'Remote MySQL'
    CLOUD_SPANNER    = 'Cloud Spanner'
    SAPNNER_EMULATOR = 'Spanner Emulator'
    LOCAL_PGSQL      = 'Local PgSQL'
    REMOTE_PGSQL      = 'Remote PgSQL'

class AppType:
    APPLICATION = 1
    SERVICE     = 2

class InstallOption:
    MANDATORY = 1
    OPTONAL   = 2

class SetupProfiles:
    JANS = 'jans'
    OPENBANKING   = 'openbanking'
    DISA_STIG = 'disa-stig'

COMPLETED = -99
ERROR = -101

suggested_mem_size = 3.7 # in GB
suggested_number_of_cpu = 2
suggested_free_disk_space = 40 #in GB
file_max = 64000
fapolicyd_rule_tmp = 'allow perm=any uid={} : dir={}'
