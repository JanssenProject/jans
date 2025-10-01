package io.jans.agama.test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FlowUtil {
    
    private static final String EXT = "txt";
    
    private static Logger LOG = LogManager.getLogger(FlowUtil.class);
    
    private FlowUtil() {}
    
    public static Map<String, String> sourcesOfFolder(String parent) {
        
        try {
            Path path = Paths.get(parent);
            LOG.debug("Reading files under {}", path.toAbsolutePath());

            return Files.walk(path).filter(p -> p.toString().endsWith("." + EXT))
                .map(p -> {

                        String code = null;
                        try {
                            code = Files.readString(p);
                        } catch (IOException e) {
                            LOG.error("Unable to read contents of {}: {}", p, e.getMessage());
                        }
                        return new SimpleEntry(p.toString(), code);
                        
                    }).collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));

        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
        
    }
    
}