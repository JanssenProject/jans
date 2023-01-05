import argparse
import glob
import logging.config
import os
import pathlib
import re
import sys
import zipfile
from collections import namedtuple

from jans.pycloudlib.persistence import PersistenceMapper
from jans.pycloudlib.utils import exec_cmd

from settings import LOGGING_CONFIG

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("entrypoint")


Library = namedtuple("Library", ["path", "basename", "meta"])

LIB_METADATA_RE = re.compile(r"(?P<name>.*)-(?P<version>\d+(?:.\d+).*)(?P<ext>\..*)")


def extract_common_libs(persistence_type):
    dist_file = f"/usr/share/java/{persistence_type}-libs.zip"

    # download if file is missing
    if not os.path.exists(dist_file):
        version = os.environ.get("CN_VERSION")
        download_url = f"https://jenkins.jans.io/maven/io/jans/jans-orm-{persistence_type}-libs/{version}/jans-orm-{persistence_type}-libs-{version}-distribution.zip"
        basename = os.path.basename(download_url)

        logger.info(f"Downloading {basename} as {dist_file}")

        out, err, code = exec_cmd(f"wget -q {download_url} -O {dist_file}")

        if code != 0:
            err = out or err
            logger.error(f"Unable to download {basename}; reason={err.decode()}")
            sys.exit(1)

    # extract
    logger.info(f"Extracting {dist_file}")
    out, err, code = exec_cmd(f"unzip -q {dist_file} -o -d /opt/jans/jetty/common/libs/{persistence_type}/")
    if code != 0:
        out = out or err
        logger.error(f"Unable to extract {dist_file}; reason={err.decode()}")
        sys.exit(1)


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
    root = f"/opt/jans/jetty/{app_name}"
    return [jar.replace(root, ".") for jar in glob.iglob(f"{root}/custom/libs/*.jar")]


def get_registered_common_libs(app_name, persistence_type):
    libs = get_persistence_common_libs(f"/opt/jans/jetty/common/libs/{persistence_type}")
    archived_libs = get_archived_libs(app_name)
    archived_lib_names = [al.meta["name"] for al in archived_libs]

    reg_libs = [
        lib.path for lib in libs
        if lib.meta["name"] not in archived_lib_names
    ]
    return reg_libs


def modify_app_xml(app_name):
    custom_libs = get_default_custom_libs(app_name)

    mapper = PersistenceMapper()
    persistence_groups = mapper.groups().keys()

    for persistence_type in ["spanner", "couchbase"]:
        if persistence_type not in persistence_groups:
            continue

        extract_common_libs(persistence_type)
        custom_libs += get_registered_common_libs(app_name, persistence_type)

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
