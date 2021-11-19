import glob


def modify_auth_server_xml():
    fn = "/opt/jans/jetty/jans-auth/webapps/jans-auth.xml"

    with open(fn) as f:
        txt = f.read()

    with open(fn, "w") as f:
        ctx = {
            "extra_classpath": ",".join([
                j.replace("/opt/jans/jetty/jans-auth", ".")
                for j in glob.iglob("/opt/jans/jetty/jans-auth/custom/libs/*.jar")
            ])
        }
        f.write(txt % ctx)


if __name__ == "__main__":
    modify_auth_server_xml()
