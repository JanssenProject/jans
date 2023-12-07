package io.jans.casa.core.label;

import org.zkoss.util.resource.LabelLocator;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Locale;

/**
 * @author jgomer
 */
public class SystemLabelLocator implements LabelLocator {

    private String baseUri;
    private String module;
    private URI defaultURI;

    public SystemLabelLocator(String baseUri, String module) {
        //baseUri ends in slash
        this.baseUri = baseUri;
        this.module = module;
        try {
            defaultURI = new URI(String.format("%s%s.properties", baseUri, module));
        } catch (Exception e) {
            defaultURI = null;
        }

    }

    public URL locate(Locale locale) throws MalformedURLException {

        URL url = null;
        if (baseUri.startsWith("file:")) {

            try {
                URI uri;
                if (locale == null) {
                    uri = defaultURI;
                } else {
                    uri = new URI(String.format("%s%s_%s.properties", baseUri, module, locale.toString()));
                }
                if (new File(uri).exists()) {
                    url = uri.toURL();
                }
            } catch (Exception e) {
                //Intentionally left empty
            }
        }
        return url;

    }

}
