import os

from dateutil.parser import parse
from dockerfile_parse import DockerfileParser
from requests_html import HTMLSession


def should_update_build(last_build, new_build):
    def as_date(text):
        return parse(text, fuzzy_with_tokens=False)
    return as_date(new_build) > as_date(last_build)


def main():
    dfparser = DockerfileParser("..")
    base_url = os.path.dirname(dfparser.envs["CN_SOURCE_URL"])
    pkg_url = os.path.basename(dfparser.envs["CN_SOURCE_URL"])

    session = HTMLSession()
    req = session.get(base_url)
    if not req.ok:
        return

    new_build = req.html.xpath(
        f"//a[contains(@href, '{pkg_url}')]/../following-sibling::td",
        first=True,
    ).text

    if should_update_build(dfparser.envs["CN_BUILD_DATE"], new_build):
        print(f"Updating CN_BUILD_DATE to {new_build}")
        # update Dockerfile in-place
        dfparser.envs["CN_BUILD_DATE"] = new_build
    else:
        print("No updates found for CN_BUILD_DATE")


if __name__ == "__main__":
    main()
