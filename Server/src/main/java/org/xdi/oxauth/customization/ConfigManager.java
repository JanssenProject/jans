package org.xdi.oxauth.customization;

import com.sun.faces.RIConstants;
import com.sun.faces.config.*;
import com.sun.faces.config.configprovider.*;
import com.sun.faces.config.processor.*;
import com.sun.faces.el.ELContextImpl;
import com.sun.faces.el.ELUtils;
import com.sun.faces.spi.*;
import com.sun.faces.util.FacesLogger;
import com.sun.faces.util.Util;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

import javax.el.ELContext;
import javax.el.ELContextEvent;
import javax.el.ELContextListener;
import javax.el.ExpressionFactory;
import javax.faces.FacesException;
import javax.faces.FactoryFinder;
import javax.faces.application.Application;
import javax.faces.application.ApplicationConfigurationPopulator;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.event.PostConstructApplicationEvent;
import javax.servlet.ServletContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sun.faces.RIConstants.ANNOTATED_CLASSES;
import static com.sun.faces.config.WebConfiguration.BooleanWebContextInitParameter.*;
import static com.sun.faces.spi.ConfigurationResourceProviderFactory.ProviderType.FaceletConfig;
import static com.sun.faces.spi.ConfigurationResourceProviderFactory.ProviderType.FacesConfig;

/**
 * Created by eugeniuparvan on 4/26/17.
 */
public class ConfigManager extends com.sun.faces.config.ConfigManager {
    private static final Logger LOGGER = FacesLogger.CONFIG.getLogger();

    private static final Pattern JAR_PATTERN = Pattern.compile("(.*/(\\S*\\.jar)).*(/faces-config.xml|/*.\\.faces-config.xml)");

    /**
     * <p>
     * A List of resource providers that search for faces-config documents.
     * By default, this contains a provider for the Mojarra, and two other
     * providers to satisfy the requirements of the specification.
     * </p>
     */
    private final List<ConfigurationResourceProvider> FACES_CONFIG_RESOURCE_PROVIDERS;

    /**
     * <p>
     * A List of resource providers that search for faces-config documents.
     * By default, this contains a provider for the Mojarra, and one other
     * providers to satisfy the requirements of the specification.
     * </p>
     */
    private final List<ConfigurationResourceProvider> FACELET_TAGLIBRARY_RESOURCE_PROVIDERS;

    /**
     * <p>
     * The <code>ConfigManager</code> will multithread the calls to the
     * <code>ConfigurationResourceProvider</code>s as well as any calls
     * to parse a resources into a DOM.  By default, we'll use only 5 threads
     * per web application.
     * </p>
     */
    private static final int NUMBER_OF_TASK_THREADS = 5;

    private static final String CONFIG_MANAGER_INSTANCE_KEY = RIConstants.FACES_PREFIX + "CONFIG_MANAGER_KEY";

    /**
     * The application-scoped key under which the Future responsible for annotation
     * scanning is associated with.
     */
    private static final String ANNOTATIONS_SCAN_TASK_KEY =
            com.sun.faces.config.ConfigManager.class.getName() + "_ANNOTATION_SCAN_TASK";


    /**
     * The initialization time FacesContext scoped key under which the
     * InjectionProvider is stored.
     */
    public static final String INJECTION_PROVIDER_KEY =
            com.sun.faces.config.ConfigManager.class.getName() + "_INJECTION_PROVIDER_TASK";


    /**
     * Name of the attribute added by ParseTask to indicate a
     * {@link Document} instance as a representation of
     * <code>/WEB-INF/faces-config.xml</code>.
     */
    public static final String WEB_INF_MARKER = "com.sun.faces.webinf";


    /**
     * <p>
     * Contains each <code>ServletContext</code> that we've initialized.
     * The <code>ServletContext</code> will be removed when the application
     * is destroyed.
     * </p>
     */
    @SuppressWarnings({"CollectionWithoutInitialCapacity"})
    private List<ServletContext> initializedContexts =
            new CopyOnWriteArrayList<ServletContext>();

    /**
     * <p>
     * The chain of {@link ConfigProcessor} instances to processing of
     * faces-config documents.
     * </p>
     */
    private final ConfigProcessor FACES_CONFIG_PROCESSOR_CHAIN;


    /**
     * <p>
     * The chain of {@link ConfigProcessor} instances to processing of
     * facelet-taglib documents.
     * </p>
     */
    private final ConfigProcessor FACELET_TAGLIB_CONFIG_PROCESSOR_CHAIN;

    /**
     * Stylesheet to convert 1.0 and 1.1 based faces-config documents
     * to our private 1.1 schema for validation.
     */
    private static final String FACES_TO_1_1_PRIVATE_XSL =
            "/com/sun/faces/jsf1_0-1_1toSchema.xsl";

    /**
     * Stylesheet to convert 1.0 facelet-taglib documents
     * from 1.0 to 2.0 for schema validation purposes.
     */
    private static final String FACELETS_TO_2_0_XSL =
            "/com/sun/faces/facelets1_0-2_0toSchema.xsl";

    private static final String FACES_CONFIG_1_X_DEFAULT_NS =
            "http://java.sun.com/JSF/Configuration";

    private static final String FACELETS_1_0_DEFAULT_NS =
            "http://java.sun.com/JSF/Facelet";


