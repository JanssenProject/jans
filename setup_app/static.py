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

class AppType:
    APPLICATION = 1
    SERVICE     = 2

class InstallOption:
    MONDATORY = 1
    OPTONAL   = 2

COMPLETED = -99
ERROR = -101

suggested_mem_size = 3.7 # in GB
suggested_number_of_cpu = 2
suggested_free_disk_space = 40 #in GB
file_max = 64000
