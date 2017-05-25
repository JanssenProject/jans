package org.gluu.jsf2.customization;

import com.sun.faces.util.FacesLogger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.lang.StringUtils;
import org.ocpsoft.rewrite.config.Configuration;
import org.ocpsoft.rewrite.config.ConfigurationBuilder;
import org.ocpsoft.rewrite.param.ParameterizedPatternSyntaxException;
import org.ocpsoft.rewrite.servlet.config.HttpConfigurationProvider;
import org.ocpsoft.rewrite.servlet.config.rule.Join;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.servlet.ServletContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Created by eugeniuparvan on 4/27/17.
 */
public class AccessRewriteConfiguration extends HttpConfigurationProvider {

    private static String REWRITE = "rewrite";

    @Override
    public Configuration getConfiguration(final ServletContext context) {
        ConfigurationBuilder builder = ConfigurationBuilder.begin();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            DocumentBuilder documentBuilder = factory.newDocumentBuilder();
            addRulesForAllXHTML(documentBuilder, context.getRealPath(""), builder);

            if (!Utils.isCustomPagesDirExists())
                return builder;
            addRulesForAllXHTML(documentBuilder, Utils.getCustomPagesPath(), builder);
        } catch (ParserConfigurationException e) {
            FacesLogger.CONFIG.getLogger().log(Level.SEVERE, "Can't parse rewrite rules", e);
        }
        return builder;
    }

    private void addRulesForAllXHTML(DocumentBuilder documentBuilder, String path, ConfigurationBuilder builder) {
        Collection<File> xhtmlFiles = FileUtils.listFiles(new File(path), new RegexFileFilter(".*\\.xhtml$"), DirectoryFileFilter.DIRECTORY);
        Collection<File> navigationFiles = FileUtils.listFiles(new File(path), new RegexFileFilter(".*\\.navigation\\.xml"), DirectoryFileFilter.DIRECTORY);
        Map<String, String> rewriteMap = getRewriteMap(documentBuilder, navigationFiles);
        for (File file : xhtmlFiles) {
            String xhtmlPath = file.getAbsolutePath();
            xhtmlPath = xhtmlPath.replace("\\", "/");
            String xhtmlUri = xhtmlPath.substring(path.length(), xhtmlPath.lastIndexOf(".xhtml"));

            try {
                String rewriteKey = file.getName().split("\\.")[0];
                if (rewriteMap.containsKey(rewriteKey))
                    builder.addRule(Join.path(rewriteMap.get(rewriteKey)).to(xhtmlUri + ".htm"));
                else
                    builder.addRule(Join.path(xhtmlUri).to(xhtmlUri + ".htm"));
            } catch (ParameterizedPatternSyntaxException ex) {
                FacesLogger.CONFIG.getLogger().log(Level.SEVERE, "Failed to add rule for " + xhtmlPath, ex);
            }

        }
    }

    private Map<String, String> getRewriteMap(DocumentBuilder documentBuilder, Collection<File> navigationFiles) {
        Map<String, String> xhtmlNameToRewriteRule = new HashMap<String, String>();
        for (File file : navigationFiles) {
            String path = file.getAbsolutePath();
            String navigationName = path.split("/")[path.split("/").length - 1];

            try {
                String value = getRewritePattern(documentBuilder.parse(file));
                if(StringUtils.isNotEmpty(value))
                    xhtmlNameToRewriteRule.put(navigationName.split("\\.")[0], value);
            } catch (Exception e) {
                FacesLogger.CONFIG.getLogger().log(Level.SEVERE, "Failed to retreive rewrite rules", e);
            }
        }
        return xhtmlNameToRewriteRule;
    }

    private String getRewritePattern(Document document) {
        NodeList nodeList = document.getDocumentElement().getChildNodes();
        if (nodeList == null)
            return null;
        for (int i = 0; i < nodeList.getLength(); ++i) {
            if (REWRITE.equals(nodeList.item(i).getNodeName()) && nodeList.item(i).getAttributes() != null && nodeList.item(i).getAttributes().getLength() > 0) {
                return nodeList.item(i).getAttributes().getNamedItem("pattern").getNodeValue();
            }
        }
        return null;
    }

    @Override
    public int priority() {
        return 10;
    }

}