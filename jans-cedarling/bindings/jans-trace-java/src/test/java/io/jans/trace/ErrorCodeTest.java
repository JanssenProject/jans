// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.testng.annotations.Test;

public class ErrorCodeTest {

    /** The error-code table of the core README is the list of stable codes. */
    private static final Path CORE_README = Paths.get("..", "..", "jans-trace-core", "README.md");

    private static final Pattern TABLE_ROW = Pattern.compile("^\\| `([A-Z_]+)` \\|", Pattern.MULTILINE);

    @Test
    public void enumMatchesTheCoreErrorCodes() throws IOException {
        Matcher rows = TABLE_ROW.matcher(new String(Files.readAllBytes(CORE_README), StandardCharsets.UTF_8));
        Set<String> core = new TreeSet<>();
        while (rows.find()) {
            core.add(rows.group(1));
        }
        Set<String> java = new TreeSet<>();
        Arrays.stream(ErrorCode.values()).forEach(code -> java.add(code.name()));
        assertEquals(java, core, "ErrorCode should list exactly the codes in " + CORE_README);
    }
}