    public ConfigManager() {

        // initialize the resource providers for faces-config documents
        List<ConfigurationResourceProvider> facesConfigProviders =
                new ArrayList<ConfigurationResourceProvider>(3);
        facesConfigProviders.add(new MojarraFacesConfigResourceProvider());
        facesConfigProviders.add(new MetaInfFacesConfigResourceProvider());
        facesConfigProviders.add(new WebAppFlowConfigResourceProvider());
        facesConfigProviders.add(new WebFacesConfigResourceProvider());
        FACES_CONFIG_RESOURCE_PROVIDERS = Collections.unmodifiableList(facesConfigProviders);

        // initialize the resource providers for facelet-taglib documents
        List<ConfigurationResourceProvider> faceletTaglibProviders =
                new ArrayList<ConfigurationResourceProvider>(3);
        faceletTaglibProviders.add(new MetaInfFaceletTaglibraryConfigProvider());
        faceletTaglibProviders.add(new WebFaceletTaglibResourceProvider());
        FACELET_TAGLIBRARY_RESOURCE_PROVIDERS = Collections.unmodifiableList(faceletTaglibProviders);

        // initialize the config processors for faces-config documents
        ConfigProcessor[] configProcessors = {
                new FactoryConfigProcessor(),
                new LifecycleConfigProcessor(),
                new ApplicationConfigProcessor(),
                new ComponentConfigProcessor(),
                new ConverterConfigProcessor(),
                new ValidatorConfigProcessor(),
                new ManagedBeanConfigProcessor(),
                new RenderKitConfigProcessor(),
                new NavigationConfigProcessor(),
                new BehaviorConfigProcessor(),
                new FacesConfigExtensionProcessor(),
                new ProtectedViewsConfigProcessor(),
                new FacesFlowDefinitionConfigProcessor(),
                new ResourceLibraryContractsConfigProcessor()
        };
        for (int i = 0; i < configProcessors.length; i++) {
            ConfigProcessor p = configProcessors[i];
            if ((i + 1) < configProcessors.length) {
                p.setNext(configProcessors[i + 1]);
            }
        }
        FACES_CONFIG_PROCESSOR_CHAIN = configProcessors[0];

        // initialize the config processor for facelet-taglib documents
        FACELET_TAGLIB_CONFIG_PROCESSOR_CHAIN = new FaceletTaglibConfigProcessor();

    }

    // ---------------------------------------------------------- Public Methods


    /**
     * @return a <code>ConfigManager</code> instance
     */
    public static ConfigManager getInstance(ServletContext sc) {
        return (ConfigManager) sc.getAttribute(CONFIG_MANAGER_INSTANCE_KEY);
    }

    public static ConfigManager createInstance(ServletContext sc) {
        ConfigManager result = new ConfigManager();
        sc.setAttribute(CONFIG_MANAGER_INSTANCE_KEY, result);
        return result;
    }

    public static void removeInstance(ServletContext sc) {
        sc.removeAttribute(CONFIG_MANAGER_INSTANCE_KEY);
    }

    private void initializeConfigProcessers(ServletContext sc) {
        ConfigProcessor p = FACES_CONFIG_PROCESSOR_CHAIN;
        do {
            p.initializeClassMetadataMap(sc);

        } while (null != (p = p.getNext()));

    }


    /**
     * <p>
     * This method bootstraps JSF based on the parsed configuration resources.
     * </p>
     *
     * @param sc the <code>ServletContext</code> for the application that
     *           requires initialization
     */
    public void initialize(ServletContext sc) {

        if (!hasBeenInitialized(sc)) {
            initializedContexts.add(sc);
            initializeConfigProcessers(sc);
            ExecutorService executor = null;
            try {
                WebConfiguration webConfig = WebConfiguration.getInstance(sc);
                boolean validating = webConfig.isOptionEnabled(ValidateFacesConfigFiles);
                if (useThreads(sc)) {
                    executor = createExecutorService();
                }

                DocumentInfo[] facesDocuments =
                        getConfigDocuments(sc,
                                getFacesConfigResourceProviders(),
                                executor,
                                validating);

                FacesConfigInfo lastFacesConfigInfo =
                        new FacesConfigInfo(facesDocuments[facesDocuments.length - 1]);

                facesDocuments = sortDocuments(facesDocuments, lastFacesConfigInfo);
                InitFacesContext context = (InitFacesContext) FacesContext.getCurrentInstance();

                InjectionProvider containerConnector =
                        InjectionProviderFactory.createInstance(context.getExternalContext());
                context.getAttributes().put(INJECTION_PROVIDER_KEY, containerConnector);

                boolean isFaceletsDisabled = false;

                // Don't perform the check unless lastFacesConfigInfo is indeed
                // *the* WEB-INF/faces-config.xml
                if (lastFacesConfigInfo.isWebInfFacesConfig()) {
                    isFaceletsDisabled =
                            isFaceletsDisabled(webConfig, lastFacesConfigInfo);
                }
                if (!lastFacesConfigInfo.isWebInfFacesConfig() || !lastFacesConfigInfo.isMetadataComplete()) {
                    // execute the Task responsible for finding annotation classes
                    ProvideMetadataToAnnotationScanTask taskMetadata = new ProvideMetadataToAnnotationScanTask(facesDocuments, containerConnector);
                    Future<Map<Class<? extends Annotation>, Set<Class<?>>>> annotationScan;
                    if (executor != null) {
                        annotationScan = executor.submit(new AnnotationScanTask(sc, context, taskMetadata));
                        pushTaskToContext(sc, annotationScan);
                    } else {
                        annotationScan =
                                new FutureTask<Map<Class<? extends Annotation>, Set<Class<?>>>>(new AnnotationScanTask(sc, context, taskMetadata));
                        ((FutureTask) annotationScan).run();
                    }
                    pushTaskToContext(sc, annotationScan);
                }

                //see if the app is running in a HA enabled env
                if (containerConnector instanceof HighAvailabilityEnabler) {
                    ((HighAvailabilityEnabler) containerConnector).enableHighAvailability(sc);
                }

                ServiceLoader<ApplicationConfigurationPopulator> populators =
                        ServiceLoader.load(ApplicationConfigurationPopulator.class);
                Document newDoc;
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setNamespaceAware(true);
                DocumentBuilder builder = dbf.newDocumentBuilder();
                DOMImplementation domImpl = builder.getDOMImplementation();
                List<DocumentInfo> programmaticDocuments = new ArrayList<DocumentInfo>();
                DocumentInfo newDocInfo;
                for (ApplicationConfigurationPopulator pop : populators) {
                    newDoc = domImpl.createDocument(RIConstants.JAVAEE_XMLNS, "faces-config", null);
                    Attr versionAttribute = newDoc.createAttribute("version");
                    versionAttribute.setValue("2.2");
                    newDoc.getDocumentElement().getAttributes().setNamedItem(versionAttribute);

                    try {
                        pop.populateApplicationConfiguration(newDoc);
                        newDocInfo = new DocumentInfo(newDoc, null);
                        programmaticDocuments.add(newDocInfo);
                    } catch (Throwable e) {
                        if (LOGGER.isLoggable(Level.INFO)) {
                            LOGGER.log(Level.INFO, "{0} thrown when invoking {1}.populateApplicationConfigurationResources: {2}",
                                    new String[]{
                                            e.getClass().getName(),
                                            pop.getClass().getName(),
                                            e.getMessage()
                                    }
                            );
                        }
                    }
                }
                if (!programmaticDocuments.isEmpty()) {
                    DocumentInfo[] newDocumentInfo = new DocumentInfo[facesDocuments.length + programmaticDocuments.size()];
                    System.arraycopy(facesDocuments, 0, newDocumentInfo, 0, facesDocuments.length);
                    int i = facesDocuments.length;
                    for (DocumentInfo cur : programmaticDocuments) {
                        newDocumentInfo[i] = cur;
                    }
                    facesDocuments = newDocumentInfo;
                }

                // process the ordered documents
                FACES_CONFIG_PROCESSOR_CHAIN.process(sc, facesDocuments);
                if (!isFaceletsDisabled) {
                    FACELET_TAGLIB_CONFIG_PROCESSOR_CHAIN.process(
                            sc, getConfigDocuments(sc,
                                    getFaceletConfigResourceProviders(),
                                    executor,
                                    validating));
                }

            } catch (Exception e) {
                // clear out any configured factories
                releaseFactories();
                Throwable t = e;
                if (!(e instanceof ConfigurationException)) {
                    t = new ConfigurationException("CONFIGURATION FAILED! " + t.getMessage(), t);
                }
                throw (ConfigurationException) t;
            } finally {
                if (executor != null) {
                    executor.shutdown();
                }
                sc.removeAttribute(ANNOTATIONS_SCAN_TASK_KEY);
            }
        }

        DbfFactory.removeSchemaMap(sc);
    }


