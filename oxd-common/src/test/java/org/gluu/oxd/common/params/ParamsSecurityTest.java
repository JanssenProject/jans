package org.gluu.oxd.common.params;

import org.apache.commons.lang.StringUtils;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.testng.Assert.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 */
public class ParamsSecurityTest {

    private static final String PARAMS_PACKAGE = "org.gluu.oxd.common.params";
    private static final String CLASS_FILE_SUFFIX = ".class";
    private static final List<Class> EXCLUSING_LIST = Arrays.<Class>asList(
            GetClientTokenParams.class,
            GetRpParams.class,
            RegisterSiteParams.class,
            GetJwksParams.class,
            EmptyParams.class,
            HasOxdIdParams.class,
            HasProtectionAccessTokenParams.class,
            IParams.class
    );

    @Test
    public void checkParamsImplementsHasProtectionAccessTokenInterface() throws IOException {
        for (Class clazz : getAllParamsClasses()) {
            if (EXCLUSING_LIST.contains(clazz)) {
                continue;
            }
            if (!HasProtectionAccessTokenParams.class.isAssignableFrom(clazz)) {
                throw new AssertionError("Params class does not implement HasProtectionAccessTokenParams interface, class: " + clazz);
            }
        }
    }

    private Set<Class> getAllParamsClasses() throws IOException {
        final URL packageResource = Thread.currentThread().getContextClassLoader().getResource(StringUtils.replace(PARAMS_PACKAGE, ".", "/"));
        final File packageFile = new File(StringUtils.replace(packageResource.getFile(), "test-classes", "classes"));
        if (!packageFile.exists()) {
            throw new SkipException("Failed to find test-classes.");
        }
        assertTrue(packageFile.isDirectory());

        final File[] classFiles = packageFile.listFiles();

        Set<Class> classes = new HashSet<>();
        for (File file : classFiles) {
            String resource = PARAMS_PACKAGE + "." + file.getName();
            int endIndex = resource.length() - CLASS_FILE_SUFFIX.length();
            String className = resource.substring(0, endIndex);
            try {
                classes.add(Class.forName(className));
            } catch (ClassNotFoundException ignore) {
            }
        }
        return classes;
    }
}
