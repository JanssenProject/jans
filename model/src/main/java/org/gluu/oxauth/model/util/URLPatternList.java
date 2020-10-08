package org.gluu.oxauth.model.util;

import org.apache.log4j.Logger;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Javier Rojas Blum
 * @version October 10, 2016
 */
public class URLPatternList {

    private static final Logger LOG = Logger.getLogger(URLPatternList.class);

    private List<URLPattern> urlPatternList;

    public URLPatternList() {
        this.urlPatternList = new ArrayList<URLPattern>();
    }

    public URLPatternList(List<String> urlPatternList) {
        this();

        if (urlPatternList != null) {
            for (String urlPattern : urlPatternList) {
                addListEntry(urlPattern);
            }
        }
    }

    public boolean isUrlListed(String uri) {
        if (urlPatternList == null) {
            return true;
        }

        URI parsedUri = URI.create(uri);

        for (URLPattern pattern : urlPatternList) {
            if (pattern.matches(parsedUri)) {
                return true;
            }
        }

        return false;
    }

    public void addListEntry(String urlPattern) {
        if (urlPatternList != null) {
            try {
                if (urlPattern.compareTo("*") == 0) {
                    LOG.debug("Unlimited access to network resources");
                    urlPatternList = null;
                } else { // specific access
                    Pattern parts = Pattern.compile("^((\\*|[A-Za-z-]+):(//)?)?(\\*|((\\*\\.)?[^*/:]+))?(:(\\d+))?(/.*)?");
                    Matcher m = parts.matcher(urlPattern);
                    if (m.matches()) {
                        String scheme = m.group(2);
                        String host = m.group(4);
                        // Special case for two urls which are allowed to have empty hosts
                        if (("file".equals(scheme) || "content".equals(scheme)) && host == null) host = "*";
                        String port = m.group(8);
                        String path = m.group(9);
                        if (scheme == null) {
                            urlPatternList.add(new URLPattern("http", host, port, path));
                            urlPatternList.add(new URLPattern("https", host, port, path));
                        } else {
                            urlPatternList.add(new URLPattern(scheme, host, port, path));
                        }
                    }
                }
            } catch (Exception e) {
                LOG.debug("Failed to add origin " + urlPattern);
            }
        }
    }

    private static class URLPattern {
        public Pattern scheme;
        public Pattern host;
        public Integer port;
        public Pattern path;

        public URLPattern(String scheme, String host, String port, String path) throws MalformedURLException {
            try {
                if (scheme == null || "*".equals(scheme)) {
                    this.scheme = null;
                } else {
                    this.scheme = Pattern.compile(regexFromPattern(scheme, false), Pattern.CASE_INSENSITIVE);
                }
                if ("*".equals(host)) {
                    this.host = null;
                } else if (host.startsWith("*.")) {
                    this.host = Pattern.compile("([a-z0-9.-]*\\.)?" + regexFromPattern(host.substring(2), false), Pattern.CASE_INSENSITIVE);
                } else {
                    this.host = Pattern.compile(regexFromPattern(host, false), Pattern.CASE_INSENSITIVE);
                }
                if (port == null || "*".equals(port)) {
                    this.port = null;
                } else {
                    this.port = Integer.parseInt(port, 10);
                }
                if (path == null || "/*".equals(path)) {
                    this.path = null;
                } else {
                    this.path = Pattern.compile(regexFromPattern(path, true));
                }
            } catch (NumberFormatException e) {
                throw new MalformedURLException("Port must be a number");
            }
        }

        public boolean matches(URI uri) {
            try {
                return ((scheme == null || scheme.matcher(uri.getScheme()).matches()) &&
                        (host == null || host.matcher(uri.getHost()).matches()) &&
                        (port == null || port.equals(uri.getPort())) &&
                        (path == null || path.matcher(uri.getPath()).matches()));
            } catch (Exception e) {
                LOG.debug(e.toString());
                return false;
            }
        }

        private String regexFromPattern(String pattern, boolean allowWildcards) {
            final String toReplace = "\\.[]{}()^$?+|";
            StringBuilder regex = new StringBuilder();
            for (int i = 0; i < pattern.length(); i++) {
                char c = pattern.charAt(i);
                if (c == '*' && allowWildcards) {
                    regex.append(".");
                } else if (toReplace.indexOf(c) > -1) {
                    regex.append('\\');
                }
                regex.append(c);
            }
            return regex.toString();
        }
    }
}