    /**
     * <p>
     * This method will remove any information about the application.
     * </p>
     *
     * @param sc the <code>ServletContext</code> for the application that
     *           needs to be removed
     */
    public void destroy(ServletContext sc) {

        // Do not call releaseFactories() here because that is done by the
        // caller.

        FACES_CONFIG_PROCESSOR_CHAIN.destroy(sc);

        initializedContexts.remove(sc);

    }


    /**
     * @param sc the <code>ServletContext</code> for the application in question
     * @return <code>true</code> if this application has already been initialized,
     * otherwise returns </code>fase</code>
     */
    public boolean hasBeenInitialized(ServletContext sc) {

        return (initializedContexts.contains(sc));

    }


    /**
     * @return the results of the annotation scan task
     */
    public static Map<Class<? extends Annotation>, Set<Class<?>>> getAnnotatedClasses(FacesContext ctx) {

        Map<String, Object> appMap =
                ctx.getExternalContext().getApplicationMap();
        //noinspection unchecked
        Future<Map<Class<? extends Annotation>, Set<Class<?>>>> scanTask =
                (Future<Map<Class<? extends Annotation>, Set<Class<?>>>>) appMap.get(ANNOTATIONS_SCAN_TASK_KEY);
        try {
            return ((scanTask != null)
                    ? scanTask.get()
                    : Collections.<Class<? extends Annotation>, Set<Class<?>>>emptyMap());
        } catch (Exception e) {
            throw new FacesException(e);
        }

    }


    // --------------------------------------------------------- Private Methods


    private static boolean useThreads(ServletContext ctx) {

        WebConfiguration config = WebConfiguration.getInstance(ctx);
        return config.isOptionEnabled(EnableThreading);

    }


    private List<ConfigurationResourceProvider> getFacesConfigResourceProviders() {

        return getConfigurationResourceProviders(FACES_CONFIG_RESOURCE_PROVIDERS,
                FacesConfig);

    }


    private List<ConfigurationResourceProvider> getFaceletConfigResourceProviders() {

        return getConfigurationResourceProviders(FACELET_TAGLIBRARY_RESOURCE_PROVIDERS,
                FaceletConfig);

    }


    private List<ConfigurationResourceProvider> getConfigurationResourceProviders(List<ConfigurationResourceProvider> defaultProviders,
                                                                                  ConfigurationResourceProviderFactory.ProviderType providerType) {

        ConfigurationResourceProvider[] custom =
                ConfigurationResourceProviderFactory.createProviders(providerType);
        if (custom.length == 0) {
            return defaultProviders;
        } else {
            List<ConfigurationResourceProvider> list = new ArrayList<ConfigurationResourceProvider>();
            list.addAll(defaultProviders);
            // insert the custom providers after the META-INF providers and
            // before those that scan /WEB-INF
            list.addAll((defaultProviders.size() - 1), Arrays.asList(custom));
            return Collections.unmodifiableList(list);
        }

    }


