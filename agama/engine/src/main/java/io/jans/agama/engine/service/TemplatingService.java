package io.jans.agama.engine.service;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;

import freemarker.core.OutputFormat;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;

import io.jans.agama.engine.exception.TemplateProcessingException;
import io.jans.agama.model.EngineConfig;
import io.jans.util.Pair;

import org.slf4j.Logger;

import static java.nio.charset.StandardCharsets.UTF_8;

@ApplicationScoped
public class TemplatingService {

    @Inject
    private Logger logger;
    
    @Inject
    private EngineConfig econf;
    
    private Configuration fmConfig;
    
    public Pair<String, String> process(String templatePath, Object dataModel, Writer writer, boolean useClassloader)
            throws TemplateProcessingException {

        try {
            //Get template, inject data, and write output
            Template t = useClassloader ? getTemplateFromClassLoader(templatePath) : getTemplate(templatePath);
            t.process(Optional.ofNullable(dataModel).orElse(Collections.emptyMap()), writer);

            String mime = Optional.ofNullable(t.getOutputFormat()).map(OutputFormat::getMimeType).orElse(null);
            String encoding = Optional.ofNullable(t.getEncoding()).orElse(fmConfig.getDefaultEncoding());
            return new Pair<>(mime, encoding);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new TemplateProcessingException(e.getMessage(), e);
        }

    }
    
    private Template getTemplate(String path) throws IOException {
        return fmConfig.getTemplate(path);
    }
    
    private Template getTemplateFromClassLoader(String path) throws IOException {
        ClassLoader loader = getClass().getClassLoader();
        Reader reader = new InputStreamReader(loader.getResourceAsStream(path), UTF_8);
        return new Template(path, reader, fmConfig);
    }
    
    @PostConstruct
    private void init() {

        fmConfig = new Configuration(Configuration.VERSION_2_3_31);
        fmConfig.setDefaultEncoding(UTF_8.toString());
        fmConfig.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        fmConfig.setLogTemplateExceptions(false);
        fmConfig.setWrapUncheckedExceptions(true);
        fmConfig.setFallbackOnNullLoopVariable(false);

        try {
            fmConfig.setDirectoryForTemplateLoading(Paths.get(econf.getRootDir()).toFile());
        } catch(IOException e) {
            logger.error("Error configuring directory for UI templates: {}", e.getMessage());
            throw new RuntimeException(e);
        }
        
    }
    
}
