import fnmatch
import os
from pathlib import Path

from helpers import get_logger
from yaml_parser import Parser

logger = get_logger("cn-analyze-chart   ")


def find_replace(directory, find, replace, filepatterm):
    for path, dirs, files in os.walk(os.path.abspath(directory)):
        for filename in fnmatch.filter(files, filepatterm):
            filepath = os.path.join(path, filename)
            with open(filepath) as f:
                s = f.read()
            s = s.replace(find, replace)
            with open(filepath, "w") as f:
                f.write(s)


global_keys_list = ["cnJackrabbitCluster", "jackrabbit", "oxtrust", "oxshibboleth", "cr-rotate"]
config_keys_list = ["cnSyncShibManifests", "cnSyncCasaManifests", "cnOxtrustBackend",
                    "cnJackrabbitPostgresUser", "cnJackrabbitPostgresPasswordFile",
                    "cnJackrabbitPostgresDatabaseName", "cnJackrabbitPostgresHost", "cnJackrabbitPostgresPort",
                    "cnJackrabbitAdminId", "cnJackrabbitAdminPassFile", "cnJackrabbitSyncInterval",
                    "cnJackrabbitUrl", "cnJackrabbitAdminIdFile", "cnDocumentStoreType", "cnOxtrustApiEnabled",
                    "cnOxtrustApiTestMode", "cnPassportEnabled", "cnCasaEnabled", "cnRadiusEnabled",
                    "cnSamlEnabled"]

non_janssen_charts = ["jackrabbit", "oxtrust", "oxshibboleth", "oxpassport", "casa", "cr-rotate", "radius"]
main_dir = "/home/runner/work/test/pygluu/kubernetes/templates/helm/gluu/"

main_values_file = Path(main_dir + "values.yaml").resolve()
main_values_file_parser = Parser(main_values_file, True)

# global values
for key in global_keys_list:
    try:
        del main_values_file_parser["global"][key]
    except KeyError:
        logger.info("Key {} has been removed previously or does not exist".format(key))

# config
for key in config_keys_list:
    try:
        del main_values_file_parser["config"]["configmap"][key]
    except KeyError:
        logger.info("Key {} has been removed previously or does not exist".format(key))

# Charts
for key in non_janssen_charts:
    try:
        del main_values_file_parser[key]
    except KeyError:
        logger.info("Key {} has been removed previously or does not exist".format(key))

main_values_file_parser.dump_it()

main_chart_file = Path(main_dir + "Chart.yaml").resolve()
main_chart_file_parser = Parser(main_chart_file, True)
chart_dependencies = []
for chart in main_chart_file_parser["dependencies"]:
    if chart["name"] not in non_janssen_charts:
        chart_dependencies.append(chart)
main_chart_file_parser["dependencies"] = chart_dependencies
main_chart_file_parser.dump_it()
find_replace(main_dir, "support@gluu.org", "support@jans.io", "*.*")
find_replace(main_dir, "https://www.gluu.org", "https://jans.io", "*.*")
find_replace(main_dir, "https://gluu.org/docs/gluu-server", "https://jans.io", "*.*")
find_replace(main_dir, "Gluu", "Janssen", "*.*")
find_replace(main_dir, "gluu", "jans", "*.*")
find_replace(main_dir, "jansfederation", "gluufederation", "*.*")
find_replace(main_dir, "GLUU", "JANS", "*.*")