    /**
     * <p>
     * Sort the <code>faces-config</code> documents found on the classpath
     * and those specified by the <code>javax.faces.CONFIG_FILES</code> context
     * init parameter.
     * </p>
     *
     * @param facesDocuments    an array of <em>all</em> <code>faces-config</code>
     *                          documents
     * @param webInfFacesConfig FacesConfigInfo representing the WEB-INF/faces-config.xml
     *                          for this app
     * @return the sorted documents
     */
    private DocumentInfo[] sortDocuments(DocumentInfo[] facesDocuments,
                                         FacesConfigInfo webInfFacesConfig) {


        int len = (webInfFacesConfig.isWebInfFacesConfig()
                ? facesDocuments.length - 1
                : facesDocuments.length);

        List<String> absoluteOrdering = webInfFacesConfig.getAbsoluteOrdering();

        if (len > 1) {
            List<DocumentOrderingWrapper> list =
                    new ArrayList<DocumentOrderingWrapper>();
            for (int i = 1; i < len; i++) {
                list.add(new DocumentOrderingWrapper(facesDocuments[i]));
            }
            DocumentOrderingWrapper[] ordering =
                    list.toArray(new DocumentOrderingWrapper[list.size()]);
            if (absoluteOrdering == null) {
                DocumentOrderingWrapper.sort(ordering);
                // sorting complete, now update the appropriate locations within
                // the original array with the sorted documentation.
                for (int i = 1; i < len; i++) {
                    facesDocuments[i] = ordering[i - 1].getDocument();
                }
                return facesDocuments;
            } else {
                DocumentOrderingWrapper[] result =
                        DocumentOrderingWrapper.sort(ordering, absoluteOrdering);
                DocumentInfo[] ret = new DocumentInfo[((webInfFacesConfig.isWebInfFacesConfig()) ? (result.length + 2) : (result.length + 1))];
                for (int i = 1; i < len; i++) {
                    ret[i] = result[i - 1].getDocument();
                }
                // add the impl specific config file
                ret[0] = facesDocuments[0];
                // add the WEB-INF if necessary
                if (webInfFacesConfig.isWebInfFacesConfig()) {
                    ret[ret.length - 1] = facesDocuments[facesDocuments.length - 1];
                }
                return ret;
            }
        }

        return facesDocuments;
    }


    /**
     * <p>
     * Utility method to check if JSF 2.0 Facelets should be disabled.
     * If it's not explicitly disabled by the context init parameter, then
     * check the version of the WEB-INF/faces-config.xml document.  If the version
     * is less than 2.0, then override the default value for the context init
     * parameter so that other parts of the system that use that config option
     * will know it has been disabled.
     * </p>
     * <p>
     * <p>
     * NOTE:  Since this method overrides a configuration value, it should
     * be called before *any* document parsing is performed the configuration
     * value may be queried by the <code>ConfigParser</code>s.
     * </p>
     *
     * @param webconfig       configuration for this application
     * @param facesConfigInfo object representing WEB-INF/faces-config.xml
     * @return <code>true</code> if Facelets should be disabled
     */
    private boolean isFaceletsDisabled(WebConfiguration webconfig,
                                       FacesConfigInfo facesConfigInfo) {

        boolean isFaceletsDisabled = webconfig.isOptionEnabled(DisableFaceletJSFViewHandler);
        if (!isFaceletsDisabled) {
            // if not explicitly disabled, make a sanity check against
            // /WEB-INF/faces-config.xml
            isFaceletsDisabled = !facesConfigInfo.isVersionGreaterOrEqual(2.0);
            webconfig.overrideContextInitParameter(DisableFaceletJSFViewHandler, isFaceletsDisabled);
        }
        return isFaceletsDisabled;

    }


    /**
     * Push the provided <code>Future</code> to the specified <code>ServletContext</code>.
     */
    private void pushTaskToContext(ServletContext sc,
                                   Future<Map<Class<? extends Annotation>, Set<Class<?>>>> scanTask) {

        sc.setAttribute(ANNOTATIONS_SCAN_TASK_KEY, scanTask);

    }


    /**
     * Publishes a {@link javax.faces.event.PostConstructApplicationEvent} event for the current
     * {@link Application} instance.
     */
    void publishPostConfigEvent() {

        FacesContext ctx = FacesContext.getCurrentInstance();
        Application app = ctx.getApplication();
        if (null == ((InitFacesContext) ctx).getELContext()) {
            ELContext elContext = new ELContextImpl(app.getELResolver());
            elContext.putContext(FacesContext.class, ctx);
            ExpressionFactory exFactory = ELUtils.getDefaultExpressionFactory(ctx);
            if (null != exFactory) {
                elContext.putContext(ExpressionFactory.class, exFactory);
            }
            UIViewRoot root = ctx.getViewRoot();
            if (null != root) {
                elContext.setLocale(root.getLocale());
            }
            ELContextListener[] listeners = app.getELContextListeners();
            if (listeners.length > 0) {
                ELContextEvent event = new ELContextEvent(elContext);
                for (ELContextListener listener : listeners) {
                    listener.contextCreated(event);
                }
            }
            ((InitFacesContext) ctx).setELContext(elContext);
        }

        app.publishEvent(ctx,
                PostConstructApplicationEvent.class,
                Application.class,
                app);

    }


