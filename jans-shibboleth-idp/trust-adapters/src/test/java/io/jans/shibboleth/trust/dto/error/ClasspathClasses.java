package io.jans.shibboleth.trust.dto.error;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Finds the classes under a set of packages by reading the classpath.
 *
 * <p>Both classpath forms have to be handled, because which one appears depends on how the build is
 * invoked: a full reactor run puts sibling modules on as {@code target/classes} directories, while a
 * single-module run, an IDE, or a CI job that builds modules separately puts them on as jars. A
 * scanner that reads only directories silently finds nothing in the second case — which is why the
 * callers also assert that the scan found a plausible number of classes.
 */
final class ClasspathClasses {

    private ClasspathClasses() {
    }

    /**
     * Every loadable class whose package is, or sits under, one of {@code packagePaths} — given in
     * slash form, e.g. {@code io/jans/kernel}. Classes that cannot be loaded in this context are
     * skipped rather than failing the scan.
     */
    static List<Class<?>> under(List<String> packagePaths) {

        List<Class<?>> found = new ArrayList<>();

        for (String entry : System.getProperty("java.class.path").split(File.pathSeparator)) {

            Path root = Path.of(entry);

            if (Files.isDirectory(root)) {

                collectFromDirectory(root, packagePaths, found);
            } else if (Files.isRegularFile(root) && entry.endsWith(".jar")) {

                collectFromJar(root, packagePaths, found);
            }
        }

        found.sort(Comparator.comparing(Class::getName));
        return found;
    }

    private static void collectFromDirectory(Path root, List<String> packagePaths, List<Class<?>> found) {

        for (String packagePath : packagePaths) {

            Path packageRoot = root.resolve(packagePath);

            if (!Files.isDirectory(packageRoot)) {

                continue;
            }

            try (var files = Files.walk(packageRoot)) {

                files.filter(path -> path.toString().endsWith(".class"))
                    .forEach(path -> add(binaryName(root.relativize(path).toString(), File.separator), found));
            } catch (IOException e) {

                throw new IllegalStateException("Could not scan " + packageRoot, e);
            }
        }
    }

    private static void collectFromJar(Path jar, List<String> packagePaths, List<Class<?>> found) {

        try (ZipFile zip = new ZipFile(jar.toFile())) {

            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {

                String name = entries.nextElement().getName();

                if (name.endsWith(".class") && startsWithAny(name, packagePaths)) {

                    add(binaryName(name, "/"), found);
                }
            }
        } catch (IOException e) {

            // an unreadable jar contributes nothing; other entries still do
        }
    }

    private static boolean startsWithAny(String entryName, List<String> packagePaths) {

        for (String packagePath : packagePaths) {

            if (entryName.startsWith(packagePath + "/")) {

                return true;
            }
        }
        return false;
    }

    private static String binaryName(String path, String separator) {

        return path.replace(separator, ".").replaceAll("\\.class$", "");
    }

    private static void add(String className, List<Class<?>> found) {

        try {

            found.add(Class.forName(className, false, ClasspathClasses.class.getClassLoader()));
        } catch (ClassNotFoundException | LinkageError e) {

            // not loadable here; the rest of the scan still stands
        }
    }
}
