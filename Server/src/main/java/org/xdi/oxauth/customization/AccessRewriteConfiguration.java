package org.xdi.oxauth.customization;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.ocpsoft.rewrite.annotation.RewriteConfiguration;
import org.ocpsoft.rewrite.config.Configuration;
import org.ocpsoft.rewrite.config.ConfigurationBuilder;
import org.ocpsoft.rewrite.servlet.config.HttpConfigurationProvider;
import org.ocpsoft.rewrite.servlet.config.rule.Join;
import org.xdi.util.StringHelper;

import javax.servlet.ServletContext;
import java.io.File;
import java.util.Collection;

/**
 * Created by eugeniuparvan on 4/27/17.
 */
@RewriteConfiguration
public class AccessRewriteConfiguration extends HttpConfigurationProvider {
    @Override
    public Configuration getConfiguration(final ServletContext context) {
        ConfigurationBuilder builder = ConfigurationBuilder.begin();
        addRulesForAllXHTML(context.getRealPath(""), builder);
        String externalResourceBase = System.getProperty("catalina.base");
        if (StringHelper.isNotEmpty(externalResourceBase)) {
            externalResourceBase += "/custom/pages";
            File folder = new File(externalResourceBase);
            if (folder.exists() && folder.isDirectory()) {
                addRulesForAllXHTML(externalResourceBase, builder);
            }
        }
        return builder;
    }

    private void addRulesForAllXHTML(String path, ConfigurationBuilder builder) {
        Collection<File> xhtmlFiles = FileUtils.listFiles(new File(path), new RegexFileFilter(".*\\.xhtml$"), DirectoryFileFilter.DIRECTORY);

        for (File files : xhtmlFiles) {
            String xhtmlPath = files.getAbsolutePath();
            String xhtmlUri = xhtmlPath.substring(path.length(), xhtmlPath.lastIndexOf(".xhtml"));
            builder.addRule(Join.path(xhtmlUri).to(xhtmlUri + ".htm"));
        }
    }

    @Override
    public int priority() {
        return 10;
    }
}
