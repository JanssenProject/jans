package io.jans.as.client.util;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang.StringUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Yuriy Z
 */
public class TestPropFile {

    private static final String TEST_PROP_FILE = "test_prop_file";

    private String propFile;

    private List<String> records = new ArrayList<>();

    public TestPropFile(String propFile) {
        this.propFile = propFile;
    }

    public static TestPropFile create(CommandLine cmd) {
        if (cmd.hasOption(TEST_PROP_FILE)) {
            return new TestPropFile(cmd.getOptionValue(TEST_PROP_FILE));
        }
        return new TestPropFile(null);
    }

    public boolean isEmpty() {
        return StringUtils.isBlank(propFile);
    }

    public boolean shouldGenerate() {
        return StringUtils.isNotBlank(propFile);
    }

    public void add(String record) {
        if (shouldGenerate())
            records.add(record);
    }

    public void generate() throws IOException {
        if (isEmpty()) {
            return;
        }

        try (FileOutputStream fosTestPropFile = new FileOutputStream(propFile)) {
            for (String rec : records) {
                fosTestPropFile.write(rec.getBytes());
                fosTestPropFile.write("\n".getBytes());
            }
        }
    }
}
