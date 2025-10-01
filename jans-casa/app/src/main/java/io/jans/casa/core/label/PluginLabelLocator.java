package io.jans.casa.core.label;

import org.zkoss.util.resource.LabelLocator;

import java.io.Closeable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

/**
 * @author jgomer
 */
public class PluginLabelLocator implements LabelLocator, Closeable {

    private static final String DEFAULT_PROPS_FILE = "zk-label";

    private JarFile jarFile;
    private String sUri;
    private String basePath;
    private String subDirectory;
    private boolean closed;

    public PluginLabelLocator(Path path, String subDir) {

        try {
            this.subDirectory = subDir;
            this.basePath = path.toString();
            this.sUri = path.toUri().toString();

            if (Files.isRegularFile(path) && basePath.toLowerCase().endsWith(".jar")) {
                jarFile = new JarFile(path.toFile(), false, ZipFile.OPEN_READ);
            }
        } catch (IOException e) {
            //Intentionally left empty
        }
    }

    public URL locate(Locale locale) throws MalformedURLException {

        URL url = null;
        if (!closed) {
            String suffix = String.format("%s%s.properties", DEFAULT_PROPS_FILE, locale == null ? "" : "_" + locale.toString());

            if (jarFile == null) {
                Path path = Paths.get(basePath, subDirectory, suffix);
                if (Files.isRegularFile(path)) {
                    url = path.toUri().toURL();
                }
            } else {
                suffix = subDirectory + "/" + suffix;
                if (jarFile.getEntry(suffix) != null) {
                    url = new URL(String.format("jar:%s!/%s", sUri, suffix));
                }
            }
        }
        return url;

    }

    public void close() throws IOException {

        if (jarFile != null) {
            //closed field is needed since there is no way to de-register a LabelLocator in ZK so it flags this label locator
            //is not needed anylonger
            closed = true;
            jarFile.close();
            jarFile = null;
        }

    }

}
