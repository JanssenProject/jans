import os

from dateutil.parser import parse
from dockerfile_parse import DockerfileParser
from requests_html import HTMLSession


def should_update_build(last_build, new_build):
    def as_date(text):
        return parse(text, fuzzy_with_tokens=False)

    return as_date(new_build) > as_date(last_build)


def update_image(image, source_url_env, build_date_env):
    dfparser = DockerfileParser(f'./{image}')
    version = dfparser.labels["version"]
    try:
        base_url = os.path.dirname(dfparser.envs[source_url_env])
        pkg_url = os.path.basename(dfparser.envs[source_url_env])
    except KeyError:
        print(f'Docker image {image} does not contain any packages to update')
        return False
    session = HTMLSession()
    req = session.get(base_url)
    if not req.ok:
        return

    new_build = req.html.xpath(
        f"//a[contains(@href, '{pkg_url}')]/../following-sibling::td",
        first=True,
    ).text

    if should_update_build(dfparser.envs[build_date_env], new_build):
        print(f"Updating {image} {build_date_env} to {new_build}")
        # update Dockerfile in-place
        dfparser.envs[build_date_env] = new_build
    else:
        print(f"No updates found for {image} {build_date_env}")


def main():
    docker_image_folders = [name for name in os.listdir(".") if (os.path.isdir(os.path.join("./", name)) and "docker-jans" in name)]

    for image in docker_image_folders:
        try:
            print(image)
            update_image(image, "CN_SOURCE_URL", "CN_BUILD_DATE")
            if image == "docker-jans-config-api":
                update_image(image, "SCIM_PLUGIN_SOURCE_URL", "SCIM_PLUGIN_BUILD_DATE")
                update_image(image, "ADMIN_UI_PLUGIN_SOURCE_URL", "ADMIN_UI_PLUGIN_BUILD_DATE")
                update_image(image, "FIDO2_PLUGIN_SOURCE_URL", "FIDO2_PLUGIN_BUILD_DATE")
                update_image(image, "USER_MGT_PLUGIN_SOURCE_URL", "USER_MGT_PLUGIN_BUILD_DATE")
        except KeyError:
            print(f'Docker image {image} does not contain any packages to update')
            continue


if __name__ == "__main__":
    main()
