package io.jans.agama.engine.misc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.agama.model.EngineConfig;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.mozilla.javascript.Scriptable;
import org.slf4j.Logger;

import static java.nio.charset.StandardCharsets.UTF_8;

@ApplicationScoped
public class FlowUtils {

    private static final Path SALT_PATH = Paths.get("/etc/jans/conf/salt");
    private static final HmacAlgorithms HASH_ALG = HmacAlgorithms.HMAC_SHA_512;
    
    @Inject
    private Logger logger;
    
    @Inject
    private ObjectMapper mapper;
    
    @Inject
    private EngineConfig engineConfig;

    public boolean serviceEnabled() {
        return engineConfig.isEnabled();
    }

    /**
     * It is assumed that values in the map are String arrays with at least one element
     * @param map
     * @return
     * @throws JsonProcessingException 
     */
    public String toJsonString(Map<String, String[]> map) throws JsonProcessingException {
        
        Map<String, Object> result = new HashMap<>();
        if (map != null) {
            
            for(String key : map.keySet()) {
                String[] list = map.get(key);
                result.put(key, list.length == 1 ? list[0] : Arrays.asList(list));
            }
        }
        
        //TODO: implement a smarter serialization? example: 
        // if key starts with prefix i: try to convert to int, b: for boolean, m: map, etc.
        return mapper.writeValueAsString(result);
    }
    
    public void printScopeIds(Scriptable scope) {
        List<String> scopeIds = Stream.of(scope.getIds()).map(Object::toString).collect(Collectors.toList());
        logger.trace("Global scope has {} ids: {}", scopeIds.size(), scopeIds);
    }
    
    public String hash(String message) throws IOException {        
        return new HmacUtils(HASH_ALG, sharedKey()).hmacHex(message);       
    }
    
    public String hash(byte[] message) throws IOException {        
        return new HmacUtils(HASH_ALG, sharedKey()).hmacHex(message);       
    }
    
    private String sharedKey() throws IOException {
        
        //I preferred not to have file contents in memory (static var)
        Properties p = new Properties();
        p.load(new StringReader(Files.readString(SALT_PATH, UTF_8)));
        return p.getProperty("encodeSalt");
        
    }
    
}