    /**
     * <p>
     * Obtains an array of <code>Document</code>s to be processed
     * by {@link ConfigManager#FACES_CONFIG_PROCESSOR_CHAIN}.
     * </p>
     *
     * @param sc         the <code>ServletContext</code> for the application to be
     *                   processed
     * @param providers  <code>List</code> of <code>ConfigurationResourceProvider</code>
     *                   instances that provide the URL of the documents to parse.
     * @param executor   the <code>ExecutorService</code> used to dispatch parse
     *                   request to
     * @param validating flag indicating whether or not the documents
     *                   should be validated
     * @return an array of <code>DocumentInfo</code>s
     */
    private static DocumentInfo[] getConfigDocuments(ServletContext sc,
                                                     List<ConfigurationResourceProvider> providers,
                                                     ExecutorService executor,
                                                     boolean validating) {

        List<FutureTask<Collection<URI>>> urlTasks =
                new ArrayList<FutureTask<Collection<URI>>>(providers.size());
        for (ConfigurationResourceProvider p : providers) {
            FutureTask<Collection<URI>> t =
                    new FutureTask<Collection<URI>>(new URITask(p, sc));
            urlTasks.add(t);
            if (executor != null) {
                executor.execute(t);
            } else {
                t.run();
            }
        }

        List<FutureTask<DocumentInfo>> docTasks =
                new ArrayList<FutureTask<DocumentInfo>>(providers.size() << 1);

        for (FutureTask<Collection<URI>> t : urlTasks) {
            try {
                Collection<URI> l = t.get();
                for (URI u : l) {
                    FutureTask<DocumentInfo> d =
                            new FutureTask<DocumentInfo>(new ParseTask(sc, validating, u));
                    docTasks.add(d);
                    if (executor != null) {
                        executor.execute(d);
                    } else {
                        d.run();
                    }
                }
            } catch (InterruptedException ignored) {
            } catch (Exception e) {
                throw new ConfigurationException(e);
            }
        }

        List<DocumentInfo> docs = new ArrayList<DocumentInfo>(docTasks.size());
        for (FutureTask<DocumentInfo> t : docTasks) {
            try {
                docs.add(t.get());
            } catch (ExecutionException e) {
                throw new ConfigurationException(e);
            } catch (InterruptedException ignored) {
            }
        }

        return docs.toArray(new DocumentInfo[docs.size()]);

    }


    /**
     * Create a new <code>ExecutorService</code> with
     * {@link #NUMBER_OF_TASK_THREADS} threads.
     */
    private static ExecutorService createExecutorService() {

        int tc = Runtime.getRuntime().availableProcessors();
        if (tc > NUMBER_OF_TASK_THREADS) {
            tc = NUMBER_OF_TASK_THREADS;
        }
        return Executors.newFixedThreadPool(tc);

    }


//    /**
//     * @param throwable Throwable
//     * @return the root cause of this error
//     */
//    private Throwable unwind(Throwable throwable) {
//
//          Throwable t = null;
//          if (throwable != null) {
//              t =  unwind(throwable.getCause());
//              if (t == null) {
//                  t = throwable;
//              }
//          }
//          return t;
//
//    }


