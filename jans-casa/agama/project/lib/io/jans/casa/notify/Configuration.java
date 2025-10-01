package io.jans.casa.notify;

import com.nimbusds.jose.*;
import com.nimbusds.jwt.*;

import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.SimpleCustomProperty;
import io.jans.service.cdi.util.CdiUtil;
import io.jans.service.custom.CustomScriptService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;
import java.text.ParseException;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Configuration {
    
    static final String AS_ENDPOINT_PROP = "AS_ENDPOINT";
    static final String CLIENT_ID_PROP = "AS_CLIENT_ID";
    static final String CLIENT_SECRET_PROP = "AS_CLIENT_SECRET";    
    
    private static Logger logger = LoggerFactory.getLogger(getClass());   
    
    private static JSONObject get() {

        logger.info("Checking super_gluu custom script");
        CustomScript script = CdiUtil.bean(CustomScriptService.class).getScriptByDisplayName("super_gluu");
        List<SimpleCustomProperty> properties = script.getConfigurationProperties();
        
        logger.debug("Looking up service mode");
        String mode = scriptPropValue("notification_service_mode", properties);
        
        if (!mode.equals("jans"))
            throw new IOException("Service mode '" + mode + "' for super gluu notifications is not supported. Use 'jans'");
        
        logger.debug("Looking up credentials file path");
        String fn = scriptPropValue("credentials_file", properties);
        logger.debug("File path is: '{}'", fn);
        
        JSONObject configs = new JSONObject(Files.readString(Paths.get(fn), UTF_8));
        
        //Append scan stuff
        logger.debug("Looking up SSA configs");
        String ass = scriptPropValue("AS_SSA", properties);
        String assid = scriptPropValue(CLIENT_ID_PROP, properties);
        String assecret = scriptPropValue(CLIENT_SECRET_PROP, properties);
        
        if (Stream.of(ass, assid, assecret).anyMatch(s -> s.length() == 0))
            throw new IOException("SSA client credentials missing. SG script not initialized?");
        
        configs.put(CLIENT_ID_PROP, assid);
        configs.put(CLIENT_SECRET_PROP, assecret);

        try {
            String iss = SignedJWT.parse(ass).getJWTClaimsSet().getIssuer();        
            logger.info("SSA issuer is {}", iss);
            configs.put(AS_ENDPOINT_PROP, iss);
        } catch (ParseException e) {
            throw new IOException(e);
        }
        return configs;
        
    }   
    
    private static String scriptPropValue(String name, List<SimpleCustomProperty> properties) {
        return properties.stream().filter(p -> name.equals(p.getValue1())).findFirst()
                    .map(SimpleCustomProperty::getValue2).orElse("");
    }
    
}
