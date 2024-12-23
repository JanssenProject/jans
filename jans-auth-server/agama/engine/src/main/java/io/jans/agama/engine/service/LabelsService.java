package io.jans.agama.engine.service;

import freemarker.template.*;

import io.jans.agama.model.EngineConfig;
import io.jans.as.model.util.Pair;
import io.jans.service.cdi.util.CdiUtil;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.*;
import java.nio.file.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

import org.slf4j.Logger;

@ApplicationScoped
public class LabelsService implements TemplateMethodModelEx {
    
    public static final String METHOD_NAME = "labels";
    
    private static final String LABELS_FILE = "labels.txt";
    //regex means: left square bracket optionally followed by horizontal spaces, then some combination of
    //letters, digits, hypens, or sharp signs. Finally right bracket optionally followed by horizontal space
    //A string matching the regex is used mark the beginning of a specific-locale section in a labels file  
    private static final Pattern SECTION_PATT = Pattern.compile("^\\[[ \\t]*([\\w-#]+)[ \\t]*\\][ \\t]*$");

    @Inject
    private Logger logger;
    
    @Inject
    private EngineConfig econf;
    
    private Map<String, Map<String, Properties>> labelsMap;     //Map<path id, Map<locale id, ....
    
    public String get(String key, Object... args) {

        Pair<String, Locale> p = getLocalizedLabel(key, CdiUtil.bean(WebContext.class).getLocale());
        String label = p.getFirst();
        
        if (label != null && args.length > 0) {
            label = formatMessage(label, p.getSecond(), args);
        }        
        return Objects.toString(label);
        
    }

    public Object exec(List args) throws TemplateModelException {
        
        int size = args.size(); 
        if (size == 0)
            throw new TemplateModelException("Call to labels(...) is missing the label key");
            
        Object key = args.get(0);
        String strKey = Objects.toString(key);

        if (!TemplateScalarModel.class.isInstance(key))
            throw new TemplateModelException("Unexpected key passed to labels(...) - value was " + 
                strKey + " but string required");
        
        Pair<String, Locale> p = getLocalizedLabel(strKey, CdiUtil.bean(WebContext.class).getLocale());
        String label = p.getFirst();
        if (label != null && size > 1) {
            
            Object[] subArgs = args.subList(1, size).toArray();
            for (int i = 0; i < subArgs.length; i++) {
                
                Object arg = subArgs[i]; 
                if (arg != null) {
                    //logger.debug("arg {} is a {}", i, arg.getClass().getName());

                    if (arg instanceof TemplateScalarModel) {
                        arg = ((TemplateScalarModel) arg).getAsString();
                    } else if (arg instanceof TemplateNumberModel) {
                        arg = ((TemplateNumberModel) arg).getAsNumber();
                    } else if (arg instanceof TemplateDateModel) {
                        arg = ((TemplateDateModel) arg).getAsDate();
                    } else if (arg instanceof TemplateBooleanModel) {
                        arg = ((TemplateBooleanModel) arg).getAsBoolean();
                    } else{
                        logger.warn("Unable to convert parameter at position {} ({}) to any " +
                                "of string/number/date/boolean", i, arg.toString());
                    }
                    subArgs[i] = arg;
                }               
            }
            label = formatMessage(label, p.getSecond(), subArgs);
        }
        
        return new SimpleScalar(Objects.toString(label));   //return "null" to avoid template render crash
        
    }
    
    public void addLabels(String path) {
        
        Path p = Paths.get(econf.getRootDir(), econf.getTemplatesPath(), path, LABELS_FILE);
        if (!Files.exists(p)) return;
        
        try {
            logger.info("Reading {}", p);
            List<String> lines = Files.readAllLines(p);
            
            Map<String, Properties> localeMap = new HashMap<>();
            List<String> section = new ArrayList<>();
            String locale = null;
            
            for (String line : lines) {
                Matcher m = SECTION_PATT.matcher(line);
                
                if (m.matches()) {
                    localeMap.put(locale == null ? null : locale.toLowerCase(), propertiesFromSection(section));
                    
                    section = new ArrayList<>();
                    locale = m.group(1);
                    //accordig to the regex, locale will be of length 1 at least
                } else {
                    section.add(line);
                }       
            }
            if (!section.isEmpty()) {
                localeMap.put(locale == null ? null : locale.toLowerCase(), propertiesFromSection(section));
            }
            
            logger.info("Setting labels for path {}", path);
            labelsMap.put(path, localeMap);
            //logger.debug(localeMap.toString());            
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        
    }
    
    public void removeLabels(String path) {        
        logger.info("Removing in-memory labels for path {}", path);
        labelsMap.remove(path);        
    }
    
    private Pair<String, Locale> getLocalizedLabel(String key, Locale locale) {
                
        Set<String> paths = labelsMap.keySet();
        String strLocale, label = null;
        Locale locCopy = locale;
        boolean first = true;
        
        //logger.debug("Locale is: {}", locale);        
        do {                
            if (!first) {
                //search in the "parent" locale
                if (locCopy.getVariant().length() > 0) {
                    locCopy = new Locale(locCopy.getLanguage(), locCopy.getCountry());
                } else if (locCopy.getCountry().length() > 0) {
                    locCopy = new Locale(locCopy.getLanguage());
                } else {
                    locCopy = null;     //resort to "default" locale
                }
            }
            
            first = false;
            strLocale = Optional.ofNullable(locCopy).map(l -> l.toString().toLowerCase()).orElse(null);
            Iterator<String> it = paths.iterator();
            
            while (it.hasNext() && label == null) {
                String path = it.next();
                Map<String, Properties> mama = labelsMap.get(path);

                //logger.debug("Path: {}, Locale: {}, key: {}", path, strLocale, key);
                label = Optional.ofNullable(mama.get(strLocale)).map(m -> m.getProperty(key)).orElse(null);
            }
        } while (locCopy != null && label == null);
        
        return new Pair<>(label, locCopy);
        
    }
    
    private String formatMessage(String pattern, Locale locale, Object[] args) {
        
        try {
            MessageFormat mf = new MessageFormat(pattern);
            Optional.ofNullable(locale).ifPresent(mf::setLocale);                    
            return mf.format(args, new StringBuffer(), null).toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return "error!";
        }
        
    }
    
    private Properties propertiesFromSection(List<String> section) {
        
        String cat = section.stream().reduce("", (a, b) -> a + "\n" + b );
        Properties p = new Properties();
        
        try (StringReader sr = new StringReader(cat)) {
            p.load(sr);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return p;
        
    }

    @PostConstruct
    private void init() {
        labelsMap = new HashMap<>();
    }
    
}