    /**
     * Calls through to {@link javax.faces.FactoryFinder#releaseFactories()}
     * ignoring any exceptions.
     */
    private void releaseFactories() {
        try {
            FactoryFinder.releaseFactories();
        } catch (FacesException ignored) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE,
                        "Exception thrown from FactoryFinder.releaseFactories()",
                        ignored);
            }
        }
    }


    // ----------------------------------------------------------- Inner Classes

    private static final class ProvideMetadataToAnnotationScanTask {
        DocumentInfo[] documentInfos;
        InjectionProvider containerConnector;
        Set<URI> uris = null;
        Set<String> jarNames = null;

        private ProvideMetadataToAnnotationScanTask(DocumentInfo[] documentInfos,
                                                    InjectionProvider containerConnector) {
            this.documentInfos = documentInfos;
            this.containerConnector = containerConnector;
        }

        private void initializeIvars(Set<Class<?>> annotatedSet) {
            if (null != uris || null != jarNames) {
                assert (null != uris && null != jarNames);
                return;
            }
            uris = new HashSet<URI>(documentInfos.length);
            jarNames = new HashSet<String>(documentInfos.length);
            for (DocumentInfo docInfo : documentInfos) {
                URI sourceURI = docInfo.getSourceURI();
                Matcher m = JAR_PATTERN.matcher(sourceURI.toString());
                if (m.matches()) {
                    String jarName = m.group(2);
                    if (!jarNames.contains(jarName)) {
                        FacesConfigInfo configInfo = new FacesConfigInfo(docInfo);
                        if (!configInfo.isMetadataComplete()) {
                            uris.add(sourceURI);
                            jarNames.add(jarName);
                        } else {
                            /*
                             * Because the container annotation scanning does not
                             * know anything about faces-config.xml metadata-complete
                             * the annotatedSet of classes will include classes that
                             * are not supposed to be included.
                             *
                             * The code below looks at the CodeSource of the class
                             * and determines whether or not it should be removed
                             * from the annotatedSet because the faces-config.xml that
                             * owns it has metadata-complete="true".
                             */
                            ArrayList<Class<?>> toRemove = new ArrayList<Class<?>>(1);
                            String sourceURIString = sourceURI.toString();
                            if (annotatedSet != null) {
                                for (Class<?> clazz : annotatedSet) {
                                    if (sourceURIString.contains(clazz.getProtectionDomain().getCodeSource().getLocation().toString())) {
                                        toRemove.add(clazz);
                                    }
                                }
                                annotatedSet.removeAll(toRemove);
                            }
                        }
                    }
                }
            }
        }

        private Set<URI> getAnnotationScanURIs(Set<Class<?>> annotatedSet) {
            initializeIvars(annotatedSet);

            return uris;

        }

        private Set<String> getJarNames(Set<Class<?>> annotatedSet) {
            initializeIvars(annotatedSet);

            return jarNames;
        }

        private com.sun.faces.spi.AnnotationScanner getAnnotationScanner() {
            com.sun.faces.spi.AnnotationScanner result = null;
            if (this.containerConnector instanceof com.sun.faces.spi.AnnotationScanner) {
                result = (com.sun.faces.spi.AnnotationScanner) this.containerConnector;
            }
            return result;
        }
    }


    /**
     * Scans the class files within a web application returning a <code>Set</code>
     * of classes that have been annotated with a standard Faces annotation.
     */
    private static class AnnotationScanTask implements Callable<Map<Class<? extends Annotation>, Set<Class<?>>>> {

        private ServletContext sc;
        private InitFacesContext facesContext;
        private AnnotationProvider provider;
        private ProvideMetadataToAnnotationScanTask metadataGetter;
        private Set<Class<?>> annotatedSet;

        // -------------------------------------------------------- Constructors


        public AnnotationScanTask(ServletContext sc, InitFacesContext facesContext, ProvideMetadataToAnnotationScanTask metadataGetter) {
            this.facesContext = facesContext;
            this.provider = AnnotationProviderFactory.createAnnotationProvider(sc);
            this.metadataGetter = metadataGetter;
            this.annotatedSet = (Set<Class<?>>) sc.getAttribute(ANNOTATED_CLASSES);
        }


        // ----------------------------------------------- Methods from Callable


        public Map<Class<? extends Annotation>, Set<Class<?>>> call() throws Exception {

            com.sun.faces.util.Timer t = com.sun.faces.util.Timer.getInstance();
            if (t != null) {
                t.startTiming();
            }

            // We are executing on a different thread.
            Method callSetCurrentInstance = facesContext.getClass().getDeclaredMethod("callSetCurrentInstance");
            callSetCurrentInstance.setAccessible(true);
            callSetCurrentInstance.invoke(facesContext);

            Set<URI> scanUris = null;
            com.sun.faces.spi.AnnotationScanner annotationScanner =
                    metadataGetter.getAnnotationScanner();

            // This is where we discover what kind of InjectionProvider
            // we have.
            if (provider instanceof DelegatingAnnotationProvider &&
                    null != annotationScanner) {
                // This InjectionProvider is capable of annotation scanning *and*
                // injection.
                ((DelegatingAnnotationProvider) provider).setAnnotationScanner(annotationScanner,
                        metadataGetter.getJarNames(annotatedSet));
                scanUris = Collections.emptySet();
            } else {
                // This InjectionProvider is capable of annotation scanning only
                scanUris = metadataGetter.getAnnotationScanURIs(annotatedSet);
            }
            //AnnotationScanner scanner = new AnnotationScanner(sc);
            Map<Class<? extends Annotation>, Set<Class<?>>> annotatedClasses =
                    provider.getAnnotatedClasses(scanUris);

            if (t != null) {
                t.stopTiming();
                t.logResult("Configuration annotation scan complete.");
            }

            return annotatedClasses;

        }


    } // END AnnotationScanTask


    /**
     * <p>
     * This <code>Callable</code> will be used by {@link ConfigManager#getConfigDocuments(javax.servlet.ServletContext, java.util.List, java.util.concurrent.ExecutorService, boolean)}.
     * It represents a single configuration resource to be parsed into a DOM.
     * </p>
     */
    private static class ParseTask implements Callable<DocumentInfo> {
        private static final String JAVAEE_SCHEMA_LEGACY_DEFAULT_NS =
                "http://java.sun.com/xml/ns/javaee";
        private static final String JAVAEE_SCHEMA_DEFAULT_NS =
                "http://xmlns.jcp.org/xml/ns/javaee";
        private static final String EMPTY_FACES_CONFIG =
                "com/sun/faces/empty-faces-config.xml";
        private static final String FACES_CONFIG_TAGNAME = "faces-config";
        private static final String FACELET_TAGLIB_TAGNAME = "facelet-taglib";
        private ServletContext servletContext;
        private URI documentURI;
        private DocumentBuilderFactory factory;
        private boolean validating;

        // -------------------------------------------------------- Constructors


        /**
         * <p>
         * Constructs a new ParseTask instance
         * </p>
         *
         * @param servletContext the servlet context.
         * @param validating     whether or not we're validating
         * @param documentURI    a URL to the configuration resource to be parsed
         * @throws Exception general error
         */
        public ParseTask(ServletContext servletContext, boolean validating, URI documentURI)
                throws Exception {
            this.servletContext = servletContext;
            this.documentURI = documentURI;
            this.validating = validating;

        }


        // ----------------------------------------------- Methods from Callable


        /**
         * @return the result of the parse operation (a DOM)
         * @throws Exception if an error occurs during the parsing process
         */
        public DocumentInfo call() throws Exception {

            try {
                com.sun.faces.util.Timer timer = com.sun.faces.util.Timer.getInstance();
                if (timer != null) {
                    timer.startTiming();
                }

                Document d = getDocument();

                if (timer != null) {
                    timer.stopTiming();
                    timer.logResult("Parse " + documentURI.toURL().toExternalForm());
                }

                return new DocumentInfo(d, documentURI);
            } catch (Exception e) {
                throw new ConfigurationException(MessageFormat.format(
                        "Unable to parse document ''{0}'': {1}",
                        documentURI.toURL().toExternalForm(),
                        e.getMessage()), e);
            }
        }


        // ----------------------------------------------------- Private Methods


        /**
         * @return <code>Document</code> based on <code>documentURI</code>.
         * @throws Exception if an error occurs during the process of building a
         *                   <code>Document</code>
         */
        private Document getDocument() throws Exception {

            Document returnDoc;
            DocumentBuilder db = getNonValidatingBuilder();
            URL documentURL = documentURI.toURL();
            InputSource is = new InputSource(getInputStream(documentURL));
            is.setSystemId(documentURI.toURL().toExternalForm());
            Document doc = null;

            try {
                doc = db.parse(is);
            } catch (SAXParseException spe) {
                // [mojarra-1693]
                // Test if this is a zero length or whitespace only faces-config.xml file.
                // If so, just make an empty Document
                InputStream stream = is.getByteStream();
                stream.close();

                is = new InputSource(getInputStream(documentURL));
                stream = is.getByteStream();
                if (streamIsZeroLengthOrEmpty(stream) &&
                        documentURL.toExternalForm().endsWith("faces-config.xml")) {
                    ClassLoader loader = this.getClass().getClassLoader();
                    is = new InputSource(getInputStream(loader.getResource(EMPTY_FACES_CONFIG)));
                    doc = db.parse(is);
                }

            }

            String documentNS = null;
            if (null == doc) {
                if (FacesFlowDefinitionConfigProcessor.uriIsFlowDefinition(documentURI)) {
                    documentNS = RIConstants.JAVAEE_XMLNS;
                    doc = FacesFlowDefinitionConfigProcessor.synthesizeEmptyFlowDefinition(documentURI);
                }
            } else {
                Element documentElement = doc.getDocumentElement();
                documentNS = documentElement.getNamespaceURI();
                String rootElementTagName = documentElement.getTagName();

                boolean isNonFacesConfigDocument = !FACES_CONFIG_TAGNAME.equals(rootElementTagName) &&
                        !FACELET_TAGLIB_TAGNAME.equals(rootElementTagName);

                if (isNonFacesConfigDocument) {
                    ClassLoader loader = this.getClass().getClassLoader();
                    is = new InputSource(getInputStream(loader.getResource(EMPTY_FACES_CONFIG)));
                    doc = db.parse(is);
                    if (LOGGER.isLoggable(Level.WARNING)) {
                        LOGGER.log(Level.WARNING, MessageFormat.format("Config document {0} with namespace URI {1} is not a faces-config or facelet-taglib file.  Ignoring.",
                                documentURI.toURL().toExternalForm(),
                                documentNS));
                    }
                    return doc;
                }
            }

            if (validating && documentNS != null) {
                DOMSource domSource
                        = new DOMSource(doc, documentURL.toExternalForm());

                /*
                 * If the Document in question is 1.2 (i.e. it has a namespace matching
                 * JAVAEE_SCHEMA_DEFAULT_NS, then perform validation using the cached schema
                 * and return.  Otherwise we assume a 1.0 or 1.1 faces-config in which case
                 * we need to transform it to reference a special 1.1 schema before validating.
                 */
                Node documentElement = ((Document) domSource.getNode()).getDocumentElement();
                if (JAVAEE_SCHEMA_DEFAULT_NS.equals(documentNS)) {
                    Attr version = (Attr)
                            documentElement.getAttributes().getNamedItem("version");
                    Schema schema;
                    if (version != null) {
                        String versionStr = version.getValue();
                        if ("2.2".equals(versionStr)) {
                            if ("facelet-taglib".equals(documentElement.getLocalName())) {
                                schema = DbfFactory.getSchema(servletContext, DbfFactory.FacesSchema.FACELET_TAGLIB_22);
                            } else {
                                schema = DbfFactory.getSchema(servletContext, DbfFactory.FacesSchema.FACES_22);
                            }
                        } else {
                            throw new ConfigurationException("Unknown Schema version: " + versionStr);
                        }
                        DocumentBuilder builder = getBuilderForSchema(schema);
                        if (builder.isValidating()) {
                            builder.getSchema().newValidator().validate(domSource);
                            returnDoc = ((Document) domSource.getNode());
                        } else {
                            returnDoc = ((Document) domSource.getNode());
                        }
                    } else {
                        // this shouldn't happen, but...
                        throw new ConfigurationException("No document version available.");
                    }
                } else if (JAVAEE_SCHEMA_LEGACY_DEFAULT_NS.equals(documentNS)) {
                    Attr version = (Attr)
                            documentElement.getAttributes().getNamedItem("version");
                    Schema schema;
                    if (version != null) {
                        String versionStr = version.getValue();
                        if ("2.0".equals(versionStr)) {
                            if ("facelet-taglib".equals(documentElement.getLocalName())) {
                                schema = DbfFactory.getSchema(servletContext, DbfFactory.FacesSchema.FACELET_TAGLIB_20);
                            } else {
                                schema = DbfFactory.getSchema(servletContext, DbfFactory.FacesSchema.FACES_20);
                            }
                        } else if ("2.1".equals(versionStr)) {
                            if ("facelet-taglib".equals(documentElement.getLocalName())) {
                                schema = DbfFactory.getSchema(servletContext, DbfFactory.FacesSchema.FACELET_TAGLIB_20);
                            } else {
                                schema = DbfFactory.getSchema(servletContext, DbfFactory.FacesSchema.FACES_21);
                            }
                        } else if ("1.2".equals(versionStr)) {
                            schema = DbfFactory.getSchema(servletContext, DbfFactory.FacesSchema.FACES_12);
                        } else {
                            throw new ConfigurationException("Unknown Schema version: " + versionStr);
                        }
                        DocumentBuilder builder = getBuilderForSchema(schema);
                        if (builder.isValidating()) {
                            builder.getSchema().newValidator().validate(domSource);
                            returnDoc = ((Document) domSource.getNode());
                        } else {
                            returnDoc = ((Document) domSource.getNode());
                        }
                    } else {
                        // this shouldn't happen, but...
                        throw new ConfigurationException("No document version available.");
                    }
                } else {
                    DOMResult domResult = new DOMResult();
                    Transformer transformer = getTransformer(documentNS);
                    transformer.transform(domSource, domResult);
                    // copy the source document URI to the transformed result
                    // so that processes that need to build URLs relative to the
                    // document will work as expected.
                    ((Document) domResult.getNode())
                            .setDocumentURI(((Document) domSource
                                    .getNode()).getDocumentURI());
                    Schema schemaToApply;
                    if (FACES_CONFIG_1_X_DEFAULT_NS.equals(documentNS)) {
                        schemaToApply = DbfFactory.getSchema(servletContext, DbfFactory.FacesSchema.FACES_11);
                    } else if (FACELETS_1_0_DEFAULT_NS.equals(documentNS)) {
                        schemaToApply = DbfFactory.getSchema(servletContext, DbfFactory.FacesSchema.FACELET_TAGLIB_20);
                    } else {
                        throw new IllegalStateException();
                    }
                    DocumentBuilder builder = getBuilderForSchema(schemaToApply);
                    if (builder.isValidating()) {
                        builder.getSchema().newValidator().validate(new DOMSource(domResult.getNode()));
                        returnDoc = (Document) domResult.getNode();
                    } else {
                        returnDoc = (Document) domResult.getNode();
                    }
                }
            } else {
                returnDoc = doc;
            }

            // mark this document as the parsed representation of the
            // WEB-INF/faces-config.xml.  This is used later in the configuration
            // processing.
            if (documentURL.toExternalForm().contains("/WEB-INF/faces-config.xml")) {
                Attr webInf = returnDoc.createAttribute(WEB_INF_MARKER);
                webInf.setValue("true");
                returnDoc.getDocumentElement().getAttributes().setNamedItem(webInf);
            }
            return returnDoc;

        }

        private boolean streamIsZeroLengthOrEmpty(InputStream is) throws IOException {
            boolean isZeroLengthOrEmpty = (0 == is.available());
            final int size = 1024;
            byte[] b = new byte[size];
            String s;
            while (!isZeroLengthOrEmpty && -1 != is.read(b, 0, size)) {
                s = (new String(b, RIConstants.CHAR_ENCODING)).trim();
                isZeroLengthOrEmpty = 0 == s.length();
                b[0] = 0;
                for (int i = 1; i < size; i += i) {
                    System.arraycopy(b, 0, b, i, ((size - i) < i) ? (size - i) : i);
                }
            }

            return isZeroLengthOrEmpty;
        }


        /**
         * Obtain a <code>Transformer</code> using the style sheet
         * referenced by the <code>XSL</code> constant.
         *
         * @return a new Tranformer instance
         * @throws Exception if a Tranformer instance could not be created
         */
        private static Transformer getTransformer(String documentNS)
                throws Exception {

            TransformerFactory factory = Util.createTransformerFactory();

            String xslToApply;
            if (FACES_CONFIG_1_X_DEFAULT_NS.equals(documentNS)) {
                xslToApply = FACES_TO_1_1_PRIVATE_XSL;
            } else if (FACELETS_1_0_DEFAULT_NS.equals(documentNS)) {
                xslToApply = FACELETS_TO_2_0_XSL;
            } else {
                throw new IllegalStateException();
            }
            return factory
                    .newTransformer(new StreamSource(getInputStream(ConfigManager
                            .class.getResource(xslToApply))));

        }


        /**
         * @param url source <code>URL</code>
         * @return an <code>InputStream</code> to the resource referred to by
         * <code>url</code>
         * @throws IOException if an error occurs
         */
        private static InputStream getInputStream(URL url) throws IOException {

            URLConnection conn = url.openConnection();
            conn.setUseCaches(false);
            return new BufferedInputStream(conn.getInputStream());

        }


        private DocumentBuilder getNonValidatingBuilder() throws Exception {

            DocumentBuilderFactory tFactory = DbfFactory.getFactory();
            tFactory.setValidating(false);
            DocumentBuilder tBuilder = tFactory.newDocumentBuilder();
            tBuilder.setEntityResolver(DbfFactory.FACES_ENTITY_RESOLVER);
            tBuilder.setErrorHandler(DbfFactory.FACES_ERROR_HANDLER);
            return tBuilder;

        }

        private DocumentBuilder getBuilderForSchema(Schema schema)
                throws Exception {
            this.factory = DbfFactory.getFactory();

            try {
                factory.setSchema(schema);
            } catch (UnsupportedOperationException upe) {
                return getNonValidatingBuilder();
            }
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setEntityResolver(DbfFactory.FACES_ENTITY_RESOLVER);
            builder.setErrorHandler(DbfFactory.FACES_ERROR_HANDLER);
            return builder;
        }

    } // END ParseTask


    /**
     * <p>
     * This <code>Callable</code> will be used by {@link ConfigManager#getConfigDocuments(javax.servlet.ServletContext, java.util.List, java.util.concurrent.ExecutorService, boolean)}.
     * It represents one or more URLs to configuration resources that require
     * processing.
     * </p>
     */
    private static class URITask implements Callable<Collection<URI>> {

        private ConfigurationResourceProvider provider;
        private ServletContext sc;


        // -------------------------------------------------------- Constructors


        /**
         * Constructs a new <code>URITask</code> instance.
         *
         * @param provider the <code>ConfigurationResourceProvider</code> from
         *                 which zero or more <code>URL</code>s will be returned
         * @param sc       the <code>ServletContext</code> of the current application
         */
        public URITask(ConfigurationResourceProvider provider,
                       ServletContext sc) {
            this.provider = provider;
            this.sc = sc;
        }


        // ----------------------------------------------- Methods from Callable


        /**
         * @return zero or more <code>URL</code> instances
         * @throws Exception if an Exception is thrown by the underlying
         *                   <code>ConfigurationResourceProvider</code>
         */
        public Collection<URI> call() throws Exception {
            Collection untypedCollection = provider.getResources(sc);
            Iterator untypedCollectionIterator = untypedCollection.iterator();
            Collection<URI> result = Collections.emptyList();
            if (untypedCollectionIterator.hasNext()) {
                Object cur = untypedCollectionIterator.next();
                // account for older versions of the provider that return Collection<URL>.
                if (cur instanceof URL) {
                    result = new ArrayList<URI>(untypedCollection.size());
                    result.add(new URI(((URL) cur).toExternalForm()));
                    while (untypedCollectionIterator.hasNext()) {
                        cur = untypedCollectionIterator.next();
                        result.add(new URI(((URL) cur).toExternalForm()));
                    }
                } else {
                    result = (Collection<URI>) untypedCollection;
                }
            }

            return result;
        }

    } // END URITask
}