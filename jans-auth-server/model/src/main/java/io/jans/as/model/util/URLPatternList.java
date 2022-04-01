/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.util;

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
    private static final String URL_PATTERN_PARTS_1 = "^((\\*|[A-Za-z-]+):(//)?)?";
    private static final String URL_PATTERN_PARTS_2 = "(\\*|((\\*\\.)?[^*/:]+))?(:(\\d+))?(/.*)?";

    private List<URLPattern> patternList;

    public URLPatternList() {
        this.patternList = new ArrayList<>();
    }

    public URLPatternList(List<String> patternList) {
        this();

        if (patternList != null) {
            for (String urlPattern : patternList) {
                addListEntry(urlPattern);
            }
        }
    }

    public boolean isUrlListed(String uri) {
        if (patternList == null) {
            return true;
        }

        URI parsedUri = URI.create(uri);

        for (URLPattern pattern : patternList) {
            if (pattern.matches(parsedUri)) {
                return true;
            }
        }

        return false;
    }

    public void addListEntry(String urlPattern) {
        if (patternList == null) {
            return;
        }
        if (urlPattern.compareTo("*") == 0) {
            LOG.debug("Unlimited access to network resources");
            patternList = null;
            return;
        }

        // specific access
        try {
            Pattern parts = Pattern.compile(URL_PATTERN_PARTS_1 + URL_PATTERN_PARTS_2);
            Matcher m = parts.matcher(urlPattern);
            addOriginURLMatcher(m);
        } catch (Exception e) {
            LOG.debug("Failed to add origin " + urlPattern);
        }
    }

    private void addOriginURLMatcher(Matcher m) throws MalformedURLException {
        if (m != null && m.matches()) {
            String scheme = m.group(2);
            String host = m.group(4);
            // Special case for two urls which are allowed to have empty hosts
            if (("file".equals(scheme) || "content".equals(scheme)) && host == null) host = "*";
            String port = m.group(8);
            String path = m.group(9);
            if (scheme == null) {
                patternList.add(new URLPattern("http", host, port, path));
                patternList.add(new URLPattern("https", host, port, path));
            } else {
                patternList.add(new URLPattern(scheme, host, port, path));
            }
        }
    }

    private static class URLPattern {
        Pattern scheme;
        Pattern host;
        Integer port;
        Pattern path;

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
