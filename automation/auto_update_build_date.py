from lxml import html
import requests
from pathlib import Path
from dateutil.parser import parse, ParserError

distributions_managed = {
    "opendj-server-legacy": {
        "url": "https://ox.gluu.org/maven/org/gluufederation/opendj/opendj-server-legacy/{}",
        "source_package": "opendj-server-legacy-{}.zip"
    },
    "oxauth-client": {
        "url": "https://maven.jans.io/maven/org/jans/oxauth-client/{}",
        "source_package": "oxauth-client-{}-jar-with-dependencies.jar"
    },
    "oxauth-server": {
        "url": "https://maven.jans.io/maven/org/jans/oxauth-server/{}",
        "source_package": "oxauth-server-{}.war"
    },
    "scim-server": {
        "url": "https://maven.jans.io/maven/org/jans/scim-server/{}",
        "source_package": "scim-server-{}.war"
    },
    "fido2-server": {
        "url": "https://maven.jans.io/maven/org/jans/fido2-server/{}",
        "source_package": "fido2-server-{}.war"
    },
}


def find_date(text):
    try:
        result = parse(text, fuzzy_with_tokens=False)
        result = str(result).replace(":00", "")
        return result
    except ParserError:
        return None


def parse_source(package_name, version):
    url = distributions_managed[package_name]["url"].format(version)
    package = distributions_managed[package_name]["source_package"].format(version)
    page = requests.get(url)
    tree = html.fromstring(page.content)
    a = '//a[@href="{}"] | //td'.format(package)
    table_rows = tree.xpath(a)
    temp_list = []
    for table_row in table_rows:
        table_row_text = table_row.text_content()
        if table_row_text in temp_list:
            i = table_rows.index(table_row)
            text = table_rows[i + 1].text_content().strip()
            if find_date(text):
                return text
            return table_rows[i + 2].text_content().strip()
        if package_name[:20] in table_row_text:
            temp_list.append(table_row_text.strip())
    return None


def find_current_jans_package_version_and_build_date(dockerfile):
    jans_packages = ["oxauth-client", "opendj-server-legacy",
                     "oxauth-server","scim-server",
                     "fido2-server"]
    wrends_version_search_string = "ENV WRENDS_VERSION="
    wrends_build_date_search_string = "ENV WRENDS_BUILD_DATE="
    jans_version_search_string = "ENV CN_VERSION="
    jans_build_date_search_string = "ENV CN_BUILD_DATE="
    jans_package = ""
    jans_version = ""
    jans_build_date = ""
    with open(dockerfile, "r") as file:
        lines = file.readlines()
        for line in lines:
            if jans_version_search_string in line:
                jans_version = line.strip(jans_version_search_string)
            elif jans_build_date_search_string in line:
                jans_build_date = line.strip(jans_build_date_search_string)
            elif wrends_version_search_string in line:
                jans_version = line.strip(wrends_version_search_string)
            elif wrends_build_date_search_string in line:
                jans_build_date = line.strip(jans_build_date_search_string)
            else:
                for package in jans_packages:
                    if package in line:
                        jans_package = package

    jans_version = jans_version.replace('"', "").strip("").strip("\n")
    jans_build_date = jans_build_date.replace('"', "").strip("").strip("\n")
    return jans_version, jans_build_date, jans_package


def update_build_date(dockerfile, old_build_date, new_build_date):
    wrends_build_date_search_string = 'WRENDS_BUILD_DATE=' + '"' + old_build_date + '"'
    jans_build_date_search_string = "CN_BUILD_DATE=" + '"' + old_build_date + '"'
    wrends_build_new_date_string = 'WRENDS_BUILD_DATE=' + '"' + new_build_date + '"'
    jans_build_new_date_string = "CN_BUILD_DATE=" + '"' + new_build_date + '"'
    with open(dockerfile, "r+") as file:
        contents = file.read()
        contents = contents.replace(wrends_build_date_search_string, wrends_build_new_date_string)
        contents = contents.replace(jans_build_date_search_string, jans_build_new_date_string)
    with open(dockerfile, "w+") as file:
        file.write(contents)


def main():
    dockerfile = Path("../Dockerfile")
    jans_version_in_dockerfile, jans_build_date_in_dockerfile, jans_package_name_in_dockerfile = \
        find_current_jans_package_version_and_build_date(dockerfile)

    jans_package_source_timestamp_string = parse_source(jans_package_name_in_dockerfile, jans_version_in_dockerfile)

    if jans_package_source_timestamp_string > jans_build_date_in_dockerfile:
        update_build_date(dockerfile, jans_build_date_in_dockerfile, jans_package_source_timestamp_string)


if __name__ == '__main__':
    main()
