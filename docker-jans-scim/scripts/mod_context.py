import argparse
import logging.config
import pathlib
import re
import zipfile
from collections import namedtuple

from settings import LOGGING_CONFIG

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("jans-scim")


Library = namedtuple("Library", ["path", "basename", "meta"])

LIB_METADATA_RE = re.compile(r"(?P<name>.*)-(?P<version>\d+.*)(?P<ext>\.jar)")


def get_lib_metadata(path_obj):
    return Library(str(path_obj), path_obj.name, LIB_METADATA_RE.search(path_obj.name).groupdict())


def get_archived_libs(app_name):
    archive_path = f"/opt/jans/jetty/{app_name}/webapps/{app_name}.war"
    with zipfile.ZipFile(archive_path) as zf:
        zp = zipfile.Path(zf).joinpath("WEB-INF/lib")
        return [get_lib_metadata(po) for po in zp.iterdir()]


def get_persistence_common_libs(dirpath):
    root_dir = pathlib.Path(dirpath)
    return [get_lib_metadata(po) for po in root_dir.rglob("*.jar")]


def get_default_custom_libs(app_name):
    return [f"/opt/jans/jetty/{app_name}/custom/libs/*"]


def get_registered_common_libs(app_name, persistence_type):
    libs = get_persistence_common_libs(f"/opt/jans/jetty/common/libs/{persistence_type}")
    archived_libs = get_archived_libs(app_name)
    archived_lib_names = [al.meta["name"] for al in archived_libs]

    return [
        lib.path for lib in libs
        if lib.meta["name"] not in archived_lib_names
    ]


def modify_app_xml(app_name):
    custom_libs = get_default_custom_libs(app_name)

    # render custom xml
    fn = f"/opt/jans/jetty/{app_name}/webapps/{app_name}.xml"

    with open(fn) as f:
        txt = f.read()

    with open(fn, "w") as f:
        ctx = {"extra_classpath": ",".join(custom_libs)}
        f.write(txt % ctx)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("app_name")
    args = parser.parse_args()
    modify_app_xml(args.app_name)
