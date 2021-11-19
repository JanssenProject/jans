import glob


def modify_config_api_xml():
    fn = "/opt/jans/jetty/jans-config-api/webapps/jans-config-api.xml"

    with open(fn) as f:
        txt = f.read()

    with open(fn, "w") as f:
        ctx = {
            "extra_classpath": ",".join([
                j.replace("/opt/jans/jetty/jans-config-api", ".")
                for j in glob.iglob("/opt/jans/jetty/jans-config-api/custom/libs/*.jar")
            ])
        }
        f.write(txt % ctx)


if __name__ == "__main__":
    modify_config_api_xml()
