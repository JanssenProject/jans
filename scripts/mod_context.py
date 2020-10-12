import glob


def modify_oxauth_xml():
    fn = "/opt/gluu/jetty/oxauth/webapps/oxauth.xml"

    with open(fn) as f:
        txt = f.read()

    with open(fn, "w") as f:
        ctx = {
            "extra_classpath": ",".join([
                j.replace("/opt/gluu/jetty/oxauth", ".")
                for j in glob.iglob("/opt/gluu/jetty/oxauth/custom/libs/*.jar")
            ])
        }
        f.write(txt % ctx)


if __name__ == "__main__":
    modify_oxauth_xml()
