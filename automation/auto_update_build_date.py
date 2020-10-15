from lxml import html
import requests
from pathlib import Path
from dateutil.parser import parse, ParserError

distributions_managed = {
    "opendj-server-legacy": {
        "url": "https://ox.gluu.org/maven/org/gluufederation/opendj/opendj-server-legacy/{}",
        "source_package": "opendj-server-legacy-{}.zip"
    },
    "passport": {
        "url": "https://ox.gluu.org/npm/passport",
        "source_package": "passport-{}.tgz"
    },
    "oxtrust-server": {
        "url": "https://ox.gluu.org/maven/org/gluu/oxtrust-server/{}",
        "source_package": "oxtrust-server-{}.war"
    },
    "oxauth-client": {
        "url": "https://ox.gluu.org/maven/org/gluu/oxauth-client/{}",
        "source_package": "oxauth-client-{}-jar-with-dependencies.jar"
    },
    "oxauth-server": {
        "url": "https://ox.gluu.org/maven/org/gluu/oxauth-server/{}",
        "source_package": "oxauth-server-{}.war"
    },
    "casa": {
        "url": "https://ox.gluu.org/maven/org/gluu/casa/{}",
        "source_package": "casa-{}.war"
    },
    "oxd-server": {
        "url": "https://ox.gluu.org/maven/org/gluu/oxd-server/{}",
        "source_package": "oxd-server-{}-distribution.zip"
    },
    "scim-server": {
        "url": "https://ox.gluu.org/maven/org/gluu/scim-server/{}",
        "source_package": "scim-server-{}.war"
    },
    "oxshibbolethIdp": {
        "url": "https://ox.gluu.org/maven/org/gluu/oxshibbolethIdp/{}",
        "source_package": "oxshibbolethIdp-{}.war"
    },
    "oxShibbolethStatic": {
        "url": "https://ox.gluu.org/maven/org/gluu/oxShibbolethStatic/{}",
        "source_package": "oxShibbolethStatic-{}.jar"
    },
    "super-gluu-radius-server": {
        "url": "https://ox.gluu.org/maven/org/gluu/super-gluu-radius-server/{}",
        # There is another package super-gluu-radius-server-{}-distribution.zip
        # but the version is the same so we can use one
        "source_package": "super-gluu-radius-server-{}.jar"
    },
    "fido2-server": {
        "url": "https://ox.gluu.org/maven/org/gluu/fido2-server/{}",
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


def find_current_gluu_package_version_and_build_date(dockerfile):
    gluu_packages = ["oxtrust-server", "oxauth-client", "opendj-server-legacy",
                     "oxauth-server", "casa", "oxd-server", "scim-server", "oxshibbolethIdp",
                     "oxShibbolethStatic", "super-gluu-radius-server", "fido2-server", "passport"]
    wrends_version_search_string = "ENV WRENDS_VERSION="
    wrends_build_date_search_string = "ENV WRENDS_BUILD_DATE="
    gluu_version_search_string = "ENV GLUU_VERSION="
    gluu_build_date_search_string = "ENV GLUU_BUILD_DATE="
    gluu_package = ""
    gluu_version = ""
    gluu_build_date = ""
    with open(dockerfile, "r") as file:
        lines = file.readlines()
        for line in lines:
            if gluu_version_search_string in line:
                gluu_version = line.strip(gluu_version_search_string)
            elif gluu_build_date_search_string in line:
                gluu_build_date = line.strip(gluu_build_date_search_string)
            elif wrends_version_search_string in line:
                gluu_version = line.strip(wrends_version_search_string)
            elif wrends_build_date_search_string in line:
                gluu_build_date = line.strip(gluu_build_date_search_string)
            else:
                for package in gluu_packages:
                    if package in line:
                        gluu_package = package

    gluu_version = gluu_version.replace('"', "").strip("").strip("\n")
    gluu_build_date = gluu_build_date.replace('"', "").strip("").strip("\n")
    return gluu_version, gluu_build_date, gluu_package


def update_build_date(dockerfile, old_build_date, new_build_date):
    wrends_build_date_search_string = 'WRENDS_BUILD_DATE=' + '"' + old_build_date + '"'
    gluu_build_date_search_string = "GLUU_BUILD_DATE=" + '"' + old_build_date + '"'
    wrends_build_new_date_string = 'WRENDS_BUILD_DATE=' + '"' + new_build_date + '"'
    gluu_build_new_date_string = "GLUU_BUILD_DATE=" + '"' + new_build_date + '"'
    with open(dockerfile, "r+") as file:
        contents = file.read()
        contents = contents.replace(wrends_build_date_search_string, wrends_build_new_date_string)
        contents = contents.replace(gluu_build_date_search_string, gluu_build_new_date_string)
    with open(dockerfile, "w+") as file:
        file.write(contents)


def main():
    dockerfile = Path("../Dockerfile")
    gluu_version_in_dockerfile, gluu_build_date_in_dockerfile, gluu_package_name_in_dockerfile = \
        find_current_gluu_package_version_and_build_date(dockerfile)

    gluu_package_source_timestamp_string = parse_source(gluu_package_name_in_dockerfile, gluu_version_in_dockerfile)

    if gluu_package_source_timestamp_string > gluu_build_date_in_dockerfile:
        update_build_date(dockerfile, gluu_build_date_in_dockerfile, gluu_package_source_timestamp_string)


if __name__ == '__main__':
    main()
