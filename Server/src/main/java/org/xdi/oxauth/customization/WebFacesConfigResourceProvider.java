package org.xdi.oxauth.customization;

import com.sun.faces.config.WebConfiguration;
import com.sun.faces.util.FacesLogger;
import com.sun.faces.util.Util;
import org.xdi.util.StringHelper;

import javax.servlet.ServletContext;
import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.sun.faces.config.WebConfiguration.WebContextInitParameter.JavaxFacesConfigFiles;

/**
 * Created by eugeniuparvan on 4/26/17.
 */
public class WebFacesConfigResourceProvider extends com.sun.faces.config.configprovider.WebFacesConfigResourceProvider {
    private static final Logger LOGGER = FacesLogger.CONFIG.getLogger();

    public Collection<URI> getResources(ServletContext context) {
        WebConfiguration webConfig = WebConfiguration.getInstance(context);
        String paths = webConfig.getOptionValue(getParameter());
        Set<URI> urls = new LinkedHashSet<URI>(6);

        String externalResourceBase = System.getProperty("catalina.base");
        if (StringHelper.isNotEmpty(externalResourceBase)) {
            externalResourceBase += "/custom/pages";
            File folder = new File(externalResourceBase);
            if (folder.exists() && folder.isDirectory()) {
                File[] directoryListing = folder.listFiles();
                if (directoryListing != null) {
                    for (File child : directoryListing) {
                        if (child.getName().contains("faces-config.xml"))
                            urls.add(child.toURI());
                    }
                }
            } else {
                LOGGER.log(Level.SEVERE, "Specified path '" + externalResourceBase + "' in 'catalina.base' not exists or not a folder!");
            }
        }

        if (paths != null) {
            for (String token : Util.split(context, paths.trim(), getSeparatorRegex())) {
                String path = token.trim();
                if (!isExcluded(path) && path.length() != 0) {
                    URI u = getContextURLForPath(context, path);
                    if (u != null) {
                        urls.add(u);
                    } else {
                        if (LOGGER.isLoggable(Level.WARNING)) {
                            LOGGER.log(Level.WARNING,
                                    "jsf.config.web_resource_not_found",
                                    new Object[]{path, JavaxFacesConfigFiles.getQualifiedName()});
                        }
                    }
                }

            }
        }
        return urls;
    }
}
