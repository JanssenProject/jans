package org.xdi.oxauth.customization;

import com.sun.faces.config.DbfFactory;
import com.sun.faces.config.DocumentInfo;
import com.sun.faces.config.processor.NavigationConfigProcessor;
import com.sun.faces.util.FacesLogger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.w3c.dom.ls.LSResourceResolver;

import javax.faces.application.Application;
import javax.faces.application.ConfigurableNavigationHandler;
import javax.faces.application.NavigationCase;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;

/**
 * Created by eugeniuparvan on 4/26/17.
 */
public class ConfigureListener extends com.sun.faces.config.ConfigureListener {

    /**
     * Location of the Faces 2.2 Schema
     */
    private static final String FACES_2_2_XSD = "/com/sun/faces/web-facesconfig_2_2.xsd";
    private static final String FACES_CONFIG_PATTERN = ".*\\.faces-config.xml$";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        super.contextInitialized(sce);

        if (!Utils.isCustomPagesDirExists())
            return;
        try {
            addNavigationRules(Utils.getCustomPagesPath(), sce.getServletContext());
            reverseNavigationRules();
        } catch (Exception e) {
            FacesLogger.CONFIG.getLogger().log(Level.SEVERE, "Can't add customized navigation rules");
        }
    }

    /**
     * Recursively finds all *.faces-config.xml files located in custom pages directory, and adds navigation rules to
     * navigation handler
     *
     * @param path to custom pages directory
     * @param servletContext
     * @throws Exception
     */
    private void addNavigationRules(String path, ServletContext servletContext) throws Exception {
        File file = new File(path);
        RegexFileFilter regexFileFilter = new RegexFileFilter(FACES_CONFIG_PATTERN);
        Collection<File> facesConfigFiles = FileUtils.listFiles(file, regexFileFilter, DirectoryFileFilter.DIRECTORY);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();

        List<DocumentInfo> documentInfoList = new ArrayList<DocumentInfo>();
        for (File files : facesConfigFiles) {
            String faceConfig = files.getAbsolutePath();
            Optional<DocumentInfo> documentInfo = getDocumentInfo(builder, faceConfig);
            if (documentInfo.isPresent())
                documentInfoList.add(documentInfo.get());
        }
        DocumentInfo[] documentInfos = documentInfoList.toArray(new DocumentInfo[documentInfoList.size()]);

        NavigationConfigProcessor navigationConfigProcessor = new NavigationConfigProcessor();
        navigationConfigProcessor.process(servletContext, documentInfos);
    }

    /**
     * Validates *.faces-config.xml file and creates DocumentInfo class
     * @param docBuilder
     * @param faceConfig
     * @return
     */
    private Optional<DocumentInfo> getDocumentInfo(DocumentBuilder docBuilder, String faceConfig) {
        try {
            InputStream xml = new FileInputStream(faceConfig);
            if (!isValidFacesConfig(xml))
                return Optional.empty();
            return Optional.of(new DocumentInfo(docBuilder.parse(new File(faceConfig)), null));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Validates *.faces-config.xml file
     * @param xml
     * @return
     */
    private boolean isValidFacesConfig(InputStream xml) {
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setResourceResolver((LSResourceResolver) DbfFactory.FACES_ENTITY_RESOLVER);

            InputStream xsd = this.getClass().getResourceAsStream(FACES_2_2_XSD);
            Schema schema = factory.newSchema(new StreamSource(xsd));

            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(xml));
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Reverse navigation rules so that rules from custom pages directory will "overwirte" the old ones.
     */
    private void reverseNavigationRules() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        Application application = facesContext.getApplication();
        ConfigurableNavigationHandler navigationHandler = (ConfigurableNavigationHandler) application.getNavigationHandler();
        Map<String, Set<NavigationCase>> navigationCases = navigationHandler.getNavigationCases();
        for (String key : navigationCases.keySet()) {
            LinkedHashSet<NavigationCase> cases = (LinkedHashSet<NavigationCase>) navigationCases.get(key);
            List<NavigationCase> caseList = new ArrayList<NavigationCase>(cases);
            Collections.reverse(caseList);

            navigationCases.get(key).clear();
            navigationCases.get(key).addAll(caseList);
        }
    }
}