package org.xdi.oxauth.customization;

import com.sun.faces.RIConstants;
import com.sun.faces.application.ApplicationAssociate;
import com.sun.faces.application.WebappLifecycleListener;
import com.sun.faces.config.ConfigurationException;
import com.sun.faces.config.InitFacesContext;
import com.sun.faces.config.Verifier;
import com.sun.faces.config.WebConfiguration;
import com.sun.faces.el.ELContextImpl;
import com.sun.faces.el.ELUtils;
import com.sun.faces.mgbean.BeanBuilder;
import com.sun.faces.mgbean.BeanManager;
import com.sun.faces.scripting.groovy.GroovyHelper;
import com.sun.faces.scripting.groovy.GroovyHelperFactory;
import com.sun.faces.util.*;
import com.sun.faces.util.Timer;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.faces.FactoryFinder;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.sun.faces.RIConstants.ANNOTATED_CLASSES;
import static com.sun.faces.config.WebConfiguration.BooleanWebContextInitParameter.*;
import static com.sun.faces.config.WebConfiguration.WebContextInitParameter.JavaxFacesProjectStage;

/**
 * Created by eugeniuparvan on 4/26/17.
 */
public class ConfigureListener extends com.sun.faces.config.ConfigureListener {
    private static final Logger LOGGER = FacesLogger.CONFIG.getLogger();
    private ScheduledThreadPoolExecutor webResourcePool;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();

        Timer timer = Timer.getInstance();
        if (timer != null) {
            timer.startTiming();
        }

        ConfigManager configManager = ConfigManager.getInstance(context);
        if (null == configManager) {
            configManager =  ConfigManager.createInstance(context);
        }
        if (configManager.hasBeenInitialized(context)) {
            return;
        }

