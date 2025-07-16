package io.jans.util;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Properties;

public abstract class BaseFido2IntegrationTest {

    protected Properties testProps;

    protected void loadTestProperties() throws Exception {
      String path = Paths.get("..", "client", "profiles", "default", "config-fido2-test.properties").toAbsolutePath().toString();
//        String path = Paths.get("..", "..", "jans-linux-setup", "jans_setup", "templates", "test", "jans-fido2", "client", "config-fido2-test.properties").toAbsolutePath().toString();
        try (InputStream input = new FileInputStream(path)) {
            testProps = new Properties();
            testProps.load(input);
            System.out.println("✅ Loaded test properties from: " + path);
        }
    }

    protected String getServerHost() {
        return testProps.getProperty("test.server.name");
    }
}
