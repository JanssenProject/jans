import os

from dateutil.parser import parse
from dockerfile_parse import DockerfileParser
from requests_html import HTMLSession


def should_update_build(last_build, new_build):
    def as_date(text):
        return parse(text, fuzzy_with_tokens=False)

    return as_date(new_build) > as_date(last_build)


def main():
    docker_image_folders = [name for name in os.listdir("../") if
                            os.path.isdir(os.path.join("../", name)) and "docker-jans" in name]

    for image in docker_image_folders:
        dfparser = DockerfileParser(f'../{image}')
        version = dfparser.labels["version"]
        try:
            base_url = os.path.dirname(dfparser.envs["CN_SOURCE_URL"])
            pkg_url = os.path.basename(dfparser.envs["CN_SOURCE_URL"])
        except KeyError:
            print(f'Docker image {image} does not contain any packages to update')
            continue

        session = HTMLSession()
        req = session.get(base_url)
        if not req.ok:
            return

        new_build = req.html.xpath(
            f"//a[contains(@href, '{pkg_url}')]/../following-sibling::td",
            first=True,
        ).text

        if should_update_build(dfparser.envs["CN_BUILD_DATE"], new_build):
            print(f"Updating {image} CN_BUILD_DATE to {new_build}")
            # update Dockerfile in-place
            dfparser.envs["CN_BUILD_DATE"] = new_build
        else:
            print(f"No updates found for {image} CN_BUILD_DATE")


if __name__ == "__main__":
    main()