        InitFacesContext initContext = new InitFacesContext(context);

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE,
                    MessageFormat.format(
                            "ConfigureListener.contextInitialized({0})",
                            getServletContextIdentifier(context)));
        }

        webConfig = WebConfiguration.getInstance(context);

        // Check to see if the FacesServlet is present in the
        // web.xml.   If it is, perform faces configuration as normal,
        // otherwise, simply return.
        Object mappingsAdded = context.getAttribute(RIConstants.FACES_INITIALIZER_MAPPINGS_ADDED);
        if (mappingsAdded != null) {
            context.removeAttribute(RIConstants.FACES_INITIALIZER_MAPPINGS_ADDED);
        }

        WebXmlProcessor webXmlProcessor = new WebXmlProcessor(context);
        if (mappingsAdded == null) {
            if (!webXmlProcessor.isFacesServletPresent()) {
                if (!webConfig.isOptionEnabled(ForceLoadFacesConfigFiles)) {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(Level.FINE,
                                "No FacesServlet found in deployment descriptor - bypassing configuration");
                    }
                    clearWebConfiguration(context);
                    configManager.destroy(context);
                    ConfigManager.removeInstance(context);
                    InitFacesContext.cleanupInitMaps(context);
                    return;
                }
            } else {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE,
                            "FacesServlet found in deployment descriptor - processing configuration.");
                }
            }
        }

        if (webXmlProcessor.isDistributablePresent()) {
            webConfig.setOptionEnabled(WebConfiguration.BooleanWebContextInitParameter.EnableDistributable, true);
            context.setAttribute(WebConfiguration.BooleanWebContextInitParameter.EnableDistributable.getQualifiedName(), Boolean.TRUE);
        }


        // bootstrap of faces required
        webAppListener = new WebappLifecycleListener(context);
        webAppListener.contextInitialized(sce);
        ReflectionUtils.initCache(Thread.currentThread().getContextClassLoader());
        Throwable caughtThrowable = null;

        try {

            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.log(Level.INFO,
                        "jsf.config.listener.version",
                        getServletContextIdentifier(context));
            }

            if (webConfig.isOptionEnabled(VerifyFacesConfigObjects)) {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.warning("jsf.config.verifyobjects.development_only");
                }
                // if we're verifying, force bean validation to occur at startup as well
                webConfig.overrideContextInitParameter(EnableLazyBeanValidation, false);

                Constructor<Verifier> constructor= (Constructor<Verifier>) Verifier.class.getDeclaredConstructors()[0];
                constructor.setAccessible(true);
                Verifier verifier = constructor.newInstance();
                constructor.setAccessible(false);
                Verifier.setCurrentInstance(verifier);
            }
            initScripting();
            configManager.initialize(context);
            if (shouldInitConfigMonitoring()) {
                initConfigMonitoring(context);
            }

            // Step 7, verify that all the configured factories are available
            // and optionall that configured objects can be created.
            Verifier v = Verifier.getCurrentInstance();
            if (v != null && !v.isApplicationValid() && LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.severe("jsf.config.verifyobjects.failures_detected");
                StringBuilder sb = new StringBuilder(128);
                for (String m : v.getMessages()) {
                    sb.append(m).append('\n');
                }
                LOGGER.severe(sb.toString());
            }
            registerELResolverAndListenerWithJsp(context, false);
            ApplicationAssociate associate =
                    ApplicationAssociate.getInstance(context);
            ELContext elctx = new ELContextImpl(initContext.getApplication().getELResolver());
            elctx.putContext(FacesContext.class, initContext);
            ExpressionFactory exFactory = ELUtils.getDefaultExpressionFactory(associate, initContext);
            if (null != exFactory) {
                elctx.putContext(ExpressionFactory.class, exFactory);
            }
            initContext.setELContext(elctx);
            if (associate != null) {
                associate.setContextName(getServletContextIdentifier(context));
                BeanManager manager = associate.getBeanManager();
                List<String> eagerBeans = manager.getEagerBeanNames();
                if (!eagerBeans.isEmpty()) {
                    for (String name : eagerBeans) {
                        manager.create(name, initContext);
                    }
                }
                boolean isErrorPagePresent = webXmlProcessor.isErrorPagePresent();
                associate.setErrorPagePresent(isErrorPagePresent);
                context.setAttribute(RIConstants.ERROR_PAGE_PRESENT_KEY_NAME,
                        isErrorPagePresent);

            }

            webConfig.doPostBringupActions();
            try {
                Method publishPostConfigEvent = configManager.getClass().getDeclaredMethod("publishPostConfigEvent");
                publishPostConfigEvent.setAccessible(true);
                publishPostConfigEvent.invoke(configManager);
                publishPostConfigEvent.setAccessible(false);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Can't invoke ConfigManager.publishPostConfigEvent method");
            }

        } catch (Throwable t) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, "Critical error during deployment: ", t);
            }
            caughtThrowable = t;

        } finally {
            sce.getServletContext().removeAttribute(ANNOTATED_CLASSES);

            Verifier.setCurrentInstance(null);
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE,
                        "jsf.config.listener.version.complete");
            }
            if (timer != null) {
                timer.stopTiming();
                timer.logResult("Initialization of context " +
                        getServletContextIdentifier(context));
            }
            if (null != caughtThrowable) {
                throw new RuntimeException(caughtThrowable);
            }
            // Bug 20458755: The InitFacesContext was not being cleaned up, resulting in
            // a partially constructed FacesContext being made available
            // to other code that re-uses this Thread at init time.
            try {
                Method removeInitContextEntryForCurrentThread = initContext.getClass().getDeclaredMethod("removeInitContextEntryForCurrentThread");
                removeInitContextEntryForCurrentThread.setAccessible(true);
                removeInitContextEntryForCurrentThread.invoke(initContext);
                removeInitContextEntryForCurrentThread.setAccessible(false);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Can't invoke InitFacesContext.removeInitContextEntryForCurrentThread method");
            }
        }
    }

    private boolean shouldInitConfigMonitoring() {

        boolean development = isDevModeEnabled();
        boolean threadingOptionSpecified = webConfig.isSet(EnableThreading);
        if (development && !threadingOptionSpecified) {
            return true;
        }
        boolean threadingOption = webConfig.isOptionEnabled(EnableThreading);
        return (development && threadingOptionSpecified && threadingOption);

    }

    private void initConfigMonitoring(ServletContext context) {

        //noinspection unchecked
        Collection<URI> webURIs =
                (Collection<URI>) context.getAttribute("com.sun.faces.webresources");
        if (isDevModeEnabled() && webURIs != null && !webURIs.isEmpty()) {
            webResourcePool = new ScheduledThreadPoolExecutor(1, new MojarraThreadFactory("WebResourceMonitor"));
            webResourcePool.scheduleAtFixedRate(new WebConfigResourceMonitor(context, webURIs),
                    2000,
                    2000,
                    TimeUnit.MILLISECONDS);
        }
        context.removeAttribute("com.sun.faces.webresources");

    }

    private boolean isDevModeEnabled() {

        // interrogate the init parameter directly vs looking up the application
        return "Development".equals(webConfig.getOptionValue(JavaxFacesProjectStage));

    }

    private void initScripting() {
        if (webConfig.isOptionEnabled(EnableGroovyScripting)) {
            GroovyHelper helper = GroovyHelperFactory.createHelper();
            if (helper != null) {
                helper.setClassLoader();
            }
        }
    }

    private static String getServletContextIdentifier(ServletContext context) {
        if (context.getMajorVersion() == 2 && context.getMinorVersion() < 5) {
            return context.getServletContextName();
        } else {
            return context.getContextPath();
        }
    }

    /**
     * This method will be invoked {@link WebConfigResourceMonitor} when
     * changes to any of the faces-config.xml files included in WEB-INF
     * are modified.
     */
    private void reload(ServletContext sc) {

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.log(Level.INFO,
                    "Reloading JSF configuration for context {0}",
                    getServletContextIdentifier(sc));
        }
        GroovyHelper helper = GroovyHelper.getCurrentInstance();
        if (helper != null) {
            helper.setClassLoader();
        }
        // tear down the application
        try {
            // this will only be true in the automated test usage scenario
            if (null != webAppListener) {
                List<HttpSession> sessions = webAppListener.getActiveSessions();
                if (sessions != null) {
                    for (HttpSession session : sessions) {
                        if (LOGGER.isLoggable(Level.INFO)) {
                            LOGGER.log(Level.INFO,
                                    "Invalidating Session {0}",
                                    session.getId());
                        }
                        session.invalidate();
                    }
                }
            }
            ApplicationAssociate associate = ApplicationAssociate.getInstance(sc);
            if (associate != null) {
                BeanManager manager = associate.getBeanManager();
                for (Map.Entry<String, BeanBuilder> entry : manager.getRegisteredBeans().entrySet()) {
                    String name = entry.getKey();
                    BeanBuilder bean = entry.getValue();
                    if (ELUtils.Scope.APPLICATION.toString().equals(bean.getScope())) {
                        if (LOGGER.isLoggable(Level.INFO)) {
                            LOGGER.log(Level.INFO,
                                    "Removing application scoped managed bean: {0}",
                                    name);
                        }
                        sc.removeAttribute(name);
                    }

                }
            }
            // Release any allocated application resources
            FactoryFinder.releaseFactories();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            FacesContext initContext = new InitFacesContext(sc);
            ApplicationAssociate
                    .clearInstance(initContext.getExternalContext());
            ApplicationAssociate.setCurrentInstance(null);
            // Release the initialization mark on this web application
            ConfigManager configManager = ConfigManager.getInstance(sc);
            if (null != configManager) {
                configManager.destroy(sc);
                ConfigManager.removeInstance(sc);
            } else {
                if (LOGGER.isLoggable(Level.SEVERE)) {
                    LOGGER.log(Level.SEVERE, "Unexpected state during reload: no ConfigManager instance in current ServletContext but one is expected to exist.");
                }
            }
            initContext.release();
            ReflectionUtils.clearCache(Thread.currentThread().getContextClassLoader());
            clearWebConfiguration(sc);
        }

        // bring the application back up, avoid re-registration of certain JSP
        // artifacts.  No verification will be performed either to make this
        // light weight.

        // init a new WebAppLifecycleListener so that the cached ApplicationAssociate
        // is removed.
        webAppListener = new WebappLifecycleListener(sc);

        FacesContext initContext = new InitFacesContext(sc);
        ReflectionUtils
                .initCache(Thread.currentThread().getContextClassLoader());

        try {
            ConfigManager configManager = ConfigManager.createInstance(sc);
            if (null != configManager) {
                configManager.initialize(sc);
            } else {
                if (LOGGER.isLoggable(Level.SEVERE)) {
                    LOGGER.log(Level.SEVERE, "Unexpected state during reload: no ConfigManager instance in current ServletContext but one is expected to exist.");
                }
            }

            registerELResolverAndListenerWithJsp(sc, true);
            ApplicationAssociate associate =
                    ApplicationAssociate.getInstance(sc);
            if (associate != null) {
                Boolean errorPagePresent = (Boolean) sc.getAttribute(RIConstants.ERROR_PAGE_PRESENT_KEY_NAME);
                if (null != errorPagePresent) {
                    associate.setErrorPagePresent(errorPagePresent);
                    associate.setContextName(getServletContextIdentifier(sc));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            initContext.release();
        }

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.log(Level.INFO,
                    "Reload complete.",
                    getServletContextIdentifier(sc));
        }

    }

    private void clearWebConfiguration(ServletContext servletContext){
        try {
            Method clear = WebConfiguration.class.getDeclaredMethod("clear", ServletContext.class);
            clear.setAccessible(true);
            clear.invoke(null, servletContext);
            clear.setAccessible(false);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Can't invoke WebConfiguration.clear method");
        }
    }
    /**
     * <p>Processes a web application's deployment descriptor looking
     * for a reference to <code>javax.faces.webapp.FacesServlet</code>.</p>
     */
    private static class WebXmlProcessor {

        private static final String WEB_XML_PATH = "/WEB-INF/web.xml";
        private static final String WEB_FRAGMENT_PATH = "META-INF/web-fragment.xml";

        private boolean facesServletPresent;
        private boolean errorPagePresent;
        private boolean distributablePresent;


        /**
         * <p>When instantiated, the web.xml of the current application
         * will be scanned looking for a references to the
         * <code>FacesServlet</code>.  <code>isFacesServletPresent()</code>
         * will return the appropriate value based on the scan.</p>
         *
         * @param context the <code>ServletContext</code> for the application
         *                of interest
         */
        WebXmlProcessor(ServletContext context) {

            if (context != null) {
                scanForFacesServlet(context);
            }

        } // END WebXmlProcessor


        /**
         * @return <code>true</code> if the <code>WebXmlProcessor</code>
         * detected a <code>FacesServlet</code> entry, otherwise return
         * <code>false</code>.</p>
         */
        boolean isFacesServletPresent() {

            return facesServletPresent;

        } // END isFacesServletPresent


        /**
         * @return <code>true</code> if <code>WEB-INF/web.xml</code> contains
         * a <code>&lt;error-page&gt;</code> element.
         */
        boolean isErrorPagePresent() {

            return errorPagePresent;

        }

        /*
         * return true if <distributable /> is present in the web.xml or a fragment.
         *
         */
        public boolean isDistributablePresent() {
            return distributablePresent;
        }


        /**
         * <p>Parse the web.xml for the current application and scan
         * for a FacesServlet entry, if found, set the
         * <code>facesServletPresent</code> property to true.
         *
         * @param context the ServletContext instance for this application
         */
        private void scanForFacesServlet(ServletContext context) {
            InputStream in = context.getResourceAsStream(WEB_XML_PATH);
            if (in == null) {
                if (context.getMajorVersion() < 3) {
                    throw new ConfigurationException("no web.xml present");
                }
            }
            SAXParserFactory factory = getConfiguredFactory();
            if (in != null) {
                try {
                    SAXParser parser = factory.newSAXParser();
                    parser.parse(in, new WebXmlHandler());
                } catch (Exception e) {
                    warnProcessingError(e, context);
                    facesServletPresent = true;
                    return;
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (Exception ioe) {
                            if (LOGGER.isLoggable(Level.FINEST)) {
                                LOGGER.log(Level.FINEST, "Closing stream", ioe);
                            }
                        }
                    }
                }
            }
            if (!facesServletPresent && context.getMajorVersion() >= 3) {
                ClassLoader cl = Util.getCurrentLoader(this);
                Enumeration<URL> urls;
                try {
                    urls = cl.getResources(WEB_FRAGMENT_PATH);
                } catch (IOException ioe) {
                    throw new ConfigurationException(ioe);
                }
                if (urls != null) {
                    while (urls.hasMoreElements() && !facesServletPresent) {
                        InputStream fragmentStream = null;
                        try {
                            URL url = urls.nextElement();
                            URLConnection conn = url.openConnection();
                            conn.setUseCaches(false);
                            fragmentStream = conn.getInputStream();
                            SAXParser parser = factory.newSAXParser();
                            parser.parse(fragmentStream, new WebXmlHandler());
                        } catch (Exception e) {
                            warnProcessingError(e, context);
                            facesServletPresent = true;
                            return;
                        } finally {
                            if (fragmentStream != null) {
                                try {
                                    fragmentStream.close();
                                } catch (IOException ioe) {
                                    if (LOGGER.isLoggable(Level.WARNING)) {
                                        LOGGER.log(Level.WARNING,
                                                "Exception whil scanning for FacesServlet", ioe);
                                    }
                                }
                            }
                        }
                    }
                }
            }

        } // END scanForFacesServlet

        /**
         * <p>Return a <code>SAXParserFactory</code> instance that is
         * non-validating and is namespace aware.</p>
         *
         * @return configured <code>SAXParserFactory</code>
         */
        private SAXParserFactory getConfiguredFactory() {

            SAXParserFactory factory = Util.createSAXParserFactory();
            factory.setValidating(false);
            factory.setNamespaceAware(true);
            return factory;

        } // END getConfiguredFactory


        private void warnProcessingError(Exception e, ServletContext sc) {

            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(Level.WARNING,
                        MessageFormat.format(
                                "jsf.configuration.web.xml.parse.failed",
                                getServletContextIdentifier(sc)),
                        e);
            }

        }


        /**
         * <p>A simple SAX handler to process the elements of interested
         * within a web application's deployment descriptor.</p>
         */
        private class WebXmlHandler extends DefaultHandler {

            private static final String ERROR_PAGE = "error-page";
            private static final String SERVLET_CLASS = "servlet-class";
            private static final String FACES_SERVLET =
                    "javax.faces.webapp.FacesServlet";

            private boolean servletClassFound;
            @SuppressWarnings({"StringBufferField"})
            private StringBuffer content;

            public InputSource resolveEntity(String publicId, String systemId)
                    throws SAXException {

                return new InputSource(new StringReader(""));

            } // END resolveEntity


            public void startElement(String uri, String localName,
                                     String qName, Attributes attributes)
                    throws SAXException {

                if (!errorPagePresent && ERROR_PAGE.equals(localName)) {
                    errorPagePresent = true;
                    return;
                }
                if (!facesServletPresent) {
                    if (SERVLET_CLASS.equals(localName)) {
                        servletClassFound = true;
                        //noinspection StringBufferWithoutInitialCapacity
                        content = new StringBuffer();
                    } else {
                        servletClassFound = false;
                    }
                }
                if ("distributable".equals(localName)) {
                    distributablePresent = true;
                }


            } // END startElement


            public void characters(char[] ch, int start, int length)
                    throws SAXException {

                if (servletClassFound && !facesServletPresent) {
                    content.append(ch, start, length);
                }

            } // END characters


            public void endElement(String uri, String localName, String qName)
                    throws SAXException {

                if (servletClassFound && !facesServletPresent &&
                        FACES_SERVLET.equals(content.toString().trim())) {
                    facesServletPresent = true;
                }

            } // END endElement

        } // END WebXmlHandler

    } // END WebXmlProcessor


    private class WebConfigResourceMonitor implements Runnable {

        private List<Monitor> monitors;
        private ServletContext sc;

        // -------------------------------------------------------- Constructors


        public WebConfigResourceMonitor(ServletContext sc, Collection<URI> uris) {

            assert (uris != null);
            this.sc = sc;
            for (URI uri : uris) {
                if (monitors == null) {
                    monitors = new ArrayList<Monitor>(uris.size());
                }
                try {
                    Monitor m = new Monitor(uri);
                    monitors.add(m);
                } catch (IOException ioe) {
                    if (LOGGER.isLoggable(Level.SEVERE)) {
                        LOGGER.severe("Unable to setup resource monitor for "
                                + uri.toString()
                                + ".  Resource will not be monitored for changes.");
                    }
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(Level.FINE,
                                ioe.toString(),
                                ioe);
                    }
                }
            }

        }


        // ----------------------------------------------- Methods from Runnable

        /**
         * PENDING javadocs
         */
        public void run() {

            assert (monitors != null);
            boolean reloaded = false;
            for (Iterator<Monitor> i = monitors.iterator(); i.hasNext(); ) {
                Monitor m = i.next();
                try {
                    if (m.hasBeenModified()) {
                        if (!reloaded) {
                            reloaded = true;
                        }
                    }
                } catch (IOException ioe) {
                    if (LOGGER.isLoggable(Level.SEVERE)) {
                        LOGGER.severe("Unable to access url "
                                + m.uri.toString()
                                + ".  Monitoring for this resource will no longer occur.");
                    }
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(Level.FINE,
                                ioe.toString(),
                                ioe);
                    }
                    i.remove();
                }
            }
            if (reloaded) {
                reload(sc);
            }

        }


        // ------------------------------------------------------- Inner Classes


        private class Monitor {

            private URI uri;
            private long timestamp = -1;

            // ---------------------------------------------------- Constructors


            Monitor(URI uri) throws IOException {

                this.uri = uri;
                this.timestamp = getLastModified();
                if (LOGGER.isLoggable(Level.INFO)) {
                    LOGGER.log(Level.INFO,
                            "Monitoring {0} for modifications",
                            uri.toURL().toExternalForm());
                }

            }


            // ----------------------------------------- Package Private Methods


            boolean hasBeenModified() throws IOException {
                long temp = getLastModified();
                if (timestamp < temp) {
                    timestamp = temp;
                    if (LOGGER.isLoggable(Level.INFO)) {
                        LOGGER.log(Level.INFO,
                                "{0} changed!",
                                uri.toURL().toExternalForm());
                    }
                    return true;
                }
                return false;

            }


            // ------------------------------------------------- Private Methods


            private long getLastModified() throws IOException {

                InputStream in = null;
                try {
                    URLConnection conn = uri.toURL().openConnection();
                    conn.connect();
                    in = conn.getInputStream();
                    return conn.getLastModified();
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException ignored) {
                            if (LOGGER.isLoggable(Level.FINEST)) {
                                LOGGER.log(Level.FINEST,
                                        "Exception while closing stream", ignored);
                            }
                        }
                    }
                }

            }

        } // END Monitor

    } // END WebConfigResourceMonitor

}
