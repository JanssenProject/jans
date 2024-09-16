from datetime import datetime
from datetime import UTC
from hashlib import md5

from jans.pycloudlib.utils import exec_cmd


def utcnow():
    return datetime.now(UTC)


def generalized_time_utc(dtime=None):
    """Calculate LDAP generalized time."""
    if not dtime:
        dtime = utcnow()
    return dtime.strftime("%Y%m%d%H%M%SZ")


def get_ads_project_base64(path):
    out, err, code = exec_cmd(f"base64 -w0 {path}")
    if code != 0:
        raise IOError(f"Unable to resolve contents of {path} as base64 strings; err={err.decode()}")
    return out.decode()


def get_ads_project_md5sum(path):
    with open(path, "rb") as f:
        return md5(f.read()).hexdigest()  # nosec: B324


CASA_AGAMA_DEPLOYMENT_ID = "202447d5-d44c-3125-b1f7-207cb33b6bf7"

CASA_AGAMA_ARCHIVE = "/usr/share/java/casa-agama-project.zip"
