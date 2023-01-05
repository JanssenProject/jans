/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jakarta.faces.FacesException;
import jakarta.faces.view.facelets.ResourceResolver;

import io.jans.util.StringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * External resource handler to customize applicaton
 *
 * @author Yuriy Movchan Date: 04/05/2016
 */
public class ExternalResourceHandler extends ResourceResolver {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalResourceHandler.class);

    private ResourceResolver parent;

    private File externalResourceBaseFolder;
    private boolean useExternalResourceBase;

    private Map<String, Boolean> pagesUpdateByViewId;

    public ExternalResourceHandler(ResourceResolver parent) {
        this.parent = parent;
        this.pagesUpdateByViewId = Collections.synchronizedMap(new HashMap<String, Boolean>());

        String externalResourceBase = System.getProperty("server.base");
        if (StringHelper.isNotEmpty(externalResourceBase)) {
            externalResourceBase += "/custom/pages";
            File folder = new File(externalResourceBase);
            if (folder.exists() && folder.isDirectory()) {
                this.externalResourceBaseFolder = folder;
                this.useExternalResourceBase = true;
            } else {
                LOG.error("Specified path '" + externalResourceBase + "' in 'server.base' not exists or not a folder!");
            }
        }
    }

    @Override
    public URL resolveUrl(String path) {
        if (!useExternalResourceBase) {
            return this.parent.resolveUrl(path);
        }

        // First try external resource folder
        final File externalResource = new File(this.externalResourceBaseFolder, path);
        if (externalResource.exists()) {
            try {
                // tryToUpdateCachedPageXML(path);

                LOG.debug("Found overriden resource: " + path);
                URL resource = externalResource.toURI().toURL();

                return resource;
            } catch (MalformedURLException ex) {
                throw new FacesException(ex);
            }
        }

        // Return default resource
        return this.parent.resolveUrl(path);
    }

    public String toString() {
        return "ExternalResourceHandler";
    }

    // /**
    // * Try to update cached pages from {@link Pages} class, with one from file
    // system related with given view id.
    // *
    // * @param viewId - a JSF view id.
    // */
    // private void tryToUpdateCachedPageXML(String viewId) {
    // if (!Contexts.isApplicationContextActive() ||
    // pagesUpdateByViewId.containsKey(viewId))
    // return;
    //
    // File pageXMLFile = getPageXMLFileOrNull(viewId);
    // if (pageXMLFile == null)
    // return;
    //
    // InputStream stream = null;
    // try {
    // URL resource = pageXMLFile.toURI().toURL();
    // stream = resource.openStream();
    //
    // if (stream == null)
    // return;
    //
    // log.debug("updating pages.xml file: " + pageXMLFile.getAbsolutePath());
    //
    // Pages pages = Pages.instance();
    // updateCachedPageOrThrow(pages, stream, viewId);
    // updatePageStacksOrThrow(pages, viewId, getUpdatedPageOrThrow(pages, viewId));
    //
    // pagesUpdateByViewId.put(viewId, true);
    // } catch (Exception e) {
    // log.error("Can't update page with viewId: " + viewId, e);
    // } finally {
    // if (stream != null)
    // Resources.closeStream(stream);
    // }
    // }
    //
    // /**
    // * @param viewId - a JSF view id.
    // * @return Get the File object of "*.page.xml" for the given view id.
    // */
    // private File getPageXMLFileOrNull(String viewId) {
    // Pattern pattern = Pattern.compile("(?<=/)(.*)(?=\\.xhtml)");
    // Matcher matcher = pattern.matcher(viewId);
    //
    // if (!matcher.find())
    // return null;
    //
    // String pageXMLPath = matcher.group() + ".page.xml";
    // final File pageXMLFile = new File(this.externalResourceBaseFolder,
    // pageXMLPath);
    // if (!pageXMLFile.exists())
    // return null;
    //
    // return pageXMLFile;
    // }
    //
    // /**
    // * Create and add the page from file system to the {@link Pages#pagesByViewId}
    // map using reflection.
    // *
    // * @param pages - Pages instance.
    // * @param stream - input stream of *.page.xml file from file system realted
    // with view id.
    // * @param viewId - a JSF view id.
    // * @throws Exception
    // */
    // private void updateCachedPageOrThrow(Pages pages, InputStream stream, String
    // viewId) throws Exception {
    // Method method = pages.getClass().getDeclaredMethod("parse",
    // InputStream.class, String.class);
    // method.setAccessible(true);
    // method.invoke(pages, stream, viewId);
    // }
    //
    // /**
    // * @param pages - Pages instance.
    // * @param viewId - a JSF view id.
    // * @return Retrieves Page object from {@link Pages#pagesByViewId} map by view
    // id using reflection.
    // * @throws Exception
    // */
    // private Page getUpdatedPageOrThrow(Pages pages, String viewId) throws
    // Exception {
    // Field pagesByViewIdField =
    // pages.getClass().getDeclaredField("pagesByViewId");
    // pagesByViewIdField.setAccessible(true);
    // Map<String, Page> pagesByViewId = (Map<String, Page>)
    // pagesByViewIdField.get(pages);
    // if (pagesByViewId == null || !pagesByViewId.containsKey(viewId))
    // throw new Exception("Updated cached page with viewId: " + viewId + " is not
    // found.");
    // return pagesByViewId.get(viewId);
    // }
    //
    // /**
    // * Update {@link Pages#pageStacksByViewId} map using reflection.
    // *
    // * @param pages - Pages instance.
    // * @param viewId - a JSF view id.
    // * @param updatedPage - newly created/updated page from file system.
    // * @throws Exception
    // */
    // private void updatePageStacksOrThrow(Pages pages, String viewId, Page
    // updatedPage) throws Exception {
    // Field pageStacksByViewIdField =
    // pages.getClass().getDeclaredField("pageStacksByViewId");
    // pageStacksByViewIdField.setAccessible(true);
    // Map<String, List<Page>> pageStacksByViewId = (Map<String, List<Page>>)
    // pageStacksByViewIdField.get(pages);
    // if (pageStacksByViewId == null)
    // throw new Exception("Can't update page stack with viewId: " + viewId);
    //
    // if (CollectionUtils.isEmpty(pageStacksByViewId.get(viewId))) {
    // pageStacksByViewId.put(viewId, Arrays.asList(new Page[]{updatedPage}));
    // return;
    // }
    // List<Page> pageStacks = pageStacksByViewId.get(viewId);
    // for (int i = 0; i < pageStacks.size(); ++i) {
    // if (pageStacks.get(i).getViewId().equals(viewId))
    // pageStacks.set(i, updatedPage);
    // }
    // }

}
